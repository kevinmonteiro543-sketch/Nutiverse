package com.monteirogames.nutriverse.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.monteirogames.nutriverse.data.local.entity.UserProfileEntity
import com.monteirogames.nutriverse.data.local.dao.UserProfileDao
import com.monteirogames.nutriverse.data.local.entity.FoodItemEntity
import com.monteirogames.nutriverse.data.local.entity.MealEntryEntity
import com.monteirogames.nutriverse.data.local.entity.PantryItemEntity
import com.monteirogames.nutriverse.data.local.dao.FoodItemDao
import com.monteirogames.nutriverse.data.local.dao.MealEntryDao
import com.monteirogames.nutriverse.data.local.dao.PantryItemDao
import com.monteirogames.nutriverse.data.local.entity.DietPlanEntity
import com.monteirogames.nutriverse.data.local.entity.DietPlanMealEntity
import com.monteirogames.nutriverse.data.local.dao.DietPlanDao
import com.monteirogames.nutriverse.data.local.entity.RecipeEntity
import com.monteirogames.nutriverse.data.local.entity.RecipeIngredientEntity
import com.monteirogames.nutriverse.data.local.entity.RecipeInstructionEntity
import com.monteirogames.nutriverse.data.local.dao.RecipeDao
import com.monteirogames.nutriverse.data.local.entity.GroceryItemEntity
import com.monteirogames.nutriverse.data.local.dao.GroceryDao
import com.monteirogames.nutriverse.data.local.entity.BudgetEntity
import com.monteirogames.nutriverse.data.local.dao.BudgetDao
import com.monteirogames.nutriverse.data.local.entity.FoodExpenseEntity
import com.monteirogames.nutriverse.data.local.dao.FoodExpenseDao
import com.monteirogames.nutriverse.data.local.entity.NotificationSettingsEntity
import com.monteirogames.nutriverse.data.local.entity.NotificationRecordEntity
import com.monteirogames.nutriverse.data.local.dao.NotificationDao
import com.monteirogames.nutriverse.data.local.entity.HealthReportEntity
import com.monteirogames.nutriverse.data.local.entity.HealthReportResultEntity
import com.monteirogames.nutriverse.data.local.dao.HealthReportDao

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE user_profile ADD COLUMN workoutFrequency TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE user_profile ADD COLUMN workoutIntensity TEXT NOT NULL DEFAULT ''")
    }
}

val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `diet_plan` (
                `id` TEXT NOT NULL,
                `userId` TEXT NOT NULL,
                `name` TEXT NOT NULL,
                `targetCalories` REAL NOT NULL,
                `targetProtein` REAL NOT NULL,
                `targetCarbs` REAL NOT NULL,
                `targetFat` REAL NOT NULL,
                `createdAt` INTEGER NOT NULL,
                `isActive` INTEGER NOT NULL,
                PRIMARY KEY(`id`)
            )
        """.trimIndent())

        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `diet_plan_meal` (
                `id` TEXT NOT NULL,
                `planId` TEXT NOT NULL,
                `dayNumber` INTEGER NOT NULL,
                `mealType` TEXT NOT NULL,
                `name` TEXT NOT NULL,
                `calories` REAL NOT NULL,
                `protein` REAL NOT NULL,
                `carbs` REAL NOT NULL,
                `fat` REAL NOT NULL,
                `prepTip` TEXT NOT NULL,
                PRIMARY KEY(`id`),
                FOREIGN KEY(`planId`) REFERENCES `diet_plan`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
        """.trimIndent())

        db.execSQL("CREATE INDEX IF NOT EXISTS `index_diet_plan_meal_planId` ON `diet_plan_meal` (`planId`)")
    }
}

val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `recipe` (
                `id` TEXT NOT NULL,
                `name` TEXT NOT NULL,
                `description` TEXT NOT NULL,
                `imageUrl` TEXT NOT NULL,
                `prepTimeMinutes` INTEGER NOT NULL,
                `cookTimeMinutes` INTEGER NOT NULL,
                `servings` INTEGER NOT NULL,
                `calories` REAL NOT NULL,
                `protein` REAL NOT NULL,
                `carbs` REAL NOT NULL,
                `fat` REAL NOT NULL,
                `difficulty` TEXT NOT NULL,
                `dietaryTags` TEXT NOT NULL,
                `allergyTags` TEXT NOT NULL,
                `isFavorite` INTEGER NOT NULL,
                `createdAt` INTEGER NOT NULL,
                PRIMARY KEY(`id`)
            )
        """.trimIndent())

        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `recipe_ingredient` (
                `id` TEXT NOT NULL,
                `recipeId` TEXT NOT NULL,
                `name` TEXT NOT NULL,
                `quantity` REAL NOT NULL,
                `unit` TEXT NOT NULL,
                PRIMARY KEY(`id`),
                FOREIGN KEY(`recipeId`) REFERENCES `recipe`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
        """.trimIndent())

        db.execSQL("CREATE INDEX IF NOT EXISTS `index_recipe_ingredient_recipeId` ON `recipe_ingredient` (`recipeId`)")

        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `recipe_instruction` (
                `id` TEXT NOT NULL,
                `recipeId` TEXT NOT NULL,
                `stepNumber` INTEGER NOT NULL,
                `instructionText` TEXT NOT NULL,
                PRIMARY KEY(`id`),
                FOREIGN KEY(`recipeId`) REFERENCES `recipe`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
        """.trimIndent())

        db.execSQL("CREATE INDEX IF NOT EXISTS `index_recipe_instruction_recipeId` ON `recipe_instruction` (`recipeId`)")
    }
}

val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `grocery_item` (
                `id` TEXT NOT NULL,
                `userId` TEXT NOT NULL,
                `name` TEXT NOT NULL,
                `quantity` REAL NOT NULL,
                `unit` TEXT NOT NULL,
                `category` TEXT NOT NULL,
                `isPurchased` INTEGER NOT NULL,
                `source` TEXT NOT NULL,
                `sourceRecipeId` TEXT,
                `createdAt` INTEGER NOT NULL,
                `purchasedAt` INTEGER,
                PRIMARY KEY(`id`)
            )
        """.trimIndent())

        db.execSQL("CREATE INDEX IF NOT EXISTS `index_grocery_item_userId` ON `grocery_item` (`userId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_grocery_item_userId_isPurchased` ON `grocery_item` (`userId`, `isPurchased`)")
    }
}

val MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `budget` (
                `id` TEXT NOT NULL,
                `userId` TEXT NOT NULL,
                `amount` REAL NOT NULL,
                `period` TEXT NOT NULL,
                `startDate` INTEGER NOT NULL,
                `endDate` INTEGER NOT NULL,
                `createdAt` INTEGER NOT NULL,
                `isActive` INTEGER NOT NULL,
                PRIMARY KEY(`id`)
            )
        """.trimIndent())

        db.execSQL("CREATE INDEX IF NOT EXISTS `index_budget_userId` ON `budget` (`userId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_budget_userId_isActive` ON `budget` (`userId`, `isActive`)")

        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `food_expense` (
                `id` TEXT NOT NULL,
                `userId` TEXT NOT NULL,
                `name` TEXT NOT NULL,
                `amount` REAL NOT NULL,
                `category` TEXT NOT NULL,
                `date` INTEGER NOT NULL,
                `notes` TEXT,
                `groceryItemId` TEXT,
                `createdAt` INTEGER NOT NULL,
                PRIMARY KEY(`id`)
            )
        """.trimIndent())

        db.execSQL("CREATE INDEX IF NOT EXISTS `index_food_expense_userId` ON `food_expense` (`userId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_food_expense_userId_date` ON `food_expense` (`userId`, `date`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_food_expense_groceryItemId` ON `food_expense` (`groceryItemId`)")
    }
}

val MIGRATION_7_8 = object : Migration(7, 8) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `notification_settings` (
                `userId` TEXT NOT NULL,
                `notificationsEnabled` INTEGER NOT NULL,
                `mealRemindersEnabled` INTEGER NOT NULL,
                `nutritionRemindersEnabled` INTEGER NOT NULL,
                `pantryExpiryEnabled` INTEGER NOT NULL,
                `groceryRemindersEnabled` INTEGER NOT NULL,
                `budgetWarningsEnabled` INTEGER NOT NULL,
                `breakfastTime` TEXT NOT NULL,
                `lunchTime` TEXT NOT NULL,
                `dinnerTime` TEXT NOT NULL,
                PRIMARY KEY(`userId`)
            )
        """.trimIndent())

        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `notification_record` (
                `id` TEXT NOT NULL,
                `userId` TEXT NOT NULL,
                `deduplicationKey` TEXT NOT NULL,
                `type` TEXT NOT NULL,
                `sentAt` INTEGER NOT NULL,
                PRIMARY KEY(`id`)
            )
        """.trimIndent())

        db.execSQL("CREATE INDEX IF NOT EXISTS `index_notification_record_userId` ON `notification_record` (`userId`)")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_notification_record_userId_deduplicationKey` ON `notification_record` (`userId`, `deduplicationKey`)")
    }
}

val MIGRATION_8_9 = object : Migration(8, 9) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `health_report` (
                `id` TEXT NOT NULL,
                `userId` TEXT NOT NULL,
                `reportName` TEXT NOT NULL,
                `reportType` TEXT NOT NULL,
                `createdAt` INTEGER NOT NULL,
                `summary` TEXT NOT NULL,
                `observationsJson` TEXT NOT NULL,
                `questionsJson` TEXT NOT NULL,
                `disclaimer` TEXT NOT NULL,
                PRIMARY KEY(`id`)
            )
        """.trimIndent())

        db.execSQL("CREATE INDEX IF NOT EXISTS `index_health_report_userId` ON `health_report` (`userId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_health_report_userId_createdAt` ON `health_report` (`userId`, `createdAt`)")

        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `health_report_result` (
                `id` TEXT NOT NULL,
                `reportId` TEXT NOT NULL,
                `testName` TEXT NOT NULL,
                `value` TEXT,
                `unit` TEXT,
                `referenceRange` TEXT,
                `interpretation` TEXT,
                `status` TEXT NOT NULL,
                `userNote` TEXT,
                PRIMARY KEY(`id`),
                FOREIGN KEY(`reportId`) REFERENCES `health_report`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
        """.trimIndent())

        db.execSQL("CREATE INDEX IF NOT EXISTS `index_health_report_result_reportId` ON `health_report_result` (`reportId`)")
    }
}

val MIGRATION_9_10 = object : Migration(9, 10) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE user_profile ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0")
    }
}

val MIGRATION_10_11 = object : Migration(10, 11) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE meal_entry ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE pantry_item ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0")
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `sync_tombstone` (
                `id` TEXT NOT NULL,
                `entityType` TEXT NOT NULL,
                `userId` TEXT NOT NULL,
                `deletedAt` INTEGER NOT NULL,
                PRIMARY KEY(`entityType`, `id`)
            )
        """.trimIndent())
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_sync_tombstone_userId_entityType` ON `sync_tombstone` (`userId`, `entityType`)")
    }
}

