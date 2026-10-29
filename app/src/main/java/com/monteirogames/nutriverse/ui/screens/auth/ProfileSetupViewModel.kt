package com.monteirogames.nutriverse.ui.screens.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.monteirogames.nutriverse.domain.model.UserProfile
import com.monteirogames.nutriverse.domain.usecase.AuthUseCases
import com.monteirogames.nutriverse.domain.usecase.ProfileSyncUseCases
import com.monteirogames.nutriverse.domain.usecase.UserProfileUseCases
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class ProfileSetupUiState {
    object Idle : ProfileSetupUiState()
    object Loading : ProfileSetupUiState()
    data class Error(val message: String) : ProfileSetupUiState()
    object Success : ProfileSetupUiState()
}

class ProfileSetupViewModel(
    private val authUseCases: AuthUseCases,
    private val userProfileUseCases: UserProfileUseCases,
    private val profileSyncUseCases: ProfileSyncUseCases? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow<ProfileSetupUiState>(ProfileSetupUiState.Idle)
    val uiState: StateFlow<ProfileSetupUiState> = _uiState.asStateFlow()

    fun saveProfile(
        name: String,
        age: String,
        gender: String,
        height: String,
        weight: String,
        goal: String,
        dietaryPreference: String,
        allergies: String,
        activityLevel: String,
        workoutFrequency: String,
        workoutIntensity: String
    ) {
        val userId = authUseCases.getCurrentUserId()
        if (userId == null) {
            _uiState.value = ProfileSetupUiState.Error("User not logged in")
            return
        }

        if (name.isBlank() || age.isBlank() || height.isBlank() || weight.isBlank()) {
            _uiState.value = ProfileSetupUiState.Error("Please fill out required fields")
            return
        }

        val parsedAge = age.toIntOrNull()
        val parsedHeight = height.toFloatOrNull()
        val parsedWeight = weight.toFloatOrNull()

        if (parsedAge == null || parsedHeight == null || parsedWeight == null) {
            _uiState.value = ProfileSetupUiState.Error("Invalid number format")
            return
        }

        viewModelScope.launch {
            _uiState.value = ProfileSetupUiState.Loading
            
            try {
                val profile = UserProfile(
                    userId = userId,
                    name = name,
                    age = parsedAge,
                    gender = gender.ifBlank { "Other" },
                    heightCm = parsedHeight,
                    weightKg = parsedWeight,
                    goal = goal.ifBlank { "Maintain Weight" },
                    dietaryPreference = dietaryPreference,
                    allergies = allergies,
                    activityLevel = activityLevel.ifBlank { "Sedentary" },
                    workoutFrequency = workoutFrequency.ifBlank { "0 days" },
                    workoutIntensity = workoutIntensity.ifBlank { "Light" },
                    updatedAt = System.currentTimeMillis()
                )
                
                userProfileUseCases.saveUserProfile(profile)

                // Safe cloud synchronization trigger (Phase 7B-2)
                try {
                    profileSyncUseCases?.syncProfile?.invoke()
                } catch (_: Exception) {}

                _uiState.value = ProfileSetupUiState.Success
            } catch (e: Exception) {
                _uiState.value = ProfileSetupUiState.Error(e.message ?: "Failed to save profile")
            }
        }
    }
}
