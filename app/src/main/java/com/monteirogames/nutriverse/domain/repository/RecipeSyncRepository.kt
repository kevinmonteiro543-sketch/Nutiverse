package com.monteirogames.nutriverse.domain.repository

import com.monteirogames.nutriverse.domain.model.Recipe
import com.monteirogames.nutriverse.domain.model.SyncState
import kotlinx.coroutines.flow.StateFlow

/**
 * Dedicated repository abstraction for Recipe cloud synchronization.
 * Synchronizes user-owned/custom recipes between local Room and Firestore `users/{FirebaseUID}/recipes/{recipeId}`.
 * Static seed recipes are protected and never uploaded as user data.
 */
interface RecipeSyncRepository {
    val syncState: StateFlow<SyncState>
    fun getSyncState(): SyncState

    suspend fun syncRecipes(): Result<Int>
    suspend fun uploadRecipe(recipe: Recipe): Result<Unit>
    suspend fun deleteRecipeInCloud(recipeId: String): Result<Unit>
}
