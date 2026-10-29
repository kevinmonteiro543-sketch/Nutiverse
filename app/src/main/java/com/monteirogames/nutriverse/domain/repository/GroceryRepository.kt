package com.monteirogames.nutriverse.domain.repository

import com.monteirogames.nutriverse.domain.model.GroceryItem
import com.monteirogames.nutriverse.domain.model.PantryItem
import com.monteirogames.nutriverse.domain.model.Recipe
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

interface GroceryRepository {
    fun getGroceryItems(userId: String): Flow<List<GroceryItem>>
    suspend fun getAllGroceryItems(userId: String): List<GroceryItem> = getGroceryItems(userId).first()
    suspend fun getGroceryItemById(id: String, userId: String): GroceryItem?
    suspend fun addGroceryItem(item: GroceryItem)
    suspend fun updateGroceryItem(item: GroceryItem)
    suspend fun deleteGroceryItem(item: GroceryItem)
    suspend fun deleteGroceryItemById(id: String, userId: String)
    suspend fun togglePurchased(id: String, userId: String, isPurchased: Boolean)
    suspend fun clearPurchasedItems(userId: String)
    suspend fun addMissingRecipeIngredients(
        userId: String,
        recipe: Recipe,
        pantryItems: List<PantryItem>
    ): Int
}
