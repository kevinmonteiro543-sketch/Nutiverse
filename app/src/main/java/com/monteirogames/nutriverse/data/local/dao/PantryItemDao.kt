package com.monteirogames.nutriverse.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.monteirogames.nutriverse.data.local.entity.PantryItemEntity
import com.monteirogames.nutriverse.data.local.entity.PantryItemWithFoodItem
import kotlinx.coroutines.flow.Flow

@Dao
interface PantryItemDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPantryItem(item: PantryItemEntity)

    @Update
    suspend fun updatePantryItem(item: PantryItemEntity)

    @Delete
    suspend fun deletePantryItem(item: PantryItemEntity)

    @Query("SELECT * FROM pantry_item WHERE id = :id AND userId = :userId LIMIT 1")
    suspend fun getPantryItemById(id: String, userId: String): PantryItemEntity?

    @Transaction
    @Query("SELECT * FROM pantry_item WHERE id = :id AND userId = :userId LIMIT 1")
    suspend fun getPantryItemWithFoodById(id: String, userId: String): PantryItemWithFoodItem?

    @Transaction
    @Query("SELECT * FROM pantry_item WHERE userId = :userId ORDER BY expiryDate ASC")
    fun getPantryItems(userId: String): Flow<List<PantryItemWithFoodItem>>

    @Transaction
    @Query("SELECT * FROM pantry_item WHERE userId = :userId ORDER BY expiryDate ASC")
    suspend fun getAllPantryItems(userId: String): List<PantryItemWithFoodItem>
}
