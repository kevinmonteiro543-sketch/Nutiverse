package com.monteirogames.nutriverse.domain.repository

import com.monteirogames.nutriverse.domain.model.Recipe
import kotlinx.coroutines.flow.Flow

interface RecipeRepository {
    fun getAllRecipes(): Flow<List<Recipe>>
    suspend fun getAllRecipesList(): List<Recipe>
    fun getRecipeById(id: String): Flow<Recipe?>
    suspend fun setFavorite(recipeId: String, isFavorite: Boolean)
    suspend fun saveRecipe(recipe: Recipe)
    suspend fun deleteRecipe(recipeId: String, userId: String = "")
    suspend fun seedInitialRecipesIfEmpty()
}
