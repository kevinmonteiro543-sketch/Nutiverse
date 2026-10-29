package com.monteirogames.nutriverse

import android.app.Application
import androidx.room.Room
import com.monteirogames.nutriverse.data.local.NutriVerseDatabase
import com.monteirogames.nutriverse.data.repository.AuthRepositoryImpl
import com.monteirogames.nutriverse.data.repository.UserProfileRepositoryImpl
import com.monteirogames.nutriverse.data.repository.MealRepositoryImpl
import com.monteirogames.nutriverse.data.repository.PantryRepositoryImpl
import com.monteirogames.nutriverse.domain.usecase.*
import com.monteirogames.nutriverse.data.remote.OpenFoodFactsApi
import com.monteirogames.nutriverse.data.repository.BarcodeRepositoryImpl
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

class NutriVerseApplication : Application() {

    lateinit var database: NutriVerseDatabase
        private set

    lateinit var authUseCases: AuthUseCases
        private set

    lateinit var userProfileUseCases: UserProfileUseCases
        private set

    lateinit var mealTrackerUseCases: MealTrackerUseCases
        private set
        
    lateinit var pantryUseCases: PantryUseCases
        private set

    lateinit var scannerUseCases: ScannerUseCases
        private set
        
    lateinit var analyzeFoodImageUseCase: AnalyzeFoodImageUseCase
        private set

    lateinit var dietPlannerUseCases: DietPlannerUseCases
        private set

    lateinit var recipeUseCases: RecipeUseCases
        private set

    lateinit var groceryUseCases: GroceryUseCases
        private set

    lateinit var budgetUseCases: BudgetUseCases
        private set

    lateinit var expenseUseCases: ExpenseUseCases
        private set

    lateinit var notificationUseCases: NotificationUseCases
        private set

    lateinit var progressAnalyticsUseCases: ProgressAnalyticsUseCases
        private set

    lateinit var healthReportUseCases: HealthReportUseCases
        private set

    lateinit var syncUseCases: SyncUseCases
        private set

    lateinit var firestoreUserRepository: com.monteirogames.nutriverse.domain.repository.FirestoreUserRepository
        private set

    lateinit var syncRepository: com.monteirogames.nutriverse.domain.repository.SyncRepository
        private set

    lateinit var profileSyncRepository: com.monteirogames.nutriverse.domain.repository.ProfileSyncRepository
        private set

    lateinit var profileSyncUseCases: com.monteirogames.nutriverse.domain.usecase.ProfileSyncUseCases
        private set

    lateinit var mealSyncRepository: com.monteirogames.nutriverse.domain.repository.MealSyncRepository
        private set

    lateinit var mealSyncUseCases: com.monteirogames.nutriverse.domain.usecase.MealSyncUseCases
        private set

    lateinit var pantrySyncRepository: com.monteirogames.nutriverse.domain.repository.PantrySyncRepository
        private set

    lateinit var pantrySyncUseCases: com.monteirogames.nutriverse.domain.usecase.PantrySyncUseCases
        private set

    lateinit var recipeSyncRepository: com.monteirogames.nutriverse.domain.repository.RecipeSyncRepository
        private set

    lateinit var recipeSyncUseCases: com.monteirogames.nutriverse.domain.usecase.RecipeSyncUseCases
        private set

    lateinit var grocerySyncRepository: com.monteirogames.nutriverse.domain.repository.GrocerySyncRepository
        private set

    lateinit var grocerySyncUseCases: com.monteirogames.nutriverse.domain.usecase.GrocerySyncUseCases
        private set

    lateinit var budgetSyncRepository: com.monteirogames.nutriverse.domain.repository.BudgetSyncRepository
        private set

    lateinit var budgetSyncUseCases: com.monteirogames.nutriverse.domain.usecase.BudgetSyncUseCases
        private set

    lateinit var expenseSyncRepository: com.monteirogames.nutriverse.domain.repository.ExpenseSyncRepository
        private set

    lateinit var expenseSyncUseCases: com.monteirogames.nutriverse.domain.usecase.ExpenseSyncUseCases
        private set

