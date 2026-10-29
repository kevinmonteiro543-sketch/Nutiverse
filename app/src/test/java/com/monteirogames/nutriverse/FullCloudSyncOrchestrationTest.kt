package com.monteirogames.nutriverse

import com.monteirogames.nutriverse.data.repository.SyncRepositoryImpl
import com.monteirogames.nutriverse.domain.model.*
import com.monteirogames.nutriverse.domain.repository.*
import com.monteirogames.nutriverse.domain.sync.SyncResolution
import com.monteirogames.nutriverse.domain.sync.TimestampConflictResolverImpl
import com.monteirogames.nutriverse.domain.usecase.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class FullCloudSyncOrchestrationTest {

    class FakeAuthRepository(var uid: String? = "test_user_uid_123") : AuthRepository {
        private val _flow = MutableStateFlow(uid)
        override val currentUserFlow: Flow<String?> = _flow.asStateFlow()
        override fun getCurrentUserId(): String? = uid
        override fun getCurrentUser(): AuthenticatedUser? = uid?.let { AuthenticatedUser(it, "$it@test.com") }
        override fun observeAuthState(): Flow<AuthenticatedUser?> = MutableStateFlow(getCurrentUser())
        override suspend fun signIn(email: String, password: String): AuthResult<String> = AuthResult.Success(uid ?: "")
        override suspend fun signUp(email: String, password: String): AuthResult<String> = AuthResult.Success(uid ?: "")
        override suspend fun sendPasswordResetEmail(email: String): AuthResult<Unit> = AuthResult.Success(Unit)
        override suspend fun signOut(): Unit { uid = null }
    }

    class FakeFirestoreUserRepository(private val authRepository: AuthRepository) : FirestoreUserRepository {
        override suspend fun createOrUpdateUserDocument(document: FirestoreUserDocument): FirestoreResult<Unit> =
            FirestoreResult.Success(Unit)
        override suspend fun getUserDocument(uid: String): FirestoreResult<FirestoreUserDocument?> =
            FirestoreResult.Success(null)
        override suspend fun getCurrentUserDocument(): FirestoreResult<FirestoreUserDocument?> {
            val uid = authRepository.getCurrentUserId() ?: return FirestoreResult.Error("Not authenticated")
            return FirestoreResult.Success(FirestoreUserDocument(uid = uid, email = "$uid@test.com"))
        }
    }

    class FakeProfileSyncRepository : ProfileSyncRepository {
        var syncCount = 0
        var shouldFail = false
        private val _state = MutableStateFlow(SyncState())
        override val syncState = _state.asStateFlow()
        override fun getSyncState(): SyncState = _state.value
        override suspend fun syncProfile(): Result<UserProfile?> {
            syncCount++
            return if (shouldFail) Result.failure(Exception("Network error in Profile"))
            else Result.success(null)
        }
        override suspend fun uploadProfile(profile: UserProfile): Result<Unit> = Result.success(Unit)
        override suspend fun downloadProfile(): Result<UserProfile?> = Result.success(null)
    }

    class FakeMealSyncRepository : MealSyncRepository {
        var syncCount = 0
        var shouldFail = false
        private val _state = MutableStateFlow(SyncState())
        override val syncState = _state.asStateFlow()
        override fun getSyncState(): SyncState = _state.value
        override suspend fun syncMeals(): Result<Int> {
            syncCount++
            return if (shouldFail) Result.failure(Exception("Network error in Meals"))
            else Result.success(3)
        }
        override suspend fun uploadMeal(meal: MealEntry): Result<Unit> = Result.success(Unit)
        override suspend fun deleteMealInCloud(mealId: String): Result<Unit> = Result.success(Unit)
    }

    class FakePantrySyncRepository : PantrySyncRepository {
        var syncCount = 0
        var shouldFail = false
        private val _state = MutableStateFlow(SyncState())
        override val syncState = _state.asStateFlow()
        override fun getSyncState(): SyncState = _state.value
        override suspend fun syncPantry(): Result<Int> {
            syncCount++
            return if (shouldFail) Result.failure(Exception("Network error in Pantry"))
            else Result.success(5)
        }
        override suspend fun uploadPantryItem(item: PantryItem): Result<Unit> = Result.success(Unit)
        override suspend fun deletePantryItemInCloud(itemId: String): Result<Unit> = Result.success(Unit)
    }

    class FakeRecipeSyncRepository : RecipeSyncRepository {
        var syncCount = 0
        var shouldFail = false
        private val _state = MutableStateFlow(SyncState())
        override val syncState = _state.asStateFlow()
        override fun getSyncState(): SyncState = _state.value
        override suspend fun syncRecipes(): Result<Int> {
            syncCount++
            return if (shouldFail) Result.failure(Exception("Network error in Recipes"))
            else Result.success(2)
        }
        override suspend fun uploadRecipe(recipe: Recipe): Result<Unit> = Result.success(Unit)
        override suspend fun deleteRecipeInCloud(recipeId: String): Result<Unit> = Result.success(Unit)
    }

    class FakeGrocerySyncRepository : GrocerySyncRepository {
        var syncCount = 0
        var shouldFail = false
        private val _state = MutableStateFlow(SyncState())
        override val syncState = _state.asStateFlow()
        override fun getSyncState(): SyncState = _state.value
        override suspend fun syncGrocery(): Result<Int> {
            syncCount++
            return if (shouldFail) Result.failure(Exception("Network error in Grocery"))
            else Result.success(4)
        }
        override suspend fun uploadGroceryItem(item: GroceryItem): Result<Unit> = Result.success(Unit)
        override suspend fun deleteGroceryItemInCloud(itemId: String): Result<Unit> = Result.success(Unit)
    }

    class FakeBudgetSyncRepository : BudgetSyncRepository {
        var syncCount = 0
        var shouldFail = false
        private val _state = MutableStateFlow(SyncState())
        override val syncState = _state.asStateFlow()
        override fun getSyncState(): SyncState = _state.value
        override suspend fun syncBudget(): Result<Int> {
            syncCount++
            return if (shouldFail) Result.failure(Exception("Network error in Budget"))
            else Result.success(1)
        }
        override suspend fun uploadBudget(budget: Budget): Result<Unit> = Result.success(Unit)
        override suspend fun deleteBudgetInCloud(budgetId: String): Result<Unit> = Result.success(Unit)
    }

    class FakeExpenseSyncRepository : ExpenseSyncRepository {
        var syncCount = 0
        var shouldFail = false
        private val _state = MutableStateFlow(SyncState())
        override val syncState = _state.asStateFlow()
        override fun getSyncState(): SyncState = _state.value
        override suspend fun syncExpenses(): Result<Int> {
            syncCount++
            return if (shouldFail) Result.failure(Exception("Network error in Expenses"))
            else Result.success(6)
        }
        override suspend fun uploadExpense(expense: FoodExpense): Result<Unit> = Result.success(Unit)
        override suspend fun deleteExpenseInCloud(expenseId: String): Result<Unit> = Result.success(Unit)
    }

    class FakeDietPlanSyncRepository : DietPlanSyncRepository {
        var syncCount = 0
        var shouldFail = false
        private val _state = MutableStateFlow(SyncState())
        override val syncState = _state.asStateFlow()
        override fun getSyncState(): SyncState = _state.value
        override suspend fun syncDietPlans(): Result<Int> {
            syncCount++
            return if (shouldFail) Result.failure(Exception("Network error in DietPlans"))
            else Result.success(1)
        }
        override suspend fun uploadDietPlan(plan: DietPlan): Result<Unit> = Result.success(Unit)
        override suspend fun deleteDietPlanInCloud(planId: String): Result<Unit> = Result.success(Unit)
    }

    class FakeHealthReportSyncRepository : HealthReportSyncRepository {
        var syncCount = 0
        var shouldFail = false
        private val _state = MutableStateFlow(SyncState())
        override val syncState = _state.asStateFlow()
        override fun getSyncState(): SyncState = _state.value
        override suspend fun syncHealthReports(): Result<Int> {
            syncCount++
            return if (shouldFail) Result.failure(Exception("Network error in HealthReports"))
            else Result.success(2)
        }
        override suspend fun uploadHealthReport(report: HealthReport): Result<Unit> = Result.success(Unit)
        override suspend fun deleteHealthReportInCloud(reportId: String): Result<Unit> = Result.success(Unit)
    }

    private lateinit var authRepo: FakeAuthRepository
    private lateinit var firestoreUserRepo: FakeFirestoreUserRepository
    private lateinit var profileSync: FakeProfileSyncRepository
    private lateinit var mealSync: FakeMealSyncRepository
    private lateinit var pantrySync: FakePantrySyncRepository
    private lateinit var recipeSync: FakeRecipeSyncRepository
    private lateinit var grocerySync: FakeGrocerySyncRepository
    private lateinit var budgetSync: FakeBudgetSyncRepository
    private lateinit var expenseSync: FakeExpenseSyncRepository
    private lateinit var dietPlanSync: FakeDietPlanSyncRepository
    private lateinit var healthReportSync: FakeHealthReportSyncRepository
    private lateinit var syncRepository: SyncRepositoryImpl
    private lateinit var syncUseCases: SyncUseCases

    @Before
    fun setup() {
        authRepo = FakeAuthRepository("user_uid_verified")
        firestoreUserRepo = FakeFirestoreUserRepository(authRepo)
        profileSync = FakeProfileSyncRepository()
        mealSync = FakeMealSyncRepository()
        pantrySync = FakePantrySyncRepository()
        recipeSync = FakeRecipeSyncRepository()
        grocerySync = FakeGrocerySyncRepository()
        budgetSync = FakeBudgetSyncRepository()
        expenseSync = FakeExpenseSyncRepository()
        dietPlanSync = FakeDietPlanSyncRepository()
        healthReportSync = FakeHealthReportSyncRepository()

        syncRepository = SyncRepositoryImpl(
            authRepository = authRepo,
            firestoreUserRepository = firestoreUserRepo,
            profileSyncRepository = profileSync,
            mealSyncRepository = mealSync,
            pantrySyncRepository = pantrySync,
            recipeSyncRepository = recipeSync,
            grocerySyncRepository = grocerySync,
            budgetSyncRepository = budgetSync,
            expenseSyncRepository = expenseSync,
            dietPlanSyncRepository = dietPlanSync,
            healthReportSyncRepository = healthReportSync
        )

        syncUseCases = SyncUseCases(
            getSyncState = GetSyncStateUseCase(syncRepository),
            observeSyncState = ObserveSyncStateUseCase(syncRepository),
            getCurrentUserDocument = GetCurrentUserDocumentUseCase(firestoreUserRepo),
            createOrUpdateUserDocument = CreateOrUpdateUserDocumentUseCase(firestoreUserRepo),
            syncAll = SyncAllUseCase(syncRepository)
        )
    }

    @Test
    fun test1_SyncAllOrchestratesAll9DomainsSuccessfully() = runTest {
        assertEquals(SyncStatus.IDLE, syncRepository.getSyncState().status)

        val result = syncUseCases.syncAll()
        assertTrue(result.isSuccess)

        // Verify every single domain sync was executed
        assertEquals(1, profileSync.syncCount)
        assertEquals(1, mealSync.syncCount)
        assertEquals(1, pantrySync.syncCount)
        assertEquals(1, recipeSync.syncCount)
        assertEquals(1, grocerySync.syncCount)
        assertEquals(1, budgetSync.syncCount)
        assertEquals(1, expenseSync.syncCount)
        assertEquals(1, dietPlanSync.syncCount)
        assertEquals(1, healthReportSync.syncCount)

        // Verify unified sync state
        val state = syncRepository.getSyncState()
        assertEquals(SyncStatus.SUCCESS, state.status)
        assertNotNull(state.lastSyncedAt)
        assertTrue(state.lastSyncedAt!! > 0)
        assertNull(state.errorMessage)
    }

    @Test
    fun test2_SyncAllFailsWhenUserUnauthenticated() = runTest {
        authRepo.uid = null

        val result = syncRepository.syncAll()
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("User is not authenticated") == true)

        // Subsystems must never be invoked if unauthenticated
        assertEquals(0, profileSync.syncCount)
        assertEquals(0, mealSync.syncCount)
        assertEquals(0, pantrySync.syncCount)
        assertEquals(0, recipeSync.syncCount)
        assertEquals(0, grocerySync.syncCount)
        assertEquals(0, budgetSync.syncCount)
        assertEquals(0, expenseSync.syncCount)
        assertEquals(0, dietPlanSync.syncCount)
        assertEquals(0, healthReportSync.syncCount)

        val state = syncRepository.getSyncState()
        assertEquals(SyncStatus.ERROR, state.status)
        assertEquals("Cannot sync: User is not authenticated.", state.errorMessage)
    }

    @Test
    fun test3_SyncAllHandlesSubsystemPartialFailuresGracefully() = runTest {
        grocerySync.shouldFail = true
        dietPlanSync.shouldFail = true

        val result = syncRepository.syncAll()
        assertTrue(result.isFailure)

        // Verify all subsystems still attempted
        assertEquals(1, profileSync.syncCount)
        assertEquals(1, mealSync.syncCount)
        assertEquals(1, pantrySync.syncCount)
        assertEquals(1, recipeSync.syncCount)
        assertEquals(1, grocerySync.syncCount)
        assertEquals(1, budgetSync.syncCount)
        assertEquals(1, expenseSync.syncCount)
        assertEquals(1, dietPlanSync.syncCount)
        assertEquals(1, healthReportSync.syncCount)

        val state = syncRepository.getSyncState()
        assertEquals(SyncStatus.ERROR, state.status)
        assertNotNull(state.errorMessage)
        assertTrue(state.errorMessage!!.contains("Grocery"))
        assertTrue(state.errorMessage!!.contains("DietPlans"))
    }

    @Test
    fun test4_ResetSyncStateRestoresIdle() = runTest {
        syncRepository.syncAll()
        assertEquals(SyncStatus.SUCCESS, syncRepository.getSyncState().status)

        syncRepository.resetSyncState()
        val resetState = syncRepository.getSyncState()
        assertEquals(SyncStatus.IDLE, resetState.status)
        assertNull(resetState.errorMessage)
    }

    @Test
    fun test5_FirestorePathsIntegrityFor9Domains() {
        val uid = "verified_user_99"
        assertEquals("users/$uid", FirestoreCollections.userDocPath(uid))
        assertEquals("users/$uid/meals/meal_1", FirestoreCollections.userMealDocPath(uid, "meal_1"))
        assertEquals("users/$uid/pantry/pantry_1", FirestoreCollections.userPantryDocPath(uid, "pantry_1"))
        assertEquals("users/$uid/recipes/recipe_1", FirestoreCollections.userRecipeDocPath(uid, "recipe_1"))
        assertEquals("users/$uid/grocery/grocery_1", FirestoreCollections.userGroceryDocPath(uid, "grocery_1"))
        assertEquals("users/$uid/expenses/expense_1", FirestoreCollections.userExpenseDocPath(uid, "expense_1"))
        assertEquals("users/$uid/budget/budget_1", FirestoreCollections.userBudgetDocPath(uid, "budget_1"))
        assertEquals("users/$uid/dietPlans/diet_1", FirestoreCollections.userDietPlanDocPath(uid, "diet_1"))
        assertEquals("users/$uid/healthReports/report_1", FirestoreCollections.userHealthReportDocPath(uid, "report_1"))
    }

    @Test
    fun test6_UserIsolationEnforcedAcrossAllEndpoints() {
        val uidA = "user_A"
        val uidB = "user_B"

        assertNotEquals(FirestoreCollections.userDocPath(uidA), FirestoreCollections.userDocPath(uidB))
        assertNotEquals(FirestoreCollections.userMealsPath(uidA), FirestoreCollections.userMealsPath(uidB))
        assertNotEquals(FirestoreCollections.userPantryPath(uidA), FirestoreCollections.userPantryPath(uidB))
        assertNotEquals(FirestoreCollections.userRecipesPath(uidA), FirestoreCollections.userRecipesPath(uidB))
        assertNotEquals(FirestoreCollections.userGroceryPath(uidA), FirestoreCollections.userGroceryPath(uidB))
        assertNotEquals(FirestoreCollections.userExpensesPath(uidA), FirestoreCollections.userExpensesPath(uidB))
        assertNotEquals(FirestoreCollections.userBudgetPath(uidA), FirestoreCollections.userBudgetPath(uidB))
        assertNotEquals(FirestoreCollections.userDietPlansPath(uidA), FirestoreCollections.userDietPlansPath(uidB))
        assertNotEquals(FirestoreCollections.userHealthReportsPath(uidA), FirestoreCollections.userHealthReportsPath(uidB))
    }

    @Test
    fun test7_TimestampConflictResolverUniformity() {
        val resolver = TimestampConflictResolverImpl()
        data class TestItem(val id: String, val time: Long)

        val localOld = TestItem("1", 1000L)
        val cloudNew = TestItem("1", 2000L)
        val resolution1 = resolver.resolve(
            localItem = localOld,
            cloudItem = cloudNew,
            getLocalTimestamp = { it.time },
            getCloudTimestamp = { it.time }
        )
        assertTrue(resolution1 is SyncResolution.DownloadToLocal)
        assertEquals(cloudNew, (resolution1 as SyncResolution.DownloadToLocal).cloudItem)

        val localNew = TestItem("1", 3000L)
        val cloudOld = TestItem("1", 2000L)
        val resolution2 = resolver.resolve(
            localItem = localNew,
            cloudItem = cloudOld,
            getLocalTimestamp = { it.time },
            getCloudTimestamp = { it.time }
        )
        assertTrue(resolution2 is SyncResolution.UploadToCloud)
        assertEquals(localNew, (resolution2 as SyncResolution.UploadToCloud).localItem)

        val equalLocal = TestItem("1", 2000L)
        val equalCloud = TestItem("1", 2000L)
        val resolution3 = resolver.resolve(
            localItem = equalLocal,
            cloudItem = equalCloud,
            getLocalTimestamp = { it.time },
            getCloudTimestamp = { it.time }
        )
        assertTrue(resolution3 is SyncResolution.UpToDate)

        val resolution4 = resolver.resolve(
            localItem = localNew,
            cloudItem = null,
            getLocalTimestamp = { it.time },
            getCloudTimestamp = { it.time }
        )
        assertTrue(resolution4 is SyncResolution.UploadToCloud)

        val resolution5 = resolver.resolve(
            localItem = null,
            cloudItem = cloudNew,
            getLocalTimestamp = { it.time },
            getCloudTimestamp = { it.time }
        )
        assertTrue(resolution5 is SyncResolution.DownloadToLocal)
    }
}