val MIGRATION_11_12 = object : Migration(11, 12) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE recipe ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE grocery_item ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE budget ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE food_expense ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0")
    }
}

val MIGRATION_12_13 = object : Migration(12, 13) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE diet_plan ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE health_report ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0")
    }
}

val MIGRATION_13_14 = object : Migration(13, 14) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `weekly_review` (
                `id` TEXT NOT NULL,
                `userId` TEXT NOT NULL,
                `startDate` TEXT NOT NULL,
                `endDate` TEXT NOT NULL,
                `startTimestamp` INTEGER NOT NULL,
                `endTimestamp` INTEGER NOT NULL,
                `displayRange` TEXT NOT NULL,
                `generatedAt` INTEGER NOT NULL,
                `trackedDays` INTEGER NOT NULL,
                `totalDays` INTEGER NOT NULL,
                `averageCalories` REAL NOT NULL,
                `averageProtein` REAL NOT NULL,
                `averageCarbs` REAL NOT NULL,
                `averageFat` REAL NOT NULL,
                `targetCalories` REAL NOT NULL,
                `targetProtein` REAL NOT NULL,
                `targetCarbs` REAL NOT NULL,
                `targetFat` REAL NOT NULL,
                `calorieAdherence` REAL NOT NULL,
                `proteinAdherence` REAL NOT NULL,
                `trackingConsistency` REAL NOT NULL,
                `totalMeals` INTEGER NOT NULL,
                `highestCalorieDay` TEXT,
                `lowestCalorieDay` TEXT,
                `summary` TEXT NOT NULL,
                `strengthsJson` TEXT NOT NULL,
                `improvementsJson` TEXT NOT NULL,
                `nutritionInsightsJson` TEXT NOT NULL,
                `recommendationsJson` TEXT NOT NULL,
                `nextWeekFocusJson` TEXT NOT NULL,
                PRIMARY KEY(`id`)
            )
        """.trimIndent())
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_weekly_review_userId` ON `weekly_review` (`userId`)")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_weekly_review_userId_startDate_endDate` ON `weekly_review` (`userId`, `startDate`, `endDate`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_weekly_review_userId_generatedAt` ON `weekly_review` (`userId`, `generatedAt`)")
    }
}

@Database(
    entities = [
        UserProfileEntity::class,
        FoodItemEntity::class,
        MealEntryEntity::class,
        PantryItemEntity::class,
        DietPlanEntity::class,
        DietPlanMealEntity::class,
        RecipeEntity::class,
        RecipeIngredientEntity::class,
        RecipeInstructionEntity::class,
        GroceryItemEntity::class,
        BudgetEntity::class,
        FoodExpenseEntity::class,
        NotificationSettingsEntity::class,
        NotificationRecordEntity::class,
        HealthReportEntity::class,
        HealthReportResultEntity::class,
        com.monteirogames.nutriverse.data.local.entity.SyncTombstoneEntity::class,
        com.monteirogames.nutriverse.data.local.entity.WeeklyReviewEntity::class
    ],
    version = 14,
    exportSchema = false
)
abstract class NutriVerseDatabase : RoomDatabase() {
    abstract fun userProfileDao(): UserProfileDao
    abstract fun foodItemDao(): FoodItemDao
    abstract fun mealEntryDao(): MealEntryDao
    abstract fun pantryItemDao(): PantryItemDao
    abstract fun dietPlanDao(): DietPlanDao
    abstract fun recipeDao(): RecipeDao
    abstract fun groceryDao(): GroceryDao
    abstract fun budgetDao(): BudgetDao
    abstract fun foodExpenseDao(): FoodExpenseDao
    abstract fun notificationDao(): NotificationDao
    abstract fun healthReportDao(): HealthReportDao
    abstract fun syncTombstoneDao(): com.monteirogames.nutriverse.data.local.dao.SyncTombstoneDao
    abstract fun weeklyReviewDao(): com.monteirogames.nutriverse.data.local.dao.WeeklyReviewDao
}
