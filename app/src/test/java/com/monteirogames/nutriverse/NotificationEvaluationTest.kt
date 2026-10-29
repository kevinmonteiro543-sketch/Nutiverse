package com.monteirogames.nutriverse

import com.monteirogames.nutriverse.domain.model.Budget
import com.monteirogames.nutriverse.domain.model.BudgetPeriod
import com.monteirogames.nutriverse.domain.model.FoodExpense
import com.monteirogames.nutriverse.domain.model.FoodItem
import com.monteirogames.nutriverse.domain.model.GroceryItem
import com.monteirogames.nutriverse.domain.model.MealEntry
import com.monteirogames.nutriverse.domain.model.NotificationSettings
import com.monteirogames.nutriverse.domain.model.NotificationType
import com.monteirogames.nutriverse.domain.model.PantryItem
import com.monteirogames.nutriverse.domain.model.UserProfile
import com.monteirogames.nutriverse.domain.repository.BudgetRepository
import com.monteirogames.nutriverse.domain.repository.ExpenseRepository
import com.monteirogames.nutriverse.domain.repository.GroceryRepository
import com.monteirogames.nutriverse.domain.repository.MealRepository
import com.monteirogames.nutriverse.domain.repository.NotificationRepository
import com.monteirogames.nutriverse.domain.repository.PantryRepository
import com.monteirogames.nutriverse.domain.repository.UserProfileRepository
import com.monteirogames.nutriverse.domain.usecase.CalculateBudgetStatusUseCase
import com.monteirogames.nutriverse.domain.usecase.CalculateNutritionTargetsUseCase
import com.monteirogames.nutriverse.domain.usecase.EvaluateNotificationsUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.Calendar

class NotificationEvaluationTest {

    private val sentKeys = mutableSetOf<String>()
    private val userSettingsMap = mutableMapOf<String, NotificationSettings>()
    private val mealsList = mutableListOf<MealEntry>()
    private val pantryList = mutableListOf<PantryItem>()
    private val groceryList = mutableListOf<GroceryItem>()
    private val expensesList = mutableListOf<FoodExpense>()
    private var activeBudget: Budget? = null
    private var testUserProfile: UserProfile? = null

    private val fakeNotificationRepo = object : NotificationRepository {
        override fun getNotificationSettings(userId: String): Flow<NotificationSettings> =
            flowOf(userSettingsMap[userId] ?: NotificationSettings(userId))

        override suspend fun getNotificationSettingsOnce(userId: String): NotificationSettings =
            userSettingsMap[userId] ?: NotificationSettings(userId)

        override suspend fun saveNotificationSettings(settings: NotificationSettings) {
            userSettingsMap[settings.userId] = settings
        }

        override suspend fun isNotificationSent(userId: String, deduplicationKey: String): Boolean =
            sentKeys.contains("${userId}_$deduplicationKey")

        override suspend fun recordNotificationSent(userId: String, deduplicationKey: String, type: String) {
            sentKeys.add("${userId}_$deduplicationKey")
        }

        override suspend fun clearUserRecords(userId: String) {
            sentKeys.removeIf { it.startsWith("${userId}_") }
        }
    }

    private val fakeMealRepo = object : MealRepository {
        override suspend fun saveFoodItem(foodItem: FoodItem) {}
        override suspend fun getFoodItemById(id: String): FoodItem? = null
        override suspend fun saveMealEntry(mealEntry: MealEntry) { mealsList.add(mealEntry) }
        override suspend fun updateMealEntry(mealEntry: MealEntry) {}
        override suspend fun deleteMealEntry(mealEntry: MealEntry) {}
        override suspend fun getMealEntryById(id: String, userId: String): MealEntry? = null
        override suspend fun getAllMeals(userId: String): List<MealEntry> =
            mealsList.filter { it.userId == userId }
        override fun getMealsForDate(userId: String, timestamp: Long): Flow<List<MealEntry>> =
            flowOf(mealsList.filter { it.userId == userId })

        override fun getMealsForDateRange(userId: String, startDate: Long, endDate: Long): Flow<List<MealEntry>> =
            flowOf(mealsList.filter { it.userId == userId && it.date in startDate..endDate })
    }

