package com.monteirogames.nutriverse.domain.usecase

import com.monteirogames.nutriverse.domain.model.Budget
import com.monteirogames.nutriverse.domain.model.SyncState
import com.monteirogames.nutriverse.domain.repository.BudgetSyncRepository
import kotlinx.coroutines.flow.StateFlow

class SyncBudgetUseCase(private val repository: BudgetSyncRepository) {
    suspend operator fun invoke(): Result<Int> = repository.syncBudget()
}

class UploadBudgetUseCase(private val repository: BudgetSyncRepository) {
    suspend operator fun invoke(budget: Budget): Result<Unit> = repository.uploadBudget(budget)
}

class DeleteBudgetFromCloudUseCase(private val repository: BudgetSyncRepository) {
    suspend operator fun invoke(budgetId: String): Result<Unit> = repository.deleteBudgetInCloud(budgetId)
}

class ObserveBudgetSyncStateUseCase(private val repository: BudgetSyncRepository) {
    operator fun invoke(): StateFlow<SyncState> = repository.syncState
}

data class BudgetSyncUseCases(
    val syncBudget: SyncBudgetUseCase,
    val uploadBudget: UploadBudgetUseCase,
    val deleteBudgetFromCloud: DeleteBudgetFromCloudUseCase,
    val observeBudgetSyncState: ObserveBudgetSyncStateUseCase
)
