package com.monteirogames.nutriverse.domain.repository

import com.monteirogames.nutriverse.domain.model.FoodExpense
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

interface ExpenseRepository {
    fun getExpenses(userId: String): Flow<List<FoodExpense>>
    suspend fun getAllExpenses(userId: String): List<FoodExpense> = getExpenses(userId).first()
    fun getExpensesForPeriod(userId: String, startDate: Long, endDate: Long): Flow<List<FoodExpense>>
    suspend fun getExpensesListForPeriod(userId: String, startDate: Long, endDate: Long): List<FoodExpense>
    suspend fun getExpenseById(id: String, userId: String): FoodExpense?
    suspend fun getExpenseByGroceryItemId(groceryItemId: String, userId: String): FoodExpense?
    suspend fun addExpense(expense: FoodExpense)
    suspend fun updateExpense(expense: FoodExpense)
    suspend fun deleteExpense(expense: FoodExpense)
    suspend fun deleteExpenseById(id: String, userId: String)
    fun getTotalSpending(userId: String): Flow<Double>
    fun getTotalSpendingForPeriod(userId: String, startDate: Long, endDate: Long): Flow<Double>
}