    private val fakeUserProfileRepo = object : UserProfileRepository {
        override suspend fun saveProfile(profile: UserProfile) { testUserProfile = profile }
        override fun getUserProfile(userId: String): Flow<UserProfile?> =
            flowOf(if (testUserProfile?.userId == userId) testUserProfile else null)
        override suspend fun clearProfile() { testUserProfile = null }
    }

    private val fakePantryRepo = object : PantryRepository {
        override suspend fun savePantryItem(pantryItem: PantryItem) {}
        override suspend fun updatePantryItem(pantryItem: PantryItem) {}
        override suspend fun deletePantryItem(pantryItem: PantryItem) {}
        override suspend fun getPantryItemById(id: String, userId: String): PantryItem? = null
        override suspend fun getAllPantryItems(userId: String): List<PantryItem> =
            pantryList.filter { it.userId == userId }
        override fun getPantryItems(userId: String): Flow<List<PantryItem>> =
            flowOf(pantryList.filter { it.userId == userId })
    }

    private val fakeGroceryRepo = object : GroceryRepository {
        override fun getGroceryItems(userId: String): Flow<List<GroceryItem>> =
            flowOf(groceryList.filter { it.userId == userId })
        override suspend fun getGroceryItemById(id: String, userId: String): GroceryItem? = null
        override suspend fun addGroceryItem(item: GroceryItem) {}
        override suspend fun updateGroceryItem(item: GroceryItem) {}
        override suspend fun deleteGroceryItem(item: GroceryItem) {}
        override suspend fun deleteGroceryItemById(id: String, userId: String) {}
        override suspend fun togglePurchased(id: String, userId: String, isPurchased: Boolean) {}
        override suspend fun clearPurchasedItems(userId: String) {}
        override suspend fun addMissingRecipeIngredients(userId: String, recipe: com.monteirogames.nutriverse.domain.model.Recipe, pantryItems: List<PantryItem>): Int = 0
    }

    private val fakeBudgetRepo = object : BudgetRepository {
        override fun getActiveBudget(userId: String): Flow<Budget?> =
            flowOf(if (activeBudget?.userId == userId) activeBudget else null)
        override suspend fun getActiveBudgetOnce(userId: String): Budget? =
            if (activeBudget?.userId == userId) activeBudget else null
        override fun getAllBudgets(userId: String): Flow<List<Budget>> = flowOf(emptyList())
        override suspend fun saveBudget(budget: Budget) { activeBudget = budget }
        override suspend fun deleteBudget(id: String, userId: String) {}
        override suspend fun deactivateAllBudgets(userId: String) {}
    }

    private val fakeExpenseRepo = object : ExpenseRepository {
        override fun getExpenses(userId: String): Flow<List<FoodExpense>> =
            flowOf(expensesList.filter { it.userId == userId })
        override fun getExpensesForPeriod(userId: String, startDate: Long, endDate: Long): Flow<List<FoodExpense>> =
            flowOf(expensesList.filter { it.userId == userId && it.date in startDate..endDate })
        override suspend fun getExpensesListForPeriod(userId: String, startDate: Long, endDate: Long): List<FoodExpense> =
            expensesList.filter { it.userId == userId && it.date in startDate..endDate }
        override suspend fun getExpenseById(id: String, userId: String): FoodExpense? = null
        override suspend fun getExpenseByGroceryItemId(groceryItemId: String, userId: String): FoodExpense? = null
        override suspend fun addExpense(expense: FoodExpense) {}
        override suspend fun updateExpense(expense: FoodExpense) {}
        override suspend fun deleteExpense(expense: FoodExpense) {}
        override suspend fun deleteExpenseById(id: String, userId: String) {}
        override fun getTotalSpending(userId: String): Flow<Double> = flowOf(expensesList.sumOf { it.amount })
        override fun getTotalSpendingForPeriod(userId: String, startDate: Long, endDate: Long): Flow<Double> =
            flowOf(expensesList.filter { it.date in startDate..endDate }.sumOf { it.amount })
    }

    private val calculateBudgetStatusUseCase = CalculateBudgetStatusUseCase(fakeBudgetRepo, fakeExpenseRepo)
    private val calculateNutritionTargetsUseCase = CalculateNutritionTargetsUseCase()

