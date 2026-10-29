package com.monteirogames.nutriverse.data.repository

import com.monteirogames.nutriverse.data.local.dao.GroceryDao
import com.monteirogames.nutriverse.data.local.dao.SyncTombstoneDao
import com.monteirogames.nutriverse.data.local.entity.GroceryItemEntity
import com.monteirogames.nutriverse.data.local.entity.SyncTombstoneEntity
import com.monteirogames.nutriverse.domain.model.GroceryCategoryHelper
import com.monteirogames.nutriverse.domain.model.GroceryItem
import com.monteirogames.nutriverse.domain.model.PantryItem
import com.monteirogames.nutriverse.domain.model.Recipe
import com.monteirogames.nutriverse.domain.repository.GroceryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

class GroceryRepositoryImpl(
    private val groceryDao: GroceryDao,
    private val syncTombstoneDao: SyncTombstoneDao? = null
) : GroceryRepository {

    override fun getGroceryItems(userId: String): Flow<List<GroceryItem>> {
        return groceryDao.getGroceryItems(userId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getAllGroceryItems(userId: String): List<GroceryItem> {
        return groceryDao.getGroceryItemsList(userId).map { it.toDomain() }
    }

    override suspend fun getGroceryItemById(id: String, userId: String): GroceryItem? {
        return groceryDao.getGroceryItemById(id, userId)?.toDomain()
    }

    override suspend fun addGroceryItem(item: GroceryItem) {
        val timestamp = if (item.updatedAt > 0L) item.updatedAt else System.currentTimeMillis()
        val itemToSave = item.copy(updatedAt = timestamp)
        groceryDao.insertGroceryItem(itemToSave.toEntity())
        syncTombstoneDao?.deleteTombstone("grocery", itemToSave.id)
    }

    override suspend fun updateGroceryItem(item: GroceryItem) {
        val itemToSave = item.copy(updatedAt = System.currentTimeMillis())
        groceryDao.updateGroceryItem(itemToSave.toEntity())
        syncTombstoneDao?.deleteTombstone("grocery", itemToSave.id)
    }

    override suspend fun deleteGroceryItem(item: GroceryItem) {
        groceryDao.deleteGroceryItem(item.toEntity())
        syncTombstoneDao?.insertTombstone(
            SyncTombstoneEntity(
                id = item.id,
                entityType = "grocery",
                userId = item.userId,
                deletedAt = System.currentTimeMillis()
            )
        )
    }

    override suspend fun deleteGroceryItemById(id: String, userId: String) {
        groceryDao.deleteGroceryItemById(id, userId)
        syncTombstoneDao?.insertTombstone(
            SyncTombstoneEntity(
                id = id,
                entityType = "grocery",
                userId = userId,
                deletedAt = System.currentTimeMillis()
            )
        )
    }

    override suspend fun togglePurchased(id: String, userId: String, isPurchased: Boolean) {
        val now = System.currentTimeMillis()
        val purchasedAt = if (isPurchased) now else null
        groceryDao.setPurchased(id, userId, isPurchased, purchasedAt, updatedAt = now)
        syncTombstoneDao?.deleteTombstone("grocery", id)
    }

    override suspend fun clearPurchasedItems(userId: String) {
        val purchasedItems = groceryDao.getPurchasedItemsList(userId)
        val now = System.currentTimeMillis()
        purchasedItems.forEach { item ->
            syncTombstoneDao?.insertTombstone(
                SyncTombstoneEntity(
                    id = item.id,
                    entityType = "grocery",
                    userId = userId,
                    deletedAt = now
                )
            )
        }
        groceryDao.clearPurchasedItems(userId)
    }

    override suspend fun addMissingRecipeIngredients(
        userId: String,
        recipe: Recipe,
        pantryItems: List<PantryItem>
    ): Int {
        // 1. Normalize pantry item names
        val normalizedPantryNames = pantryItems.map {
            GroceryCategoryHelper.normalizeItemName(it.foodItem.name)
        }

        // 2. Fetch existing unpurchased grocery items to prevent duplicates
        val existingGroceryItems = groceryDao.getGroceryItemsList(userId).filter { !it.isPurchased }
        val existingMap = existingGroceryItems.associateBy {
            GroceryCategoryHelper.normalizeItemName(it.name)
        }

        var addedCount = 0

        for (ingredient in recipe.ingredients) {
            val normIngName = GroceryCategoryHelper.normalizeItemName(ingredient.name)

            // Check if available in pantry
            val isInPantry = normalizedPantryNames.any { pName ->
                normIngName.contains(pName) || pName.contains(normIngName) ||
                        hasTokenOverlap(normIngName, pName)
            }

            if (!isInPantry) {
                // Not in pantry! Check if already in unpurchased grocery list
                val existing = existingMap[normIngName]
                if (existing != null) {
                    // Item already in grocery list: increment quantity if units match
                    if (existing.unit.equals(ingredient.unit, ignoreCase = true)) {
                        val updated = existing.copy(
                            quantity = existing.quantity + ingredient.quantity,
                            updatedAt = System.currentTimeMillis()
                        )
                        groceryDao.updateGroceryItem(updated)
                    }
                    addedCount++
                } else {
                    // New grocery item
                    val category = GroceryCategoryHelper.inferCategory(ingredient.name)
                    val now = System.currentTimeMillis()
                    val newItem = GroceryItemEntity(
                        id = UUID.randomUUID().toString(),
                        userId = userId,
                        name = ingredient.name.trim(),
                        quantity = ingredient.quantity,
                        unit = ingredient.unit.trim().ifBlank { "pcs" },
                        category = category,
                        isPurchased = false,
                        source = "RECIPE",
                        sourceRecipeId = recipe.id,
                        createdAt = now,
                        updatedAt = now
                    )
                    groceryDao.insertGroceryItem(newItem)
                    syncTombstoneDao?.deleteTombstone("grocery", newItem.id)
                    addedCount++
                }
            }
        }

        return addedCount
    }

    private fun hasTokenOverlap(a: String, b: String): Boolean {
        val tokensA = a.split(" ").filter { it.length >= 3 }
        val tokensB = b.split(" ").filter { it.length >= 3 }
        for (ta in tokensA) {
            val stemA = ta.removeSuffix("s").removeSuffix("es")
            for (tb in tokensB) {
                val stemB = tb.removeSuffix("s").removeSuffix("es")
                if (stemA == stemB || (stemA.length >= 4 && stemB.contains(stemA)) || (stemB.length >= 4 && stemA.contains(stemB))) {
                    return true
                }
            }
        }
        return false
    }

    private fun GroceryItemEntity.toDomain(): GroceryItem {
        return GroceryItem(
            id = id,
            userId = userId,
            name = name,
            quantity = quantity,
            unit = unit,
            category = category,
            isPurchased = isPurchased,
            source = source,
            sourceRecipeId = sourceRecipeId,
            createdAt = createdAt,
            purchasedAt = purchasedAt,
            updatedAt = if (updatedAt > 0L) updatedAt else createdAt
        )
    }

    private fun GroceryItem.toEntity(): GroceryItemEntity {
        return GroceryItemEntity(
            id = id,
            userId = userId,
            name = name,
            quantity = quantity,
            unit = unit,
            category = category,
            isPurchased = isPurchased,
            source = source,
            sourceRecipeId = sourceRecipeId,
            createdAt = createdAt,
            purchasedAt = purchasedAt,
            updatedAt = if (updatedAt > 0L) updatedAt else createdAt
        )
    }
}
