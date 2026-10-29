package com.monteirogames.nutriverse.domain.model

data class DailyNutritionRecord(
    val dayTimestamp: Long,
    val dateLabel: String,
    val dayOfWeek: String,
    val fullDateLabel: String,
    val hasData: Boolean,
    val calories: Float,
    val protein: Float,
    val carbs: Float,
    val fat: Float,
    val targetCalories: Float,
    val targetProtein: Float,
    val targetCarbs: Float,
    val targetFat: Float,
    val mealCount: Int,
    val isProteinTargetMet: Boolean = hasData && targetProtein > 0 && protein >= targetProtein,
    val isCalorieTargetInRange: Boolean = hasData && targetCalories > 0 && calories in (targetCalories * 0.9f)..(targetCalories * 1.1f)
)
