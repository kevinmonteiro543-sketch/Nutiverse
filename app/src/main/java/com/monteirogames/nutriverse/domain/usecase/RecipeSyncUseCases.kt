package com.monteirogames.nutriverse.domain.usecase

import com.monteirogames.nutriverse.domain.model.Recipe
import com.monteirogames.nutriverse.domain.model.SyncState
import com.monteirogames.nutriverse.domain.repository.RecipeSyncRepository
import kotlinx.coroutines.flow.StateFlow

class SyncRecipesUseCase(private val repository: RecipeSyncRepository) {
    suspend operator fun invoke(): Result<Int> = repository.syncRecipes()
}

class UploadRecipeUseCase(private val repository: RecipeSyncRepository) {
    suspend operator fun invoke(recipe: Recipe): Result<Unit> = repository.uploadRecipe(recipe)
}

class DeleteRecipeFromCloudUseCase(private val repository: RecipeSyncRepository) {
    suspend operator fun invoke(recipeId: String): Result<Unit> = repository.deleteRecipeInCloud(recipeId)
}

class ObserveRecipeSyncStateUseCase(private val repository: RecipeSyncRepository) {
    operator fun invoke(): StateFlow<SyncState> = repository.syncState
}

data class RecipeSyncUseCases(
    val syncRecipes: SyncRecipesUseCase,
    val uploadRecipe: UploadRecipeUseCase,
    val deleteRecipeFromCloud: DeleteRecipeFromCloudUseCase,
    val observeRecipeSyncState: ObserveRecipeSyncStateUseCase
)
