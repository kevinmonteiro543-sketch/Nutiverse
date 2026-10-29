package com.monteirogames.nutriverse

import android.content.Context
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import com.monteirogames.nutriverse.data.local.*
import com.monteirogames.nutriverse.data.local.entity.FoodItemEntity
import com.monteirogames.nutriverse.data.local.entity.MealEntryEntity
import com.monteirogames.nutriverse.data.local.entity.UserProfileEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.IOException

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class DatabaseMigrationAndSchemaTest {

    private lateinit var database: NutriVerseDatabase
    private lateinit var context: Context

    @Before
    fun createDb() {
        context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(
            context,
            NutriVerseDatabase::class.java
        ).addMigrations(
            MIGRATION_2_3,
            MIGRATION_3_4,
            MIGRATION_4_5,
            MIGRATION_5_6,
            MIGRATION_6_7,
            MIGRATION_7_8,
            MIGRATION_8_9,
            MIGRATION_9_10,
            MIGRATION_10_11,
            MIGRATION_11_12,
            MIGRATION_12_13,
            MIGRATION_13_14
        ).build()
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        database.close()
    }

    // 1. Fresh database initializes at schema version 14 with all DAOs available
    @Test
    fun testFreshDatabaseInitializesAtVersion14() = runBlocking {
        assertNotNull(database.userProfileDao())
        assertNotNull(database.foodItemDao())
        assertNotNull(database.mealEntryDao())
        assertNotNull(database.pantryItemDao())
        assertNotNull(database.dietPlanDao())
        assertNotNull(database.recipeDao())
        assertNotNull(database.groceryDao())
        assertNotNull(database.budgetDao())
        assertNotNull(database.foodExpenseDao())
        assertNotNull(database.notificationDao())
        assertNotNull(database.healthReportDao())
        assertNotNull(database.syncTombstoneDao())
        assertNotNull(database.weeklyReviewDao())

        // Insert and verify UserProfile
        val profile = UserProfileEntity(
            userId = "test_user_mig",
            name = "Test User",
            age = 28,
            gender = "Female",
            heightCm = 168f,
            weightKg = 62f,
            goal = "Healthy Eating",
            dietaryPreference = "Vegetarian",
            allergies = "None",
            activityLevel = "Moderate",
            workoutFrequency = "3-4 times/week",
            workoutIntensity = "Moderate",
            updatedAt = 1000L
        )
        database.userProfileDao().insertProfile(profile)
        val loadedProfile = database.userProfileDao().getUserProfile("test_user_mig").first()
        assertNotNull(loadedProfile)
        assertEquals("test_user_mig", loadedProfile?.userId)
        assertEquals("3-4 times/week", loadedProfile?.workoutFrequency)
    }

    // 2. Meal entry and food item storage works properly
    @Test
    fun testMealEntryAndFoodItemStorage() = runBlocking {
        val food = FoodItemEntity(
            id = "food_1",
            name = "Brown Rice",
            calories = 111f,
            protein = 2.6f,
            carbs = 23f,
            fat = 0.9f,
            servingSize = 100f,
            servingUnit = "g",
            barcode = null
        )
        database.foodItemDao().insertFoodItem(food)

        val meal = MealEntryEntity(
            id = "meal_1",
            userId = "test_user_mig",
            foodItemId = "food_1",
            date = System.currentTimeMillis(),
            mealType = "Lunch",
            quantity = 1.5f,
            loggedAt = System.currentTimeMillis(),
            updatedAt = 2000L
        )
        database.mealEntryDao().insertMealEntry(meal)

        val meals = database.mealEntryDao().getMealsForDateRange("test_user_mig", 0L, Long.MAX_VALUE).first()
        assertEquals(1, meals.size)
        assertEquals("Brown Rice", meals[0].foodItem.name)
        assertEquals(1.5f, meals[0].mealEntry.quantity, 0.01f)
    }

    // 3. Sequential migrations execute without SQL syntax errors
    @Test
    fun testMigrationsExecuteSqlCleanly() {
        // Open a direct temporary SQLite database to execute migrations on raw tables
        val helperConfig = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name("migration_test.db")
            .callback(object : SupportSQLiteOpenHelper.Callback(2) {
                override fun onCreate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                    // Create base tables as they were at version 2
                    db.execSQL("""
                        CREATE TABLE IF NOT EXISTS `user_profile` (
                            `userId` TEXT NOT NULL,
                            `name` TEXT NOT NULL,
                            `age` INTEGER NOT NULL,
                            `gender` TEXT NOT NULL,
                            `heightCm` REAL NOT NULL,
                            `weightKg` REAL NOT NULL,
                            `goal` TEXT NOT NULL,
                            `dietaryPreference` TEXT NOT NULL,
                            `allergies` TEXT NOT NULL,
                            `activityLevel` TEXT NOT NULL,
                            PRIMARY KEY(`userId`)
                        )
                    """.trimIndent())
                    db.execSQL("""
                        CREATE TABLE IF NOT EXISTS `meal_entry` (
                            `id` TEXT NOT NULL,
                            `userId` TEXT NOT NULL,
                            `foodItemId` TEXT NOT NULL,
                            `quantity` REAL NOT NULL,
                            `mealType` TEXT NOT NULL,
                            `date` INTEGER NOT NULL,
                            `calculatedCalories` REAL NOT NULL,
                            `calculatedProtein` REAL NOT NULL,
                            `calculatedCarbs` REAL NOT NULL,
                            `calculatedFat` REAL NOT NULL,
                            PRIMARY KEY(`id`)
                        )
                    """.trimIndent())
                    db.execSQL("""
                        CREATE TABLE IF NOT EXISTS `pantry_item` (
                            `id` TEXT NOT NULL,
                            `userId` TEXT NOT NULL,
                            `foodItemId` TEXT NOT NULL,
                            `quantity` REAL NOT NULL,
                            `unit` TEXT NOT NULL,
                            `expiryDate` INTEGER NOT NULL,
                            `addedDate` INTEGER NOT NULL,
                            PRIMARY KEY(`id`)
                        )
                    """.trimIndent())
                }

                override fun onUpgrade(
                    db: androidx.sqlite.db.SupportSQLiteDatabase,
                    oldVersion: Int,
                    newVersion: Int
                ) {}
            })
            .build()

        val factory = FrameworkSQLiteOpenHelperFactory()
        val helper = factory.create(helperConfig)
        val db = helper.writableDatabase

        // Execute migrations from 2 to 14 in sequence
        MIGRATION_2_3.migrate(db)
        MIGRATION_3_4.migrate(db)
        MIGRATION_4_5.migrate(db)
        MIGRATION_5_6.migrate(db)
        MIGRATION_6_7.migrate(db)
        MIGRATION_7_8.migrate(db)
        MIGRATION_8_9.migrate(db)
        MIGRATION_9_10.migrate(db)
        MIGRATION_10_11.migrate(db)
        MIGRATION_11_12.migrate(db)
        MIGRATION_12_13.migrate(db)
        MIGRATION_13_14.migrate(db)

        // Verify that newly created tables and columns exist
        val cursor = db.query("SELECT workoutFrequency FROM user_profile LIMIT 0")
        assertNotNull(cursor)
        cursor.close()

        val tombstoneCursor = db.query("SELECT * FROM sync_tombstone LIMIT 0")
        assertNotNull(tombstoneCursor)
        tombstoneCursor.close()

        val weeklyReviewCursor = db.query("SELECT * FROM weekly_review LIMIT 0")
        assertNotNull(weeklyReviewCursor)
        weeklyReviewCursor.close()

        db.close()
        context.deleteDatabase("migration_test.db")
    }
}
