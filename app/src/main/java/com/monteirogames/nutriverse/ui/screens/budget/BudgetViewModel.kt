package com.monteirogames.nutriverse.ui.screens.budget

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.monteirogames.nutriverse.domain.model.Budget
import com.monteirogames.nutriverse.domain.model.BudgetPeriod
import com.monteirogames.nutriverse.domain.model.BudgetPeriodHelper
import com.monteirogames.nutriverse.domain.model.BudgetSummary
import com.monteirogames.nutriverse.domain.model.FoodExpense
import com.monteirogames.nutriverse.domain.usecase.AuthUseCases
import com.monteirogames.nutriverse.domain.usecase.BudgetUseCases
import com.monteirogames.nutriverse.domain.usecase.ExpenseUseCases
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

data class BudgetUiState(
    val isLoading: Boolean = true,
    val summary: BudgetSummary = BudgetSummary(
        budget = null,
        totalBudget = 0.0,
        totalSpent = 0.0,
        remaining = 0.0,
        percentageUsed = 0f,
        isOverBudget = false,
        overBudgetAmount = 0.0,
        periodLabel = "Monthly Budget",
        periodRangeLabel = ""
    ),
    val allExpenses: List<FoodExpense> = emptyList(),
    val filteredExpenses: List<FoodExpense> = emptyList(),
    val categorySpending: Map<String, Double> = emptyMap(),
    val filterMode: String = "CURRENT_PERIOD", // "CURRENT_PERIOD" or "ALL_TIME"
    val errorMessage: String? = null
)

class BudgetViewModel(
    private val authUseCases: AuthUseCases,
    private val budgetUseCases: BudgetUseCases,
    private val expenseUseCases: ExpenseUseCases
) : ViewModel() {

    private val _uiState = MutableStateFlow(BudgetUiState())
    val uiState: StateFlow<BudgetUiState> = _uiState.asStateFlow()

    private var currentUserId: String = ""

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            authUseCases.getCurrentUserFlow().collectLatest { user ->
                val userId = user ?: authUseCases.getCurrentUserId() ?: "guest_user"
                currentUserId = userId
                observeBudgetAndExpenses(userId)
            }
        }
    }

    private fun observeBudgetAndExpenses(userId: String) {
        viewModelScope.launch {
            combine(
                budgetUseCases.calculateBudgetStatus(userId),
                expenseUseCases.getExpenses(userId)
            ) { summary, allExpenses ->
                val filterMode = _uiState.value.filterMode
                val filtered = if (filterMode == "CURRENT_PERIOD") {
                    val activeBudget = summary.budget
                    val period = activeBudget?.period ?: BudgetPeriod.MONTHLY.name
                    val (start, end) = BudgetPeriodHelper.getBoundsForPeriod(period)
                    allExpenses.filter { it.date in start..end }
                } else {
                    allExpenses
                }

                val categoryMap = filtered.groupBy { it.category }
                    .mapValues { entry -> entry.value.sumOf { it.amount } }

                _uiState.value.copy(
                    isLoading = false,
                    summary = summary,
                    allExpenses = allExpenses,
                    filteredExpenses = filtered,
                    categorySpending = categoryMap,
                    errorMessage = null
                )
            }.catch { e ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = e.message ?: "Failed to load budget data"
                )
            }.collectLatest { updatedState ->
                _uiState.value = updatedState
            }
        }
    }

    fun setFilterMode(mode: String) {
        val all = _uiState.value.allExpenses
        val summary = _uiState.value.summary
        val filtered = if (mode == "CURRENT_PERIOD") {
            val activeBudget = summary.budget
            val period = activeBudget?.period ?: BudgetPeriod.MONTHLY.name
            val (start, end) = BudgetPeriodHelper.getBoundsForPeriod(period)
            all.filter { it.date in start..end }
        } else {
            all
        }

        val categoryMap = filtered.groupBy { it.category }
            .mapValues { entry -> entry.value.sumOf { it.amount } }

        _uiState.value = _uiState.value.copy(
            filterMode = mode,
            filteredExpenses = filtered,
            categorySpending = categoryMap
        )
    }

    fun setBudget(amount: Double, period: String) {
        viewModelScope.launch {
            try {
                val existing = _uiState.value.summary.budget
                budgetUseCases.saveBudget(
                    userId = currentUserId,
                    amount = amount,
                    period = period,
                    existingId = existing?.id
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(errorMessage = e.message)
            }
        }
    }

    fun deleteBudget() {
        viewModelScope.launch {
            try {
                val active = _uiState.value.summary.budget ?: return@launch
                budgetUseCases.deleteBudget(active.id, currentUserId)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(errorMessage = e.message)
            }
        }
    }

    fun addExpense(
        name: String,
        amount: Double,
        category: String,
        date: Long,
        notes: String?,
        groceryItemId: String? = null
    ) {
        viewModelScope.launch {
            try {
                expenseUseCases.addExpense(
                    userId = currentUserId,
                    name = name,
                    amount = amount,
                    category = category,
                    date = date,
                    notes = notes,
                    groceryItemId = groceryItemId
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(errorMessage = e.message)
            }
        }
    }

    fun updateExpense(expense: FoodExpense) {
        viewModelScope.launch {
            try {
                expenseUseCases.updateExpense(expense)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(errorMessage = e.message)
            }
        }
    }

    fun deleteExpense(expense: FoodExpense) {
        viewModelScope.launch {
            try {
                expenseUseCases.deleteExpense(expense)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(errorMessage = e.message)
            }
        }
    }

    fun clearErrorMessage() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
}
