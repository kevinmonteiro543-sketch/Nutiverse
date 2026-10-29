package com.monteirogames.nutriverse.domain.repository

import com.monteirogames.nutriverse.domain.model.FoodExpense
import com.monteirogames.nutriverse.domain.model.SyncState
import kotlinx.coroutines.flow.StateFlow

/**
 * Dedicated repository abstraction for Food Expense cloud synchronization.
 * Synchronizes expenses between local Room and Firestore `users/{FirebaseUID}/expenses/{expenseId}`.
 */
interface ExpenseSyncRepository {
    val syncState: StateFlow<SyncState>
    fun getSyncState(): SyncState

    suspend fun syncExpenses(): Result<Int>
    suspend fun uploadExpense(expense: FoodExpense): Result<Unit>
    suspend fun deleteExpenseInCloud(expenseId: String): Result<Unit>
}
