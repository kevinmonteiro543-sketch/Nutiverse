package com.monteirogames.nutriverse.data.repository

import com.monteirogames.nutriverse.data.local.dao.BudgetDao
import com.monteirogames.nutriverse.data.local.dao.SyncTombstoneDao
import com.monteirogames.nutriverse.data.local.entity.BudgetEntity
import com.monteirogames.nutriverse.data.local.entity.SyncTombstoneEntity
import com.monteirogames.nutriverse.domain.model.Budget
import com.monteirogames.nutriverse.domain.repository.BudgetRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class BudgetRepositoryImpl(
    private val budgetDao: BudgetDao,
    private val syncTombstoneDao: SyncTombstoneDao? = null
) : BudgetRepository {

    override fun getActiveBudget(userId: String): Flow<Budget?> {
        return budgetDao.getActiveBudget(userId).map { it?.toDomain() }
    }

    override suspend fun getActiveBudgetOnce(userId: String): Budget? {
        return budgetDao.getActiveBudgetOnce(userId)?.toDomain()
    }

    override fun getAllBudgets(userId: String): Flow<List<Budget>> {
        return budgetDao.getAllBudgets(userId).map { list -> list.map { it.toDomain() } }
    }

    override suspend fun getAllBudgetsList(userId: String): List<Budget> {
        return budgetDao.getAllBudgetsList(userId).map { it.toDomain() }
    }

    override suspend fun saveBudget(budget: Budget) {
        val timestamp = if (budget.updatedAt > 0L) budget.updatedAt else System.currentTimeMillis()
        val budgetToSave = budget.copy(updatedAt = timestamp)
        if (budgetToSave.isActive) {
            // Deactivate existing budgets for the user so only one is active
            budgetDao.deactivateAllBudgets(budgetToSave.userId)
        }
        budgetDao.insertBudget(budgetToSave.toEntity())
        syncTombstoneDao?.deleteTombstone("budget", budgetToSave.id)
    }

    override suspend fun deleteBudget(id: String, userId: String) {
        budgetDao.deleteBudgetById(id, userId)
        syncTombstoneDao?.insertTombstone(
            SyncTombstoneEntity(
                id = id,
                entityType = "budget",
                userId = userId,
                deletedAt = System.currentTimeMillis()
            )
        )
    }

    override suspend fun deactivateAllBudgets(userId: String) {
        budgetDao.deactivateAllBudgets(userId)
    }

    private fun BudgetEntity.toDomain(): Budget {
        return Budget(
            id = id,
            userId = userId,
            amount = amount,
            period = period,
            startDate = startDate,
            endDate = endDate,
            createdAt = createdAt,
            isActive = isActive,
            updatedAt = if (updatedAt > 0L) updatedAt else createdAt
        )
    }

    private fun Budget.toEntity(): BudgetEntity {
        return BudgetEntity(
            id = id,
            userId = userId,
            amount = amount,
            period = period,
            startDate = startDate,
            endDate = endDate,
            createdAt = createdAt,
            isActive = isActive,
            updatedAt = if (updatedAt > 0L) updatedAt else createdAt
        )
    }
}
