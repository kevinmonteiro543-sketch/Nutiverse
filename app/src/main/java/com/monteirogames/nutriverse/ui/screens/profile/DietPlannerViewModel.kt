package com.monteirogames.nutriverse.ui.screens.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.monteirogames.nutriverse.domain.model.DayPlan
import com.monteirogames.nutriverse.domain.model.DietPlan
import com.monteirogames.nutriverse.domain.model.DietPlanMeal
import com.monteirogames.nutriverse.domain.model.UserProfile
import com.monteirogames.nutriverse.domain.usecase.AuthUseCases
import com.monteirogames.nutriverse.domain.usecase.DietPlannerUseCases
import com.monteirogames.nutriverse.domain.usecase.NutritionTargets
import com.monteirogames.nutriverse.domain.usecase.UserProfileUseCases
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

sealed interface DietPlannerUiState {
    object Loading : DietPlannerUiState
    
    data class IncompleteProfile(
        val message: String = "Complete your profile before generating a meal plan."
    ) : DietPlannerUiState

    data class Ready(
        val userProfile: UserProfile,
        val nutritionTargets: NutritionTargets,
        val activePlan: DietPlan?,
        val selectedDay: Int = 1
    ) : DietPlannerUiState

    data class Generating(
        val userProfile: UserProfile,
        val nutritionTargets: NutritionTargets,
        val activePlan: DietPlan?
    ) : DietPlannerUiState

    data class Reviewing(
        val userProfile: UserProfile,
        val nutritionTargets: NutritionTargets,
        val draftPlan: DietPlan,
        val selectedDay: Int = 1,
        val isSaving: Boolean = false
    ) : DietPlannerUiState

    data class Error(
        val message: String,
        val userProfile: UserProfile?,
        val nutritionTargets: NutritionTargets?,
        val activePlan: DietPlan?
    ) : DietPlannerUiState
}

class DietPlannerViewModel(
    private val userProfileUseCases: UserProfileUseCases,
    private val dietPlannerUseCases: DietPlannerUseCases,
    private val authUseCases: AuthUseCases
) : ViewModel() {

    private val _uiState = MutableStateFlow<DietPlannerUiState>(DietPlannerUiState.Loading)
    val uiState: StateFlow<DietPlannerUiState> = _uiState.asStateFlow()

    private var currentUserProfile: UserProfile? = null
    private var currentNutritionTargets: NutritionTargets? = null
    private var currentActivePlan: DietPlan? = null

    init {
        loadData()
    }

    private fun loadData() {
        val userId = authUseCases.getCurrentUserId()
        if (userId == null) {
            _uiState.value = DietPlannerUiState.IncompleteProfile("User not authenticated.")
            return
        }

        viewModelScope.launch {
            userProfileUseCases.getUserProfile(userId).collectLatest { profile ->
                currentUserProfile = profile
                if (profile == null || profile.age <= 0 || profile.heightCm <= 0f || profile.weightKg <= 0f) {
                    _uiState.value = DietPlannerUiState.IncompleteProfile(
                        "Complete your profile before generating a meal plan."
                    )
                } else {
                    val targets = userProfileUseCases.calculateNutritionTargets(profile)
                    currentNutritionTargets = targets
                    
                    // Observe active diet plan from Room
                    dietPlannerUseCases.getActiveDietPlan(userId).collectLatest { activePlan ->
                        currentActivePlan = activePlan
                        val currentState = _uiState.value
                        if (currentState !is DietPlannerUiState.Generating && currentState !is DietPlannerUiState.Reviewing) {
                            _uiState.value = DietPlannerUiState.Ready(
                                userProfile = profile,
                                nutritionTargets = targets,
                                activePlan = activePlan,
                                selectedDay = 1
                            )
                        }
                    }
                }
            }
        }
    }

    fun selectDay(dayNumber: Int) {
        when (val state = _uiState.value) {
            is DietPlannerUiState.Ready -> {
                _uiState.value = state.copy(selectedDay = dayNumber)
            }
            is DietPlannerUiState.Reviewing -> {
                _uiState.value = state.copy(selectedDay = dayNumber)
            }
            else -> Unit
        }
    }

    fun generate7DayPlan() {
        if (_uiState.value is DietPlannerUiState.Generating) {
            return
        }

        val profile = currentUserProfile
        val targets = currentNutritionTargets

        if (profile == null || targets == null) {
            _uiState.value = DietPlannerUiState.IncompleteProfile(
                "Complete your profile before generating a meal plan."
            )
            return
        }

        _uiState.value = DietPlannerUiState.Generating(
            userProfile = profile,
            nutritionTargets = targets,
            activePlan = currentActivePlan
        )

        viewModelScope.launch {
            val result = dietPlannerUseCases.generateDietPlan(profile, targets)
            result.onSuccess { plan ->
                _uiState.value = DietPlannerUiState.Reviewing(
                    userProfile = profile,
                    nutritionTargets = targets,
                    draftPlan = plan,
                    selectedDay = 1,
                    isSaving = false
                )
            }.onFailure { error ->
                _uiState.value = DietPlannerUiState.Error(
                    message = error.message ?: "Failed to generate plan. Please try again.",
                    userProfile = profile,
                    nutritionTargets = targets,
                    activePlan = currentActivePlan
                )
            }
        }
    }

    fun updateMeal(dayNumber: Int, updatedMeal: DietPlanMeal) {
        val currentState = _uiState.value as? DietPlannerUiState.Reviewing ?: return
        val currentPlan = currentState.draftPlan

        val updatedDays = currentPlan.days.map { day ->
            if (day.dayNumber == dayNumber) {
                val updatedMeals = day.meals.map { meal ->
                    if (meal.id == updatedMeal.id) updatedMeal else meal
                }
                DayPlan(dayNumber = day.dayNumber, meals = updatedMeals)
            } else {
                day
            }
        }

        val updatedPlan = currentPlan.copy(days = updatedDays)
        _uiState.value = currentState.copy(draftPlan = updatedPlan)
    }

    fun savePlan() {
        val currentState = _uiState.value as? DietPlannerUiState.Reviewing ?: return
        if (currentState.isSaving) return
        val planToSave = currentState.draftPlan

        _uiState.value = currentState.copy(isSaving = true)

        viewModelScope.launch {
            dietPlannerUseCases.saveDietPlan(planToSave)
            currentActivePlan = planToSave
            _uiState.value = DietPlannerUiState.Ready(
                userProfile = currentState.userProfile,
                nutritionTargets = currentState.nutritionTargets,
                activePlan = planToSave,
                selectedDay = currentState.selectedDay
            )
        }
    }

    fun cancelReview() {
        val profile = currentUserProfile
        val targets = currentNutritionTargets
        if (profile != null && targets != null) {
            _uiState.value = DietPlannerUiState.Ready(
                userProfile = profile,
                nutritionTargets = targets,
                activePlan = currentActivePlan,
                selectedDay = 1
            )
        } else {
            _uiState.value = DietPlannerUiState.IncompleteProfile()
        }
    }

    fun dismissError() {
        val profile = currentUserProfile
        val targets = currentNutritionTargets
        if (profile != null && targets != null) {
            _uiState.value = DietPlannerUiState.Ready(
                userProfile = profile,
                nutritionTargets = targets,
                activePlan = currentActivePlan,
                selectedDay = 1
            )
        } else {
            _uiState.value = DietPlannerUiState.IncompleteProfile()
        }
    }
}
