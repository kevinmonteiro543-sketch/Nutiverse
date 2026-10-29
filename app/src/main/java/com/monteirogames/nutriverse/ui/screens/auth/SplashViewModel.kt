package com.monteirogames.nutriverse.ui.screens.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.monteirogames.nutriverse.domain.usecase.AuthUseCases
import com.monteirogames.nutriverse.domain.usecase.ProfileSyncUseCases
import com.monteirogames.nutriverse.domain.usecase.UserProfileUseCases
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

sealed class SplashState {
    object Loading : SplashState()
    object NavigateToLogin : SplashState()
    object NavigateToProfileSetup : SplashState()
    object NavigateToMain : SplashState()
}

class SplashViewModel(
    private val authUseCases: AuthUseCases,
    private val userProfileUseCases: UserProfileUseCases,
    private val profileSyncUseCases: ProfileSyncUseCases? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow<SplashState>(SplashState.Loading)
    val uiState: StateFlow<SplashState> = _uiState.asStateFlow()

    init {
        checkAuthState()
    }

    private fun checkAuthState() {
        viewModelScope.launch {
            val userId = authUseCases.getCurrentUserId()
            if (userId == null) {
                _uiState.value = SplashState.NavigateToLogin
            } else {
                var profile = userProfileUseCases.getUserProfile(userId).firstOrNull()
                // If local Room profile is missing, check cloud profile via safe first-sync
                if (profile == null && profileSyncUseCases != null) {
                    try {
                        profileSyncUseCases.syncProfile()
                        profile = userProfileUseCases.getUserProfile(userId).firstOrNull()
                    } catch (_: Exception) {}
                }

                if (profile == null) {
                    _uiState.value = SplashState.NavigateToProfileSetup
                } else {
                    _uiState.value = SplashState.NavigateToMain
                }
            }
        }
    }
}
