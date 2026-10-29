package com.monteirogames.nutriverse

import com.monteirogames.nutriverse.data.local.entity.toDomain
import com.monteirogames.nutriverse.data.local.entity.toEntity
import com.monteirogames.nutriverse.domain.model.*
import com.monteirogames.nutriverse.domain.repository.MealRepository
import com.monteirogames.nutriverse.domain.repository.UserProfileRepository
import com.monteirogames.nutriverse.domain.repository.WeeklyReviewAnalysisRepository
import com.monteirogames.nutriverse.domain.repository.WeeklyReviewRepository
import com.monteirogames.nutriverse.domain.usecase.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class WeeklyAiReviewTest {

    // Helper to generate calendar timestamps
    private fun createTimestamp(year: Int, month: Int, day: Int, hour: Int = 12): Long {
        return Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month)
            set(Calendar.DAY_OF_MONTH, day)
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    // ==========================================
    // 1. Period Helper Tests
    // ==========================================

    @Test
    fun testPeriodCalculationExcludesTodayAndSpans7Days() {
        val currentFixedTime = createTimestamp(2026, Calendar.OCTOBER, 7, 15) // Oct 7, 2026
        val period = WeeklyReviewPeriodHelper.calculatePreviousWeekPeriod(currentFixedTime)

        val isoFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

        // Yesterday should be Oct 6, 2026
        assertEquals("2026-10-06", period.endDate)
        // 6 days before yesterday should be Sep 30, 2026 (7 days total: Sep 30 - Oct 6)
        assertEquals("2026-09-30", period.startDate)

        // Verify timestamps
        val endCal = Calendar.getInstance().apply { timeInMillis = period.endTimestamp }
        assertEquals(2026, endCal.get(Calendar.YEAR))
        assertEquals(Calendar.OCTOBER, endCal.get(Calendar.MONTH))
        assertEquals(6, endCal.get(Calendar.DAY_OF_MONTH))
        assertEquals(23, endCal.get(Calendar.HOUR_OF_DAY))
        assertEquals(59, endCal.get(Calendar.MINUTE))
        assertEquals(59, endCal.get(Calendar.SECOND))

        val startCal = Calendar.getInstance().apply { timeInMillis = period.startTimestamp }
        assertEquals(2026, startCal.get(Calendar.YEAR))
        assertEquals(Calendar.SEPTEMBER, startCal.get(Calendar.MONTH))
        assertEquals(30, startCal.get(Calendar.DAY_OF_MONTH))
        assertEquals(0, startCal.get(Calendar.HOUR_OF_DAY))
        assertEquals(0, startCal.get(Calendar.MINUTE))

        // Range format should contain both months
        assertTrue(period.displayRange.contains("Sep 30"))
        assertTrue(period.displayRange.contains("Oct 6"))
    }

    @Test
    fun testPeriodCalculationHandlesNewYearBoundary() {
        val newYearDay = createTimestamp(2027, Calendar.JANUARY, 3, 10) // Jan 3, 2027
        val period = WeeklyReviewPeriodHelper.calculatePreviousWeekPeriod(newYearDay)

        assertEquals("2027-01-02", period.endDate)
        assertEquals("2026-12-27", period.startDate)
        assertTrue(period.displayRange.contains("2026"))
        assertTrue(period.displayRange.contains("2027"))
    }

    // ==========================================
    // 2. Metrics Calculation Tests
    // ==========================================

    @Test
    fun testMetricsCalculationWithZeroMeals() = runBlocking {
        val currentFixedTime = createTimestamp(2026, Calendar.OCTOBER, 7, 12)
        val period = WeeklyReviewPeriodHelper.calculatePreviousWeekPeriod(currentFixedTime)

        val fakeMealRepo = FakeTestMealRepository(emptyList())
        val fakeProfileRepo = FakeTestUserProfileRepository()

        val useCase = CalculateWeeklyReviewMetricsUseCase(
            mealRepository = fakeMealRepo,
            userProfileRepository = fakeProfileRepo,
            calculateNutritionTargetsUseCase = CalculateNutritionTargetsUseCase()
        )

        val metrics = useCase("user_123", currentFixedTime)

        assertEquals(0, metrics.trackedDays)
        assertEquals(7, metrics.totalDays)
        assertEquals(0f, metrics.averageCalories, 0.01f)
        assertEquals(0f, metrics.averageProtein, 0.01f)
        assertEquals(0f, metrics.trackingConsistency, 0.01f)
        assertEquals(0, metrics.totalMeals)
        assertNull(metrics.highestCalorieDay)
        assertNull(metrics.lowestCalorieDay)
    }

    @Test
    fun testMetricsCalculationOnlyAveragesAcrossTrackedDays() = runBlocking {
        val currentFixedTime = createTimestamp(2026, Calendar.OCTOBER, 7, 12)
        // Period is Sep 30, 2026 to Oct 6, 2026 (7 days)
        val food = FoodItem(
            id = "f1",
            name = "Chicken Rice",
            calories = 500f,
            protein = 40f,
            carbs = 60f,
            fat = 10f,
            servingSize = 1f,
            servingUnit = "serving"
        )

        // Log 3 days: Oct 1, Oct 2, Oct 3
        val date1 = createTimestamp(2026, Calendar.OCTOBER, 1, 13)
        val meal1 = MealEntry(
            id = "m1", userId = "user_123", foodItem = food, quantity = 2f,
            mealType = "Lunch", date = date1, loggedAt = date1 // 1000 kcal, 80g protein
        )
        val date2 = createTimestamp(2026, Calendar.OCTOBER, 2, 19)
        val meal2 = MealEntry(
            id = "m2", userId = "user_123", foodItem = food, quantity = 4f,
            mealType = "Dinner", date = date2, loggedAt = date2 // 2000 kcal, 160g protein
        )
        val date3 = createTimestamp(2026, Calendar.OCTOBER, 3, 12)
        val meal3 = MealEntry(
            id = "m3", userId = "user_123", foodItem = food, quantity = 3f,
            mealType = "Lunch", date = date3, loggedAt = date3 // 1500 kcal, 120g protein
        )

        val fakeMealRepo = FakeTestMealRepository(listOf(meal1, meal2, meal3))
        val fakeProfileRepo = FakeTestUserProfileRepository()

        val useCase = CalculateWeeklyReviewMetricsUseCase(
            mealRepository = fakeMealRepo,
            userProfileRepository = fakeProfileRepo,
            calculateNutritionTargetsUseCase = CalculateNutritionTargetsUseCase()
        )

        val metrics = useCase("user_123", currentFixedTime)

        assertEquals(3, metrics.trackedDays)
        assertEquals(7, metrics.totalDays)
        assertEquals(3, metrics.totalMeals)

        // Consistency: 3/7 * 100 = 42.857%
        assertEquals(42.857f, metrics.trackingConsistency, 0.1f)

        // Daily calorie averages across 3 tracked days only: (1000 + 2000 + 1500) / 3 = 1500 kcal
        // (Must NOT be divided by 7!)
        assertEquals(1500f, metrics.averageCalories, 0.01f)

        // Protein average across 3 tracked days: (80 + 160 + 120) / 3 = 120g
        assertEquals(120f, metrics.averageProtein, 0.01f)

        // Highest day should be Oct 2 (2000 kcal)
        assertNotNull(metrics.highestCalorieDay)
        assertTrue(metrics.highestCalorieDay!!.contains("2000"))

        // Lowest day should be Oct 1 (1000 kcal)
        assertNotNull(metrics.lowestCalorieDay)
        assertTrue(metrics.lowestCalorieDay!!.contains("1000"))
    }

    // ==========================================
    // 3. Minimum Tracking Requirement & AI Generation
    // ==========================================

    @Test
    fun testGenerateReviewFailsWhenLessThan2DaysTracked() = runBlocking {
        val currentFixedTime = createTimestamp(2026, Calendar.OCTOBER, 7, 12)
        val food = FoodItem(
            id = "f1", name = "Salad", calories = 300f, protein = 15f, carbs = 20f, fat = 5f,
            servingSize = 1f, servingUnit = "serving"
        )

        // Only 1 day tracked
        val date1 = createTimestamp(2026, Calendar.OCTOBER, 2, 12)
        val meal1 = MealEntry(
            id = "m1", userId = "user_123", foodItem = food, quantity = 1f,
            mealType = "Lunch", date = date1, loggedAt = date1
        )

        val fakeMealRepo = FakeTestMealRepository(listOf(meal1))
        val fakeProfileRepo = FakeTestUserProfileRepository()
        val fakeReviewRepo = FakeTestWeeklyReviewRepository()
        val fakeAnalysisRepo = FakeTestWeeklyReviewAnalysisRepository()

        val metricsUseCase = CalculateWeeklyReviewMetricsUseCase(
            mealRepository = fakeMealRepo,
            userProfileRepository = fakeProfileRepo,
            calculateNutritionTargetsUseCase = CalculateNutritionTargetsUseCase()
        )

        val generateUseCase = GenerateWeeklyReviewUseCase(
            calculateMetricsUseCase = metricsUseCase,
            weeklyReviewRepository = fakeReviewRepo,
            weeklyReviewAnalysisRepository = fakeAnalysisRepo
        )

        val result = generateUseCase("user_123", forceRegenerate = false, currentTimeMillis = currentFixedTime)

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is InsufficientReviewDataException)
        assertTrue(result.exceptionOrNull()!!.message!!.contains("at least 2 days"))
        assertEquals(0, fakeAnalysisRepo.callCount) // Gemini must not be called
    }

    @Test
    fun testGenerateReviewSucceedsAndCachesReviewForSamePeriod() = runBlocking {
        val currentFixedTime = createTimestamp(2026, Calendar.OCTOBER, 7, 12)
        val food = FoodItem(
            id = "f1", name = "Oatmeal", calories = 400f, protein = 25f, carbs = 50f, fat = 8f,
            servingSize = 1f, servingUnit = "serving"
        )

        // 2 days tracked
        val date1 = createTimestamp(2026, Calendar.OCTOBER, 1, 9)
        val meal1 = MealEntry(
            id = "m1", userId = "user_123", foodItem = food, quantity = 2f,
            mealType = "Breakfast", date = date1, loggedAt = date1
        )
        val date2 = createTimestamp(2026, Calendar.OCTOBER, 2, 9)
        val meal2 = MealEntry(
            id = "m2", userId = "user_123", foodItem = food, quantity = 2f,
            mealType = "Breakfast", date = date2, loggedAt = date2
        )

        val fakeMealRepo = FakeTestMealRepository(listOf(meal1, meal2))
        val fakeProfileRepo = FakeTestUserProfileRepository()
        val fakeReviewRepo = FakeTestWeeklyReviewRepository()
        val fakeAnalysisRepo = FakeTestWeeklyReviewAnalysisRepository()

        val metricsUseCase = CalculateWeeklyReviewMetricsUseCase(
            mealRepository = fakeMealRepo,
            userProfileRepository = fakeProfileRepo,
            calculateNutritionTargetsUseCase = CalculateNutritionTargetsUseCase()
        )

        val generateUseCase = GenerateWeeklyReviewUseCase(
            calculateMetricsUseCase = metricsUseCase,
            weeklyReviewRepository = fakeReviewRepo,
            weeklyReviewAnalysisRepository = fakeAnalysisRepo
        )

        // First generation -> calls AI
        val firstResult = generateUseCase("user_123", forceRegenerate = false, currentTimeMillis = currentFixedTime)
        assertTrue(firstResult.isSuccess)
        val review = firstResult.getOrThrow()
        assertEquals("user_123", review.userId)
        assertEquals(1, fakeAnalysisRepo.callCount)
        assertNotNull(fakeReviewRepo.savedReview)

        // Second generation without force -> returns cached review, does NOT call AI again
        val secondResult = generateUseCase("user_123", forceRegenerate = false, currentTimeMillis = currentFixedTime)
        assertTrue(secondResult.isSuccess)
        assertEquals(review.id, secondResult.getOrThrow().id)
        assertEquals(1, fakeAnalysisRepo.callCount) // Still 1

        // Third generation with forceRegenerate = true -> calls AI again
        val thirdResult = generateUseCase("user_123", forceRegenerate = true, currentTimeMillis = currentFixedTime)
        assertTrue(thirdResult.isSuccess)
        assertEquals(2, fakeAnalysisRepo.callCount) // Incremented to 2
    }

    // ==========================================
    // 4. Parser & Markdown Sanitization Tests
    // ==========================================

    @Test
    fun testParserHandlesStandardValidJson() {
        val json = """
            {
              "summary": "You demonstrated solid tracking consistency this week.",
              "strengths": ["Met protein goal on 4 days", "Consistent breakfast logging"],
              "improvements": ["Try to log weekend dinners", "Increase hydration"],
              "nutritionInsights": ["Macronutrient distribution was well balanced."],
              "recommendations": ["Prepare high-protein snacks in advance."],
              "nextWeekFocus": ["Log meals across all 7 days."]
            }
        """.trimIndent()

        val result = WeeklyReviewAnalysisParser.parse(json)
        assertTrue(result.isSuccess)
        val response = result.getOrThrow()

        assertEquals("You demonstrated solid tracking consistency this week.", response.summary)
        assertEquals(2, response.strengths.size)
        assertEquals(2, response.improvements.size)
        assertEquals(1, response.nutritionInsights.size)
        assertEquals(1, response.recommendations.size)
        assertEquals(1, response.nextWeekFocus.size)
    }

    @Test
    fun testParserSanitizesMarkdownCodeBlocks() {
        val markdownWrapped = """
            ```json
            {
              "summary": "Great job tracking your nutrition!",
              "strengths": ["Strong protein intake"],
              "improvements": ["Include more vegetables"],
              "nutritionInsights": ["Calorie intake aligned closely with your target."],
              "recommendations": ["Add leafy greens to dinner."],
              "nextWeekFocus": ["Maintain steady daily logging."]
            }
            ```
        """.trimIndent()

        val result = WeeklyReviewAnalysisParser.parse(markdownWrapped)
        assertTrue(result.isSuccess)
        val response = result.getOrThrow()
        assertEquals("Great job tracking your nutrition!", response.summary)
        assertEquals("Strong protein intake", response.strengths.first())
    }

    @Test
    fun testParserProvidesConstructiveDefaultsOnEmptyFields() {
        val partialJson = """
            {
              "summary": "Brief summary here."
            }
        """.trimIndent()

        val result = WeeklyReviewAnalysisParser.parse(partialJson)
        assertTrue(result.isSuccess)
        val response = result.getOrThrow()
        assertEquals("Brief summary here.", response.summary)
        assertFalse(response.strengths.isEmpty())
        assertFalse(response.improvements.isEmpty())
        assertFalse(response.recommendations.isEmpty())
    }

    // ==========================================
    // 5. Room Entity Mapping Tests
    // ==========================================

    @Test
    fun testEntityDomainMappingIsLossless() {
        val metrics = WeeklyReviewMetrics(
            startDate = "2026-09-30",
            endDate = "2026-10-06",
            startTimestamp = 1760000000000L,
            endTimestamp = 1760604799999L,
            displayRange = "Sep 30 – Oct 6, 2026",
            trackedDays = 5,
            totalDays = 7,
            averageCalories = 2100f,
            averageProtein = 140f,
            averageCarbs = 230f,
            averageFat = 65f,
            targetCalories = 2200f,
            targetProtein = 140f,
            targetCarbs = 250f,
            targetFat = 70f,
            calorieAdherence = 80f,
            proteinAdherence = 100f,
            trackingConsistency = 71.4f,
            totalMeals = 15,
            highestCalorieDay = "Friday (2,400 kcal)",
            lowestCalorieDay = "Tuesday (1,800 kcal)"
        )

        val domain = WeeklyReview(
            id = "review_test_id",
            userId = "user_456",
            startDate = "2026-09-30",
            endDate = "2026-10-06",
            generatedAt = 1760610000000L,
            metrics = metrics,
            summary = "Remarkable progress with consistent protein pacing.",
            strengths = listOf("Strength 1", "Strength 2"),
            improvements = listOf("Area 1"),
            nutritionInsights = listOf("Insight 1", "Insight 2"),
            recommendations = listOf("Rec 1"),
            nextWeekFocus = listOf("Focus 1")
        )

        val entity = domain.toEntity()
        val mappedBack = entity.toDomain()

        assertEquals(domain.id, mappedBack.id)
        assertEquals(domain.userId, mappedBack.userId)
        assertEquals(domain.startDate, mappedBack.startDate)
        assertEquals(domain.endDate, mappedBack.endDate)
        assertEquals(domain.summary, mappedBack.summary)
        assertEquals(domain.strengths, mappedBack.strengths)
        assertEquals(domain.improvements, mappedBack.improvements)
        assertEquals(domain.nutritionInsights, mappedBack.nutritionInsights)
        assertEquals(domain.recommendations, mappedBack.recommendations)
        assertEquals(domain.nextWeekFocus, mappedBack.nextWeekFocus)

        assertEquals(metrics.trackedDays, mappedBack.metrics.trackedDays)
        assertEquals(metrics.averageCalories, mappedBack.metrics.averageCalories, 0.01f)
        assertEquals(metrics.highestCalorieDay, mappedBack.metrics.highestCalorieDay)
        assertEquals(metrics.lowestCalorieDay, mappedBack.metrics.lowestCalorieDay)
    }

    // ==========================================
    // Test Fakes
    // ==========================================

    private class FakeTestMealRepository(
        private val meals: List<MealEntry>
    ) : MealRepository {
        override suspend fun saveFoodItem(foodItem: FoodItem) {}
        override suspend fun getFoodItemById(id: String): FoodItem? = null
        override suspend fun saveMealEntry(mealEntry: MealEntry) {}
        override suspend fun updateMealEntry(mealEntry: MealEntry) {}
        override suspend fun deleteMealEntry(mealEntry: MealEntry) {}
        override suspend fun getMealEntryById(id: String, userId: String): MealEntry? = null
        override suspend fun getAllMeals(userId: String): List<MealEntry> = meals
        override fun getMealsForDate(userId: String, timestamp: Long) = flowOf(meals)
        override fun getMealsForDateRange(userId: String, startDate: Long, endDate: Long): Flow<List<MealEntry>> {
            return flowOf(meals.filter { it.date in startDate..endDate })
        }
    }

    private class FakeTestUserProfileRepository : UserProfileRepository {
        override fun getUserProfile(userId: String): Flow<UserProfile?> {
            return flowOf(
                UserProfile(
                    userId = userId,
                    name = "Test User",
                    age = 28,
                    gender = "male",
                    heightCm = 178f,
                    weightKg = 75f,
                    goal = "maintenance",
                    dietaryPreference = "none",
                    allergies = "none",
                    activityLevel = "moderate"
                )
            )
        }
        override suspend fun saveProfile(profile: UserProfile) {}
        override suspend fun clearProfile() {}
    }

    private class FakeTestWeeklyReviewRepository : WeeklyReviewRepository {
        var savedReview: WeeklyReview? = null

        override suspend fun saveReview(review: WeeklyReview) {
            savedReview = review
        }

        override suspend fun getReviewForPeriod(userId: String, startDate: String, endDate: String): WeeklyReview? {
            return savedReview?.takeIf { it.userId == userId && it.startDate == startDate && it.endDate == endDate }
        }

        override fun getLatestReview(userId: String): Flow<WeeklyReview?> {
            return flowOf(savedReview)
        }

        override fun getAllReviews(userId: String): Flow<List<WeeklyReview>> {
            return flowOf(listOfNotNull(savedReview))
        }

        override fun getReviewById(id: String): Flow<WeeklyReview?> {
            return flowOf(savedReview?.takeIf { it.id == id })
        }

        override suspend fun deleteReviewById(id: String) {
            if (savedReview?.id == id) savedReview = null
        }
    }

    private class FakeTestWeeklyReviewAnalysisRepository : WeeklyReviewAnalysisRepository {
        var callCount = 0

        override suspend fun generateAiReview(metrics: WeeklyReviewMetrics): Result<WeeklyReviewAiResponse> {
            callCount++
            return Result.success(
                WeeklyReviewAiResponse(
                    summary = "AI coaching summary generated for ${metrics.displayRange}.",
                    strengths = listOf("Consistent meal timing", "Solid protein intake"),
                    improvements = listOf("Log hydration"),
                    nutritionInsights = listOf("Calories averaged ${metrics.averageCalories.toInt()} kcal"),
                    recommendations = listOf("Pre-portion lunches"),
                    nextWeekFocus = listOf("Maintain 5+ days tracking")
                )
            )
        }
    }
}
