package com.monteirogames.nutriverse.domain.usecase

import com.monteirogames.nutriverse.domain.model.FoodExpense
import com.monteirogames.nutriverse.domain.model.SyncState
import com.monteirogames.nutriverse.domain.repository.ExpenseSyncRepository
import kotlinx.coroutines.flow.StateFlow

class SyncExpensesUseCase(private val repository: ExpenseSyncRepository) {
    suspend operator fun invoke(): Result<Int> = repository.syncExpenses()
}

class UploadExpenseUseCase(private val repository: ExpenseSyncRepository) {
    suspend operator fun invoke(expense: FoodExpense): Result<Unit> = repository.uploadExpense(expense)
}

class DeleteExpenseFromCloudUseCase(private val repository: ExpenseSyncRepository) {
    suspend operator fun invoke(expenseId: String): Result<Unit> = repository.deleteExpenseInCloud(expenseId)
}

class ObserveExpenseSyncStateUseCase(private val repository: ExpenseSyncRepository) {
    operator fun invoke(): StateFlow<SyncState> = repository.syncState
}

data class ExpenseSyncUseCases(
    val syncExpenses: SyncExpensesUseCase,
    val uploadExpense: UploadExpenseUseCase,
    val deleteExpenseFromCloud: DeleteExpenseFromCloudUseCase,
    val observeExpenseSyncState: ObserveExpenseSyncStateUseCase
)
