package com.monteirogames.nutriverse.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.monteirogames.nutriverse.data.local.entity.MealEntryEntity
import com.monteirogames.nutriverse.data.local.entity.MealEntryWithFoodItem
import kotlinx.coroutines.flow.Flow

@Dao
interface MealEntryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMealEntry(entry: MealEntryEntity)

    @Update
    suspend fun updateMealEntry(entry: MealEntryEntity)

    @Delete
    suspend fun deleteMealEntry(entry: MealEntryEntity)
    
    @Query("SELECT * FROM meal_entry WHERE id = :id AND userId = :userId LIMIT 1")
    suspend fun getMealEntryById(id: String, userId: String): MealEntryEntity?

    @Transaction
    @Query("SELECT * FROM meal_entry WHERE id = :id AND userId = :userId LIMIT 1")
    suspend fun getMealWithFoodById(id: String, userId: String): MealEntryWithFoodItem?

    @Transaction
    @Query("SELECT * FROM meal_entry WHERE userId = :userId AND date >= :startOfDay AND date <= :endOfDay ORDER BY loggedAt DESC")
    fun getMealsForDateRange(userId: String, startOfDay: Long, endOfDay: Long): Flow<List<MealEntryWithFoodItem>>

    @Transaction
    @Query("SELECT * FROM meal_entry WHERE userId = :userId ORDER BY loggedAt DESC")
    suspend fun getAllMeals(userId: String): List<MealEntryWithFoodItem>
}