    private val evaluateNotificationsUseCase = EvaluateNotificationsUseCase(
        notificationRepository = fakeNotificationRepo,
        mealRepository = fakeMealRepo,
        userProfileRepository = fakeUserProfileRepo,
        calculateNutritionTargetsUseCase = calculateNutritionTargetsUseCase,
        pantryRepository = fakePantryRepo,
        groceryRepository = fakeGroceryRepo,
        budgetRepository = fakeBudgetRepo,
        expenseRepository = fakeExpenseRepo,
        calculateBudgetStatusUseCase = calculateBudgetStatusUseCase
    )

    @Before
    fun setUp() {
        sentKeys.clear()
        userSettingsMap.clear()
        mealsList.clear()
        pantryList.clear()
        groceryList.clear()
        expensesList.clear()
        activeBudget = null
        testUserProfile = null
    }

    private fun getTestTime(hour: Int, minute: Int): Long {
        return Calendar.getInstance().apply {
            set(2026, Calendar.OCTOBER, 3, hour, minute, 0)
        }.timeInMillis
    }

    // 1. Meal already logged -> no meal reminder.
    @Test
    fun test1_mealAlreadyLogged_noMealReminder() = runBlocking {
        val userId = "user1"
        val time = getTestTime(8, 30) // Within breakfast window (08:00 - 11:00)

        // Log breakfast
        mealsList.add(
            MealEntry(
                id = "m1",
                userId = userId,
                foodItem = FoodItem("f1", "Eggs", 150f, 12f, 1f, 10f, 1f, "serving"),
                date = time,
                mealType = "Breakfast",
                quantity = 1f,
                loggedAt = time
            )
        )

        val notifications = evaluateNotificationsUseCase(userId, time)
        val mealNotifs = notifications.filter { it.type == NotificationType.MEAL }
        assertTrue(mealNotifs.none { it.title.contains("Breakfast") })
    }

    // 2. Meal not logged -> reminder eligible.
    @Test
    fun test2_mealNotLogged_reminderEligible() = runBlocking {
        val userId = "user1"
        val time = getTestTime(8, 30) // Within breakfast window

        val notifications = evaluateNotificationsUseCase(userId, time)
        val breakfastNotif = notifications.find { it.title.contains("Breakfast") }
        assertTrue(breakfastNotif != null)
        assertEquals(NotificationType.MEAL, breakfastNotif?.type)
    }

    // 3. Nutrition reminder below threshold -> eligible.
    @Test
    fun test3_nutritionReminderBelowThreshold_eligible() = runBlocking {
        val userId = "user1"
        val time = getTestTime(19, 0) // 7:00 PM (evening)

        testUserProfile = UserProfile(
            userId = userId,
            name = "Test User",
            age = 28,
            gender = "Male",
            heightCm = 175f,
            weightKg = 70f,
            goal = "Maintain weight",
            dietaryPreference = "Standard",
            allergies = "",
            activityLevel = "Moderately active"
        )
        // Only 2g protein consumed today (well below targets)
        mealsList.add(
            MealEntry(
                id = "m1",
                userId = userId,
                foodItem = FoodItem("f1", "Toast", 100f, 2f, 20f, 1f, 1f, "slice"),
                date = time,
                mealType = "Breakfast",
                quantity = 1f,
                loggedAt = time
            )
        )

        val notifications = evaluateNotificationsUseCase(userId, time)
        val nutritionNotif = notifications.find { it.type == NotificationType.NUTRITION }
        assertTrue(nutritionNotif != null)
        assertTrue(nutritionNotif!!.message.contains("protein target"))
    }

    // 4. Nutrition reminder already sent today -> not eligible again.
    @Test
    fun test4_nutritionReminderAlreadySent_notEligibleAgain() = runBlocking {
        val userId = "user1"
        val time = getTestTime(19, 0)
        testUserProfile = UserProfile(
            userId = userId,
            name = "Test User",
            age = 28,
            gender = "Male",
            heightCm = 175f,
            weightKg = 70f,
            goal = "Maintain weight",
            dietaryPreference = "Standard",
            allergies = "",
            activityLevel = "Moderately active"
        )

        val firstRun = evaluateNotificationsUseCase(userId, time)
        val notif = firstRun.find { it.type == NotificationType.NUTRITION }
        assertTrue(notif != null)

        // Mark it sent
        evaluateNotificationsUseCase.markNotificationSent(notif!!)

        // Evaluate again on same day
        val secondRun = evaluateNotificationsUseCase(userId, time)
        assertTrue(secondRun.none { it.type == NotificationType.NUTRITION })
    }

