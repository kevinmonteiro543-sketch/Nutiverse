package com.monteirogames.nutriverse.domain.usecase

import com.monteirogames.nutriverse.domain.model.DietPlan
import com.monteirogames.nutriverse.domain.model.SyncState
import com.monteirogames.nutriverse.domain.repository.DietPlanSyncRepository
import kotlinx.coroutines.flow.StateFlow

class SyncDietPlansUseCase(private val repository: DietPlanSyncRepository) {
    suspend operator fun invoke(): Result<Int> = repository.syncDietPlans()
}

class UploadDietPlanUseCase(private val repository: DietPlanSyncRepository) {
    suspend operator fun invoke(plan: DietPlan): Result<Unit> = repository.uploadDietPlan(plan)
}

class DeleteDietPlanFromCloudUseCase(private val repository: DietPlanSyncRepository) {
    suspend operator fun invoke(planId: String): Result<Unit> = repository.deleteDietPlanInCloud(planId)
}

class ObserveDietPlanSyncStateUseCase(private val repository: DietPlanSyncRepository) {
    operator fun invoke(): StateFlow<SyncState> = repository.syncState
}

data class DietPlanSyncUseCases(
    val syncDietPlans: SyncDietPlansUseCase,
    val uploadDietPlan: UploadDietPlanUseCase,
    val deleteDietPlanFromCloud: DeleteDietPlanFromCloudUseCase,
    val observeDietPlanSyncState: ObserveDietPlanSyncStateUseCase
)
