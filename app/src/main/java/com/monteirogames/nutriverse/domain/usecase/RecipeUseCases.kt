package com.monteirogames.nutriverse.domain.usecase

import com.monteirogames.nutriverse.domain.model.Recipe
import com.monteirogames.nutriverse.domain.repository.RecipeRepository
import kotlinx.coroutines.flow.Flow

class GetAllRecipesUseCase(private val repository: RecipeRepository) {
    operator fun invoke(): Flow<List<Recipe>> = repository.getAllRecipes()
}

class GetRecipeByIdUseCase(private val repository: RecipeRepository) {
    operator fun invoke(id: String): Flow<Recipe?> = repository.getRecipeById(id)
}

class ToggleRecipeFavoriteUseCase(private val repository: RecipeRepository) {
    suspend operator fun invoke(recipeId: String, isFavorite: Boolean) {
        repository.setFavorite(recipeId, isFavorite)
    }
}

class SeedRecipesUseCase(private val repository: RecipeRepository) {
    suspend operator fun invoke() {
        repository.seedInitialRecipesIfEmpty()
    }
}

data class RecipeUseCases(
    val getAllRecipes: GetAllRecipesUseCase,
    val getRecipeById: GetRecipeByIdUseCase,
    val toggleRecipeFavorite: ToggleRecipeFavoriteUseCase,
    val seedRecipes: SeedRecipesUseCase,
    val getSmartRecipeSuggestions: SmartRecipeRecommendationUseCase = SmartRecipeRecommendationUseCase()
)
