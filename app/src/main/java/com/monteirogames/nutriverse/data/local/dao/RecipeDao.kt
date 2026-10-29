package com.monteirogames.nutriverse.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Relation
import androidx.room.Transaction
import com.monteirogames.nutriverse.data.local.entity.RecipeEntity
import com.monteirogames.nutriverse.data.local.entity.RecipeIngredientEntity
import com.monteirogames.nutriverse.data.local.entity.RecipeInstructionEntity
import kotlinx.coroutines.flow.Flow

data class RecipeWithDetails(
    @Embedded val recipe: RecipeEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "recipeId"
    )
    val ingredients: List<RecipeIngredientEntity>,
    @Relation(
        parentColumn = "id",
        entityColumn = "recipeId"
    )
    val instructions: List<RecipeInstructionEntity>
)

@Dao
interface RecipeDao {

    @Transaction
    @Query("SELECT * FROM recipe ORDER BY name ASC")
    fun getAllRecipesWithDetails(): Flow<List<RecipeWithDetails>>

    @Transaction
    @Query("SELECT * FROM recipe ORDER BY name ASC")
    suspend fun getAllRecipesList(): List<RecipeWithDetails>

    @Transaction
    @Query("SELECT * FROM recipe WHERE id = :recipeId LIMIT 1")
    fun getRecipeWithDetailsById(recipeId: String): Flow<RecipeWithDetails?>

    @Transaction
    @Query("SELECT * FROM recipe WHERE id = :recipeId LIMIT 1")
    suspend fun getRecipeWithDetailsByIdOnce(recipeId: String): RecipeWithDetails?

    @Query("SELECT COUNT(*) FROM recipe")
    suspend fun getRecipeCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecipe(recipe: RecipeEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecipes(recipes: List<RecipeEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIngredients(ingredients: List<RecipeIngredientEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInstructions(instructions: List<RecipeInstructionEntity>)

    @Query("DELETE FROM recipe_ingredient WHERE recipeId = :recipeId")
    suspend fun deleteIngredientsForRecipe(recipeId: String)

    @Query("DELETE FROM recipe_instruction WHERE recipeId = :recipeId")
    suspend fun deleteInstructionsForRecipe(recipeId: String)

    @Transaction
    suspend fun insertFullRecipe(
        recipe: RecipeEntity,
        ingredients: List<RecipeIngredientEntity>,
        instructions: List<RecipeInstructionEntity>
    ) {
        insertRecipe(recipe)
        deleteIngredientsForRecipe(recipe.id)
        deleteInstructionsForRecipe(recipe.id)
        insertIngredients(ingredients)
        insertInstructions(instructions)
    }

    @Transaction
    suspend fun insertFullRecipes(
        recipes: List<RecipeEntity>,
        ingredients: List<RecipeIngredientEntity>,
        instructions: List<RecipeInstructionEntity>
    ) {
        insertRecipes(recipes)
        insertIngredients(ingredients)
        insertInstructions(instructions)
    }

    @Query("UPDATE recipe SET isFavorite = :isFavorite, updatedAt = :updatedAt WHERE id = :recipeId")
    suspend fun updateFavorite(recipeId: String, isFavorite: Boolean, updatedAt: Long = System.currentTimeMillis())

    @Delete
    suspend fun deleteRecipe(recipe: RecipeEntity)

    @Query("DELETE FROM recipe WHERE id = :recipeId")
    suspend fun deleteRecipeById(recipeId: String)
}
