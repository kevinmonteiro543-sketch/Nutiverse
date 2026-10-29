package com.monteirogames.nutriverse.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.monteirogames.nutriverse.data.local.entity.GroceryItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GroceryDao {

    @Query("SELECT * FROM grocery_item WHERE userId = :userId ORDER BY createdAt DESC")
    fun getGroceryItems(userId: String): Flow<List<GroceryItemEntity>>

    @Query("SELECT * FROM grocery_item WHERE id = :id AND userId = :userId LIMIT 1")
    suspend fun getGroceryItemById(id: String, userId: String): GroceryItemEntity?

    @Query("SELECT * FROM grocery_item WHERE userId = :userId")
    suspend fun getGroceryItemsList(userId: String): List<GroceryItemEntity>

    @Query("SELECT * FROM grocery_item WHERE userId = :userId AND isPurchased = 1")
    suspend fun getPurchasedItemsList(userId: String): List<GroceryItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGroceryItem(item: GroceryItemEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGroceryItems(items: List<GroceryItemEntity>)

    @Update
    suspend fun updateGroceryItem(item: GroceryItemEntity)

    @Delete
    suspend fun deleteGroceryItem(item: GroceryItemEntity)

    @Query("DELETE FROM grocery_item WHERE id = :id AND userId = :userId")
    suspend fun deleteGroceryItemById(id: String, userId: String)

    @Query("DELETE FROM grocery_item WHERE userId = :userId AND isPurchased = 1")
    suspend fun clearPurchasedItems(userId: String)

    @Query("UPDATE grocery_item SET isPurchased = :isPurchased, purchasedAt = :purchasedAt, updatedAt = :updatedAt WHERE id = :id AND userId = :userId")
    suspend fun setPurchased(
        id: String,
        userId: String,
        isPurchased: Boolean,
        purchasedAt: Long?,
        updatedAt: Long = System.currentTimeMillis()
    )
}
