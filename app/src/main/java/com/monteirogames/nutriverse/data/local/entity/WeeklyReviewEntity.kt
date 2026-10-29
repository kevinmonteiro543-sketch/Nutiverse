package com.monteirogames.nutriverse.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.monteirogames.nutriverse.domain.model.WeeklyReview
import com.monteirogames.nutriverse.domain.model.WeeklyReviewMetrics
import org.json.JSONArray

@Entity(
    tableName = "weekly_review",
    indices = [
        Index(value = ["userId"]),
        Index(value = ["userId", "startDate", "endDate"], unique = true),
        Index(value = ["userId", "generatedAt"])
    ]
)
data class WeeklyReviewEntity(
    @PrimaryKey
    val id: String,
    val userId: String,
    val startDate: String,
    val endDate: String,
    val startTimestamp: Long,
    val endTimestamp: Long,
    val displayRange: String,
    val generatedAt: Long,
    val trackedDays: Int,
    val totalDays: Int,
    val averageCalories: Float,
    val averageProtein: Float,
    val averageCarbs: Float,
    val averageFat: Float,
    val targetCalories: Float,
    val targetProtein: Float,
    val targetCarbs: Float,
    val targetFat: Float,
    val calorieAdherence: Float,
    val proteinAdherence: Float,
    val trackingConsistency: Float,
    val totalMeals: Int,
    val highestCalorieDay: String?,
    val lowestCalorieDay: String?,
    val summary: String,
    val strengthsJson: String,
    val improvementsJson: String,
    val nutritionInsightsJson: String,
    val recommendationsJson: String,
    val nextWeekFocusJson: String
)

fun WeeklyReview.toEntity(): WeeklyReviewEntity {
    return WeeklyReviewEntity(
        id = id,
        userId = userId,
        startDate = startDate,
        endDate = endDate,
        startTimestamp = metrics.startTimestamp,
        endTimestamp = metrics.endTimestamp,
        displayRange = metrics.displayRange,
        generatedAt = generatedAt,
        trackedDays = metrics.trackedDays,
        totalDays = metrics.totalDays,
        averageCalories = metrics.averageCalories,
        averageProtein = metrics.averageProtein,
        averageCarbs = metrics.averageCarbs,
        averageFat = metrics.averageFat,
        targetCalories = metrics.targetCalories,
        targetProtein = metrics.targetProtein,
        targetCarbs = metrics.targetCarbs,
        targetFat = metrics.targetFat,
        calorieAdherence = metrics.calorieAdherence,
        proteinAdherence = metrics.proteinAdherence,
        trackingConsistency = metrics.trackingConsistency,
        totalMeals = metrics.totalMeals,
        highestCalorieDay = metrics.highestCalorieDay,
        lowestCalorieDay = metrics.lowestCalorieDay,
        summary = summary,
        strengthsJson = JSONArray(strengths).toString(),
        improvementsJson = JSONArray(improvements).toString(),
        nutritionInsightsJson = JSONArray(nutritionInsights).toString(),
        recommendationsJson = JSONArray(recommendations).toString(),
        nextWeekFocusJson = JSONArray(nextWeekFocus).toString()
    )
}

fun WeeklyReviewEntity.toDomain(): WeeklyReview {
    val parseJsonList = { jsonStr: String ->
        val list = mutableListOf<String>()
        try {
            val array = JSONArray(jsonStr)
            for (i in 0 until array.length()) {
                list.add(array.optString(i))
            }
        } catch (_: Exception) {}
        list
    }

    return WeeklyReview(
        id = id,
        userId = userId,
        startDate = startDate,
        endDate = endDate,
        generatedAt = generatedAt,
        metrics = WeeklyReviewMetrics(
            startDate = startDate,
            endDate = endDate,
            startTimestamp = startTimestamp,
            endTimestamp = endTimestamp,
            displayRange = displayRange,
            trackedDays = trackedDays,
            totalDays = totalDays,
            averageCalories = averageCalories,
            averageProtein = averageProtein,
            averageCarbs = averageCarbs,
            averageFat = averageFat,
            targetCalories = targetCalories,
            targetProtein = targetProtein,
            targetCarbs = targetCarbs,
            targetFat = targetFat,
            calorieAdherence = calorieAdherence,
            proteinAdherence = proteinAdherence,
            trackingConsistency = trackingConsistency,
            totalMeals = totalMeals,
            highestCalorieDay = highestCalorieDay,
            lowestCalorieDay = lowestCalorieDay
        ),
        summary = summary,
        strengths = parseJsonList(strengthsJson),
        improvements = parseJsonList(improvementsJson),
        nutritionInsights = parseJsonList(nutritionInsightsJson),
        recommendations = parseJsonList(recommendationsJson),
        nextWeekFocus = parseJsonList(nextWeekFocusJson)
    )
}
