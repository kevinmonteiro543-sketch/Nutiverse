package com.monteirogames.nutriverse.domain.model

import java.util.UUID

/**
 * Deterministically computed metrics across the previous 7 completed calendar days.
 * Untracked days are explicitly excluded from averages and adherence metrics.
 */
data class WeeklyReviewMetrics(
    val startDate: String,
    val endDate: String,
    val startTimestamp: Long,
    val endTimestamp: Long,
    val displayRange: String,
    val trackedDays: Int,
    val totalDays: Int = 7,
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
    val highestCalorieDay: String? = null,
    val lowestCalorieDay: String? = null
)

/**
 * Structured coaching response interpreted and synthesized by Gemini.
 */
data class WeeklyReviewAiResponse(
    val summary: String,
    val strengths: List<String>,
    val improvements: List<String>,
    val nutritionInsights: List<String>,
    val recommendations: List<String>,
    val nextWeekFocus: List<String>
)

/**
 * Persisted weekly AI review domain aggregate.
 */
data class WeeklyReview(
    val id: String = UUID.randomUUID().toString(),
    val userId: String,
    val startDate: String,
    val endDate: String,
    val generatedAt: Long = System.currentTimeMillis(),
    val metrics: WeeklyReviewMetrics,
    val summary: String,
    val strengths: List<String>,
    val improvements: List<String>,
    val nutritionInsights: List<String>,
    val recommendations: List<String>,
    val nextWeekFocus: List<String>
)
