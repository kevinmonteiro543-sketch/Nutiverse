package com.monteirogames.nutriverse.domain.model

data class RecipeSuggestion(
    val recipe: Recipe,
    val matchScore: Int, // 0 to 100%
    val pantryScore: Float, // 0 to 50
    val dietScore: Float, // 0 to 20
    val nutritionScore: Float, // 0 to 20
    val goalScore: Float, // 0 to 10
    val matchedPantryCount: Int,
    val totalIngredientsCount: Int,
    val matchedIngredients: List<String>,
    val missingIngredients: List<String>,
    val expiringIngredients: List<String>,
    val highlights: List<String>,
    val isExcludedDueToAllergies: Boolean = false,
    val allergyConflictReason: String? = null
)
