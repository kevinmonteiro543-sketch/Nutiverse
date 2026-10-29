package com.monteirogames.nutriverse.domain.repository

import com.monteirogames.nutriverse.domain.model.DietPlan
import com.monteirogames.nutriverse.domain.model.SyncState
import kotlinx.coroutines.flow.StateFlow

interface DietPlanSyncRepository {
    val syncState: StateFlow<SyncState>
    fun getSyncState(): SyncState
    suspend fun syncDietPlans(): Result<Int>
    suspend fun uploadDietPlan(plan: DietPlan): Result<Unit>
    suspend fun deleteDietPlanInCloud(planId: String): Result<Unit>
}
