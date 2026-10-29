package com.monteirogames.nutriverse.data.repository

import com.monteirogames.nutriverse.data.local.dao.FoodItemDao
import com.monteirogames.nutriverse.data.local.dao.PantryItemDao
import com.monteirogames.nutriverse.data.local.dao.SyncTombstoneDao
import com.monteirogames.nutriverse.data.local.entity.PantryItemEntity
import com.monteirogames.nutriverse.data.local.entity.PantryItemWithFoodItem
import com.monteirogames.nutriverse.data.local.entity.SyncTombstoneEntity
import com.monteirogames.nutriverse.domain.model.PantryItem
import com.monteirogames.nutriverse.domain.repository.PantryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class PantryRepositoryImpl(
    private val foodItemDao: FoodItemDao,
    private val pantryItemDao: PantryItemDao,
    private val syncTombstoneDao: SyncTombstoneDao? = null
) : PantryRepository {

    override suspend fun savePantryItem(pantryItem: PantryItem) {
        val timestamp = if (pantryItem.updatedAt > 0L) pantryItem.updatedAt else System.currentTimeMillis()
        val itemToSave = pantryItem.copy(updatedAt = timestamp)
        foodItemDao.insertFoodItem(itemToSave.foodItem.toEntity())
        pantryItemDao.insertPantryItem(itemToSave.toEntity())
        syncTombstoneDao?.deleteTombstone(entityType = "pantry", id = itemToSave.id)
    }

    override suspend fun updatePantryItem(pantryItem: PantryItem) {
        val itemToSave = pantryItem.copy(updatedAt = System.currentTimeMillis())
        foodItemDao.insertFoodItem(itemToSave.foodItem.toEntity())
        pantryItemDao.updatePantryItem(itemToSave.toEntity())
        syncTombstoneDao?.deleteTombstone(entityType = "pantry", id = itemToSave.id)
    }

    override suspend fun deletePantryItem(pantryItem: PantryItem) {
        pantryItemDao.deletePantryItem(pantryItem.toEntity())
        syncTombstoneDao?.insertTombstone(
            SyncTombstoneEntity(
                id = pantryItem.id,
                entityType = "pantry",
                userId = pantryItem.userId,
                deletedAt = System.currentTimeMillis()
            )
        )
    }

    override suspend fun getPantryItemById(id: String, userId: String): PantryItem? {
        val entity = pantryItemDao.getPantryItemById(id, userId) ?: return null
        val foodEntity = foodItemDao.getFoodItemById(entity.foodItemId) ?: return null
        return PantryItemWithFoodItem(entity, foodEntity).toDomain()
    }

    override suspend fun getAllPantryItems(userId: String): List<PantryItem> {
        return pantryItemDao.getAllPantryItems(userId).map { it.toDomain() }
    }

    override fun getPantryItems(userId: String): Flow<List<PantryItem>> {
        return pantryItemDao.getPantryItems(userId).map { list ->
            list.map { it.toDomain() }
        }
    }
}

fun PantryItemWithFoodItem.toDomain() = PantryItem(
    id = pantryItem.id,
    userId = pantryItem.userId,
    foodItem = foodItem.toDomain(),
    quantity = pantryItem.quantity,
    unit = pantryItem.unit,
    expiryDate = pantryItem.expiryDate,
    dateAdded = pantryItem.dateAdded,
    updatedAt = if (pantryItem.updatedAt > 0L) pantryItem.updatedAt else pantryItem.dateAdded
)

fun PantryItem.toEntity() = PantryItemEntity(
    id = id,
    userId = userId,
    foodItemId = foodItem.id,
    quantity = quantity,
    unit = unit,
    expiryDate = expiryDate,
    dateAdded = dateAdded,
    updatedAt = if (updatedAt > 0L) updatedAt else dateAdded
)
