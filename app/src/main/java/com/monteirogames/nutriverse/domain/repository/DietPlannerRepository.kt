package com.monteirogames.nutriverse.domain.repository

import com.monteirogames.nutriverse.domain.model.DietPlan
import com.monteirogames.nutriverse.domain.model.UserProfile
import com.monteirogames.nutriverse.domain.usecase.NutritionTargets
import kotlinx.coroutines.flow.Flow

interface DietPlannerRepository {
    suspend fun generate7DayPlan(
        userProfile: UserProfile,
        nutritionTargets: NutritionTargets
    ): Result<DietPlan>

    suspend fun saveDietPlan(plan: DietPlan)

    fun getActiveDietPlan(userId: String): Flow<DietPlan?>
    suspend fun getActiveDietPlanOnce(userId: String): DietPlan? = null

    fun getAllDietPlans(userId: String): Flow<List<DietPlan>>
    suspend fun getAllDietPlansList(userId: String): List<DietPlan>

    suspend fun getDietPlanById(planId: String, userId: String): DietPlan?

    suspend fun deleteDietPlan(planId: String, userId: String)

    suspend fun clearDietPlan(userId: String)
}
