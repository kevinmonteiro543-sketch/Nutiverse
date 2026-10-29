package com.monteirogames.nutriverse

import com.monteirogames.nutriverse.domain.model.Budget
import com.monteirogames.nutriverse.domain.model.BudgetPeriod
import com.monteirogames.nutriverse.domain.model.BudgetPeriodHelper
import com.monteirogames.nutriverse.domain.model.ExpenseCategoryHelper
import com.monteirogames.nutriverse.domain.model.FoodExpense
import com.monteirogames.nutriverse.domain.repository.BudgetRepository
import com.monteirogames.nutriverse.domain.repository.ExpenseRepository
import com.monteirogames.nutriverse.domain.usecase.CalculateBudgetStatusUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class BudgetCalculationTest {

    private val fakeBudgetRepository = object : BudgetRepository {
        override fun getActiveBudget(userId: String): Flow<Budget?> = flowOf(null)
        override suspend fun getActiveBudgetOnce(userId: String): Budget? = null
        override fun getAllBudgets(userId: String): Flow<List<Budget>> = flowOf(emptyList())
        override suspend fun saveBudget(budget: Budget) {}
        override suspend fun deleteBudget(id: String, userId: String) {}
        override suspend fun deactivateAllBudgets(userId: String) {}
    }

    private val fakeExpenseRepository = object : ExpenseRepository {
        override fun getExpenses(userId: String): Flow<List<FoodExpense>> = flowOf(emptyList())
        override fun getExpensesForPeriod(userId: String, startDate: Long, endDate: Long): Flow<List<FoodExpense>> = flowOf(emptyList())
        override suspend fun getExpensesListForPeriod(userId: String, startDate: Long, endDate: Long): List<FoodExpense> = emptyList()
        override suspend fun getExpenseById(id: String, userId: String): FoodExpense? = null
        override suspend fun getExpenseByGroceryItemId(groceryItemId: String, userId: String): FoodExpense? = null
        override suspend fun addExpense(expense: FoodExpense) {}
        override suspend fun updateExpense(expense: FoodExpense) {}
        override suspend fun deleteExpense(expense: FoodExpense) {}
        override suspend fun deleteExpenseById(id: String, userId: String) {}
        override fun getTotalSpending(userId: String): Flow<Double> = flowOf(0.0)
        override fun getTotalSpendingForPeriod(userId: String, startDate: Long, endDate: Long): Flow<Double> = flowOf(0.0)
    }

    private val calculateStatusUseCase = CalculateBudgetStatusUseCase(fakeBudgetRepository, fakeExpenseRepository)

    @Test
    fun testBudgetCalculations_underBudget() {
        val now = System.currentTimeMillis()
        val budget = Budget(
            id = "b1",
            userId = "user1",
            amount = 5000.0,
            period = BudgetPeriod.MONTHLY.name,
            startDate = now - 100000,
            endDate = now + 100000
        )

        val expenses = listOf(
            FoodExpense("e1", "user1", "Groceries", 1500.0, "Groceries", now),
            FoodExpense("e2", "user1", "Milk", 500.0, "Dairy", now)
        )

        val summary = calculateStatusUseCase.calculate(budget, expenses, now)

        assertEquals(5000.0, summary.totalBudget, 0.01)
        assertEquals(2000.0, summary.totalSpent, 0.01)
        assertEquals(3000.0, summary.remaining, 0.01)
        assertEquals(40f, summary.percentageUsed, 0.1f)
        assertFalse(summary.isOverBudget)
        assertEquals(0.0, summary.overBudgetAmount, 0.01)
    }

    @Test
    fun testBudgetCalculations_overBudget() {
        val now = System.currentTimeMillis()
        val budget = Budget(
            id = "b1",
            userId = "user1",
            amount = 5000.0,
            period = BudgetPeriod.MONTHLY.name,
            startDate = now - 100000,
            endDate = now + 100000
        )

        val expenses = listOf(
            FoodExpense("e1", "user1", "Supermarket", 4000.0, "Groceries", now),
            FoodExpense("e2", "user1", "Dinner Out", 1500.0, "Eating Out", now)
        )

        val summary = calculateStatusUseCase.calculate(budget, expenses, now)

        assertEquals(5000.0, summary.totalBudget, 0.01)
        assertEquals(5500.0, summary.totalSpent, 0.01)
        assertEquals(0.0, summary.remaining, 0.01)
        assertEquals(110f, summary.percentageUsed, 0.1f)
        assertTrue(summary.isOverBudget)
        assertEquals(500.0, summary.overBudgetAmount, 0.01)
    }

    @Test
    fun testBudgetCalculations_noActiveBudget() {
        val now = System.currentTimeMillis()
        val expenses = listOf(
            FoodExpense("e1", "user1", "Coffee", 200.0, "Beverages", now)
        )

        val summary = calculateStatusUseCase.calculate(null, expenses, now)

        assertEquals(0.0, summary.totalBudget, 0.01)
        assertEquals(200.0, summary.totalSpent, 0.01)
        assertEquals(0.0, summary.remaining, 0.01)
        assertEquals(0f, summary.percentageUsed, 0.01f)
        assertFalse(summary.isOverBudget)
    }

    @Test
    fun testCurrencyFormatting() {
        val formatted = BudgetPeriodHelper.formatCurrency(5000.0)
        assertTrue(formatted.startsWith(BudgetPeriodHelper.CURRENCY_SYMBOL))
        assertTrue(formatted.contains("5,000") || formatted.contains("5000"))
    }

    @Test
    fun testPeriodBounds() {
        val calendar = Calendar.getInstance().apply {
            set(2026, Calendar.OCTOBER, 15, 12, 0, 0)
        }
        val testTime = calendar.timeInMillis

        val (monthStart, monthEnd) = BudgetPeriodHelper.getCurrentMonthBounds(testTime)
        val calStart = Calendar.getInstance().apply { timeInMillis = monthStart }
        val calEnd = Calendar.getInstance().apply { timeInMillis = monthEnd }

        assertEquals(1, calStart.get(Calendar.DAY_OF_MONTH))
        assertEquals(31, calEnd.get(Calendar.DAY_OF_MONTH))
        assertEquals(Calendar.OCTOBER, calStart.get(Calendar.MONTH))
        assertEquals(Calendar.OCTOBER, calEnd.get(Calendar.MONTH))
    }

    @Test
    fun testCategoryInference() {
        assertEquals("Dairy", ExpenseCategoryHelper.inferCategory("Whole Milk 1L"))
        assertEquals("Meat & Seafood", ExpenseCategoryHelper.inferCategory("Chicken Breast 500g"))
        assertEquals("Fruits & Vegetables", ExpenseCategoryHelper.inferCategory("Fresh Spinach & Tomatoes"))
        assertEquals("Eating Out", ExpenseCategoryHelper.inferCategory("Cafe lunch delivery"))
        assertEquals("Beverages", ExpenseCategoryHelper.inferCategory("Green Tea"))
    }
}
