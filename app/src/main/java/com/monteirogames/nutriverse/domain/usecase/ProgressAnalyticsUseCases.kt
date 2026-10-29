package com.monteirogames.nutriverse.domain.usecase

import com.monteirogames.nutriverse.domain.model.AnalyticsTimeRange
import com.monteirogames.nutriverse.domain.model.DailyNutritionRecord
import com.monteirogames.nutriverse.domain.model.MealEntry
import com.monteirogames.nutriverse.domain.model.ProgressAnalyticsData
import com.monteirogames.nutriverse.domain.repository.MealRepository
import com.monteirogames.nutriverse.domain.repository.UserProfileRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.roundToInt

class CalculateProgressAnalyticsUseCase(
    private val mealRepository: MealRepository,
    private val userProfileRepository: UserProfileRepository,
    private val calculateNutritionTargetsUseCase: CalculateNutritionTargetsUseCase
) {

    operator fun invoke(
        userId: String,
        timeRange: AnalyticsTimeRange,
        currentTimeMillis: Long = System.currentTimeMillis()
    ): Flow<ProgressAnalyticsData> {
        val numDays = timeRange.days
        val dayBuckets = calculateDayBuckets(numDays, currentTimeMillis)
        val startOfRange = dayBuckets.first().startOfDay
        val endOfRange = dayBuckets.last().endOfDay

        val profileFlow = userProfileRepository.getUserProfile(userId)
        val mealsFlow = mealRepository.getMealsForDateRange(userId, startOfRange, endOfRange)

        return combine(profileFlow, mealsFlow) { profile, meals ->
            val targets = profile?.let { calculateNutritionTargetsUseCase(it) }
                ?: NutritionTargets(
                    calories = 2000f,
                    protein = 120f,
                    carbs = 220f,
                    fat = 65f
                )

            buildAnalyticsData(
                timeRange = timeRange,
                dayBuckets = dayBuckets,
                meals = meals,
                targets = targets
            )
        }
    }

    data class DayBucket(
        val startOfDay: Long,
        val endOfDay: Long,
        val dateLabel: String,
        val dayOfWeek: String,
        val fullDateLabel: String
    )

    fun calculateDayBuckets(numDays: Int, currentTimeMillis: Long): List<DayBucket> {
        val buckets = ArrayList<DayBucket>(numDays)
        val dateFormat = SimpleDateFormat("MMM d", Locale.getDefault())
        val dayFormat = SimpleDateFormat("EEE", Locale.getDefault())
        val fullFormat = SimpleDateFormat("EEEE, MMM d", Locale.getDefault())

        for (i in (numDays - 1) downTo 0) {
            val cal = Calendar.getInstance().apply {
                timeInMillis = currentTimeMillis
                add(Calendar.DAY_OF_YEAR, -i)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val startOfDay = cal.timeInMillis

            cal.set(Calendar.HOUR_OF_DAY, 23)
            cal.set(Calendar.MINUTE, 59)
            cal.set(Calendar.SECOND, 59)
            cal.set(Calendar.MILLISECOND, 999)
            val endOfDay = cal.timeInMillis

            val calForLabels = Calendar.getInstance().apply { timeInMillis = startOfDay }
            buckets.add(
                DayBucket(
                    startOfDay = startOfDay,
                    endOfDay = endOfDay,
                    dateLabel = dateFormat.format(calForLabels.time),
                    dayOfWeek = dayFormat.format(calForLabels.time),
                    fullDateLabel = fullFormat.format(calForLabels.time)
                )
            )
        }
        return buckets
    }

    fun buildAnalyticsData(
        timeRange: AnalyticsTimeRange,
        dayBuckets: List<DayBucket>,
        meals: List<MealEntry>,
        targets: NutritionTargets
    ): ProgressAnalyticsData {
        val dailyRecords = dayBuckets.map { bucket ->
            val dayMeals = meals.filter { it.date in bucket.startOfDay..bucket.endOfDay }
            val hasData = dayMeals.isNotEmpty()
            val calories = if (hasData) dayMeals.sumOf { it.calculatedCalories.toDouble() }.toFloat() else 0f
            val protein = if (hasData) dayMeals.sumOf { it.calculatedProtein.toDouble() }.toFloat() else 0f
            val carbs = if (hasData) dayMeals.sumOf { it.calculatedCarbs.toDouble() }.toFloat() else 0f
            val fat = if (hasData) dayMeals.sumOf { it.calculatedFat.toDouble() }.toFloat() else 0f

            DailyNutritionRecord(
                dayTimestamp = bucket.startOfDay,
                dateLabel = bucket.dateLabel,
                dayOfWeek = bucket.dayOfWeek,
                fullDateLabel = bucket.fullDateLabel,
                hasData = hasData,
                calories = calories,
                protein = protein,
                carbs = carbs,
                fat = fat,
                targetCalories = targets.calories,
                targetProtein = targets.protein,
                targetCarbs = targets.carbs,
                targetFat = targets.fat,
                mealCount = dayMeals.size
            )
        }

        val trackedDays = dailyRecords.filter { it.hasData }
        val trackedDaysCount = trackedDays.size
        val totalCalendarDays = dayBuckets.size

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

        val proteinAdherenceDays = if (trackedDaysCount > 0 && targets.protein > 0f) {
            trackedDays.count { it.protein >= targets.protein }
        } else 0

        val proteinAdherencePercent = if (trackedDaysCount > 0) {
            (proteinAdherenceDays.toFloat() / trackedDaysCount.toFloat()) * 100f
        } else 0f

        val calorieRangeDays = if (trackedDaysCount > 0 && targets.calories > 0f) {
            trackedDays.count { it.calories in (targets.calories * 0.9f)..(targets.calories * 1.1f) }
        } else 0

        val calorieRangePercent = if (trackedDaysCount > 0) {
            (calorieRangeDays.toFloat() / trackedDaysCount.toFloat()) * 100f
        } else 0f

        val highestCalorieDay = if (trackedDaysCount > 0) trackedDays.maxByOrNull { it.calories } else null
        val lowestCalorieDay = if (trackedDaysCount > 0) trackedDays.minByOrNull { it.calories } else null

        val insights = generateDeterministicInsights(
            totalCalendarDays = totalCalendarDays,
            trackedDaysCount = trackedDaysCount,
            averageCalories = averageCalories,
            averageProtein = averageProtein,
            targets = targets,
            proteinAdherenceDays = proteinAdherenceDays,
            proteinAdherencePercent = proteinAdherencePercent,
            calorieRangeDays = calorieRangeDays,
            calorieRangePercent = calorieRangePercent,
            highestCalorieDay = highestCalorieDay,
            lowestCalorieDay = lowestCalorieDay
        )

        return ProgressAnalyticsData(
            timeRange = timeRange,
            startDate = dayBuckets.first().startOfDay,
            endDate = dayBuckets.last().endOfDay,
            totalCalendarDays = totalCalendarDays,
            trackedDaysCount = trackedDaysCount,
            dailyRecords = dailyRecords,
            averageCalories = averageCalories,
            averageProtein = averageProtein,
            averageCarbs = averageCarbs,
            averageFat = averageFat,
            targetCalories = targets.calories,
            targetProtein = targets.protein,
            targetCarbs = targets.carbs,
            targetFat = targets.fat,
            proteinAdherenceDays = proteinAdherenceDays,
            proteinAdherencePercent = proteinAdherencePercent,
            calorieRangeDays = calorieRangeDays,
            calorieRangePercent = calorieRangePercent,
            highestCalorieDay = highestCalorieDay,
            lowestCalorieDay = lowestCalorieDay,
            insights = insights
        )
    }

    private fun generateDeterministicInsights(
        totalCalendarDays: Int,
        trackedDaysCount: Int,
        averageCalories: Float,
        averageProtein: Float,
        targets: NutritionTargets,
        proteinAdherenceDays: Int,
        proteinAdherencePercent: Float,
        calorieRangeDays: Int,
        calorieRangePercent: Float,
        highestCalorieDay: DailyNutritionRecord?,
        lowestCalorieDay: DailyNutritionRecord?
    ): List<String> {
        if (trackedDaysCount == 0) {
            return listOf("No meal records found in this $totalCalendarDays-day period. Log meals in the Meal Tracker to start tracking your progress.")
        }

        val list = mutableListOf<String>()

        // 1. Extreme days
        if (highestCalorieDay != null) {
            list.add("Highest calorie intake: ${highestCalorieDay.dayOfWeek}, ${highestCalorieDay.dateLabel} (${highestCalorieDay.calories.roundToInt()} kcal).")
        }
        if (trackedDaysCount >= 3 && lowestCalorieDay != null && lowestCalorieDay.dayTimestamp != highestCalorieDay?.dayTimestamp) {
            list.add("Lowest calorie intake: ${lowestCalorieDay.dayOfWeek}, ${lowestCalorieDay.dateLabel} (${lowestCalorieDay.calories.roundToInt()} kcal).")
        }

        // 2. Calorie target proximity
        if (targets.calories > 0f) {
            val diff = averageCalories - targets.calories
            val diffPercent = (diff / targets.calories) * 100f
            when {
                kotlin.math.abs(diffPercent) <= 5f ->
                    list.add("Average daily intake (${averageCalories.roundToInt()} kcal) is within ${kotlin.math.abs(diffPercent).roundToInt()}% of your configured target.")
                diffPercent < -5f ->
                    list.add("Average daily intake (${averageCalories.roundToInt()} kcal) is ${kotlin.math.abs(diffPercent).roundToInt()}% below your configured target (${targets.calories.roundToInt()} kcal).")
                else ->
                    list.add("Average daily intake (${averageCalories.roundToInt()} kcal) is ${diffPercent.roundToInt()}% above your configured target (${targets.calories.roundToInt()} kcal).")
            }
        }

        // 3. Protein adherence
        if (targets.protein > 0f) {
            list.add("Protein target reached on $proteinAdherenceDays of $trackedDaysCount tracked days (${proteinAdherencePercent.roundToInt()}% adherence).")
        }

        // 4. Calorie target range
        if (targets.calories > 0f) {
            list.add("Target range (90%–110%) maintained on $calorieRangeDays of $trackedDaysCount tracked days (${calorieRangePercent.roundToInt()}%).")
        }

        // 5. Tracking consistency
        val trackingPercent = ((trackedDaysCount.toFloat() / totalCalendarDays.toFloat()) * 100f).roundToInt()
        list.add("Logged meals on $trackedDaysCount of $totalCalendarDays calendar days ($trackingPercent% tracking consistency).")

        return list
    }
}

data class ProgressAnalyticsUseCases(
    val calculateProgressAnalytics: CalculateProgressAnalyticsUseCase
)
