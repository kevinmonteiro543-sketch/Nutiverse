package com.monteirogames.nutriverse.data.local.dao

import androidx.room.*
import com.monteirogames.nutriverse.data.local.entity.FoodExpenseEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FoodExpenseDao {
    @Query("SELECT * FROM food_expense WHERE userId = :userId ORDER BY date DESC, createdAt DESC")
    fun getExpenses(userId: String): Flow<List<FoodExpenseEntity>>

    @Query("SELECT * FROM food_expense WHERE userId = :userId ORDER BY date DESC, createdAt DESC")
    suspend fun getAllExpensesList(userId: String): List<FoodExpenseEntity>

    @Query("SELECT * FROM food_expense WHERE userId = :userId AND date >= :startDate AND date <= :endDate ORDER BY date DESC, createdAt DESC")
    fun getExpensesForPeriod(userId: String, startDate: Long, endDate: Long): Flow<List<FoodExpenseEntity>>

    @Query("SELECT * FROM food_expense WHERE userId = :userId AND date >= :startDate AND date <= :endDate ORDER BY date DESC, createdAt DESC")
    suspend fun getExpensesListForPeriod(userId: String, startDate: Long, endDate: Long): List<FoodExpenseEntity>

    @Query("SELECT * FROM food_expense WHERE id = :id AND userId = :userId")
    suspend fun getExpenseById(id: String, userId: String): FoodExpenseEntity?

    @Query("SELECT * FROM food_expense WHERE groceryItemId = :groceryItemId AND userId = :userId LIMIT 1")
    suspend fun getExpenseByGroceryItemId(groceryItemId: String, userId: String): FoodExpenseEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: FoodExpenseEntity)

    @Update
    suspend fun updateExpense(expense: FoodExpenseEntity)

    @Delete
    suspend fun deleteExpense(expense: FoodExpenseEntity)

    @Query("DELETE FROM food_expense WHERE id = :id AND userId = :userId")
    suspend fun deleteExpenseById(id: String, userId: String)

    @Query("SELECT SUM(amount) FROM food_expense WHERE userId = :userId")
    fun getTotalSpending(userId: String): Flow<Double?>

    @Query("SELECT SUM(amount) FROM food_expense WHERE userId = :userId AND date >= :startDate AND date <= :endDate")
    fun getTotalSpendingForPeriod(userId: String, startDate: Long, endDate: Long): Flow<Double?>

    @Query("DELETE FROM food_expense WHERE userId = :userId")
    suspend fun deleteAllExpenses(userId: String)
}
