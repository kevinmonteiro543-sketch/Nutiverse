package com.monteirogames.nutriverse.domain.model

data class MealEntry(
    val id: String,
    val userId: String,
    val foodItem: FoodItem,
    val date: Long,
    val mealType: String,
    val quantity: Float,
    val loggedAt: Long,
    val updatedAt: Long = loggedAt
) {
    // Deterministic calculation of nutrients based on quantity vs serving size
    val calculatedCalories: Float get() = foodItem.calories * (quantity / foodItem.servingSize)
    val calculatedProtein: Float get() = foodItem.protein * (quantity / foodItem.servingSize)
    val calculatedCarbs: Float get() = foodItem.carbs * (quantity / foodItem.servingSize)
    val calculatedFat: Float get() = foodItem.fat * (quantity / foodItem.servingSize)
}

/**
 * Typealias ensuring standard domain naming compatibility for Meal/MealEntry.
 */
typealias Meal = MealEntry
