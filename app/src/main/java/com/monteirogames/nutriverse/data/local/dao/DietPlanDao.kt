package com.monteirogames.nutriverse.data.local.dao

import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Relation
import androidx.room.Transaction
import com.monteirogames.nutriverse.data.local.entity.DietPlanEntity
import com.monteirogames.nutriverse.data.local.entity.DietPlanMealEntity
import kotlinx.coroutines.flow.Flow

data class DietPlanWithMeals(
    @Embedded val plan: DietPlanEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "planId"
    )
    val meals: List<DietPlanMealEntity>
)

@Dao
interface DietPlanDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlan(plan: DietPlanEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMeals(meals: List<DietPlanMealEntity>)

    @Query("UPDATE diet_plan SET isActive = 0 WHERE userId = :userId")
    suspend fun deactivatePlansForUser(userId: String)

    @Transaction
    suspend fun saveActivePlan(plan: DietPlanEntity, meals: List<DietPlanMealEntity>) {
        deactivatePlansForUser(plan.userId)
        insertPlan(plan)
        deleteMealsForPlan(plan.id)
        insertMeals(meals)
    }

    @Transaction
    suspend fun insertOrUpdatePlanWithMeals(plan: DietPlanEntity, meals: List<DietPlanMealEntity>) {
        insertPlan(plan)
        deleteMealsForPlan(plan.id)
        insertMeals(meals)
    }

    @Transaction
    @Query("SELECT * FROM diet_plan WHERE userId = :userId AND isActive = 1 ORDER BY createdAt DESC LIMIT 1")
    fun getActivePlanWithMeals(userId: String): Flow<DietPlanWithMeals?>

    @Transaction
    @Query("SELECT * FROM diet_plan WHERE userId = :userId AND isActive = 1 ORDER BY createdAt DESC LIMIT 1")
    suspend fun getActivePlanWithMealsOnce(userId: String): DietPlanWithMeals?

    @Transaction
    @Query("SELECT * FROM diet_plan WHERE userId = :userId ORDER BY createdAt DESC")
    fun getAllPlansWithMeals(userId: String): Flow<List<DietPlanWithMeals>>

    @Transaction
    @Query("SELECT * FROM diet_plan WHERE userId = :userId ORDER BY createdAt DESC")
    suspend fun getAllPlansWithMealsList(userId: String): List<DietPlanWithMeals>

    @Transaction
    @Query("SELECT * FROM diet_plan WHERE id = :planId LIMIT 1")
    suspend fun getPlanWithMealsById(planId: String): DietPlanWithMeals?

    @Query("DELETE FROM diet_plan_meal WHERE planId = :planId")
    suspend fun deleteMealsForPlan(planId: String)

    @Query("DELETE FROM diet_plan WHERE id = :planId AND userId = :userId")
    suspend fun deletePlanById(planId: String, userId: String)

    @Query("DELETE FROM diet_plan WHERE id = :planId")
    suspend fun deletePlanById(planId: String)

    @Query("DELETE FROM diet_plan WHERE userId = :userId")
    suspend fun clearPlansForUser(userId: String)
}
