package com.monteirogames.nutriverse.domain.usecase

import com.monteirogames.nutriverse.domain.model.MealEntry
import com.monteirogames.nutriverse.domain.model.SyncState
import com.monteirogames.nutriverse.domain.repository.MealSyncRepository
import kotlinx.coroutines.flow.StateFlow

class SyncMealsUseCase(private val repository: MealSyncRepository) {
    suspend operator fun invoke(): Result<Int> {
        return repository.syncMeals()
    }
}

class UploadMealUseCase(private val repository: MealSyncRepository) {
    suspend operator fun invoke(meal: MealEntry): Result<Unit> {
        return repository.uploadMeal(meal)
    }
}

class DeleteMealFromCloudUseCase(private val repository: MealSyncRepository) {
    suspend operator fun invoke(mealId: String): Result<Unit> {
        return repository.deleteMealInCloud(mealId)
    }
}

class ObserveMealSyncStateUseCase(private val repository: MealSyncRepository) {
    operator fun invoke(): StateFlow<SyncState> {
        return repository.syncState
    }
}

data class MealSyncUseCases(
    val syncMeals: SyncMealsUseCase,
    val uploadMeal: UploadMealUseCase,
    val deleteMealFromCloud: DeleteMealFromCloudUseCase,
    val observeMealSyncState: ObserveMealSyncStateUseCase
)
