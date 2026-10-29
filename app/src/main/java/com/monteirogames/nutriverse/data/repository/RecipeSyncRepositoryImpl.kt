package com.monteirogames.nutriverse.data.repository

import com.google.android.gms.tasks.Task
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.SetOptions
import com.monteirogames.nutriverse.data.local.dao.SyncTombstoneDao
import com.monteirogames.nutriverse.data.local.entity.SyncTombstoneEntity
import com.monteirogames.nutriverse.domain.model.FirestoreCollections
import com.monteirogames.nutriverse.domain.model.FirestoreRecipeDocument
import com.monteirogames.nutriverse.domain.model.Recipe
import com.monteirogames.nutriverse.domain.model.SyncState
import com.monteirogames.nutriverse.domain.model.SyncStatus
import com.monteirogames.nutriverse.domain.model.toFirestoreDocument
import com.monteirogames.nutriverse.domain.model.toRecipe
import com.monteirogames.nutriverse.domain.repository.AuthRepository
import com.monteirogames.nutriverse.domain.repository.RecipeRepository
import com.monteirogames.nutriverse.domain.repository.RecipeSyncRepository
import com.monteirogames.nutriverse.domain.sync.SyncResolution
import com.monteirogames.nutriverse.domain.sync.TimestampConflictResolver
import com.monteirogames.nutriverse.domain.sync.TimestampConflictResolverImpl
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class RecipeSyncRepositoryImpl(
    private val authRepository: AuthRepository,
    private val recipeRepository: RecipeRepository,
    private val syncTombstoneDao: SyncTombstoneDao,
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val conflictResolver: TimestampConflictResolver = TimestampConflictResolverImpl()
) : RecipeSyncRepository {

    private val _syncState = MutableStateFlow(SyncState(status = SyncStatus.IDLE))
    override val syncState: StateFlow<SyncState> = _syncState.asStateFlow()

    override fun getSyncState(): SyncState = _syncState.value

    override suspend fun syncRecipes(): Result<Int> {
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
            // 1. Gather local data (filter out system seed recipes)
            val allRecipes = recipeRepository.getAllRecipesList()
            val localUserRecipes = allRecipes.filter { !it.id.startsWith("seed_recipe_") }
            val localMap = localUserRecipes.associateBy { it.id }
            val tombstones = syncTombstoneDao.getTombstonesForUser(currentUid, "recipe").associateBy { it.id }

            // 2. Query Firestore users/{uid}/recipes
            val snapshot = firestore.collection(FirestoreCollections.userRecipesPath(currentUid))
                .get()
                .awaitResult()

            val cloudMap = snapshot.documents.mapNotNull { doc ->
                val data = doc.data ?: return@mapNotNull null
                FirestoreRecipeDocument.fromMap(data, fallbackId = doc.id, fallbackUserId = currentUid)
            }.associateBy { it.id }

            // 3. Process all distinct user recipe IDs
            val allIds = (localMap.keys + cloudMap.keys + tombstones.keys).toSet()
            var syncedCount = 0

            for (id in allIds) {
                // Safeguard: never sync seed recipes
                if (id.startsWith("seed_recipe_")) continue

                val localItem = localMap[id]
                val cloudDoc = cloudMap[id]
                val tombstone = tombstones[id]

                when {
                    // Scenario 1: Local record was deleted (tombstone exists)
                    tombstone != null -> {
                        if (cloudDoc == null) {
                            firestore.document(FirestoreCollections.userRecipeDocPath(currentUid, id))
                                .set(
                                    mapOf(
                                        "id" to id,
                                        "userId" to currentUid,
                                        "isDeleted" to true,
                                        "updatedAt" to tombstone.deletedAt,
                                        "schemaVersion" to FirestoreRecipeDocument.CURRENT_SCHEMA_VERSION
                                    ),
                                    SetOptions.merge()
                                )
                                .awaitResult()
                            syncedCount++
                        } else if (cloudDoc.isDeleted) {
                            if (cloudDoc.updatedAt >= tombstone.deletedAt) {
                                syncTombstoneDao.deleteTombstone("recipe", id)
                            }
                        } else {
                            if (tombstone.deletedAt >= cloudDoc.updatedAt) {
                                firestore.document(FirestoreCollections.userRecipeDocPath(currentUid, id))
                                    .set(
                                        mapOf(
                                            "id" to id,
                                            "userId" to currentUid,
                                            "isDeleted" to true,
                                            "updatedAt" to tombstone.deletedAt,
                                            "schemaVersion" to FirestoreRecipeDocument.CURRENT_SCHEMA_VERSION
                                        ),
                                        SetOptions.merge()
                                    )
                                    .awaitResult()
                                syncedCount++
                            } else {
                                // Cloud was updated AFTER local deletion -> resurrect locally
                                recipeRepository.saveRecipe(cloudDoc.toRecipe())
                                syncTombstoneDao.deleteTombstone("recipe", id)
                                syncedCount++
                            }
                        }
                    }

                    // Scenario 2: Cloud doc exists and is marked as deleted (no local tombstone)
                    cloudDoc != null && cloudDoc.isDeleted -> {
                        if (localItem != null) {
                            if (cloudDoc.updatedAt >= localItem.updatedAt) {
                                recipeRepository.deleteRecipe(localItem.id, currentUid)
                                syncedCount++
                            } else {
                                firestore.document(FirestoreCollections.userRecipeDocPath(currentUid, id))
                                    .set(localItem.toFirestoreDocument(userId = currentUid, isDeleted = false).toMap())
                                    .awaitResult()
                                syncedCount++
                            }
                        }
                    }

                    // Scenario 3: Standard sync resolution for active items
                    else -> {
                        val cloudItem = cloudDoc?.toRecipe()
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
                                    recipeRepository.saveRecipe(updated)
                                    updated
                                } else {
                                    resolution.localItem
                                }

                                firestore.document(FirestoreCollections.userRecipeDocPath(currentUid, id))
                                    .set(toUpload.toFirestoreDocument(userId = currentUid, isDeleted = false).toMap())
                                    .awaitResult()
                                syncedCount++
                            }

                            is SyncResolution.DownloadToLocal -> {
                                recipeRepository.saveRecipe(resolution.cloudItem)
                                syncTombstoneDao.deleteTombstone("recipe", id)
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

    override suspend fun uploadRecipe(recipe: Recipe): Result<Unit> {
        val currentUid = authRepository.getCurrentUserId()
        if (currentUid.isNullOrBlank()) {
            return Result.failure(IllegalStateException("User is not authenticated"))
        }

        // Never upload static seed recipes
        if (recipe.id.startsWith("seed_recipe_")) {
            return Result.success(Unit)
        }

        return try {
            val validRecipe = if (recipe.updatedAt == 0L) {
                recipe.copy(updatedAt = System.currentTimeMillis())
            } else {
                recipe
            }

            firestore.document(FirestoreCollections.userRecipeDocPath(currentUid, validRecipe.id))
                .set(validRecipe.toFirestoreDocument(userId = currentUid, isDeleted = false).toMap())
                .awaitResult()

            _syncState.value = SyncState(status = SyncStatus.SUCCESS, lastSyncedAt = System.currentTimeMillis())
            Result.success(Unit)
        } catch (e: Exception) {
            val friendly = mapException(e)
            _syncState.value = SyncState(status = SyncStatus.ERROR, errorMessage = friendly)
            Result.failure(Exception(friendly, e))
        }
    }

    override suspend fun deleteRecipeInCloud(recipeId: String): Result<Unit> {
        val currentUid = authRepository.getCurrentUserId()
        if (currentUid.isNullOrBlank()) {
            return Result.failure(IllegalStateException("User is not authenticated"))
        }

        if (recipeId.startsWith("seed_recipe_")) {
            return Result.success(Unit)
        }

        return try {
            val now = System.currentTimeMillis()
            firestore.document(FirestoreCollections.userRecipeDocPath(currentUid, recipeId))
                .set(
                    mapOf(
                        "id" to recipeId,
                        "userId" to currentUid,
                        "isDeleted" to true,
                        "updatedAt" to now,
                        "schemaVersion" to FirestoreRecipeDocument.CURRENT_SCHEMA_VERSION
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
                        "Access denied. You do not have permission to sync recipes."
                    FirebaseFirestoreException.Code.UNAVAILABLE ->
                        "Cloud service is temporarily unavailable. Recipes remain saved locally."
                    FirebaseFirestoreException.Code.UNAUTHENTICATED ->
                        "User is not authenticated. Please sign in to sync."
                    FirebaseFirestoreException.Code.DEADLINE_EXCEEDED ->
                        "Sync operation timed out. Will retry when connection improves."
                    else -> "Cloud recipe synchronization failed: ${e.code.name}"
                }
            }
            is FirebaseNetworkException ->
                "Network unavailable. Recipe changes remain saved locally."
            else -> {
                val msg = e.localizedMessage ?: e.message ?: "Cloud synchronization error"
                if (msg.contains("network", ignoreCase = true) || msg.contains("unable to resolve", ignoreCase = true)) {
                    "Network unavailable. Recipe changes remain saved locally."
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
