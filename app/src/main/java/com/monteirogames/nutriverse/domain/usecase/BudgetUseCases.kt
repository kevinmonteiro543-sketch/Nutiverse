package com.monteirogames.nutriverse.domain.usecase

import com.monteirogames.nutriverse.domain.model.Budget
import com.monteirogames.nutriverse.domain.model.BudgetPeriod
import com.monteirogames.nutriverse.domain.model.BudgetPeriodHelper
import com.monteirogames.nutriverse.domain.model.BudgetSummary
import com.monteirogames.nutriverse.domain.repository.BudgetRepository
import com.monteirogames.nutriverse.domain.repository.ExpenseRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.util.UUID

class GetActiveBudgetUseCase(private val budgetRepository: BudgetRepository) {
    operator fun invoke(userId: String): Flow<Budget?> = budgetRepository.getActiveBudget(userId)
}

class SaveBudgetUseCase(private val budgetRepository: BudgetRepository) {
    suspend operator fun invoke(
        userId: String,
        amount: Double,
        period: String = BudgetPeriod.MONTHLY.name,
        existingId: String? = null
    ): Budget {
        require(amount > 0) { "Budget amount must be greater than zero" }

        val (startDate, endDate) = BudgetPeriodHelper.getBoundsForPeriod(period)

        val budget = Budget(
            id = existingId ?: UUID.randomUUID().toString(),
            userId = userId,
            amount = amount,
            period = period,
            startDate = startDate,
            endDate = endDate,
            createdAt = System.currentTimeMillis(),
            isActive = true
        )
        budgetRepository.saveBudget(budget)
        return budget
    }
}

class DeleteBudgetUseCase(private val budgetRepository: BudgetRepository) {
    suspend operator fun invoke(id: String, userId: String) {
        budgetRepository.deleteBudget(id, userId)
    }
}

class CalculateBudgetStatusUseCase(
    private val budgetRepository: BudgetRepository,
    private val expenseRepository: ExpenseRepository
) {
    operator fun invoke(userId: String): Flow<BudgetSummary> {
        return combine(
            budgetRepository.getActiveBudget(userId),
            expenseRepository.getExpenses(userId)
        ) { budget, allExpenses ->
            calculate(budget, allExpenses)
        }
    }

    fun calculate(
        budget: Budget?,
        expenses: List<com.monteirogames.nutriverse.domain.model.FoodExpense>,
        currentTimeMillis: Long = System.currentTimeMillis()
    ): BudgetSummary {
        if (budget == null) {
            val (curStart, curEnd) = BudgetPeriodHelper.getCurrentMonthBounds(currentTimeMillis)
            val monthSpent = expenses.filter { it.date in curStart..curEnd }.sumOf { it.amount }
            val rangeLabel = BudgetPeriodHelper.formatDateRange(curStart, curEnd)

            return BudgetSummary(
                budget = null,
                totalBudget = 0.0,
                totalSpent = monthSpent,
                remaining = 0.0,
                percentageUsed = 0f,
                isOverBudget = false,
                overBudgetAmount = 0.0,
                periodLabel = "No Active Budget",
                periodRangeLabel = rangeLabel
            )
        }

        // Active budget applies to the CURRENT calendar period of that type
        val (periodStart, periodEnd) = BudgetPeriodHelper.getBoundsForPeriod(budget.period, currentTimeMillis)
        val periodSpent = expenses.filter { it.date in periodStart..periodEnd }.sumOf { it.amount }
        val remaining = budget.amount - periodSpent
        val isOverBudget = periodSpent > budget.amount
        val overBudgetAmount = if (isOverBudget) periodSpent - budget.amount else 0.0

        val rawPercentage = if (budget.amount > 0) {
            ((periodSpent / budget.amount) * 100).toFloat()
        } else {
            0f
        }
        val percentageUsed = rawPercentage.coerceAtLeast(0f)

        val periodLabel = if (budget.period.equals(BudgetPeriod.WEEKLY.name, ignoreCase = true)) {
            "Weekly Budget"
        } else {
            "Monthly Budget"
        }
        val periodRangeLabel = BudgetPeriodHelper.formatDateRange(periodStart, periodEnd)

        return BudgetSummary(
            budget = budget,
            totalBudget = budget.amount,
            totalSpent = periodSpent,
            remaining = if (remaining < 0) 0.0 else remaining,
            percentageUsed = percentageUsed,
            isOverBudget = isOverBudget,
            overBudgetAmount = overBudgetAmount,
            periodLabel = periodLabel,
            periodRangeLabel = periodRangeLabel
        )
    }
}

data class BudgetUseCases(
    val getActiveBudget: GetActiveBudgetUseCase,
    val saveBudget: SaveBudgetUseCase,
    val deleteBudget: DeleteBudgetUseCase,
    val calculateBudgetStatus: CalculateBudgetStatusUseCase
)
