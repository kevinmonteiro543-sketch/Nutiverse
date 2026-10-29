package com.monteirogames.nutriverse.domain.usecase

import com.monteirogames.nutriverse.domain.model.*
import com.monteirogames.nutriverse.domain.repository.MealRepository
import com.monteirogames.nutriverse.domain.repository.UserProfileRepository
import com.monteirogames.nutriverse.domain.repository.WeeklyReviewAnalysisRepository
import com.monteirogames.nutriverse.domain.repository.WeeklyReviewRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.UUID
import kotlin.math.roundToInt

class CalculateWeeklyReviewMetricsUseCase(
    private val mealRepository: MealRepository,
    private val userProfileRepository: UserProfileRepository,
    private val calculateNutritionTargetsUseCase: CalculateNutritionTargetsUseCase
) {

    suspend operator fun invoke(
        userId: String,
        currentTimeMillis: Long = System.currentTimeMillis()
    ): WeeklyReviewMetrics {
        val period = WeeklyReviewPeriodHelper.calculatePreviousWeekPeriod(currentTimeMillis)
        val profile = userProfileRepository.getUserProfile(userId).firstOrNull()

        val targets = profile?.let { calculateNutritionTargetsUseCase(it) }
            ?: NutritionTargets(calories = 2000f, protein = 120f, carbs = 220f, fat = 65f)

        val meals = mealRepository.getMealsForDateRange(userId, period.startTimestamp, period.endTimestamp).firstOrNull()
            ?: emptyList()

        // Break period into 7 distinct calendar day buckets
        val dayBuckets = mutableListOf<DayWindow>()
        val dayNameFormat = SimpleDateFormat("EEEE, MMM d", Locale.getDefault())

        val cal = Calendar.getInstance().apply {
            timeInMillis = period.startTimestamp
        }

        for (i in 0 until 7) {
            val startOfDay = cal.apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis

            val endOfDay = cal.apply {
                set(Calendar.HOUR_OF_DAY, 23)
                set(Calendar.MINUTE, 59)
                set(Calendar.SECOND, 59)
                set(Calendar.MILLISECOND, 999)
            }.timeInMillis

            dayBuckets.add(
                DayWindow(
                    start = startOfDay,
                    end = endOfDay,
                    label = dayNameFormat.format(cal.time)
                )
            )
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }

        // Calculate daily stats for each calendar day
        val trackedDays = mutableListOf<DayStats>()
        var totalMealsCount = 0

        for (window in dayBuckets) {
            val dayMeals = meals.filter { it.date in window.start..window.end }
            if (dayMeals.isNotEmpty()) {
                val calSum = dayMeals.sumOf { it.calculatedCalories.toDouble() }.toFloat()
                val proSum = dayMeals.sumOf { it.calculatedProtein.toDouble() }.toFloat()
                val carbSum = dayMeals.sumOf { it.calculatedCarbs.toDouble() }.toFloat()
                val fatSum = dayMeals.sumOf { it.calculatedFat.toDouble() }.toFloat()
                totalMealsCount += dayMeals.size

                trackedDays.add(
                    DayStats(
                        label = window.label,
                        calories = calSum,
                        protein = proSum,
                        carbs = carbSum,
                        fat = fatSum
                    )
                )
            }
        }

        val trackedDaysCount = trackedDays.size
        val trackingConsistency = (trackedDaysCount.toFloat() / 7f) * 100f

        val averageCalories = if (trackedDaysCount > 0) {
            (trackedDays.sumOf { it.calories.toDouble() } / trackedDaysCount).toFloat()
        } else 0f

        val averageProtein = if (trackedDaysCount > 0) {
            (trackedDays.sumOf { it.protein.toDouble() } / trackedDaysCount).toFloat()
        } else 0f

        val averageCarbs = if (trackedDaysCount > 0) {
            (trackedDays.sumOf { it.carbs.toDouble() } / trackedDaysCount).toFloat()
        } else 0f

        val averageFat = if (trackedDaysCount > 0) {
            (trackedDays.sumOf { it.fat.toDouble() } / trackedDaysCount).toFloat()
        } else 0f

        // Adherence metrics across tracked days only
        val calorieAdherence = if (trackedDaysCount > 0 && targets.calories > 0f) {
            val inRangeCount = trackedDays.count { it.calories in (targets.calories * 0.9f)..(targets.calories * 1.1f) }
            (inRangeCount.toFloat() / trackedDaysCount.toFloat()) * 100f
        } else 0f

        val proteinAdherence = if (trackedDaysCount > 0 && targets.protein > 0f) {
            val metCount = trackedDays.count { it.protein >= targets.protein }
            (metCount.toFloat() / trackedDaysCount.toFloat()) * 100f
        } else 0f

        val highestDay = trackedDays.maxByOrNull { it.calories }
        val lowestDay = trackedDays.minByOrNull { it.calories }

        val highestCalorieDayStr = highestDay?.let {
            "${it.label} (${it.calories.roundToInt()} kcal)"
        }
        val lowestCalorieDayStr = if (trackedDaysCount >= 2 && lowestDay != null && lowestDay != highestDay) {
            "${lowestDay.label} (${lowestDay.calories.roundToInt()} kcal)"
        } else null

        return WeeklyReviewMetrics(
            startDate = period.startDate,
            endDate = period.endDate,
            startTimestamp = period.startTimestamp,
            endTimestamp = period.endTimestamp,
            displayRange = period.displayRange,
            trackedDays = trackedDaysCount,
            totalDays = 7,
            averageCalories = averageCalories,
            averageProtein = averageProtein,
            averageCarbs = averageCarbs,
            averageFat = averageFat,
            targetCalories = targets.calories,
            targetProtein = targets.protein,
            targetCarbs = targets.carbs,
            targetFat = targets.fat,
            calorieAdherence = calorieAdherence,
            proteinAdherence = proteinAdherence,
            trackingConsistency = trackingConsistency,
            totalMeals = totalMealsCount,
            highestCalorieDay = highestCalorieDayStr,
            lowestCalorieDay = lowestCalorieDayStr
        )
    }

    private data class DayWindow(val start: Long, val end: Long, val label: String)
    private data class DayStats(val label: String, val calories: Float, val protein: Float, val carbs: Float, val fat: Float)
}

