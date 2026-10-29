package com.monteirogames.nutriverse.domain.model

data class RecipeIngredient(
    val id: String,
    val recipeId: String,
    val name: String,
    val quantity: Float,
    val unit: String
)

data class RecipeInstruction(
    val id: String,
    val recipeId: String,
    val stepNumber: Int,
    val instructionText: String
)

data class Recipe(
    val id: String,
    val name: String,
    val description: String,
    val imageUrl: String = "",
    val prepTimeMinutes: Int,
    val cookTimeMinutes: Int,
    val servings: Int,
    val calories: Float,
    val protein: Float,
    val carbs: Float,
    val fat: Float,
    val difficulty: String, // "Easy", "Medium", "Hard"
    val dietaryTags: List<String> = emptyList(), // "High Protein", "Vegetarian", "Indian", "Quick", "Breakfast", "Lunch", "Dinner", "Snacks"
    val allergyTags: List<String> = emptyList(), // "Gluten-Free", "Dairy-Free", "Nut-Free"
    val ingredients: List<RecipeIngredient> = emptyList(),
    val instructions: List<RecipeInstruction> = emptyList(),
    val isFavorite: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = createdAt
)
