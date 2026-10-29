package com.monteirogames.nutriverse.domain.repository

import com.monteirogames.nutriverse.domain.model.Budget
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

interface BudgetRepository {
    fun getActiveBudget(userId: String): Flow<Budget?>
    suspend fun getActiveBudgetOnce(userId: String): Budget?
    fun getAllBudgets(userId: String): Flow<List<Budget>>
    suspend fun getAllBudgetsList(userId: String): List<Budget> = getAllBudgets(userId).first()
    suspend fun saveBudget(budget: Budget)
    suspend fun deleteBudget(id: String, userId: String)
    suspend fun deactivateAllBudgets(userId: String)
}
