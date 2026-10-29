package com.monteirogames.nutriverse.ui.screens.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.monteirogames.nutriverse.domain.model.MealEntry
import com.monteirogames.nutriverse.domain.model.UserProfile
import com.monteirogames.nutriverse.domain.usecase.AuthUseCases
import com.monteirogames.nutriverse.domain.usecase.DailyNutrition
import com.monteirogames.nutriverse.domain.usecase.MealTrackerUseCases
import com.monteirogames.nutriverse.domain.usecase.UserProfileUseCases
import com.monteirogames.nutriverse.domain.usecase.NutritionTargets
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

data class DashboardUiState(
    val isLoading: Boolean = true,
    val userProfile: UserProfile? = null,
    val nutritionTargets: NutritionTargets? = null,
    val dailyNutrition: DailyNutrition = DailyNutrition(0f, 0f, 0f, 0f),
    val todayMeals: List<MealEntry> = emptyList(),
    val budgetSummary: com.monteirogames.nutriverse.domain.model.BudgetSummary? = null
)

class DashboardViewModel(
    private val authUseCases: AuthUseCases,
    private val userProfileUseCases: UserProfileUseCases,
    private val mealTrackerUseCases: MealTrackerUseCases,
    private val budgetUseCases: com.monteirogames.nutriverse.domain.usecase.BudgetUseCases? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        loadDashboardData()
    }

    private fun loadDashboardData() {
        val userId = authUseCases.getCurrentUserId() ?: return
        
        viewModelScope.launch {
            userProfileUseCases.getUserProfile(userId).collectLatest { profile ->
                val targets = profile?.let { userProfileUseCases.calculateNutritionTargets(it) }
                _uiState.value = _uiState.value.copy(
                    userProfile = profile,
                    nutritionTargets = targets
                )
            }
        }
        
        val today = System.currentTimeMillis()
        
        viewModelScope.launch {
            mealTrackerUseCases.calculateDailyNutrition(userId, today).collectLatest { nutrition ->
                _uiState.value = _uiState.value.copy(dailyNutrition = nutrition)
            }
        }
        
        viewModelScope.launch {
            mealTrackerUseCases.observeTodayMeals(userId, today).collectLatest { meals ->
                _uiState.value = _uiState.value.copy(todayMeals = meals, isLoading = false)
            }
        }

        budgetUseCases?.let { bUseCases ->
            viewModelScope.launch {
                bUseCases.calculateBudgetStatus(userId).collectLatest { summary ->
                    _uiState.value = _uiState.value.copy(budgetSummary = summary)
                }
            }
        }
    }
}
