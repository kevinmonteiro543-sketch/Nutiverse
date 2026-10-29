package com.monteirogames.nutriverse

import com.monteirogames.nutriverse.data.catalog.BuiltInFoodCatalog
import com.monteirogames.nutriverse.data.repository.FoodCatalogRepositoryImpl
import com.monteirogames.nutriverse.domain.model.FoodItem
import com.monteirogames.nutriverse.domain.model.MealEntry
import com.monteirogames.nutriverse.domain.repository.MealRepository
import com.monteirogames.nutriverse.domain.usecase.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.math.roundToInt

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class BuiltInFoodCatalogTest {

    private lateinit var repository: FoodCatalogRepositoryImpl
    private lateinit var searchUseCase: SearchCatalogFoodsUseCase
    private lateinit var getCategoriesUseCase: GetCatalogCategoriesUseCase
    private lateinit var getFoodByIdUseCase: GetCatalogFoodByIdUseCase

    @Before
    fun setup() {
        repository = FoodCatalogRepositoryImpl()
        searchUseCase = SearchCatalogFoodsUseCase(repository)
        getCategoriesUseCase = GetCatalogCategoriesUseCase(repository)
        getFoodByIdUseCase = GetCatalogFoodByIdUseCase(repository)
    }

    // 1. Catalog is non-empty and contains intended common-food categories
    @Test
    fun testCatalogIsNonEmptyAndContainsIntendedCategories() {
        val foods = repository.getAllFoods()
        assertTrue("Catalog should contain at least 50 foods", foods.size >= 50)
        assertTrue("Catalog should contain between 50 and 100 foods", foods.size in 50..100)

        val categories = getCategoriesUseCase()
        assertTrue(categories.contains("Grains & Breads"))
        assertTrue(categories.contains("Lentils & Pulses"))
        assertTrue(categories.contains("Dairy"))
        assertTrue(categories.contains("Eggs & Meat"))
        assertTrue(categories.contains("Vegetables"))
        assertTrue(categories.contains("Fruits"))
        assertTrue(categories.contains("Nuts & Seeds"))
        assertTrue(categories.contains("Oils & Fats"))
        assertTrue(categories.contains("Snacks & Dishes"))
        assertTrue(categories.contains("Beverages"))
    }

    // 2. Catalog IDs are unique
    @Test
    fun testCatalogIdsAreUnique() {
        val foods = repository.getAllFoods()
        val ids = foods.map { it.id }
        val uniqueIds = ids.toSet()
        assertEquals("Every catalog food item must have a unique ID", ids.size, uniqueIds.size)
    }

    // 3. Nutrition values are valid and non-negative
    @Test
    fun testNutritionValuesAreValidAndNonNegative() {
        val foods = repository.getAllFoods()
        for (food in foods) {
            assertTrue("Calories must be >= 0 for ${food.name}", food.caloriesPer100g >= 0f)
            assertTrue("Protein must be >= 0 for ${food.name}", food.proteinPer100g >= 0f)
            assertTrue("Carbs must be >= 0 for ${food.name}", food.carbsPer100g >= 0f)
            assertTrue("Fat must be >= 0 for ${food.name}", food.fatPer100g >= 0f)
            assertTrue("Name must not be blank for ID ${food.id}", food.name.isNotBlank())
            assertTrue("Category must not be blank for ID ${food.id}", food.category.isNotBlank())
            assertTrue("Default quantity must be > 0 for ${food.name}", food.defaultQuantityGrams > 0f)
        }
    }

    // 4. Raw and cooked variants are distinguishable
    @Test
    fun testRawAndCookedVariantsAreDistinguishable() {
        val riceCooked = getFoodByIdUseCase("cat_rice_cooked")
        val riceRaw = getFoodByIdUseCase("cat_rice_raw")
        assertNotNull(riceCooked)
        assertNotNull(riceRaw)
        assertTrue(riceCooked!!.isCooked)
        assertFalse(riceRaw!!.isCooked)
        assertNotEquals(riceCooked.caloriesPer100g, riceRaw.caloriesPer100g)

        val oatsDry = getFoodByIdUseCase("cat_oats_rolled_dry")
        val oatsCooked = getFoodByIdUseCase("cat_oats_cooked_water")
        assertNotNull(oatsDry)
        assertNotNull(oatsCooked)
        assertFalse(oatsDry!!.isCooked)
        assertTrue(oatsCooked!!.isCooked)

        val chickpeasRaw = getFoodByIdUseCase("cat_chickpeas_raw")
        val chickpeasBoiled = getFoodByIdUseCase("cat_chickpeas_boiled")
        assertNotNull(chickpeasRaw)
        assertNotNull(chickpeasBoiled)
        assertFalse(chickpeasRaw!!.isCooked)
        assertTrue(chickpeasBoiled!!.isCooked)
    }

    // 5. Search is case-insensitive
    @Test
    fun testSearchIsCaseInsensitive() {
        val upperResults = searchUseCase("ROTI")
        val lowerResults = searchUseCase("roti")
        val mixedResults = searchUseCase("RoTi")

        assertFalse(upperResults.isEmpty())
        assertEquals(upperResults.size, lowerResults.size)
        assertEquals(lowerResults.size, mixedResults.size)
        assertTrue(lowerResults.any { it.name.contains("Roti", ignoreCase = true) })
    }

    // 6. Search handles empty input
    @Test
    fun testSearchHandlesEmptyInput() {
        val allFoods = repository.getAllFoods()
        val emptySearchResults = searchUseCase("")
        val whitespaceSearchResults = searchUseCase("   ")

        assertEquals(allFoods.size, emptySearchResults.size)
        assertEquals(allFoods.size, whitespaceSearchResults.size)
    }

    // 7. Category filtering works
    @Test
    fun testCategoryFilteringWorks() {
        val dairyFoods = searchUseCase(query = "", category = "Dairy")
        assertFalse(dairyFoods.isEmpty())
        assertTrue(dairyFoods.all { it.category == "Dairy" })

        val lentilsFoods = searchUseCase(query = "", category = "Lentils & Pulses")
        assertFalse(lentilsFoods.isEmpty())
        assertTrue(lentilsFoods.all { it.category == "Lentils & Pulses" })

        val allCategoryFoods = searchUseCase(query = "", category = "All")
        assertEquals(repository.getAllFoods().size, allCategoryFoods.size)

        // Combined search and category filter
        val paneerInDairy = searchUseCase(query = "paneer", category = "Dairy")
        assertFalse(paneerInDairy.isEmpty())
        assertTrue(paneerInDairy.all { it.category == "Dairy" && it.name.contains("paneer", ignoreCase = true) })
    }

    // 8. Per-100-g nutrition calculations are correct
    @Test
    fun testPer100gNutritionCalculationIsExact() {
        val roti = getFoodByIdUseCase("cat_roti_chapati")!!
        val nutrition100g = roti.calculateNutrition(100f)

        assertEquals(roti.caloriesPer100g, nutrition100g.calories, 0.001f)
        assertEquals(roti.proteinPer100g, nutrition100g.protein, 0.001f)
        assertEquals(roti.carbsPer100g, nutrition100g.carbs, 0.001f)
        assertEquals(roti.fatPer100g, nutrition100g.fat, 0.001f)
    }

    // 9. Different quantities scale nutrition correctly
    @Test
    fun testQuantitiesScaleNutritionDeterministically() {
        val egg = getFoodByIdUseCase("cat_egg_whole_boiled")!!
        // 1 egg is 50g -> exactly 0.5x of per 100g values
        val egg50g = egg.calculateNutrition(50f)
        assertEquals(egg.caloriesPer100g * 0.5f, egg50g.calories, 0.01f)
        assertEquals(egg.proteinPer100g * 0.5f, egg50g.protein, 0.01f)
        assertEquals(egg.carbsPer100g * 0.5f, egg50g.carbs, 0.01f)
        assertEquals(egg.fatPer100g * 0.5f, egg50g.fat, 0.01f)

        // 200g portion -> exactly 2.0x of per 100g values
        val rice = getFoodByIdUseCase("cat_rice_cooked")!!
        val rice200g = rice.calculateNutrition(200f)
        assertEquals(rice.caloriesPer100g * 2.0f, rice200g.calories, 0.01f)
        assertEquals(rice.proteinPer100g * 2.0f, rice200g.protein, 0.01f)
        assertEquals(rice.carbsPer100g * 2.0f, rice200g.carbs, 0.01f)
        assertEquals(rice.fatPer100g * 2.0f, rice200g.fat, 0.01f)
    }

    // 10. Invalid quantities are rejected
    @Test
    fun testInvalidQuantitiesAreRejected() = runBlocking {
        val fakeMealRepo = FakeMealRepository()
        val mealTrackerUseCases = createFakeMealTrackerUseCases(fakeMealRepo)
        val addCatalogFoodUseCase = AddCatalogFoodToMealUseCase(mealTrackerUseCases)
        val roti = getFoodByIdUseCase("cat_roti_chapati")!!

        val zeroResult = addCatalogFoodUseCase("user1", roti, 0f, "Lunch")
        assertTrue("Zero quantity must be rejected", zeroResult.isFailure)

        val negativeResult = addCatalogFoodUseCase("user1", roti, -50f, "Lunch")
        assertTrue("Negative quantity must be rejected", negativeResult.isFailure)

        val excessiveResult = addCatalogFoodUseCase("user1", roti, 6000f, "Lunch")
        assertTrue("Quantity > 5000g must be rejected", excessiveResult.isFailure)
    }

    // 11. Rounding is applied only for display where applicable
    @Test
    fun testInternalPrecisionPreservedBeforeDisplayRounding() {
        val chicken = getFoodByIdUseCase("cat_chicken_breast_cooked")!!
        // 75g portion -> factor 0.75
        val nutrition = chicken.calculateNutrition(75f)
        // 165 * 0.75 = 123.75
        assertEquals(123.75f, nutrition.calories, 0.001f)
        // Display rounding
        assertEquals(124, nutrition.calories.roundToInt())
    }

    // 12. Adding a selected food uses the existing meal-tracking flow
    @Test
    fun testAddingCatalogFoodSavesThroughExistingMealRepository() = runBlocking {
        val fakeMealRepo = FakeMealRepository()
        val mealTrackerUseCases = createFakeMealTrackerUseCases(fakeMealRepo)
        val addCatalogFoodUseCase = AddCatalogFoodToMealUseCase(mealTrackerUseCases)
        val dal = getFoodByIdUseCase("cat_dal_yellow_cooked")!!

        val result = addCatalogFoodUseCase(
            userId = "test_user_777",
            catalogFood = dal,
            quantityInGrams = 150f,
            mealType = "Dinner",
            date = 1760000000000L
        )

        assertTrue(result.isSuccess)
        val savedMeal = result.getOrThrow()
        assertEquals("test_user_777", savedMeal.userId)
        assertEquals(dal.name, savedMeal.foodItem.name)
        assertEquals(150f, savedMeal.quantity, 0.01f)
        assertEquals("Dinner", savedMeal.mealType)

        // Verify repository received it
        assertEquals(1, fakeMealRepo.savedMeals.size)
        val repoMeal = fakeMealRepo.savedMeals.first()
        assertEquals(savedMeal.id, repoMeal.id)
        assertEquals(150f, repoMeal.quantity, 0.01f)

        // Verify FoodItem was saved with per 100g basis and serving size 100
        assertNotNull(fakeMealRepo.savedFoodItem)
        assertEquals(100f, fakeMealRepo.savedFoodItem!!.servingSize, 0.01f)
        assertEquals("g", fakeMealRepo.savedFoodItem!!.servingUnit)

        // Calculated calories via existing MealEntry property matches
        val expectedCalories = dal.caloriesPer100g * (150f / 100f)
        assertEquals(expectedCalories, repoMeal.calculatedCalories, 0.01f)
    }

    // 13. Meal type and selected date are preserved
    @Test
    fun testMealTypeAndDatePreserved() = runBlocking {
        val fakeMealRepo = FakeMealRepository()
        val mealTrackerUseCases = createFakeMealTrackerUseCases(fakeMealRepo)
        val addCatalogFoodUseCase = AddCatalogFoodToMealUseCase(mealTrackerUseCases)
        val apple = getFoodByIdUseCase("cat_apple")!!

        val customDate = 1765000000000L
        val result = addCatalogFoodUseCase(
            userId = "user_alpha",
            catalogFood = apple,
            quantityInGrams = 120f,
            mealType = "Breakfast",
            date = customDate
        )

        assertTrue(result.isSuccess)
        val meal = result.getOrThrow()
        assertEquals("Breakfast", meal.mealType)
        assertEquals(customDate, meal.date)
    }

    // 14. Daily nutrition totals update correctly
    @Test
    fun testDailyNutritionTotalsUpdateCorrectly() = runBlocking {
        val fakeMealRepo = FakeMealRepository()
        val mealTrackerUseCases = createFakeMealTrackerUseCases(fakeMealRepo)
        val addCatalogFoodUseCase = AddCatalogFoodToMealUseCase(mealTrackerUseCases)

        val roti = getFoodByIdUseCase("cat_roti_chapati")!! // 297 kcal, 9g P, 55g C, 3.5g F per 100g
        val dal = getFoodByIdUseCase("cat_dal_yellow_cooked")!! // 98 kcal, 5.5g P, 12.5g C, 3.0g F per 100g

        val today = 1760000000000L
        // Log 80g Roti: 237.6 kcal, 7.2g P, 44g C, 2.8g F
        addCatalogFoodUseCase("user_totals", roti, 80f, "Lunch", today)
        // Log 150g Dal: 147 kcal, 8.25g P, 18.75g C, 4.5g F
        addCatalogFoodUseCase("user_totals", dal, 150f, "Lunch", today)

        val dailyNutrition = mealTrackerUseCases.calculateDailyNutrition("user_totals", today).first()

        val expectedCalories = (297f * 0.8f) + (98f * 1.5f)
        val expectedProtein = (9.0f * 0.8f) + (5.5f * 1.5f)
        val expectedCarbs = (55.0f * 0.8f) + (12.5f * 1.5f)
        val expectedFat = (3.5f * 0.8f) + (3.0f * 1.5f)

        assertEquals(expectedCalories, dailyNutrition.calories, 0.1f)
        assertEquals(expectedProtein, dailyNutrition.protein, 0.1f)
        assertEquals(expectedCarbs, dailyNutrition.carbs, 0.1f)
        assertEquals(expectedFat, dailyNutrition.fat, 0.1f)
    }

    // 15. Existing user isolation remains intact
    @Test
    fun testUserIsolationRemainsIntact() = runBlocking {
        val fakeMealRepo = FakeMealRepository()
        val mealTrackerUseCases = createFakeMealTrackerUseCases(fakeMealRepo)
        val addCatalogFoodUseCase = AddCatalogFoodToMealUseCase(mealTrackerUseCases)

        val egg = getFoodByIdUseCase("cat_egg_whole_boiled")!!
        val today = 1760000000000L

        addCatalogFoodUseCase("user_A", egg, 50f, "Breakfast", today)
        addCatalogFoodUseCase("user_B", egg, 100f, "Breakfast", today)

        val userAMeals = mealTrackerUseCases.observeTodayMeals("user_A", today).first()
        val userBMeals = mealTrackerUseCases.observeTodayMeals("user_B", today).first()

        assertEquals(1, userAMeals.size)
        assertEquals("user_A", userAMeals.first().userId)
        assertEquals(50f, userAMeals.first().quantity, 0.01f)

        assertEquals(1, userBMeals.size)
        assertEquals("user_B", userBMeals.first().userId)
        assertEquals(100f, userBMeals.first().quantity, 0.01f)
    }

    // ==========================================
    // Test Fakes & Helpers
    // ==========================================

    private fun createFakeMealTrackerUseCases(repository: MealRepository): MealTrackerUseCases {
        return MealTrackerUseCases(
            addMeal = AddMealUseCase(repository),
            updateMeal = UpdateMealUseCase(repository),
            deleteMeal = DeleteMealUseCase(repository),
            getMeal = GetMealEntryUseCase(repository),
            observeTodayMeals = ObserveTodayMealsUseCase(repository),
            calculateDailyNutrition = CalculateDailyNutritionUseCase(repository)
        )
    }

    private class FakeMealRepository : MealRepository {
        val savedMeals = mutableListOf<MealEntry>()
        var savedFoodItem: FoodItem? = null

        override suspend fun saveFoodItem(foodItem: FoodItem) {
            savedFoodItem = foodItem
        }

        override suspend fun getFoodItemById(id: String): FoodItem? {
            return savedFoodItem?.takeIf { it.id == id }
        }

        override suspend fun saveMealEntry(mealEntry: MealEntry) {
            savedMeals.add(mealEntry)
        }

        override suspend fun updateMealEntry(mealEntry: MealEntry) {
            val idx = savedMeals.indexOfFirst { it.id == mealEntry.id }
            if (idx >= 0) savedMeals[idx] = mealEntry else savedMeals.add(mealEntry)
        }

        override suspend fun deleteMealEntry(mealEntry: MealEntry) {
            savedMeals.removeAll { it.id == mealEntry.id }
        }

        override suspend fun getMealEntryById(id: String, userId: String): MealEntry? {
            return savedMeals.find { it.id == id && it.userId == userId }
        }

        override suspend fun getAllMeals(userId: String): List<MealEntry> {
            return savedMeals.filter { it.userId == userId }
        }

        override fun getMealsForDate(userId: String, timestamp: Long): Flow<List<MealEntry>> {
            return flowOf(savedMeals.filter { it.userId == userId })
        }

        override fun getMealsForDateRange(userId: String, startDate: Long, endDate: Long): Flow<List<MealEntry>> {
            return flowOf(savedMeals.filter { it.userId == userId && it.date in startDate..endDate })
        }
    }
}
