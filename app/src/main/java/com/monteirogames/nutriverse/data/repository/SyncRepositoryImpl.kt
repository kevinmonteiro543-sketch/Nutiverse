package com.monteirogames.nutriverse.data.repository

import com.monteirogames.nutriverse.domain.model.SyncState
import com.monteirogames.nutriverse.domain.model.SyncStatus
import com.monteirogames.nutriverse.domain.repository.AuthRepository
import com.monteirogames.nutriverse.domain.repository.BudgetSyncRepository
import com.monteirogames.nutriverse.domain.repository.DietPlanSyncRepository
import com.monteirogames.nutriverse.domain.repository.ExpenseSyncRepository
import com.monteirogames.nutriverse.domain.repository.FirestoreUserRepository
import com.monteirogames.nutriverse.domain.repository.GrocerySyncRepository
import com.monteirogames.nutriverse.domain.repository.HealthReportSyncRepository
import com.monteirogames.nutriverse.domain.repository.MealSyncRepository
import com.monteirogames.nutriverse.domain.repository.PantrySyncRepository
import com.monteirogames.nutriverse.domain.repository.ProfileSyncRepository
import com.monteirogames.nutriverse.domain.repository.RecipeSyncRepository
import com.monteirogames.nutriverse.domain.repository.SyncRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SyncRepositoryImpl(
    private val authRepository: AuthRepository,
    private val firestoreUserRepository: FirestoreUserRepository,
    private val profileSyncRepository: ProfileSyncRepository? = null,
    private val mealSyncRepository: MealSyncRepository? = null,
    private val pantrySyncRepository: PantrySyncRepository? = null,
    private val recipeSyncRepository: RecipeSyncRepository? = null,
    private val grocerySyncRepository: GrocerySyncRepository? = null,
    private val budgetSyncRepository: BudgetSyncRepository? = null,
    private val expenseSyncRepository: ExpenseSyncRepository? = null,
    private val dietPlanSyncRepository: DietPlanSyncRepository? = null,
    private val healthReportSyncRepository: HealthReportSyncRepository? = null
) : SyncRepository {

    private val _syncState = MutableStateFlow(SyncState(status = SyncStatus.IDLE))
    override val syncState: StateFlow<SyncState> = _syncState.asStateFlow()

    override fun getSyncState(): SyncState = _syncState.value

    override suspend fun resetSyncState() {
        _syncState.value = SyncState(status = SyncStatus.IDLE)
    }

    // Phase 7B-2: Real Profile synchronization wired to ProfileSyncRepository
    override suspend fun syncProfile(): Result<Unit> {
        return profileSyncRepository?.syncProfile()?.map { } ?: Result.success(Unit)
    }

    // Phase 7B-3: Real Meals and Pantry synchronization wired to MealSyncRepository & PantrySyncRepository
    override suspend fun syncMeals(): Result<Unit> {
        return mealSyncRepository?.syncMeals()?.map { } ?: Result.success(Unit)
    }

    override suspend fun syncPantry(): Result<Unit> {
        return pantrySyncRepository?.syncPantry()?.map { } ?: Result.success(Unit)
    }

    // Phase 7B-4: Real Recipes, Grocery, Budget, and Expenses synchronization
    override suspend fun syncRecipes(): Result<Unit> {
        return recipeSyncRepository?.syncRecipes()?.map { } ?: Result.success(Unit)
    }

    override suspend fun syncGroceries(): Result<Unit> {
        return grocerySyncRepository?.syncGrocery()?.map { } ?: Result.success(Unit)
    }

    override suspend fun syncBudget(): Result<Unit> {
        return budgetSyncRepository?.syncBudget()?.map { } ?: Result.success(Unit)
    }

    override suspend fun syncExpenses(): Result<Unit> {
        return expenseSyncRepository?.syncExpenses()?.map { } ?: Result.success(Unit)
    }

    // Phase 7B-5: Real Diet Plans and Health Reports synchronization
    override suspend fun syncDietPlans(): Result<Unit> {
        return dietPlanSyncRepository?.syncDietPlans()?.map { } ?: Result.success(Unit)
    }

    override suspend fun syncHealthReports(): Result<Unit> {
        return healthReportSyncRepository?.syncHealthReports()?.map { } ?: Result.success(Unit)
    }

    // Phase 7B-6: Full Cloud Sync Orchestration across all 9 domains
    override suspend fun syncAll(): Result<Unit> {
        val currentUid = authRepository.getCurrentUserId()
        if (currentUid.isNullOrBlank()) {
            val err = "Cannot sync: User is not authenticated."
            _syncState.value = SyncState(status = SyncStatus.ERROR, errorMessage = err)
            return Result.failure(IllegalStateException(err))
        }

        _syncState.value = SyncState(status = SyncStatus.SYNCING)

        val failures = mutableListOf<String>()

        val profileRes = syncProfile()
        if (profileRes.isFailure) failures.add("Profile: ${profileRes.exceptionOrNull()?.message ?: "error"}")

        val mealRes = syncMeals()
        if (mealRes.isFailure) failures.add("Meals: ${mealRes.exceptionOrNull()?.message ?: "error"}")

        val pantryRes = syncPantry()
        if (pantryRes.isFailure) failures.add("Pantry: ${pantryRes.exceptionOrNull()?.message ?: "error"}")

        val recipeRes = syncRecipes()
        if (recipeRes.isFailure) failures.add("Recipes: ${recipeRes.exceptionOrNull()?.message ?: "error"}")

        val groceryRes = syncGroceries()
        if (groceryRes.isFailure) failures.add("Grocery: ${groceryRes.exceptionOrNull()?.message ?: "error"}")

        val budgetRes = syncBudget()
        if (budgetRes.isFailure) failures.add("Budget: ${budgetRes.exceptionOrNull()?.message ?: "error"}")

        val expenseRes = syncExpenses()
        if (expenseRes.isFailure) failures.add("Expenses: ${expenseRes.exceptionOrNull()?.message ?: "error"}")

        val dietPlanRes = syncDietPlans()
        if (dietPlanRes.isFailure) failures.add("DietPlans: ${dietPlanRes.exceptionOrNull()?.message ?: "error"}")

        val healthReportRes = syncHealthReports()
        if (healthReportRes.isFailure) failures.add("HealthReports: ${healthReportRes.exceptionOrNull()?.message ?: "error"}")

        return if (failures.isEmpty()) {
            val now = System.currentTimeMillis()
            _syncState.value = SyncState(status = SyncStatus.SUCCESS, lastSyncedAt = now)
            Result.success(Unit)
        } else {
            val errorMsg = "Sync completed with failures: ${failures.joinToString("; ")}"
            _syncState.value = SyncState(status = SyncStatus.ERROR, errorMessage = errorMsg)
            Result.failure(Exception(errorMsg))
        }
    }
}
