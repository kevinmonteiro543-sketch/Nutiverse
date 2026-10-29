package com.monteirogames.nutriverse.data.repository

import com.monteirogames.nutriverse.data.local.dao.FoodItemDao
import com.monteirogames.nutriverse.data.local.dao.MealEntryDao
import com.monteirogames.nutriverse.data.local.dao.SyncTombstoneDao
import com.monteirogames.nutriverse.data.local.entity.FoodItemEntity
import com.monteirogames.nutriverse.data.local.entity.MealEntryEntity
import com.monteirogames.nutriverse.data.local.entity.MealEntryWithFoodItem
import com.monteirogames.nutriverse.data.local.entity.SyncTombstoneEntity
import com.monteirogames.nutriverse.domain.model.FoodItem
import com.monteirogames.nutriverse.domain.model.MealEntry
import com.monteirogames.nutriverse.domain.repository.MealRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.Calendar

class MealRepositoryImpl(
    private val foodItemDao: FoodItemDao,
    private val mealEntryDao: MealEntryDao,
    private val syncTombstoneDao: SyncTombstoneDao? = null
) : MealRepository {

    override suspend fun saveFoodItem(foodItem: FoodItem) {
        foodItemDao.insertFoodItem(foodItem.toEntity())
    }

    override suspend fun getFoodItemById(id: String): FoodItem? {
        return foodItemDao.getFoodItemById(id)?.toDomain()
    }

    override suspend fun saveMealEntry(mealEntry: MealEntry) {
        val timestamp = if (mealEntry.updatedAt > 0L) mealEntry.updatedAt else System.currentTimeMillis()
        val entryToSave = mealEntry.copy(updatedAt = timestamp)
        mealEntryDao.insertMealEntry(entryToSave.toEntity())
        syncTombstoneDao?.deleteTombstone(entityType = "meal", id = entryToSave.id)
    }

    override suspend fun updateMealEntry(mealEntry: MealEntry) {
        val entryToSave = mealEntry.copy(updatedAt = System.currentTimeMillis())
        mealEntryDao.updateMealEntry(entryToSave.toEntity())
        syncTombstoneDao?.deleteTombstone(entityType = "meal", id = entryToSave.id)
    }

    override suspend fun deleteMealEntry(mealEntry: MealEntry) {
        mealEntryDao.deleteMealEntry(mealEntry.toEntity())
        syncTombstoneDao?.insertTombstone(
            SyncTombstoneEntity(
                id = mealEntry.id,
                entityType = "meal",
                userId = mealEntry.userId,
                deletedAt = System.currentTimeMillis()
            )
        )
    }

    override suspend fun getMealEntryById(id: String, userId: String): MealEntry? {
        val entity = mealEntryDao.getMealEntryById(id, userId) ?: return null
        val foodEntity = foodItemDao.getFoodItemById(entity.foodItemId) ?: return null
        return MealEntryWithFoodItem(entity, foodEntity).toDomain()
    }

    override suspend fun getAllMeals(userId: String): List<MealEntry> {
        return mealEntryDao.getAllMeals(userId).map { it.toDomain() }
    }

    override fun getMealsForDate(userId: String, timestamp: Long): Flow<List<MealEntry>> {
        val cal = Calendar.getInstance()
        cal.timeInMillis = timestamp
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val startOfDay = cal.timeInMillis
        
        cal.add(Calendar.DAY_OF_MONTH, 1)
        val endOfDay = cal.timeInMillis - 1
        
        return mealEntryDao.getMealsForDateRange(userId, startOfDay, endOfDay).map { list ->
            list.map { it.toDomain() }
        }
    }

    override fun getMealsForDateRange(userId: String, startDate: Long, endDate: Long): Flow<List<MealEntry>> {
        return mealEntryDao.getMealsForDateRange(userId, startDate, endDate).map { list ->
            list.map { it.toDomain() }
        }
    }
}

fun FoodItemEntity.toDomain() = FoodItem(
    id = id,
    name = name,
    calories = calories,
    protein = protein,
    carbs = carbs,
    fat = fat,
    servingSize = servingSize,
    servingUnit = servingUnit,
    barcode = barcode
)

fun FoodItem.toEntity() = FoodItemEntity(
    id = id,
    name = name,
    calories = calories,
    protein = protein,
    carbs = carbs,
    fat = fat,
    servingSize = servingSize,
    servingUnit = servingUnit,
    barcode = barcode
)

fun MealEntryWithFoodItem.toDomain() = MealEntry(
    id = mealEntry.id,
    userId = mealEntry.userId,
    foodItem = foodItem.toDomain(),
    date = mealEntry.date,
    mealType = mealEntry.mealType,
    quantity = mealEntry.quantity,
    loggedAt = mealEntry.loggedAt,
    updatedAt = if (mealEntry.updatedAt > 0L) mealEntry.updatedAt else mealEntry.loggedAt
)

fun MealEntry.toEntity() = MealEntryEntity(
    id = id,
    userId = userId,
    foodItemId = foodItem.id,
    date = date,
    mealType = mealType,
    quantity = quantity,
    loggedAt = loggedAt,
    updatedAt = if (updatedAt > 0L) updatedAt else loggedAt
)
