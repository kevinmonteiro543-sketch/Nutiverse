package com.monteirogames.nutriverse.data.repository

import com.google.android.gms.tasks.Task
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.SetOptions
import com.monteirogames.nutriverse.data.local.dao.SyncTombstoneDao
import com.monteirogames.nutriverse.data.local.entity.SyncTombstoneEntity
import com.monteirogames.nutriverse.domain.model.DietPlan
import com.monteirogames.nutriverse.domain.model.FirestoreCollections
import com.monteirogames.nutriverse.domain.model.FirestoreDietPlanDocument
import com.monteirogames.nutriverse.domain.model.SyncState
import com.monteirogames.nutriverse.domain.model.SyncStatus
import com.monteirogames.nutriverse.domain.model.toFirestoreDocument
import com.monteirogames.nutriverse.domain.repository.AuthRepository
import com.monteirogames.nutriverse.domain.repository.DietPlanSyncRepository
import com.monteirogames.nutriverse.domain.repository.DietPlannerRepository
import com.monteirogames.nutriverse.domain.sync.SyncResolution
import com.monteirogames.nutriverse.domain.sync.TimestampConflictResolver
import com.monteirogames.nutriverse.domain.sync.TimestampConflictResolverImpl
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class DietPlanSyncRepositoryImpl(
    private val authRepository: AuthRepository,
    private val dietPlannerRepository: DietPlannerRepository,
    private val syncTombstoneDao: SyncTombstoneDao,
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val conflictResolver: TimestampConflictResolver = TimestampConflictResolverImpl()
) : DietPlanSyncRepository {

    private val _syncState = MutableStateFlow(SyncState(status = SyncStatus.IDLE))
    override val syncState: StateFlow<SyncState> = _syncState.asStateFlow()

    override fun getSyncState(): SyncState = _syncState.value

    override suspend fun syncDietPlans(): Result<Int> {
        val currentUid = authRepository.getCurrentUserId()
        if (currentUid.isNullOrBlank()) {
            _syncState.value = SyncState(
                status = SyncStatus.ERROR,
                errorMessage = "Cannot sync diet plans: User is not authenticated."
            )
            return Result.failure(IllegalStateException("Cannot sync diet plans: User is not authenticated."))
        }

        _syncState.value = SyncState(status = SyncStatus.SYNCING)

        return try {
            // 1. Gather local data and tombstones
            val localPlans = dietPlannerRepository.getAllDietPlansList(currentUid)
            val localMap = localPlans.associateBy { it.id }
            val tombstones = syncTombstoneDao.getTombstonesForUser(currentUid, "dietPlan").associateBy { it.id }

            // 2. Query Firestore users/{uid}/dietPlans
            val snapshot = firestore.collection(FirestoreCollections.userDietPlansPath(currentUid))
                .get()
                .awaitResult()

            val cloudMap = snapshot.documents.mapNotNull { doc ->
                val data = doc.data ?: return@mapNotNull null
                FirestoreDietPlanDocument.fromMap(data, fallbackId = doc.id, fallbackUserId = currentUid)
            }.associateBy { it.id }

            // 3. Process all distinct diet plan IDs
            val allIds = (localMap.keys + cloudMap.keys + tombstones.keys).toSet()
            var syncedCount = 0

            for (id in allIds) {
                val localItem = localMap[id]
                val cloudDoc = cloudMap[id]
                val tombstone = tombstones[id]

                when {
                    // Scenario 1: Local record was deleted (tombstone exists)
                    tombstone != null -> {
                        if (cloudDoc == null) {
                            firestore.document(FirestoreCollections.userDietPlanDocPath(currentUid, id))
                                .set(
                                    mapOf(
                                        "id" to id,
                                        "userId" to currentUid,
                                        "isDeleted" to true,
                                        "updatedAt" to tombstone.deletedAt,
                                        "schemaVersion" to FirestoreDietPlanDocument.CURRENT_SCHEMA_VERSION
                                    ),
                                    SetOptions.merge()
                                )
                                .awaitResult()
                            syncedCount++
                        } else if (cloudDoc.isDeleted) {
                            if (cloudDoc.updatedAt >= tombstone.deletedAt) {
                                syncTombstoneDao.deleteTombstone("dietPlan", id)
                            }
                        } else {
                            if (tombstone.deletedAt >= cloudDoc.updatedAt) {
                                firestore.document(FirestoreCollections.userDietPlanDocPath(currentUid, id))
                                    .set(
                                        mapOf(
                                            "id" to id,
                                            "userId" to currentUid,
                                            "isDeleted" to true,
                                            "updatedAt" to tombstone.deletedAt,
                                            "schemaVersion" to FirestoreDietPlanDocument.CURRENT_SCHEMA_VERSION
                                        ),
                                        SetOptions.merge()
                                    )
                                    .awaitResult()
                                syncedCount++
                            } else {
                                // Cloud updated after local deletion -> resurrect locally
                                dietPlannerRepository.saveDietPlan(cloudDoc.toDietPlan())
                                syncTombstoneDao.deleteTombstone("dietPlan", id)
                                syncedCount++
                            }
                        }
                    }

                    // Scenario 2: Cloud doc exists and is marked as deleted
                    cloudDoc != null && cloudDoc.isDeleted -> {
                        if (localItem != null) {
                            if (cloudDoc.updatedAt >= localItem.updatedAt) {
                                dietPlannerRepository.deleteDietPlan(localItem.id, currentUid)
                                syncedCount++
                            } else {
                                firestore.document(FirestoreCollections.userDietPlanDocPath(currentUid, id))
                                    .set(localItem.toFirestoreDocument(userId = currentUid, isDeleted = false).toMap())
                                    .awaitResult()
                                syncedCount++
                            }
                        }
                    }

                    // Scenario 3: Standard sync resolution for active items
                    else -> {
                        val cloudItem = cloudDoc?.toDietPlan()
                        val resolution = conflictResolver.resolve(
                            localItem = localItem,
                            cloudItem = cloudItem,
                            getLocalTimestamp = { it.updatedAt },
                            getCloudTimestamp = { it.updatedAt }
                        )

                        when (resolution) {
                            is SyncResolution.UploadToCloud -> {
                                val toUpload = if (resolution.localItem.updatedAt == 0L) {
                                    val updated = resolution.localItem.copy(updatedAt = System.currentTimeMillis())
                                    dietPlannerRepository.saveDietPlan(updated)
                                    updated
                                } else {
                                    resolution.localItem
                                }

                                firestore.document(FirestoreCollections.userDietPlanDocPath(currentUid, id))
                                    .set(toUpload.toFirestoreDocument(userId = currentUid, isDeleted = false).toMap())
                                    .awaitResult()
                                syncedCount++
                            }

                            is SyncResolution.DownloadToLocal -> {
                                dietPlannerRepository.saveDietPlan(resolution.cloudItem)
                                syncedCount++
                            }

                            is SyncResolution.UpToDate -> {
                                // Already synchronized
                            }
                        }
                    }
                }
            }

            _syncState.value = SyncState(
                status = SyncStatus.SUCCESS,
                lastSyncedAt = System.currentTimeMillis()
            )
            Result.success(syncedCount)
        } catch (e: Exception) {
            val isOffline = e is FirebaseNetworkException ||
                    (e is FirebaseFirestoreException && e.code == FirebaseFirestoreException.Code.UNAVAILABLE)

            val errorMsg = if (isOffline) {
                "Offline: Diet plan sync deferred."
            } else {
                "Diet plan sync failed: ${e.message ?: "Unknown error"}"
            }

            _syncState.value = SyncState(
                status = SyncStatus.ERROR,
                errorMessage = errorMsg
            )
            Result.failure(e)
        }
    }

    override suspend fun uploadDietPlan(plan: DietPlan): Result<Unit> {
        val currentUid = authRepository.getCurrentUserId()
        if (currentUid.isNullOrBlank()) {
            return Result.failure(IllegalStateException("Cannot upload: User is not authenticated."))
        }

        return try {
            val doc = plan.toFirestoreDocument(userId = currentUid, isDeleted = false)
            firestore.document(FirestoreCollections.userDietPlanDocPath(currentUid, plan.id))
                .set(doc.toMap())
                .awaitResult()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteDietPlanInCloud(planId: String): Result<Unit> {
        val currentUid = authRepository.getCurrentUserId()
        if (currentUid.isNullOrBlank()) {
            return Result.failure(IllegalStateException("Cannot delete: User is not authenticated."))
        }

        return try {
            firestore.document(FirestoreCollections.userDietPlanDocPath(currentUid, planId))
                .set(
                    mapOf(
                        "id" to planId,
                        "userId" to currentUid,
                        "isDeleted" to true,
                        "updatedAt" to System.currentTimeMillis(),
                        "schemaVersion" to FirestoreDietPlanDocument.CURRENT_SCHEMA_VERSION
                    ),
                    SetOptions.merge()
                )
                .awaitResult()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun <T> Task<T>.awaitResult(): T = suspendCancellableCoroutine { continuation ->
        addOnSuccessListener { result ->
            continuation.resume(result)
        }
        addOnFailureListener { exception ->
            continuation.resumeWithException(exception)
        }
    }
}
