package com.monteirogames.nutriverse.domain.repository

import com.monteirogames.nutriverse.domain.model.MealEntry
import com.monteirogames.nutriverse.domain.model.SyncState
import kotlinx.coroutines.flow.StateFlow

/**
 * Dedicated repository abstraction for Meal cloud synchronization.
 * Synchronizes meal entries between local Room and Firestore users/{FirebaseUID}/meals/{mealId}.
 */
interface MealSyncRepository {
    val syncState: StateFlow<SyncState>
    fun getSyncState(): SyncState

    suspend fun syncMeals(): Result<Int>
    suspend fun uploadMeal(meal: MealEntry): Result<Unit>
    suspend fun deleteMealInCloud(mealId: String): Result<Unit>
}
