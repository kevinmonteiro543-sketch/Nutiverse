package com.monteirogames.nutriverse

import com.monteirogames.nutriverse.domain.model.AnalyticsTimeRange
import com.monteirogames.nutriverse.domain.model.FoodItem
import com.monteirogames.nutriverse.domain.model.MealEntry
import com.monteirogames.nutriverse.domain.usecase.CalculateNutritionTargetsUseCase
import com.monteirogames.nutriverse.domain.usecase.CalculateProgressAnalyticsUseCase
import com.monteirogames.nutriverse.domain.usecase.NutritionTargets
import org.junit.Assert.*
import org.junit.Test
import java.util.Calendar

class ProgressAnalyticsTest {

    private val dummyCalculateUseCase = CalculateProgressAnalyticsUseCase(
        mealRepository = object : com.monteirogames.nutriverse.domain.repository.MealRepository {
            override suspend fun saveFoodItem(foodItem: FoodItem) {}
            override suspend fun getFoodItemById(id: String): FoodItem? = null
            override suspend fun saveMealEntry(mealEntry: MealEntry) {}
            override suspend fun updateMealEntry(mealEntry: MealEntry) {}
            override suspend fun deleteMealEntry(mealEntry: MealEntry) {}
            override suspend fun getMealEntryById(id: String, userId: String): MealEntry? = null
            override suspend fun getAllMeals(userId: String): List<MealEntry> = emptyList()
            override fun getMealsForDate(userId: String, timestamp: Long) = kotlinx.coroutines.flow.emptyFlow<List<MealEntry>>()
            override fun getMealsForDateRange(userId: String, startDate: Long, endDate: Long) = kotlinx.coroutines.flow.emptyFlow<List<MealEntry>>()
        },
        userProfileRepository = object : com.monteirogames.nutriverse.domain.repository.UserProfileRepository {
            override fun getUserProfile(userId: String) = kotlinx.coroutines.flow.emptyFlow<com.monteirogames.nutriverse.domain.model.UserProfile?>()
            override suspend fun saveProfile(profile: com.monteirogames.nutriverse.domain.model.UserProfile) {}
            override suspend fun clearProfile() {}
        },
        calculateNutritionTargetsUseCase = CalculateNutritionTargetsUseCase()
    )

    @Test
    fun testDayBuckets7Days() {
        val testTime = 1760000000000L // arbitrary timestamp
        val buckets = dummyCalculateUseCase.calculateDayBuckets(7, testTime)

        assertEquals(7, buckets.size)
        // Ensure chronological order
        for (i in 0 until buckets.size - 1) {
            assertTrue(buckets[i].startOfDay < buckets[i + 1].startOfDay)
            assertTrue(buckets[i].endOfDay < buckets[i + 1].endOfDay)
            assertEquals(buckets[i].startOfDay + 86399999L, buckets[i].endOfDay)
        }
    }

    @Test
    fun testDayBuckets30Days() {
        val testTime = 1760000000000L
        val buckets = dummyCalculateUseCase.calculateDayBuckets(30, testTime)

        assertEquals(30, buckets.size)
        for (i in 0 until buckets.size - 1) {
            assertTrue(buckets[i].startOfDay < buckets[i + 1].startOfDay)
        }
    }

    @Test
    fun testBuildAnalyticsDataWithNoMeals() {
        val testTime = 1760000000000L
        val buckets = dummyCalculateUseCase.calculateDayBuckets(7, testTime)
        val targets = NutritionTargets(calories = 2000f, protein = 140f, carbs = 220f, fat = 65f)

        val result = dummyCalculateUseCase.buildAnalyticsData(
            timeRange = AnalyticsTimeRange.LAST_7_DAYS,
            dayBuckets = buckets,
            meals = emptyList(),
            targets = targets
        )

        assertEquals(7, result.totalCalendarDays)
        assertEquals(0, result.trackedDaysCount)
        assertFalse(result.hasSufficientData)
        assertEquals(0f, result.averageCalories, 0.01f)
        assertEquals(0f, result.averageProtein, 0.01f)
        assertEquals(0, result.proteinAdherenceDays)
        assertEquals(0f, result.proteinAdherencePercent, 0.01f)
        assertEquals(0, result.calorieRangeDays)
        assertEquals(0f, result.calorieRangePercent, 0.01f)
        assertNull(result.highestCalorieDay)
        assertNull(result.lowestCalorieDay)
        assertTrue(result.insights.isNotEmpty())
        assertTrue(result.insights.first().contains("No meal records"))
    }

