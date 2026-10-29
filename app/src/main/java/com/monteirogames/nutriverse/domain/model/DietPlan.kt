package com.monteirogames.nutriverse.domain.model

data class DietPlanMeal(
    val id: String,
    val dayNumber: Int, // 1 to 7
    val mealType: String, // Breakfast, Lunch, Dinner, Snack
    val name: String,
    val calories: Float,
    val protein: Float,
    val carbs: Float,
    val fat: Float,
    val prepTip: String = ""
)

data class DayPlan(
    val dayNumber: Int,
    val meals: List<DietPlanMeal>
) {
    val totalCalories: Float get() = meals.sumOf { it.calories.toDouble() }.toFloat()
    val totalProtein: Float get() = meals.sumOf { it.protein.toDouble() }.toFloat()
    val totalCarbs: Float get() = meals.sumOf { it.carbs.toDouble() }.toFloat()
    val totalFat: Float get() = meals.sumOf { it.fat.toDouble() }.toFloat()
}

data class DietPlan(
    val id: String,
    val userId: String,
    val name: String,
    val targetCalories: Float,
    val targetProtein: Float,
    val targetCarbs: Float,
    val targetFat: Float,
    val createdAt: Long,
    val days: List<DayPlan>,
    val updatedAt: Long = createdAt
)