    lateinit var dietPlanSyncRepository: com.monteirogames.nutriverse.domain.repository.DietPlanSyncRepository
        private set

    lateinit var dietPlanSyncUseCases: com.monteirogames.nutriverse.domain.usecase.DietPlanSyncUseCases
        private set

    lateinit var healthReportSyncRepository: com.monteirogames.nutriverse.domain.repository.HealthReportSyncRepository
        private set

    lateinit var healthReportSyncUseCases: com.monteirogames.nutriverse.domain.usecase.HealthReportSyncUseCases
        private set

    lateinit var weeklyReviewRepository: com.monteirogames.nutriverse.domain.repository.WeeklyReviewRepository
        private set

    lateinit var weeklyReviewUseCases: com.monteirogames.nutriverse.domain.usecase.WeeklyReviewUseCases
        private set

    lateinit var foodCatalogRepository: com.monteirogames.nutriverse.domain.repository.FoodCatalogRepository
        private set

    lateinit var foodCatalogUseCases: com.monteirogames.nutriverse.domain.usecase.FoodCatalogUseCases
        private set

    override fun onCreate() {
        super.onCreate()
        
        FirebaseApp.initializeApp(this)
        val firebaseAppCheck = FirebaseAppCheck.getInstance()
        if (BuildConfig.DEBUG) {
            firebaseAppCheck.installAppCheckProviderFactory(
                DebugAppCheckProviderFactory.getInstance()
            )
            try {
                firebaseAppCheck.getAppCheckToken(false)
            } catch (_: Exception) {}
        } else {
            firebaseAppCheck.installAppCheckProviderFactory(
                PlayIntegrityAppCheckProviderFactory.getInstance()
            )
        }
        
        database = Room.databaseBuilder(
            this,
            NutriVerseDatabase::class.java,
            "nutriverse_db"
        ).addMigrations(
            com.monteirogames.nutriverse.data.local.MIGRATION_2_3,
            com.monteirogames.nutriverse.data.local.MIGRATION_3_4,
            com.monteirogames.nutriverse.data.local.MIGRATION_4_5,
            com.monteirogames.nutriverse.data.local.MIGRATION_5_6,
            com.monteirogames.nutriverse.data.local.MIGRATION_6_7,
            com.monteirogames.nutriverse.data.local.MIGRATION_7_8,
            com.monteirogames.nutriverse.data.local.MIGRATION_8_9,
            com.monteirogames.nutriverse.data.local.MIGRATION_9_10,
            com.monteirogames.nutriverse.data.local.MIGRATION_10_11,
            com.monteirogames.nutriverse.data.local.MIGRATION_11_12,
            com.monteirogames.nutriverse.data.local.MIGRATION_12_13,
            com.monteirogames.nutriverse.data.local.MIGRATION_13_14
        ).build()
        
        val authRepository = com.monteirogames.nutriverse.data.repository.FirebaseAuthRepository()
        val userProfileRepository = UserProfileRepositoryImpl(database.userProfileDao())
        val mealRepository = MealRepositoryImpl(database.foodItemDao(), database.mealEntryDao(), database.syncTombstoneDao())
        val pantryRepository = PantryRepositoryImpl(database.foodItemDao(), database.pantryItemDao(), database.syncTombstoneDao())

        authUseCases = AuthUseCases(
            signIn = SignInUseCase(authRepository),
            signUp = SignUpUseCase(authRepository),
            signOut = SignOutUseCase(authRepository),
            getCurrentUserId = GetCurrentUserIdUseCase(authRepository),
            getCurrentUserFlow = GetCurrentUserFlowUseCase(authRepository),
            sendPasswordResetEmail = SendPasswordResetEmailUseCase(authRepository),
            getCurrentUser = GetCurrentUserUseCase(authRepository),
            observeAuthState = ObserveAuthStateUseCase(authRepository)
        )

        userProfileUseCases = UserProfileUseCases(
            getUserProfile = GetUserProfileUseCase(userProfileRepository),
            saveUserProfile = SaveUserProfileUseCase(userProfileRepository),
            clearUserProfile = ClearUserProfileUseCase(userProfileRepository),
            calculateNutritionTargets = CalculateNutritionTargetsUseCase()
        )
        
        mealTrackerUseCases = MealTrackerUseCases(
            addMeal = AddMealUseCase(mealRepository),
            updateMeal = UpdateMealUseCase(mealRepository),
            deleteMeal = DeleteMealUseCase(mealRepository),
            getMeal = GetMealEntryUseCase(mealRepository),
            observeTodayMeals = ObserveTodayMealsUseCase(mealRepository),
            calculateDailyNutrition = CalculateDailyNutritionUseCase(mealRepository)
        )
        
        pantryUseCases = PantryUseCases(
            addPantryItem = AddPantryItemUseCase(pantryRepository),
            updatePantryItem = UpdatePantryItemUseCase(pantryRepository),
            deletePantryItem = DeletePantryItemUseCase(pantryRepository),
            getPantryItem = GetPantryItemUseCase(pantryRepository),
            observePantryItems = ObservePantryItemsUseCase(pantryRepository)
        )

        val moshi = Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl("https://world.openfoodfacts.org/")
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()

        val api = retrofit.create(OpenFoodFactsApi::class.java)
        val barcodeRepository = BarcodeRepositoryImpl(api)

        scannerUseCases = ScannerUseCases(
            getProductFromBarcode = GetProductFromBarcodeUseCase(barcodeRepository)
        )
        
        val aiFoodRecognitionRepository = com.monteirogames.nutriverse.data.repository.AiFoodRecognitionRepositoryImpl()
        analyzeFoodImageUseCase = AnalyzeFoodImageUseCase(aiFoodRecognitionRepository)

        val dietPlanDao = database.dietPlanDao()
        val dietPlannerRepository = com.monteirogames.nutriverse.data.repository.DietPlannerRepositoryImpl(dietPlanDao, database.syncTombstoneDao())
        dietPlannerUseCases = DietPlannerUseCases(
            generateDietPlan = GenerateDietPlanUseCase(dietPlannerRepository),
            saveDietPlan = SaveDietPlanUseCase(dietPlannerRepository),
            getActiveDietPlan = GetActiveDietPlanUseCase(dietPlannerRepository),
            clearDietPlan = ClearDietPlanUseCase(dietPlannerRepository)
        )

        val recipeDao = database.recipeDao()
        val recipeRepository = com.monteirogames.nutriverse.data.repository.RecipeRepositoryImpl(recipeDao, database.syncTombstoneDao())
        recipeUseCases = RecipeUseCases(
            getAllRecipes = GetAllRecipesUseCase(recipeRepository),
            getRecipeById = GetRecipeByIdUseCase(recipeRepository),
            toggleRecipeFavorite = ToggleRecipeFavoriteUseCase(recipeRepository),
            seedRecipes = SeedRecipesUseCase(recipeRepository),
            getSmartRecipeSuggestions = com.monteirogames.nutriverse.domain.usecase.SmartRecipeRecommendationUseCase()
        )

        val groceryDao = database.groceryDao()
        val groceryRepository = com.monteirogames.nutriverse.data.repository.GroceryRepositoryImpl(groceryDao, database.syncTombstoneDao())
        groceryUseCases = GroceryUseCases(
            getGroceryItems = GetGroceryItemsUseCase(groceryRepository),
            addGroceryItem = AddGroceryItemUseCase(groceryRepository),
            updateGroceryItem = UpdateGroceryItemUseCase(groceryRepository),
            deleteGroceryItem = DeleteGroceryItemUseCase(groceryRepository),
            toggleGroceryPurchased = ToggleGroceryPurchasedUseCase(groceryRepository),
            clearPurchasedGrocery = ClearPurchasedGroceryUseCase(groceryRepository),
            addMissingRecipeIngredients = AddMissingRecipeIngredientsUseCase(groceryRepository),
            addGroceryItemToPantry = AddGroceryItemToPantryUseCase(pantryRepository)
        )

        val budgetDao = database.budgetDao()
        val budgetRepository = com.monteirogames.nutriverse.data.repository.BudgetRepositoryImpl(budgetDao, database.syncTombstoneDao())

        val foodExpenseDao = database.foodExpenseDao()
        val expenseRepository = com.monteirogames.nutriverse.data.repository.ExpenseRepositoryImpl(foodExpenseDao, database.syncTombstoneDao())

        budgetUseCases = BudgetUseCases(
            getActiveBudget = GetActiveBudgetUseCase(budgetRepository),
            saveBudget = SaveBudgetUseCase(budgetRepository),
            deleteBudget = DeleteBudgetUseCase(budgetRepository),
            calculateBudgetStatus = CalculateBudgetStatusUseCase(budgetRepository, expenseRepository)
        )

        expenseUseCases = ExpenseUseCases(
            getExpenses = GetExpensesUseCase(expenseRepository),
            getExpensesForPeriod = GetExpensesForPeriodUseCase(expenseRepository),
            addExpense = AddExpenseUseCase(expenseRepository),
            updateExpense = UpdateExpenseUseCase(expenseRepository),
            deleteExpense = DeleteExpenseUseCase(expenseRepository),
            getSpendingByCategory = GetSpendingByCategoryUseCase(expenseRepository),
            getTotalSpending = GetTotalSpendingUseCase(expenseRepository)
        )

        // Local Notifications
        com.monteirogames.nutriverse.notifications.NotificationChannels.createChannels(this)

        val notificationDao = database.notificationDao()
        val notificationRepository = com.monteirogames.nutriverse.data.repository.NotificationRepositoryImpl(notificationDao)

        notificationUseCases = NotificationUseCases(
            getNotificationSettings = GetNotificationSettingsUseCase(notificationRepository),
            updateNotificationSettings = UpdateNotificationSettingsUseCase(notificationRepository),
            evaluateNotifications = EvaluateNotificationsUseCase(
                notificationRepository = notificationRepository,
                mealRepository = mealRepository,
                userProfileRepository = userProfileRepository,
                calculateNutritionTargetsUseCase = userProfileUseCases.calculateNutritionTargets,
                pantryRepository = pantryRepository,
                groceryRepository = groceryRepository,
                budgetRepository = budgetRepository,
                expenseRepository = expenseRepository,
                calculateBudgetStatusUseCase = budgetUseCases.calculateBudgetStatus
            )
        )

        com.monteirogames.nutriverse.notifications.NotificationScheduler.schedulePeriodicEvaluation(this)

        progressAnalyticsUseCases = ProgressAnalyticsUseCases(
            calculateProgressAnalytics = CalculateProgressAnalyticsUseCase(
                mealRepository = mealRepository,
                userProfileRepository = userProfileRepository,
                calculateNutritionTargetsUseCase = userProfileUseCases.calculateNutritionTargets
            )
        )

        val healthReportDao = database.healthReportDao()
        val healthReportRepository = com.monteirogames.nutriverse.data.repository.HealthReportRepositoryImpl(healthReportDao, database.syncTombstoneDao())
        val healthReportAnalysisRepository = com.monteirogames.nutriverse.data.repository.HealthReportAnalysisRepositoryImpl()

        healthReportUseCases = HealthReportUseCases(
            analyzeHealthReport = AnalyzeHealthReportUseCase(healthReportAnalysisRepository),
            getHealthReportHistory = GetHealthReportHistoryUseCase(healthReportRepository),
            getHealthReport = GetHealthReportUseCase(healthReportRepository),
            saveHealthReport = SaveHealthReportUseCase(healthReportRepository),
            deleteHealthReport = DeleteHealthReportUseCase(healthReportRepository)
        )

        firestoreUserRepository = com.monteirogames.nutriverse.data.repository.FirestoreUserRepositoryImpl(
            authRepository = authRepository
        )
        profileSyncRepository = com.monteirogames.nutriverse.data.repository.ProfileSyncRepositoryImpl(
            authRepository = authRepository,
            userProfileRepository = userProfileRepository
        )
        profileSyncUseCases = com.monteirogames.nutriverse.domain.usecase.ProfileSyncUseCases(
            syncProfile = com.monteirogames.nutriverse.domain.usecase.SyncProfileUseCase(profileSyncRepository),
            observeProfileSyncState = com.monteirogames.nutriverse.domain.usecase.ObserveProfileSyncStateUseCase(profileSyncRepository)
        )
        mealSyncRepository = com.monteirogames.nutriverse.data.repository.MealSyncRepositoryImpl(
            authRepository = authRepository,
            mealRepository = mealRepository,
            foodItemDao = database.foodItemDao(),
            mealEntryDao = database.mealEntryDao(),
            syncTombstoneDao = database.syncTombstoneDao()
        )
        mealSyncUseCases = com.monteirogames.nutriverse.domain.usecase.MealSyncUseCases(
            syncMeals = com.monteirogames.nutriverse.domain.usecase.SyncMealsUseCase(mealSyncRepository),
            uploadMeal = com.monteirogames.nutriverse.domain.usecase.UploadMealUseCase(mealSyncRepository),
            deleteMealFromCloud = com.monteirogames.nutriverse.domain.usecase.DeleteMealFromCloudUseCase(mealSyncRepository),
            observeMealSyncState = com.monteirogames.nutriverse.domain.usecase.ObserveMealSyncStateUseCase(mealSyncRepository)
        )
        pantrySyncRepository = com.monteirogames.nutriverse.data.repository.PantrySyncRepositoryImpl(
            authRepository = authRepository,
            pantryRepository = pantryRepository,
            foodItemDao = database.foodItemDao(),
            pantryItemDao = database.pantryItemDao(),
            syncTombstoneDao = database.syncTombstoneDao()
        )
        pantrySyncUseCases = com.monteirogames.nutriverse.domain.usecase.PantrySyncUseCases(
            syncPantry = com.monteirogames.nutriverse.domain.usecase.SyncPantryUseCase(pantrySyncRepository),
            uploadPantryItem = com.monteirogames.nutriverse.domain.usecase.UploadPantryItemUseCase(pantrySyncRepository),
            deletePantryItemFromCloud = com.monteirogames.nutriverse.domain.usecase.DeletePantryItemFromCloudUseCase(pantrySyncRepository),
            observePantrySyncState = com.monteirogames.nutriverse.domain.usecase.ObservePantrySyncStateUseCase(pantrySyncRepository)
        )
        recipeSyncRepository = com.monteirogames.nutriverse.data.repository.RecipeSyncRepositoryImpl(
            authRepository = authRepository,
            recipeRepository = recipeRepository,
            syncTombstoneDao = database.syncTombstoneDao()
        )
        recipeSyncUseCases = com.monteirogames.nutriverse.domain.usecase.RecipeSyncUseCases(
            syncRecipes = com.monteirogames.nutriverse.domain.usecase.SyncRecipesUseCase(recipeSyncRepository),
            uploadRecipe = com.monteirogames.nutriverse.domain.usecase.UploadRecipeUseCase(recipeSyncRepository),
            deleteRecipeFromCloud = com.monteirogames.nutriverse.domain.usecase.DeleteRecipeFromCloudUseCase(recipeSyncRepository),
            observeRecipeSyncState = com.monteirogames.nutriverse.domain.usecase.ObserveRecipeSyncStateUseCase(recipeSyncRepository)
        )
        grocerySyncRepository = com.monteirogames.nutriverse.data.repository.GrocerySyncRepositoryImpl(
            authRepository = authRepository,
            groceryRepository = groceryRepository,
            groceryDao = database.groceryDao(),
            syncTombstoneDao = database.syncTombstoneDao()
        )
        grocerySyncUseCases = com.monteirogames.nutriverse.domain.usecase.GrocerySyncUseCases(
            syncGrocery = com.monteirogames.nutriverse.domain.usecase.SyncGroceryUseCase(grocerySyncRepository),
            uploadGroceryItem = com.monteirogames.nutriverse.domain.usecase.UploadGroceryItemUseCase(grocerySyncRepository),
            deleteGroceryItemFromCloud = com.monteirogames.nutriverse.domain.usecase.DeleteGroceryItemFromCloudUseCase(grocerySyncRepository),
            observeGrocerySyncState = com.monteirogames.nutriverse.domain.usecase.ObserveGrocerySyncStateUseCase(grocerySyncRepository)
        )
        budgetSyncRepository = com.monteirogames.nutriverse.data.repository.BudgetSyncRepositoryImpl(
            authRepository = authRepository,
            budgetRepository = budgetRepository,
            syncTombstoneDao = database.syncTombstoneDao()
        )
        budgetSyncUseCases = com.monteirogames.nutriverse.domain.usecase.BudgetSyncUseCases(
            syncBudget = com.monteirogames.nutriverse.domain.usecase.SyncBudgetUseCase(budgetSyncRepository),
            uploadBudget = com.monteirogames.nutriverse.domain.usecase.UploadBudgetUseCase(budgetSyncRepository),
            deleteBudgetFromCloud = com.monteirogames.nutriverse.domain.usecase.DeleteBudgetFromCloudUseCase(budgetSyncRepository),
            observeBudgetSyncState = com.monteirogames.nutriverse.domain.usecase.ObserveBudgetSyncStateUseCase(budgetSyncRepository)
        )
        expenseSyncRepository = com.monteirogames.nutriverse.data.repository.ExpenseSyncRepositoryImpl(
            authRepository = authRepository,
            expenseRepository = expenseRepository,
            syncTombstoneDao = database.syncTombstoneDao()
        )
        expenseSyncUseCases = com.monteirogames.nutriverse.domain.usecase.ExpenseSyncUseCases(
            syncExpenses = com.monteirogames.nutriverse.domain.usecase.SyncExpensesUseCase(expenseSyncRepository),
            uploadExpense = com.monteirogames.nutriverse.domain.usecase.UploadExpenseUseCase(expenseSyncRepository),
            deleteExpenseFromCloud = com.monteirogames.nutriverse.domain.usecase.DeleteExpenseFromCloudUseCase(expenseSyncRepository),
            observeExpenseSyncState = com.monteirogames.nutriverse.domain.usecase.ObserveExpenseSyncStateUseCase(expenseSyncRepository)
        )
        dietPlanSyncRepository = com.monteirogames.nutriverse.data.repository.DietPlanSyncRepositoryImpl(
            authRepository = authRepository,
            dietPlannerRepository = dietPlannerRepository,
            syncTombstoneDao = database.syncTombstoneDao()
        )
        dietPlanSyncUseCases = com.monteirogames.nutriverse.domain.usecase.DietPlanSyncUseCases(
            syncDietPlans = com.monteirogames.nutriverse.domain.usecase.SyncDietPlansUseCase(dietPlanSyncRepository),
            uploadDietPlan = com.monteirogames.nutriverse.domain.usecase.UploadDietPlanUseCase(dietPlanSyncRepository),
            deleteDietPlanFromCloud = com.monteirogames.nutriverse.domain.usecase.DeleteDietPlanFromCloudUseCase(dietPlanSyncRepository),
            observeDietPlanSyncState = com.monteirogames.nutriverse.domain.usecase.ObserveDietPlanSyncStateUseCase(dietPlanSyncRepository)
        )
        healthReportSyncRepository = com.monteirogames.nutriverse.data.repository.HealthReportSyncRepositoryImpl(
            authRepository = authRepository,
            healthReportRepository = healthReportRepository,
            syncTombstoneDao = database.syncTombstoneDao()
        )
        healthReportSyncUseCases = com.monteirogames.nutriverse.domain.usecase.HealthReportSyncUseCases(
            syncHealthReports = com.monteirogames.nutriverse.domain.usecase.SyncHealthReportsUseCase(healthReportSyncRepository),
            uploadHealthReport = com.monteirogames.nutriverse.domain.usecase.UploadHealthReportUseCase(healthReportSyncRepository),
            deleteHealthReportFromCloud = com.monteirogames.nutriverse.domain.usecase.DeleteHealthReportFromCloudUseCase(healthReportSyncRepository),
            observeHealthReportSyncState = com.monteirogames.nutriverse.domain.usecase.ObserveHealthReportSyncStateUseCase(healthReportSyncRepository)
        )
        syncRepository = com.monteirogames.nutriverse.data.repository.SyncRepositoryImpl(
            authRepository = authRepository,
            firestoreUserRepository = firestoreUserRepository,
            profileSyncRepository = profileSyncRepository,
            mealSyncRepository = mealSyncRepository,
            pantrySyncRepository = pantrySyncRepository,
            recipeSyncRepository = recipeSyncRepository,
            grocerySyncRepository = grocerySyncRepository,
            budgetSyncRepository = budgetSyncRepository,
            expenseSyncRepository = expenseSyncRepository,
            dietPlanSyncRepository = dietPlanSyncRepository,
            healthReportSyncRepository = healthReportSyncRepository
        )
        syncUseCases = SyncUseCases(
            getSyncState = GetSyncStateUseCase(syncRepository),
            observeSyncState = ObserveSyncStateUseCase(syncRepository),
            getCurrentUserDocument = GetCurrentUserDocumentUseCase(firestoreUserRepository),
            createOrUpdateUserDocument = CreateOrUpdateUserDocumentUseCase(firestoreUserRepository),
            syncAll = com.monteirogames.nutriverse.domain.usecase.SyncAllUseCase(syncRepository)
        )

        val weeklyReviewDao = database.weeklyReviewDao()
        weeklyReviewRepository = com.monteirogames.nutriverse.data.repository.WeeklyReviewRepositoryImpl(weeklyReviewDao)
        val weeklyReviewAnalysisRepository = com.monteirogames.nutriverse.data.repository.WeeklyReviewAnalysisRepositoryImpl()
        val calculateWeeklyMetricsUseCase = com.monteirogames.nutriverse.domain.usecase.CalculateWeeklyReviewMetricsUseCase(
            mealRepository = mealRepository,
            userProfileRepository = userProfileRepository,
            calculateNutritionTargetsUseCase = userProfileUseCases.calculateNutritionTargets
        )
        weeklyReviewUseCases = com.monteirogames.nutriverse.domain.usecase.WeeklyReviewUseCases(
            calculateMetrics = calculateWeeklyMetricsUseCase,
            generateWeeklyReview = com.monteirogames.nutriverse.domain.usecase.GenerateWeeklyReviewUseCase(
                calculateMetricsUseCase = calculateWeeklyMetricsUseCase,
                weeklyReviewRepository = weeklyReviewRepository,
                weeklyReviewAnalysisRepository = weeklyReviewAnalysisRepository
            ),
            getLatestWeeklyReview = com.monteirogames.nutriverse.domain.usecase.GetLatestWeeklyReviewUseCase(weeklyReviewRepository),
            getWeeklyReviewHistory = com.monteirogames.nutriverse.domain.usecase.GetWeeklyReviewHistoryUseCase(weeklyReviewRepository),
            getWeeklyReviewById = com.monteirogames.nutriverse.domain.usecase.GetWeeklyReviewByIdUseCase(weeklyReviewRepository),
            deleteWeeklyReview = com.monteirogames.nutriverse.domain.usecase.DeleteWeeklyReviewUseCase(weeklyReviewRepository)
        )

        foodCatalogRepository = com.monteirogames.nutriverse.data.repository.FoodCatalogRepositoryImpl()
        foodCatalogUseCases = com.monteirogames.nutriverse.domain.usecase.FoodCatalogUseCases(
            searchFoods = com.monteirogames.nutriverse.domain.usecase.SearchCatalogFoodsUseCase(foodCatalogRepository),
            getCategories = com.monteirogames.nutriverse.domain.usecase.GetCatalogCategoriesUseCase(foodCatalogRepository),
            getFoodById = com.monteirogames.nutriverse.domain.usecase.GetCatalogFoodByIdUseCase(foodCatalogRepository),
            addCatalogFoodToMeal = com.monteirogames.nutriverse.domain.usecase.AddCatalogFoodToMealUseCase(mealTrackerUseCases)
        )
    }
}
