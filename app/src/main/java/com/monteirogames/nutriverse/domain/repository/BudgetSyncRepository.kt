package com.monteirogames.nutriverse.domain.repository

import com.monteirogames.nutriverse.domain.model.Budget
import com.monteirogames.nutriverse.domain.model.SyncState
import kotlinx.coroutines.flow.StateFlow

/**
 * Dedicated repository abstraction for Budget cloud synchronization.
 * Synchronizes budget configuration between local Room and Firestore `users/{FirebaseUID}/budget/{budgetId}`.
 */
interface BudgetSyncRepository {
    val syncState: StateFlow<SyncState>
    fun getSyncState(): SyncState

    suspend fun syncBudget(): Result<Int>
    suspend fun uploadBudget(budget: Budget): Result<Unit>
    suspend fun deleteBudgetInCloud(budgetId: String): Result<Unit>
}
