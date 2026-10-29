package com.monteirogames.nutriverse.ui.screens.mealtracker

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.monteirogames.nutriverse.domain.model.FoodItem
import com.monteirogames.nutriverse.domain.model.MealEntry
import com.monteirogames.nutriverse.domain.usecase.AuthUseCases
import com.monteirogames.nutriverse.domain.usecase.MealTrackerUseCases
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.UUID

data class MealTrackerUiState(
    val isLoading: Boolean = true,
    val meals: List<MealEntry> = emptyList()
)

class MealTrackerViewModel(
    private val authUseCases: AuthUseCases,
    private val mealTrackerUseCases: MealTrackerUseCases
) : ViewModel() {

    private val _uiState = MutableStateFlow(MealTrackerUiState())
    val uiState: StateFlow<MealTrackerUiState> = _uiState.asStateFlow()

    init {
        loadMeals()
    }

    private fun loadMeals() {
        val userId = authUseCases.getCurrentUserId() ?: return
        val today = System.currentTimeMillis()
        
        viewModelScope.launch {
            mealTrackerUseCases.observeTodayMeals(userId, today).collectLatest { meals ->
                _uiState.value = _uiState.value.copy(meals = meals, isLoading = false)
            }
        }
    }

    fun deleteMeal(meal: MealEntry) {
        viewModelScope.launch {
            mealTrackerUseCases.deleteMeal(meal)
        }
    }
}
