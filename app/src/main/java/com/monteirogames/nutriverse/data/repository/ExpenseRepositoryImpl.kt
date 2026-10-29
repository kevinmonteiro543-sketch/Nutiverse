package com.monteirogames.nutriverse.data.repository

import com.monteirogames.nutriverse.data.local.dao.FoodExpenseDao
import com.monteirogames.nutriverse.data.local.dao.SyncTombstoneDao
import com.monteirogames.nutriverse.data.local.entity.FoodExpenseEntity
import com.monteirogames.nutriverse.data.local.entity.SyncTombstoneEntity
import com.monteirogames.nutriverse.domain.model.FoodExpense
import com.monteirogames.nutriverse.domain.repository.ExpenseRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ExpenseRepositoryImpl(
    private val expenseDao: FoodExpenseDao,
    private val syncTombstoneDao: SyncTombstoneDao? = null
) : ExpenseRepository {

    override fun getExpenses(userId: String): Flow<List<FoodExpense>> {
        return expenseDao.getExpenses(userId).map { list -> list.map { it.toDomain() } }
    }

    override suspend fun getAllExpenses(userId: String): List<FoodExpense> {
        return expenseDao.getAllExpensesList(userId).map { it.toDomain() }
    }

    override fun getExpensesForPeriod(
        userId: String,
        startDate: Long,
        endDate: Long
    ): Flow<List<FoodExpense>> {
        return expenseDao.getExpensesForPeriod(userId, startDate, endDate)
            .map { list -> list.map { it.toDomain() } }
    }

    override suspend fun getExpensesListForPeriod(
        userId: String,
        startDate: Long,
        endDate: Long
    ): List<FoodExpense> {
        return expenseDao.getExpensesListForPeriod(userId, startDate, endDate).map { it.toDomain() }
    }

    override suspend fun getExpenseById(id: String, userId: String): FoodExpense? {
        return expenseDao.getExpenseById(id, userId)?.toDomain()
    }

    override suspend fun getExpenseByGroceryItemId(
        groceryItemId: String,
        userId: String
    ): FoodExpense? {
        return expenseDao.getExpenseByGroceryItemId(groceryItemId, userId)?.toDomain()
    }

    override suspend fun addExpense(expense: FoodExpense) {
        val timestamp = if (expense.updatedAt > 0L) expense.updatedAt else System.currentTimeMillis()
        val expenseToSave = expense.copy(updatedAt = timestamp)
        expenseDao.insertExpense(expenseToSave.toEntity())
        syncTombstoneDao?.deleteTombstone("expense", expenseToSave.id)
    }

    override suspend fun updateExpense(expense: FoodExpense) {
        val expenseToSave = expense.copy(updatedAt = System.currentTimeMillis())
        expenseDao.updateExpense(expenseToSave.toEntity())
        syncTombstoneDao?.deleteTombstone("expense", expenseToSave.id)
    }

    override suspend fun deleteExpense(expense: FoodExpense) {
        expenseDao.deleteExpense(expense.toEntity())
        syncTombstoneDao?.insertTombstone(
            SyncTombstoneEntity(
                id = expense.id,
                entityType = "expense",
                userId = expense.userId,
                deletedAt = System.currentTimeMillis()
            )
        )
    }

    override suspend fun deleteExpenseById(id: String, userId: String) {
        expenseDao.deleteExpenseById(id, userId)
        syncTombstoneDao?.insertTombstone(
            SyncTombstoneEntity(
                id = id,
                entityType = "expense",
                userId = userId,
                deletedAt = System.currentTimeMillis()
            )
        )
    }

    override fun getTotalSpending(userId: String): Flow<Double> {
        return expenseDao.getTotalSpending(userId).map { it ?: 0.0 }
    }

    override fun getTotalSpendingForPeriod(
        userId: String,
        startDate: Long,
        endDate: Long
    ): Flow<Double> {
        return expenseDao.getTotalSpendingForPeriod(userId, startDate, endDate).map { it ?: 0.0 }
    }

    private fun FoodExpenseEntity.toDomain(): FoodExpense {
        return FoodExpense(
            id = id,
            userId = userId,
            name = name,
            amount = amount,
            category = category,
            date = date,
            notes = notes,
            groceryItemId = groceryItemId,
            createdAt = createdAt,
            updatedAt = if (updatedAt > 0L) updatedAt else createdAt
        )
    }

    private fun FoodExpense.toEntity(): FoodExpenseEntity {
        return FoodExpenseEntity(
            id = id,
            userId = userId,
            name = name,
            amount = amount,
            category = category,
            date = date,
            notes = notes,
            groceryItemId = groceryItemId,
            createdAt = createdAt,
            updatedAt = if (updatedAt > 0L) updatedAt else createdAt
        )
    }
}
