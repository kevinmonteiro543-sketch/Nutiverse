package com.monteirogames.nutriverse.data.repository

import com.google.android.gms.tasks.Task
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.SetOptions
import com.monteirogames.nutriverse.data.local.dao.GroceryDao
import com.monteirogames.nutriverse.data.local.dao.SyncTombstoneDao
import com.monteirogames.nutriverse.data.local.entity.SyncTombstoneEntity
import com.monteirogames.nutriverse.domain.model.FirestoreCollections
import com.monteirogames.nutriverse.domain.model.FirestoreGroceryDocument
import com.monteirogames.nutriverse.domain.model.GroceryItem
import com.monteirogames.nutriverse.domain.model.SyncState
import com.monteirogames.nutriverse.domain.model.SyncStatus
import com.monteirogames.nutriverse.domain.model.toFirestoreDocument
import com.monteirogames.nutriverse.domain.model.toGroceryItem
import com.monteirogames.nutriverse.domain.repository.AuthRepository
import com.monteirogames.nutriverse.domain.repository.GroceryRepository
import com.monteirogames.nutriverse.domain.repository.GrocerySyncRepository
import com.monteirogames.nutriverse.domain.sync.SyncResolution
import com.monteirogames.nutriverse.domain.sync.TimestampConflictResolver
import com.monteirogames.nutriverse.domain.sync.TimestampConflictResolverImpl
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class GrocerySyncRepositoryImpl(
    private val authRepository: AuthRepository,
    private val groceryRepository: GroceryRepository,
    private val groceryDao: GroceryDao,
    private val syncTombstoneDao: SyncTombstoneDao,
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val conflictResolver: TimestampConflictResolver = TimestampConflictResolverImpl()
) : GrocerySyncRepository {

    private val _syncState = MutableStateFlow(SyncState(status = SyncStatus.IDLE))
    override val syncState: StateFlow<SyncState> = _syncState.asStateFlow()

    override fun getSyncState(): SyncState = _syncState.value

    override suspend fun syncGrocery(): Result<Int> {
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
            val localItems = groceryRepository.getAllGroceryItems(currentUid)
            val localMap = localItems.associateBy { it.id }
            val tombstones = syncTombstoneDao.getTombstonesForUser(currentUid, "grocery").associateBy { it.id }

            val snapshot = firestore.collection(FirestoreCollections.userGroceryPath(currentUid))
                .get()
                .awaitResult()

            val cloudMap = snapshot.documents.mapNotNull { doc ->
                val data = doc.data ?: return@mapNotNull null
                FirestoreGroceryDocument.fromMap(data, fallbackId = doc.id, fallbackUserId = currentUid)
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
                            firestore.document(FirestoreCollections.userGroceryDocPath(currentUid, id))
                                .set(
                                    mapOf(
                                        "id" to id,
                                        "userId" to currentUid,
                                        "isDeleted" to true,
                                        "updatedAt" to tombstone.deletedAt,
                                        "schemaVersion" to FirestoreGroceryDocument.CURRENT_SCHEMA_VERSION
                                    ),
                                    SetOptions.merge()
                                )
                                .awaitResult()
                            syncedCount++
                        } else if (cloudDoc.isDeleted) {
                            if (cloudDoc.updatedAt >= tombstone.deletedAt) {
                                syncTombstoneDao.deleteTombstone("grocery", id)
                            }
                        } else {
                            if (tombstone.deletedAt >= cloudDoc.updatedAt) {
                                firestore.document(FirestoreCollections.userGroceryDocPath(currentUid, id))
                                    .set(
                                        mapOf(
                                            "id" to id,
                                            "userId" to currentUid,
                                            "isDeleted" to true,
                                            "updatedAt" to tombstone.deletedAt,
                                            "schemaVersion" to FirestoreGroceryDocument.CURRENT_SCHEMA_VERSION
                                        ),
                                        SetOptions.merge()
                                    )
                                    .awaitResult()
                                syncedCount++
                            } else {
                                val restored = cloudDoc.toGroceryItem()
                                groceryDao.insertGroceryItem(restored.toEntity())
                                syncTombstoneDao.deleteTombstone("grocery", id)
                                syncedCount++
                            }
                        }
                    }

                    cloudDoc != null && cloudDoc.isDeleted -> {
                        if (localItem != null) {
                            if (cloudDoc.updatedAt >= localItem.updatedAt) {
                                groceryDao.deleteGroceryItem(localItem.toEntity())
                                syncTombstoneDao.insertTombstone(
                                    SyncTombstoneEntity(
                                        id = id,
                                        entityType = "grocery",
                                        userId = currentUid,
                                        deletedAt = cloudDoc.updatedAt
                                    )
                                )
                                syncedCount++
                            } else {
                                firestore.document(FirestoreCollections.userGroceryDocPath(currentUid, id))
                                    .set(localItem.toFirestoreDocument(isDeleted = false).toMap())
                                    .awaitResult()
                                syncedCount++
                            }
                        }
                    }

                    else -> {
                        val cloudItem = cloudDoc?.toGroceryItem()
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
                                    groceryRepository.updateGroceryItem(updated)
                                    updated
                                } else {
                                    resolution.localItem
                                }

                                firestore.document(FirestoreCollections.userGroceryDocPath(currentUid, id))
                                    .set(toUpload.toFirestoreDocument(isDeleted = false).toMap())
                                    .awaitResult()
                                syncedCount++
                            }

                            is SyncResolution.DownloadToLocal -> {
                                groceryDao.insertGroceryItem(resolution.cloudItem.toEntity())
                                syncTombstoneDao.deleteTombstone("grocery", id)
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

    override suspend fun uploadGroceryItem(item: GroceryItem): Result<Unit> {
        val currentUid = authRepository.getCurrentUserId()
        if (currentUid.isNullOrBlank()) {
            return Result.failure(IllegalStateException("User is not authenticated"))
        }

        if (item.userId != currentUid) {
            return Result.failure(SecurityException("Cannot upload grocery item for a different user"))
        }

        return try {
            val valid = if (item.updatedAt == 0L) {
                item.copy(updatedAt = System.currentTimeMillis())
            } else {
                item
            }

            firestore.document(FirestoreCollections.userGroceryDocPath(currentUid, valid.id))
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

    override suspend fun deleteGroceryItemInCloud(itemId: String): Result<Unit> {
        val currentUid = authRepository.getCurrentUserId()
        if (currentUid.isNullOrBlank()) {
            return Result.failure(IllegalStateException("User is not authenticated"))
        }

        return try {
            val now = System.currentTimeMillis()
            firestore.document(FirestoreCollections.userGroceryDocPath(currentUid, itemId))
                .set(
                    mapOf(
                        "id" to itemId,
                        "userId" to currentUid,
                        "isDeleted" to true,
                        "updatedAt" to now,
                        "schemaVersion" to FirestoreGroceryDocument.CURRENT_SCHEMA_VERSION
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

    private fun GroceryItem.toEntity() = com.monteirogames.nutriverse.data.local.entity.GroceryItemEntity(
        id = id,
        userId = userId,
        name = name,
        quantity = quantity,
        unit = unit,
        category = category,
        isPurchased = isPurchased,
        source = source,
        sourceRecipeId = sourceRecipeId,
        createdAt = createdAt,
        purchasedAt = purchasedAt,
        updatedAt = updatedAt
    )

    private fun mapException(e: Exception): String {
        return when (e) {
            is FirebaseFirestoreException -> {
                when (e.code) {
                    FirebaseFirestoreException.Code.PERMISSION_DENIED ->
                        "Access denied. You do not have permission to sync grocery items."
                    FirebaseFirestoreException.Code.UNAVAILABLE ->
                        "Cloud service is temporarily unavailable. Grocery items remain saved locally."
                    FirebaseFirestoreException.Code.UNAUTHENTICATED ->
                        "User is not authenticated. Please sign in to sync."
                    FirebaseFirestoreException.Code.DEADLINE_EXCEEDED ->
                        "Sync operation timed out. Will retry when connection improves."
                    else -> "Cloud grocery synchronization failed: ${e.code.name}"
                }
            }
            is FirebaseNetworkException ->
                "Network unavailable. Grocery changes remain saved locally."
            else -> {
                val msg = e.localizedMessage ?: e.message ?: "Cloud synchronization error"
                if (msg.contains("network", ignoreCase = true) || msg.contains("unable to resolve", ignoreCase = true)) {
                    "Network unavailable. Grocery changes remain saved locally."
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
