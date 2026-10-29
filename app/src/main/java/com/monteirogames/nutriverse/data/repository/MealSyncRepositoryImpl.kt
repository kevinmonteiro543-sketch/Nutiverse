package com.monteirogames.nutriverse.data.repository

import com.google.android.gms.tasks.Task
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.SetOptions
import com.monteirogames.nutriverse.data.local.dao.FoodItemDao
import com.monteirogames.nutriverse.data.local.dao.MealEntryDao
import com.monteirogames.nutriverse.data.local.dao.SyncTombstoneDao
import com.monteirogames.nutriverse.data.local.entity.SyncTombstoneEntity
import com.monteirogames.nutriverse.domain.model.FirestoreCollections
import com.monteirogames.nutriverse.domain.model.FirestoreMealDocument
import com.monteirogames.nutriverse.domain.model.MealEntry
import com.monteirogames.nutriverse.domain.model.SyncState
import com.monteirogames.nutriverse.domain.model.SyncStatus
import com.monteirogames.nutriverse.domain.model.toFirestoreDocument
import com.monteirogames.nutriverse.domain.model.toMealEntry
import com.monteirogames.nutriverse.domain.repository.AuthRepository
import com.monteirogames.nutriverse.domain.repository.MealRepository
import com.monteirogames.nutriverse.domain.repository.MealSyncRepository
import com.monteirogames.nutriverse.domain.sync.SyncResolution
import com.monteirogames.nutriverse.domain.sync.TimestampConflictResolver
import com.monteirogames.nutriverse.domain.sync.TimestampConflictResolverImpl
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class MealSyncRepositoryImpl(
    private val authRepository: AuthRepository,
    private val mealRepository: MealRepository,
    private val foodItemDao: FoodItemDao,
    private val mealEntryDao: MealEntryDao,
    private val syncTombstoneDao: SyncTombstoneDao,
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val conflictResolver: TimestampConflictResolver = TimestampConflictResolverImpl()
) : MealSyncRepository {

    private val _syncState = MutableStateFlow(SyncState(status = SyncStatus.IDLE))
    override val syncState: StateFlow<SyncState> = _syncState.asStateFlow()

    override fun getSyncState(): SyncState = _syncState.value

    override suspend fun syncMeals(): Result<Int> {
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
            // 1. Gather local data
            val localMeals = mealRepository.getAllMeals(currentUid)
            val localMap = localMeals.associateBy { it.id }
            val tombstones = syncTombstoneDao.getTombstonesForUser(currentUid, "meal").associateBy { it.id }

            // 2. Query Firestore users/{uid}/meals
            val snapshot = firestore.collection(FirestoreCollections.userMealsPath(currentUid))
                .get()
                .awaitResult()

            val cloudMap = snapshot.documents.mapNotNull { doc ->
                val data = doc.data ?: return@mapNotNull null
                FirestoreMealDocument.fromMap(data, fallbackId = doc.id, fallbackUserId = currentUid)
            }.associateBy { it.id }

            // 3. Process all distinct IDs
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
                            // Upload deletion tombstone to cloud so future devices know it was deleted
                            firestore.document(FirestoreCollections.userMealDocPath(currentUid, id))
                                .set(
                                    mapOf(
                                        "id" to id,
                                        "userId" to currentUid,
                                        "isDeleted" to true,
                                        "updatedAt" to tombstone.deletedAt,
                                        "schemaVersion" to FirestoreMealDocument.CURRENT_SCHEMA_VERSION
                                    ),
                                    SetOptions.merge()
                                )
                                .awaitResult()
                            syncedCount++
                        } else if (cloudDoc.isDeleted) {
                            // Cloud already marked deleted
                            if (cloudDoc.updatedAt >= tombstone.deletedAt) {
                                syncTombstoneDao.deleteTombstone("meal", id)
                            }
                        } else {
                            // Cloud document exists and is active
                            if (tombstone.deletedAt >= cloudDoc.updatedAt) {
                                // Local deletion happened after/at cloud update -> propagate deletion to cloud
                                firestore.document(FirestoreCollections.userMealDocPath(currentUid, id))
                                    .set(
                                        mapOf(
                                            "id" to id,
                                            "userId" to currentUid,
                                            "isDeleted" to true,
                                            "updatedAt" to tombstone.deletedAt,
                                            "schemaVersion" to FirestoreMealDocument.CURRENT_SCHEMA_VERSION
                                        ),
                                        SetOptions.merge()
                                    )
                                    .awaitResult()
                                syncedCount++
                            } else {
                                // Cloud was updated AFTER local deletion -> resurrect locally
                                val restoredMeal = cloudDoc.toMealEntry()
                                foodItemDao.insertFoodItem(restoredMeal.foodItem.toEntity())
                                mealEntryDao.insertMealEntry(restoredMeal.toEntity())
                                syncTombstoneDao.deleteTombstone("meal", id)
                                syncedCount++
                            }
                        }
                    }

                    // Scenario 2: Cloud doc exists and is marked as deleted (no local tombstone)
                    cloudDoc != null && cloudDoc.isDeleted -> {
                        if (localItem != null) {
                            if (cloudDoc.updatedAt >= localItem.updatedAt) {
                                // Cloud deletion is newer -> delete local item and add local tombstone
                                mealEntryDao.deleteMealEntry(localItem.toEntity())
                                syncTombstoneDao.insertTombstone(
                                    SyncTombstoneEntity(
                                        id = id,
                                        entityType = "meal",
                                        userId = currentUid,
                                        deletedAt = cloudDoc.updatedAt
                                    )
                                )
                                syncedCount++
                            } else {
                                // Local item was created/updated after cloud deletion -> upload active item
                                firestore.document(FirestoreCollections.userMealDocPath(currentUid, id))
                                    .set(localItem.toFirestoreDocument(isDeleted = false).toMap())
                                    .awaitResult()
                                syncedCount++
                            }
                        }
                    }

                    // Scenario 3: Standard sync resolution for active items
                    else -> {
                        val cloudItem = cloudDoc?.toMealEntry()
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
                                    mealRepository.saveMealEntry(updated)
                                    updated
                                } else {
                                    resolution.localItem
                                }

                                firestore.document(FirestoreCollections.userMealDocPath(currentUid, id))
                                    .set(toUpload.toFirestoreDocument(isDeleted = false).toMap())
                                    .awaitResult()
                                syncedCount++
                            }

                            is SyncResolution.DownloadToLocal -> {
                                val itemToSave = resolution.cloudItem
                                foodItemDao.insertFoodItem(itemToSave.foodItem.toEntity())
                                mealEntryDao.insertMealEntry(itemToSave.toEntity())
                                syncTombstoneDao.deleteTombstone("meal", id)
                                syncedCount++
                            }

                            is SyncResolution.UpToDate -> {
                                // Timestamps match -> no network or db mutation needed
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
            _syncState.value = SyncState(
                status = SyncStatus.ERROR,
                errorMessage = friendly
            )
            Result.failure(Exception(friendly, e))
        }
    }

    override suspend fun uploadMeal(meal: MealEntry): Result<Unit> {
        val currentUid = authRepository.getCurrentUserId()
        if (currentUid.isNullOrBlank()) {
            return Result.failure(IllegalStateException("User is not authenticated"))
        }

        if (meal.userId != currentUid) {
            return Result.failure(SecurityException("Cannot upload meal for a different user identity"))
        }

        return try {
            val validMeal = if (meal.updatedAt == 0L) {
                meal.copy(updatedAt = System.currentTimeMillis())
            } else {
                meal
            }

            firestore.document(FirestoreCollections.userMealDocPath(currentUid, validMeal.id))
                .set(validMeal.toFirestoreDocument(isDeleted = false).toMap())
                .awaitResult()

            _syncState.value = SyncState(
                status = SyncStatus.SUCCESS,
                lastSyncedAt = System.currentTimeMillis()
            )
            Result.success(Unit)
        } catch (e: Exception) {
            val friendly = mapException(e)
            _syncState.value = SyncState(status = SyncStatus.ERROR, errorMessage = friendly)
            Result.failure(Exception(friendly, e))
        }
    }

    override suspend fun deleteMealInCloud(mealId: String): Result<Unit> {
        val currentUid = authRepository.getCurrentUserId()
        if (currentUid.isNullOrBlank()) {
            return Result.failure(IllegalStateException("User is not authenticated"))
        }

        return try {
            val now = System.currentTimeMillis()
            firestore.document(FirestoreCollections.userMealDocPath(currentUid, mealId))
                .set(
                    mapOf(
                        "id" to mealId,
                        "userId" to currentUid,
                        "isDeleted" to true,
                        "updatedAt" to now,
                        "schemaVersion" to FirestoreMealDocument.CURRENT_SCHEMA_VERSION
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
                        "Access denied. You do not have permission to sync meals."
                    FirebaseFirestoreException.Code.UNAVAILABLE ->
                        "Cloud service is temporarily unavailable. Meals remain saved locally."
                    FirebaseFirestoreException.Code.UNAUTHENTICATED ->
                        "User is not authenticated. Please sign in to sync."
                    FirebaseFirestoreException.Code.DEADLINE_EXCEEDED ->
                        "Sync operation timed out. Will retry when connection improves."
                    else -> "Cloud meals synchronization failed: ${e.code.name}"
                }
            }
            is FirebaseNetworkException ->
                "Network unavailable. Meal changes remain saved locally."
            else -> {
                val msg = e.localizedMessage ?: e.message ?: "Cloud synchronization error"
                if (msg.contains("network", ignoreCase = true) || msg.contains("unable to resolve", ignoreCase = true)) {
                    "Network unavailable. Meal changes remain saved locally."
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