    @Test
    fun testBuildAnalyticsDataWithRealMeals() {
        val cal = Calendar.getInstance().apply {
            set(2026, Calendar.OCTOBER, 10, 12, 0, 0)
        }
        val currentTime = cal.timeInMillis

        val buckets = dummyCalculateUseCase.calculateDayBuckets(7, currentTime)
        val targets = NutritionTargets(calories = 2000f, protein = 140f, carbs = 220f, fat = 65f)

        fun createMeal(bucketIndex: Int, cals: Float, prot: Float, carbs: Float, fat: Float): MealEntry {
            val bucket = buckets[bucketIndex]
            val food = FoodItem(
                id = "food_$bucketIndex",
                name = "Test Food $bucketIndex",
                calories = cals,
                protein = prot,
                carbs = carbs,
                fat = fat,
                servingSize = 100f,
                servingUnit = "g"
            )
            return MealEntry(
                id = "meal_$bucketIndex",
                userId = "user_1",
                foodItem = food,
                date = bucket.startOfDay + 3600000L, // 1 hour into the day
                mealType = "Lunch",
                quantity = 100f,
                loggedAt = bucket.startOfDay + 3600000L
            )
        }

        // Add meals to buckets 0, 1, 2, 4, 6 (5 tracked days out of 7)
        // Bucket 0: 2100 cals, 145g prot (in range 1800-2200, prot met >= 140)
        // Bucket 1: 1950 cals, 150g prot (in range, prot met)
        // Bucket 2: 2500 cals, 130g prot (over range, prot not met)
        // Bucket 4: 1600 cals, 100g prot (under range, prot not met)
        // Bucket 6: 2050 cals, 142g prot (in range, prot met)
        // Buckets 3 and 5: untracked
        val meals = listOf(
            createMeal(0, 2100f, 145f, 200f, 60f),
            createMeal(1, 1950f, 150f, 190f, 55f),
            createMeal(2, 2500f, 130f, 250f, 80f),
            createMeal(4, 1600f, 100f, 150f, 50f),
            createMeal(6, 2050f, 142f, 210f, 65f)
        )

        val result = dummyCalculateUseCase.buildAnalyticsData(
            timeRange = AnalyticsTimeRange.LAST_7_DAYS,
            dayBuckets = buckets,
            meals = meals,
            targets = targets
        )

        assertEquals(7, result.totalCalendarDays)
        assertEquals(5, result.trackedDaysCount)
        assertTrue(result.hasSufficientData)

        // Average calories: (2100 + 1950 + 2500 + 1600 + 2050) / 5 = 10200 / 5 = 2040.0
        assertEquals(2040f, result.averageCalories, 0.01f)

        // Protein adherence: 3 of 5 met target (145, 150, 142) -> 60.0%
        assertEquals(3, result.proteinAdherenceDays)
        assertEquals(60f, result.proteinAdherencePercent, 0.01f)

        // Calorie target range (90%-110% of 2000 is 1800..2200):
        // 2100 (yes), 1950 (yes), 2500 (no), 1600 (no), 2050 (yes) -> 3 of 5 -> 60.0%
        assertEquals(3, result.calorieRangeDays)
        assertEquals(60f, result.calorieRangePercent, 0.01f)

        // Highest and lowest
        assertNotNull(result.highestCalorieDay)
        assertEquals(2500f, result.highestCalorieDay!!.calories, 0.01f)
        assertNotNull(result.lowestCalorieDay)
        assertEquals(1600f, result.lowestCalorieDay!!.calories, 0.01f)

        // Check untracked days (buckets 3 and 5)
        assertFalse(result.dailyRecords[3].hasData)
        assertFalse(result.dailyRecords[5].hasData)

        // Check insights
        assertTrue(result.insights.isNotEmpty())
        val insightsString = result.insights.joinToString(" ")
        assertTrue(insightsString.contains("Highest calorie intake"))
        assertTrue(insightsString.contains("Lowest calorie intake"))
        assertTrue(insightsString.contains("Protein target"))
        assertTrue(insightsString.contains("5 of 7 calendar days"))
    }
}
