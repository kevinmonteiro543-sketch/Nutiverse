package com.monteirogames.nutriverse.domain.model

data class ProgressAnalyticsData(
    val timeRange: AnalyticsTimeRange,
    val startDate: Long,
    val endDate: Long,
    val totalCalendarDays: Int,
    val trackedDaysCount: Int,
    val dailyRecords: List<DailyNutritionRecord>,
    val averageCalories: Float,
    val averageProtein: Float,
    val averageCarbs: Float,
    val averageFat: Float,
    val targetCalories: Float,
    val targetProtein: Float,
    val targetCarbs: Float,
    val targetFat: Float,
    val proteinAdherenceDays: Int,
    val proteinAdherencePercent: Float,
    val calorieRangeDays: Int,
    val calorieRangePercent: Float,
    val highestCalorieDay: DailyNutritionRecord?,
    val lowestCalorieDay: DailyNutritionRecord?,
    val insights: List<String>
) {
    val hasSufficientData: Boolean get() = trackedDaysCount > 0
}