    // 5. Pantry item expiring in 3 days -> eligible.
    @Test
    fun test5_pantryItemExpiringIn3Days_eligible() = runBlocking {
        val userId = "user1"
        val now = getTestTime(12, 0)
        val expiryTime = now + (2L * 24 * 60 * 60 * 1000L) // 2 days

        pantryList.add(
            PantryItem(
                id = "p1",
                userId = userId,
                foodItem = FoodItem("f1", "Greek Yogurt", 120f, 15f, 5f, 2f, 1f, "cup"),
                quantity = 1f,
                unit = "cup",
                expiryDate = expiryTime,
                dateAdded = now
            )
        )

        val notifications = evaluateNotificationsUseCase(userId, now)
        val pantryNotif = notifications.find { it.type == NotificationType.PANTRY }
        assertTrue(pantryNotif != null)
        assertTrue(pantryNotif!!.message.contains("Greek Yogurt"))
    }

    // 6. Same pantry item evaluated again -> duplicate prevented.
    @Test
    fun test6_samePantryItemEvaluatedAgain_duplicatePrevented() = runBlocking {
        val userId = "user1"
        val now = getTestTime(12, 0)
        val expiryTime = now + (2L * 24 * 60 * 60 * 1000L)

        pantryList.add(
            PantryItem(
                id = "p1",
                userId = userId,
                foodItem = FoodItem("f1", "Milk", 120f, 8f, 12f, 5f, 1f, "cup"),
                quantity = 1f,
                unit = "cup",
                expiryDate = expiryTime,
                dateAdded = now
            )
        )

        val firstRun = evaluateNotificationsUseCase(userId, now)
        val pantryNotif = firstRun.find { it.type == NotificationType.PANTRY }
        assertTrue(pantryNotif != null)

        evaluateNotificationsUseCase.markNotificationSent(pantryNotif!!)

        val secondRun = evaluateNotificationsUseCase(userId, now)
        assertTrue(secondRun.none { it.type == NotificationType.PANTRY })
    }

    // 7. Grocery list empty -> no grocery notification.
    @Test
    fun test7_groceryListEmpty_noGroceryNotification() = runBlocking {
        val userId = "user1"
        val now = getTestTime(12, 0)

        val notifications = evaluateNotificationsUseCase(userId, now)
        assertTrue(notifications.none { it.type == NotificationType.GROCERY })
    }

    // 8. Grocery list has items -> eligible.
    @Test
    fun test8_groceryListHasItems_eligible() = runBlocking {
        val userId = "user1"
        val now = getTestTime(12, 0)

        groceryList.add(
            GroceryItem(
                id = "g1",
                userId = userId,
                name = "Almond Milk",
                quantity = 1f,
                unit = "bottle",
                category = "Dairy",
                isPurchased = false
            )
        )

        val notifications = evaluateNotificationsUseCase(userId, now)
        val groceryNotif = notifications.find { it.type == NotificationType.GROCERY }
        assertTrue(groceryNotif != null)
        assertTrue(groceryNotif!!.message.contains("1 item"))
    }

    // 9. Budget below threshold -> no warning.
    @Test
    fun test9_budgetBelowThreshold_noWarning() = runBlocking {
        val userId = "user1"
        val now = getTestTime(12, 0)

        activeBudget = Budget(
            id = "b1",
            userId = userId,
            amount = 10000.0,
            period = BudgetPeriod.MONTHLY.name,
            startDate = now - 100000,
            endDate = now + 100000
        )
        // 50% spent
        expensesList.add(FoodExpense("e1", userId, "Supermarket", 5000.0, "Groceries", now))

        val notifications = evaluateNotificationsUseCase(userId, now)
        assertTrue(notifications.none { it.type == NotificationType.BUDGET })
    }

    // 10. Budget crosses 75% -> eligible.
    @Test
    fun test10_budgetCrosses75_eligible() = runBlocking {
        val userId = "user1"
        val now = getTestTime(12, 0)

        activeBudget = Budget(
            id = "b1",
            userId = userId,
            amount = 10000.0,
            period = BudgetPeriod.MONTHLY.name,
            startDate = now - 100000,
            endDate = now + 100000
        )
        // 78% spent
        expensesList.add(FoodExpense("e1", userId, "Supermarket", 7800.0, "Groceries", now))

        val notifications = evaluateNotificationsUseCase(userId, now)
        val budgetNotif = notifications.find { it.type == NotificationType.BUDGET }
        assertTrue(budgetNotif != null)
        assertTrue(budgetNotif!!.message.contains("78%"))
    }

