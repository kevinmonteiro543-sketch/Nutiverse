package com.monteirogames.nutriverse.data.repository

import com.google.android.gms.tasks.Task
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.SetOptions
import com.monteirogames.nutriverse.data.local.dao.SyncTombstoneDao
import com.monteirogames.nutriverse.data.local.entity.SyncTombstoneEntity
import com.monteirogames.nutriverse.domain.model.Budget
import com.monteirogames.nutriverse.domain.model.FirestoreBudgetDocument
import com.monteirogames.nutriverse.domain.model.FirestoreCollections
import com.monteirogames.nutriverse.domain.model.SyncState
import com.monteirogames.nutriverse.domain.model.SyncStatus
import com.monteirogames.nutriverse.domain.model.toBudget
import com.monteirogames.nutriverse.domain.model.toFirestoreDocument
import com.monteirogames.nutriverse.domain.repository.AuthRepository
import com.monteirogames.nutriverse.domain.repository.BudgetRepository
import com.monteirogames.nutriverse.domain.repository.BudgetSyncRepository
import com.monteirogames.nutriverse.domain.sync.SyncResolution
import com.monteirogames.nutriverse.domain.sync.TimestampConflictResolver
import com.monteirogames.nutriverse.domain.sync.TimestampConflictResolverImpl
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class BudgetSyncRepositoryImpl(
    private val authRepository: AuthRepository,
    private val budgetRepository: BudgetRepository,
    private val syncTombstoneDao: SyncTombstoneDao,
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val conflictResolver: TimestampConflictResolver = TimestampConflictResolverImpl()
) : BudgetSyncRepository {

    private val _syncState = MutableStateFlow(SyncState(status = SyncStatus.IDLE))
    override val syncState: StateFlow<SyncState> = _syncState.asStateFlow()

    override fun getSyncState(): SyncState = _syncState.value

    override suspend fun syncBudget(): Result<Int> {
        val currentUid = authRepository.getCurrentUserId()
        if (currentUid.isNullOrBlank()) {
            _syncState.value = SyncState(
                status = SyncStatus.ERROR,
                errorMessage = "Cannot sync: User is not authenticated."
            )
            return Result.failure(IllegalStateException("Cannot sync: User is not authenticated."))
        }

        _syncState.value = SyncState(status = SyncStatus.SYNCING)

        return try {
            val localBudgets = budgetRepository.getAllBudgetsList(currentUid)
            val localMap = localBudgets.associateBy { it.id }
            val tombstones = syncTombstoneDao.getTombstonesForUser(currentUid, "budget").associateBy { it.id }

            val snapshot = firestore.collection(FirestoreCollections.userBudgetPath(currentUid))
                .get()
                .awaitResult()

            val cloudMap = snapshot.documents.mapNotNull { doc ->
                val data = doc.data ?: return@mapNotNull null
                FirestoreBudgetDocument.fromMap(data, fallbackId = doc.id, fallbackUserId = currentUid)
            }.associateBy { it.id }

            val allIds = (localMap.keys + cloudMap.keys + tombstones.keys).toSet()
            var syncedCount = 0

            for (id in allIds) {
                val localItem = localMap[id]
                val cloudDoc = cloudMap[id]
                val tombstone = tombstones[id]

                when {
                    tombstone != null -> {
                        if (cloudDoc == null) {
                            firestore.document(FirestoreCollections.userBudgetDocPath(currentUid, id))
                                .set(
                                    mapOf(
                                        "id" to id,
                                        "userId" to currentUid,
                                        "isDeleted" to true,
                                        "updatedAt" to tombstone.deletedAt,
                                        "schemaVersion" to FirestoreBudgetDocument.CURRENT_SCHEMA_VERSION
                                    ),
                                    SetOptions.merge()
                                )
                                .awaitResult()
                            syncedCount++
                        } else if (cloudDoc.isDeleted) {
                            if (cloudDoc.updatedAt >= tombstone.deletedAt) {
                                syncTombstoneDao.deleteTombstone("budget", id)
                            }
                        } else {
                            if (tombstone.deletedAt >= cloudDoc.updatedAt) {
                                firestore.document(FirestoreCollections.userBudgetDocPath(currentUid, id))
                                    .set(
                                        mapOf(
                                            "id" to id,
                                            "userId" to currentUid,
                                            "isDeleted" to true,
                                            "updatedAt" to tombstone.deletedAt,
                                            "schemaVersion" to FirestoreBudgetDocument.CURRENT_SCHEMA_VERSION
                                        ),
                                        SetOptions.merge()
                                    )
                                    .awaitResult()
                                syncedCount++
                            } else {
                                budgetRepository.saveBudget(cloudDoc.toBudget())
                                syncTombstoneDao.deleteTombstone("budget", id)
                                syncedCount++
                            }
                        }
                    }

                    cloudDoc != null && cloudDoc.isDeleted -> {
                        if (localItem != null) {
                            if (cloudDoc.updatedAt >= localItem.updatedAt) {
                                budgetRepository.deleteBudget(localItem.id, currentUid)
                                syncedCount++
                            } else {
                                firestore.document(FirestoreCollections.userBudgetDocPath(currentUid, id))
                                    .set(localItem.toFirestoreDocument(isDeleted = false).toMap())
                                    .awaitResult()
                                syncedCount++
                            }
                        }
                    }

                    else -> {
                        val cloudItem = cloudDoc?.toBudget()
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
                                    budgetRepository.saveBudget(updated)
                                    updated
                                } else {
                                    resolution.localItem
                                }

                                firestore.document(FirestoreCollections.userBudgetDocPath(currentUid, id))
                                    .set(toUpload.toFirestoreDocument(isDeleted = false).toMap())
                                    .awaitResult()
                                syncedCount++
                            }

                            is SyncResolution.DownloadToLocal -> {
                                budgetRepository.saveBudget(resolution.cloudItem)
                                syncTombstoneDao.deleteTombstone("budget", id)
                                syncedCount++
                            }

                            is SyncResolution.UpToDate -> {
                                // Up to date
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
            val friendly = mapException(e)
            _syncState.value = SyncState(status = SyncStatus.ERROR, errorMessage = friendly)
            Result.failure(Exception(friendly, e))
        }
    }

    override suspend fun uploadBudget(budget: Budget): Result<Unit> {
        val currentUid = authRepository.getCurrentUserId()
        if (currentUid.isNullOrBlank()) {
            return Result.failure(IllegalStateException("User is not authenticated"))
        }

        if (budget.userId != currentUid) {
            return Result.failure(SecurityException("Cannot upload budget for a different user"))
        }

        return try {
            val valid = if (budget.updatedAt == 0L) {
                budget.copy(updatedAt = System.currentTimeMillis())
            } else {
                budget
            }

            firestore.document(FirestoreCollections.userBudgetDocPath(currentUid, valid.id))
                .set(valid.toFirestoreDocument(isDeleted = false).toMap())
                .awaitResult()

            _syncState.value = SyncState(status = SyncStatus.SUCCESS, lastSyncedAt = System.currentTimeMillis())
            Result.success(Unit)
        } catch (e: Exception) {
            val friendly = mapException(e)
            _syncState.value = SyncState(status = SyncStatus.ERROR, errorMessage = friendly)
            Result.failure(Exception(friendly, e))
        }
    }

    override suspend fun deleteBudgetInCloud(budgetId: String): Result<Unit> {
        val currentUid = authRepository.getCurrentUserId()
        if (currentUid.isNullOrBlank()) {
            return Result.failure(IllegalStateException("User is not authenticated"))
        }

        return try {
            val now = System.currentTimeMillis()
            firestore.document(FirestoreCollections.userBudgetDocPath(currentUid, budgetId))
                .set(
                    mapOf(
                        "id" to budgetId,
                        "userId" to currentUid,
                        "isDeleted" to true,
                        "updatedAt" to now,
                        "schemaVersion" to FirestoreBudgetDocument.CURRENT_SCHEMA_VERSION
                    ),
                    SetOptions.merge()
                )
                .awaitResult()

            Result.success(Unit)
        } catch (e: Exception) {
            val friendly = mapException(e)
            Result.failure(Exception(friendly, e))
        }
    }

    private fun mapException(e: Exception): String {
        return when (e) {
            is FirebaseFirestoreException -> {
                when (e.code) {
                    FirebaseFirestoreException.Code.PERMISSION_DENIED ->
                        "Access denied. You do not have permission to sync budgets."
                    FirebaseFirestoreException.Code.UNAVAILABLE ->
                        "Cloud service is temporarily unavailable. Budget remains saved locally."
                    FirebaseFirestoreException.Code.UNAUTHENTICATED ->
                        "User is not authenticated. Please sign in to sync."
                    FirebaseFirestoreException.Code.DEADLINE_EXCEEDED ->
                        "Sync operation timed out. Will retry when connection improves."
                    else -> "Cloud budget synchronization failed: ${e.code.name}"
                }
            }
            is FirebaseNetworkException ->
                "Network unavailable. Budget changes remain saved locally."
            else -> {
                val msg = e.localizedMessage ?: e.message ?: "Cloud synchronization error"
                if (msg.contains("network", ignoreCase = true) || msg.contains("unable to resolve", ignoreCase = true)) {
                    "Network unavailable. Budget changes remain saved locally."
                } else {
                    msg
                }
            }
        }
    }

    companion object {
        suspend fun <T> Task<T>.awaitResult(): T = suspendCancellableCoroutine { continuation ->
            addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    continuation.resume(task.result)
                } else {
                    continuation.resumeWithException(
                        task.exception ?: RuntimeException("Firestore task failed without exception")
                    )
                }
            }
        }
    }
}
