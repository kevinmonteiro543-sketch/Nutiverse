package com.monteirogames.nutriverse.domain.usecase

import com.monteirogames.nutriverse.domain.model.ExpenseCategoryHelper
import com.monteirogames.nutriverse.domain.model.FoodExpense
import com.monteirogames.nutriverse.domain.repository.ExpenseRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

class GetExpensesUseCase(private val repository: ExpenseRepository) {
    operator fun invoke(userId: String): Flow<List<FoodExpense>> = repository.getExpenses(userId)
}

class GetExpensesForPeriodUseCase(private val repository: ExpenseRepository) {
    operator fun invoke(userId: String, startDate: Long, endDate: Long): Flow<List<FoodExpense>> =
        repository.getExpensesForPeriod(userId, startDate, endDate)
}

class AddExpenseUseCase(private val repository: ExpenseRepository) {
    suspend operator fun invoke(
        userId: String,
        name: String,
        amount: Double,
        category: String,
        date: Long = System.currentTimeMillis(),
        notes: String? = null,
        groceryItemId: String? = null
    ): FoodExpense {
        require(name.isNotBlank()) { "Expense name cannot be empty" }
        require(amount > 0) { "Expense amount must be greater than zero" }
        require(date > 0) { "Date must be valid" }

        val resolvedCategory = if (category.isBlank()) {
            ExpenseCategoryHelper.inferCategory(name)
        } else {
            category
        }

        val expense = FoodExpense(
            id = UUID.randomUUID().toString(),
            userId = userId,
            name = name.trim(),
            amount = amount,
            category = resolvedCategory,
            date = date,
            notes = notes?.trim()?.ifBlank { null },
            groceryItemId = groceryItemId,
            createdAt = System.currentTimeMillis()
        )
        repository.addExpense(expense)
        return expense
    }
}

class UpdateExpenseUseCase(private val repository: ExpenseRepository) {
    suspend operator fun invoke(expense: FoodExpense) {
        require(expense.name.isNotBlank()) { "Expense name cannot be empty" }
        require(expense.amount > 0) { "Expense amount must be greater than zero" }
        require(expense.date > 0) { "Date must be valid" }
        repository.updateExpense(expense)
    }
}

class DeleteExpenseUseCase(private val repository: ExpenseRepository) {
    suspend operator fun invoke(expense: FoodExpense) {
        repository.deleteExpense(expense)
    }

    suspend operator fun invoke(id: String, userId: String) {
        repository.deleteExpenseById(id, userId)
    }
}

class GetSpendingByCategoryUseCase(private val repository: ExpenseRepository) {
    operator fun invoke(userId: String, startDate: Long, endDate: Long): Flow<Map<String, Double>> {
        return repository.getExpensesForPeriod(userId, startDate, endDate).map { list ->
            list.groupBy { it.category }
                .mapValues { entry -> entry.value.sumOf { it.amount } }
        }
    }
}

class GetTotalSpendingUseCase(private val repository: ExpenseRepository) {
    operator fun invoke(userId: String): Flow<Double> = repository.getTotalSpending(userId)

    operator fun invoke(userId: String, startDate: Long, endDate: Long): Flow<Double> =
        repository.getTotalSpendingForPeriod(userId, startDate, endDate)
}

data class ExpenseUseCases(
    val getExpenses: GetExpensesUseCase,
    val getExpensesForPeriod: GetExpensesForPeriodUseCase,
    val addExpense: AddExpenseUseCase,
    val updateExpense: UpdateExpenseUseCase,
    val deleteExpense: DeleteExpenseUseCase,
    val getSpendingByCategory: GetSpendingByCategoryUseCase,
    val getTotalSpending: GetTotalSpendingUseCase
)
