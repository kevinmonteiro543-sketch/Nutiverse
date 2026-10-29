package com.monteirogames.nutriverse.domain.repository

import com.monteirogames.nutriverse.domain.model.SyncState
import kotlinx.coroutines.flow.StateFlow

/**
 * Sync abstraction for cloud data synchronization.
 * Local Room database remains the single source of truth for the UI.
 */
interface SyncRepository {
    val syncState: StateFlow<SyncState>
    fun getSyncState(): SyncState
    suspend fun resetSyncState()

    // Subsystem sync operations
    suspend fun syncProfile(): Result<Unit>
    suspend fun syncMeals(): Result<Unit>
    suspend fun syncPantry(): Result<Unit>
    suspend fun syncRecipes(): Result<Unit>
    suspend fun syncGroceries(): Result<Unit>
    suspend fun syncGrocery(): Result<Unit> = syncGroceries()
    suspend fun syncBudget(): Result<Unit>
    suspend fun syncExpenses(): Result<Unit>
    suspend fun syncDietPlans(): Result<Unit>
    suspend fun syncHealthReports(): Result<Unit>
    suspend fun syncAll(): Result<Unit>
}
