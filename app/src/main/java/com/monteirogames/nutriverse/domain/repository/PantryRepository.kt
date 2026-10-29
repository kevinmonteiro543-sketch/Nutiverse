package com.monteirogames.nutriverse.domain.repository

import com.monteirogames.nutriverse.domain.model.PantryItem
import kotlinx.coroutines.flow.Flow

interface PantryRepository {
    suspend fun savePantryItem(pantryItem: PantryItem)
    suspend fun updatePantryItem(pantryItem: PantryItem)
    suspend fun deletePantryItem(pantryItem: PantryItem)
    suspend fun getPantryItemById(id: String, userId: String): PantryItem?
    suspend fun getAllPantryItems(userId: String): List<PantryItem>
    
    fun getPantryItems(userId: String): Flow<List<PantryItem>>
}
