package com.monteirogames.nutriverse.domain.usecase

import com.monteirogames.nutriverse.domain.model.DietPlan
import com.monteirogames.nutriverse.domain.model.UserProfile
import com.monteirogames.nutriverse.domain.repository.DietPlannerRepository
import kotlinx.coroutines.flow.Flow

class GenerateDietPlanUseCase(private val repository: DietPlannerRepository) {
    suspend operator fun invoke(
        userProfile: UserProfile,
        nutritionTargets: NutritionTargets
    ): Result<DietPlan> {
        return repository.generate7DayPlan(userProfile, nutritionTargets)
    }
}

class SaveDietPlanUseCase(private val repository: DietPlannerRepository) {
    suspend operator fun invoke(plan: DietPlan) {
        repository.saveDietPlan(plan)
    }
}

class GetActiveDietPlanUseCase(private val repository: DietPlannerRepository) {
    operator fun invoke(userId: String): Flow<DietPlan?> {
        return repository.getActiveDietPlan(userId)
    }
}

class ClearDietPlanUseCase(private val repository: DietPlannerRepository) {
    suspend operator fun invoke(userId: String) {
        repository.clearDietPlan(userId)
    }
}

data class DietPlannerUseCases(
    val generateDietPlan: GenerateDietPlanUseCase,
    val saveDietPlan: SaveDietPlanUseCase,
    val getActiveDietPlan: GetActiveDietPlanUseCase,
    val clearDietPlan: ClearDietPlanUseCase
)
