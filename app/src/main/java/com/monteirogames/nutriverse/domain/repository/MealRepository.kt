package com.monteirogames.nutriverse.domain.repository

import com.monteirogames.nutriverse.domain.model.FoodItem
import com.monteirogames.nutriverse.domain.model.MealEntry
import kotlinx.coroutines.flow.Flow

interface MealRepository {
    suspend fun saveFoodItem(foodItem: FoodItem)
    suspend fun getFoodItemById(id: String): FoodItem?
    
    suspend fun saveMealEntry(mealEntry: MealEntry)
    suspend fun updateMealEntry(mealEntry: MealEntry)
    suspend fun deleteMealEntry(mealEntry: MealEntry)
    suspend fun getMealEntryById(id: String, userId: String): MealEntry?
    suspend fun getAllMeals(userId: String): List<MealEntry>
    
    fun getMealsForDate(userId: String, timestamp: Long): Flow<List<MealEntry>>
    fun getMealsForDateRange(userId: String, startDate: Long, endDate: Long): Flow<List<MealEntry>>
}