class GenerateWeeklyReviewUseCase(
    private val calculateMetricsUseCase: CalculateWeeklyReviewMetricsUseCase,
    private val weeklyReviewRepository: WeeklyReviewRepository,
    private val weeklyReviewAnalysisRepository: WeeklyReviewAnalysisRepository
) {

    suspend operator fun invoke(
        userId: String,
        forceRegenerate: Boolean = false,
        currentTimeMillis: Long = System.currentTimeMillis()
    ): Result<WeeklyReview> {
        val metrics = calculateMetricsUseCase(userId, currentTimeMillis)

        // Minimum data requirement: At least 2 tracked days required
        if (metrics.trackedDays < 2) {
            return Result.failure(
                InsufficientReviewDataException(
                    "Track at least 2 days of meals to generate your weekly AI review."
                )
            )
        }

        // Duplicate generation protection: reuse existing saved review for same user & period
        if (!forceRegenerate) {
            val existing = weeklyReviewRepository.getReviewForPeriod(userId, metrics.startDate, metrics.endDate)
            if (existing != null) {
                return Result.success(existing)
            }
        }

        // Request AI synthesis from Gemini
        val aiResult = weeklyReviewAnalysisRepository.generateAiReview(metrics)
        if (aiResult.isFailure) {
            return Result.failure(aiResult.exceptionOrNull() ?: Exception("AI coaching analysis failed."))
        }

        val aiResponse = aiResult.getOrThrow()

        val review = WeeklyReview(
            id = UUID.randomUUID().toString(),
            userId = userId,
            startDate = metrics.startDate,
            endDate = metrics.endDate,
            generatedAt = System.currentTimeMillis(),
            metrics = metrics,
            summary = aiResponse.summary,
            strengths = aiResponse.strengths,
            improvements = aiResponse.improvements,
            nutritionInsights = aiResponse.nutritionInsights,
            recommendations = aiResponse.recommendations,
            nextWeekFocus = aiResponse.nextWeekFocus
        )

        weeklyReviewRepository.saveReview(review)
        return Result.success(review)
    }
}

class InsufficientReviewDataException(message: String) : Exception(message)

class GetLatestWeeklyReviewUseCase(
    private val weeklyReviewRepository: WeeklyReviewRepository
) {
    operator fun invoke(userId: String): Flow<WeeklyReview?> {
        return weeklyReviewRepository.getLatestReview(userId)
    }
}

class GetWeeklyReviewHistoryUseCase(
    private val weeklyReviewRepository: WeeklyReviewRepository
) {
    operator fun invoke(userId: String): Flow<List<WeeklyReview>> {
        return weeklyReviewRepository.getAllReviews(userId)
    }
}

class GetWeeklyReviewByIdUseCase(
    private val weeklyReviewRepository: WeeklyReviewRepository
) {
    operator fun invoke(id: String): Flow<WeeklyReview?> {
        return weeklyReviewRepository.getReviewById(id)
    }
}

class DeleteWeeklyReviewUseCase(
    private val weeklyReviewRepository: WeeklyReviewRepository
) {
    suspend operator fun invoke(id: String) {
        weeklyReviewRepository.deleteReviewById(id)
    }
}

data class WeeklyReviewUseCases(
    val calculateMetrics: CalculateWeeklyReviewMetricsUseCase,
    val generateWeeklyReview: GenerateWeeklyReviewUseCase,
    val getLatestWeeklyReview: GetLatestWeeklyReviewUseCase,
    val getWeeklyReviewHistory: GetWeeklyReviewHistoryUseCase,
    val getWeeklyReviewById: GetWeeklyReviewByIdUseCase,
    val deleteWeeklyReview: DeleteWeeklyReviewUseCase
)