    // 11. Same 75% warning evaluated again -> no duplicate.
    @Test
    fun test11_same75WarningEvaluatedAgain_noDuplicate() = runBlocking {
        val userId = "user1"
        val now = getTestTime(12, 0)

        activeBudget = Budget(
            id = "b1",
            userId = userId,
            amount = 10000.0,
            period = BudgetPeriod.MONTHLY.name,
            startDate = now - 100000,
            endDate = now + 100000
        )
        expensesList.add(FoodExpense("e1", userId, "Supermarket", 7800.0, "Groceries", now))

        val firstRun = evaluateNotificationsUseCase(userId, now)
        val budgetNotif = firstRun.find { it.type == NotificationType.BUDGET }
        assertTrue(budgetNotif != null)

        evaluateNotificationsUseCase.markNotificationSent(budgetNotif!!)

        val secondRun = evaluateNotificationsUseCase(userId, now)
        assertTrue(secondRun.none { it.type == NotificationType.BUDGET })
    }

    // 12. Budget crosses 90% -> 90% warning eligible.
    @Test
    fun test12_budgetCrosses90_eligible() = runBlocking {
        val userId = "user1"
        val now = getTestTime(12, 0)

        activeBudget = Budget(
            id = "b1",
            userId = userId,
            amount = 10000.0,
            period = BudgetPeriod.MONTHLY.name,
            startDate = now - 100000,
            endDate = now + 100000
        )
        // 92% spent
        expensesList.add(FoodExpense("e1", userId, "Supermarket", 9200.0, "Groceries", now))

        val notifications = evaluateNotificationsUseCase(userId, now)
        val budgetNotif = notifications.find { it.type == NotificationType.BUDGET }
        assertTrue(budgetNotif != null)
        assertTrue(budgetNotif!!.message.contains("92%"))
    }

    // 13. Budget crosses 100% -> 100% warning eligible.
    @Test
    fun test13_budgetCrosses100_eligible() = runBlocking {
        val userId = "user1"
        val now = getTestTime(12, 0)

        activeBudget = Budget(
            id = "b1",
            userId = userId,
            amount = 10000.0,
            period = BudgetPeriod.MONTHLY.name,
            startDate = now - 100000,
            endDate = now + 100000
        )
        // 105% spent
        expensesList.add(FoodExpense("e1", userId, "Supermarket", 10500.0, "Groceries", now))

        val notifications = evaluateNotificationsUseCase(userId, now)
        val budgetNotif = notifications.find { it.type == NotificationType.BUDGET }
        assertTrue(budgetNotif != null)
        assertTrue(budgetNotif!!.message.contains("reached"))
    }

    // 14. Notification type disabled -> no notification.
    @Test
    fun test14_notificationTypeDisabled_noNotification() = runBlocking {
        val userId = "user1"
        val now = getTestTime(8, 30)

        // Disable meal reminders
        userSettingsMap[userId] = NotificationSettings(
            userId = userId,
            notificationsEnabled = true,
            mealRemindersEnabled = false
        )

        val notifications = evaluateNotificationsUseCase(userId, now)
        assertTrue(notifications.none { it.type == NotificationType.MEAL })
    }

    // 15. Different user IDs -> notification state remains isolated.
    @Test
    fun test15_differentUserIds_stateRemainsIsolated() = runBlocking {
        val user1 = "user1"
        val user2 = "user2"
        val now = getTestTime(8, 30)

        // user1 has sent breakfast notification
        val user1Run = evaluateNotificationsUseCase(user1, now)
        val notif1 = user1Run.find { it.type == NotificationType.MEAL }
        assertTrue(notif1 != null)
        evaluateNotificationsUseCase.markNotificationSent(notif1!!)

        // user1 should have no more breakfast notification
        val user1SecondRun = evaluateNotificationsUseCase(user1, now)
        assertTrue(user1SecondRun.none { it.type == NotificationType.MEAL })

        // user2 MUST still be eligible for their breakfast notification!
        val user2Run = evaluateNotificationsUseCase(user2, now)
        val notif2 = user2Run.find { it.type == NotificationType.MEAL }
        assertTrue(notif2 != null)
        assertEquals(user2, notif2?.userId)
    }
}
