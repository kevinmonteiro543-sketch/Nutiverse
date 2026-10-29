package com.monteirogames.nutriverse.data.repository

import com.monteirogames.nutriverse.data.local.dao.RecipeDao
import com.monteirogames.nutriverse.data.local.dao.RecipeWithDetails
import com.monteirogames.nutriverse.data.local.dao.SyncTombstoneDao
import com.monteirogames.nutriverse.data.local.entity.RecipeEntity
import com.monteirogames.nutriverse.data.local.entity.RecipeIngredientEntity
import com.monteirogames.nutriverse.data.local.entity.RecipeInstructionEntity
import com.monteirogames.nutriverse.data.local.entity.SyncTombstoneEntity
import com.monteirogames.nutriverse.data.local.seed.RecipeSeedData
import com.monteirogames.nutriverse.domain.model.Recipe
import com.monteirogames.nutriverse.domain.model.RecipeIngredient
import com.monteirogames.nutriverse.domain.model.RecipeInstruction
import com.monteirogames.nutriverse.domain.repository.RecipeRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class RecipeRepositoryImpl(
    private val recipeDao: RecipeDao,
    private val syncTombstoneDao: SyncTombstoneDao? = null
) : RecipeRepository {

    override fun getAllRecipes(): Flow<List<Recipe>> {
        return recipeDao.getAllRecipesWithDetails().map { list ->
            list.map { it.toDomainModel() }
        }
    }

    override suspend fun getAllRecipesList(): List<Recipe> = withContext(Dispatchers.IO) {
        recipeDao.getAllRecipesList().map { it.toDomainModel() }
    }

    override fun getRecipeById(id: String): Flow<Recipe?> {
        return recipeDao.getRecipeWithDetailsById(id).map { it?.toDomainModel() }
    }

    override suspend fun setFavorite(recipeId: String, isFavorite: Boolean) {
        withContext(Dispatchers.IO) {
            recipeDao.updateFavorite(recipeId, isFavorite, updatedAt = System.currentTimeMillis())
        }
    }

    override suspend fun saveRecipe(recipe: Recipe) {
        withContext(Dispatchers.IO) {
            val timestamp = if (recipe.updatedAt > 0L) recipe.updatedAt else System.currentTimeMillis()
            val recipeEntity = RecipeEntity(
                id = recipe.id,
                name = recipe.name,
                description = recipe.description,
                imageUrl = recipe.imageUrl,
                prepTimeMinutes = recipe.prepTimeMinutes,
                cookTimeMinutes = recipe.cookTimeMinutes,
                servings = recipe.servings,
                calories = recipe.calories,
                protein = recipe.protein,
                carbs = recipe.carbs,
                fat = recipe.fat,
                difficulty = recipe.difficulty,
                dietaryTags = recipe.dietaryTags.joinToString(","),
                allergyTags = recipe.allergyTags.joinToString(","),
                isFavorite = recipe.isFavorite,
                createdAt = recipe.createdAt,
                updatedAt = timestamp
            )

            val ingredients = recipe.ingredients.map {
                RecipeIngredientEntity(
                    id = it.id,
                    recipeId = recipe.id,
                    name = it.name,
                    quantity = it.quantity,
                    unit = it.unit
                )
            }

            val instructions = recipe.instructions.map {
                RecipeInstructionEntity(
                    id = it.id,
                    recipeId = recipe.id,
                    stepNumber = it.stepNumber,
                    instructionText = it.instructionText
                )
            }

            recipeDao.insertFullRecipe(recipeEntity, ingredients, instructions)
            syncTombstoneDao?.deleteTombstone("recipe", recipe.id)
        }
    }

    override suspend fun deleteRecipe(recipeId: String, userId: String) {
        withContext(Dispatchers.IO) {
            recipeDao.deleteRecipeById(recipeId)
            if (userId.isNotBlank() && !recipeId.startsWith("seed_recipe_")) {
                syncTombstoneDao?.insertTombstone(
                    SyncTombstoneEntity(
                        id = recipeId,
                        entityType = "recipe",
                        userId = userId,
                        deletedAt = System.currentTimeMillis()
                    )
                )
            }
        }
    }

    override suspend fun seedInitialRecipesIfEmpty() {
        withContext(Dispatchers.IO) {
            val count = recipeDao.getRecipeCount()
            if (count == 0) {
                val seedBundles = RecipeSeedData.getInitialRecipes()
                val recipes = seedBundles.map { it.recipe }
                val ingredients = seedBundles.flatMap { it.ingredients }
                val instructions = seedBundles.flatMap { it.instructions }
                recipeDao.insertFullRecipes(recipes, ingredients, instructions)
            }
        }
    }

    private fun RecipeWithDetails.toDomainModel(): Recipe {
        val dietaryList = recipe.dietaryTags
            .split(",")
            .map { it.trim() }
            .filter { it.isNotBlank() }

        val allergyList = recipe.allergyTags
            .split(",")
            .map { it.trim() }
            .filter { it.isNotBlank() }

        val domainIngredients = ingredients.map {
            RecipeIngredient(
                id = it.id,
                recipeId = it.recipeId,
                name = it.name,
                quantity = it.quantity,
                unit = it.unit
            )
        }

        val domainInstructions = instructions
            .sortedBy { it.stepNumber }
            .map {
                RecipeInstruction(
                    id = it.id,
                    recipeId = it.recipeId,
                    stepNumber = it.stepNumber,
                    instructionText = it.instructionText
                )
            }

        return Recipe(
            id = recipe.id,
            name = recipe.name,
            description = recipe.description,
            imageUrl = recipe.imageUrl,
            prepTimeMinutes = recipe.prepTimeMinutes,
            cookTimeMinutes = recipe.cookTimeMinutes,
            servings = recipe.servings,
            calories = recipe.calories,
            protein = recipe.protein,
            carbs = recipe.carbs,
            fat = recipe.fat,
            difficulty = recipe.difficulty,
            dietaryTags = dietaryList,
            allergyTags = allergyList,
            ingredients = domainIngredients,
            instructions = domainInstructions,
            isFavorite = recipe.isFavorite,
            createdAt = recipe.createdAt,
            updatedAt = if (recipe.updatedAt > 0L) recipe.updatedAt else recipe.createdAt
        )
    }
}
