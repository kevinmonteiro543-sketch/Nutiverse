package com.monteirogames.nutriverse.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.monteirogames.nutriverse.data.local.entity.FoodItemEntity

@Dao
interface FoodItemDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFoodItem(foodItem: FoodItemEntity)

    @Query("SELECT * FROM food_item WHERE id = :id")
    suspend fun getFoodItemById(id: String): FoodItemEntity?
}
