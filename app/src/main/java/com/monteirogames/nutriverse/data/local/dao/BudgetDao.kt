package com.monteirogames.nutriverse.data.local.dao

import androidx.room.*
import com.monteirogames.nutriverse.data.local.entity.BudgetEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BudgetDao {
    @Query("SELECT * FROM budget WHERE userId = :userId AND isActive = 1 ORDER BY createdAt DESC LIMIT 1")
    fun getActiveBudget(userId: String): Flow<BudgetEntity?>

    @Query("SELECT * FROM budget WHERE userId = :userId AND isActive = 1 ORDER BY createdAt DESC LIMIT 1")
    suspend fun getActiveBudgetOnce(userId: String): BudgetEntity?

    @Query("SELECT * FROM budget WHERE userId = :userId ORDER BY createdAt DESC")
    fun getAllBudgets(userId: String): Flow<List<BudgetEntity>>

    @Query("SELECT * FROM budget WHERE userId = :userId ORDER BY createdAt DESC")
    suspend fun getAllBudgetsList(userId: String): List<BudgetEntity>

    @Query("SELECT * FROM budget WHERE id = :id AND userId = :userId LIMIT 1")
    suspend fun getBudgetById(id: String, userId: String): BudgetEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBudget(budget: BudgetEntity)

    @Update
    suspend fun updateBudget(budget: BudgetEntity)

    @Query("UPDATE budget SET isActive = 0 WHERE userId = :userId")
    suspend fun deactivateAllBudgets(userId: String)

    @Query("DELETE FROM budget WHERE id = :id AND userId = :userId")
    suspend fun deleteBudgetById(id: String, userId: String)

    @Query("DELETE FROM budget WHERE userId = :userId")
    suspend fun deleteAllBudgets(userId: String)
}
