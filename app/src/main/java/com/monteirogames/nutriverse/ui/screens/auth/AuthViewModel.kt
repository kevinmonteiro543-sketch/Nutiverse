package com.monteirogames.nutriverse.ui.screens.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.monteirogames.nutriverse.domain.repository.AuthResult
import com.monteirogames.nutriverse.domain.usecase.AuthUseCases
import com.monteirogames.nutriverse.domain.usecase.UserProfileUseCases
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

enum class AuthMode { LOGIN, REGISTER }

sealed class AuthUiState {
    object Idle : AuthUiState()
    object Loading : AuthUiState()
    data class Error(val message: String) : AuthUiState()
    data class PasswordResetSent(val email: String, val message: String) : AuthUiState()
    object SuccessProfileNeeded : AuthUiState()
    object SuccessProfileExists : AuthUiState()
}

class AuthViewModel(
    private val authUseCases: AuthUseCases,
    private val userProfileUseCases: UserProfileUseCases
) : ViewModel() {

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    private val _mode = MutableStateFlow(AuthMode.LOGIN)
    val mode: StateFlow<AuthMode> = _mode.asStateFlow()

    fun toggleMode() {
        _mode.value = if (_mode.value == AuthMode.LOGIN) AuthMode.REGISTER else AuthMode.LOGIN
        _uiState.value = AuthUiState.Idle
    }

    fun setMode(newMode: AuthMode) {
        if (_mode.value != newMode) {
            _mode.value = newMode
            _uiState.value = AuthUiState.Idle
        }
    }
    
    fun resetError() {
        if (_uiState.value is AuthUiState.Error || _uiState.value is AuthUiState.PasswordResetSent) {
            _uiState.value = AuthUiState.Idle
        }
    }

    fun authenticate(email: String, password: String) {
        authenticate(email, password, "")
    }

    fun authenticate(email: String, password: String, confirmPassword: String) {
        val trimmedEmail = email.trim()

        if (trimmedEmail.isBlank() || password.isBlank()) {
            _uiState.value = AuthUiState.Error("Email and password cannot be empty")
            return
        }

        if (!isValidEmail(trimmedEmail)) {
            _uiState.value = AuthUiState.Error("Please enter a valid email address")
            return
        }

        if (_mode.value == AuthMode.REGISTER) {
            if (password.length < 6) {
                _uiState.value = AuthUiState.Error("Password must be at least 6 characters long")
                return
            }
            if (password != confirmPassword) {
                _uiState.value = AuthUiState.Error("Passwords do not match")
                return
            }
        }

        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            
            val result = if (_mode.value == AuthMode.LOGIN) {
                authUseCases.signIn(trimmedEmail, password)
            } else {
                authUseCases.signUp(trimmedEmail, password)
            }

            when (result) {
                is AuthResult.Success -> {
                    checkProfile(result.data)
                }
                is AuthResult.Error -> {
                    _uiState.value = AuthUiState.Error(result.message)
                }
                AuthResult.Loading -> { }
            }
        }
    }

    fun sendPasswordReset(email: String) {
        val trimmedEmail = email.trim()
        if (trimmedEmail.isBlank()) {
            _uiState.value = AuthUiState.Error("Please enter your email to reset your password")
            return
        }
        if (!isValidEmail(trimmedEmail)) {
            _uiState.value = AuthUiState.Error("Please enter a valid email address")
            return
        }

        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            when (val result = authUseCases.sendPasswordResetEmail(trimmedEmail)) {
                is AuthResult.Success -> {
                    _uiState.value = AuthUiState.PasswordResetSent(
                        email = trimmedEmail,
                        message = "Password reset email sent to $trimmedEmail. Please check your inbox."
                    )
                }
                is AuthResult.Error -> {
                    _uiState.value = AuthUiState.Error(result.message)
                }
                AuthResult.Loading -> { }
            }
        }
    }

    private suspend fun checkProfile(userId: String) {
        val profile = userProfileUseCases.getUserProfile(userId).firstOrNull()
        if (profile == null) {
            _uiState.value = AuthUiState.SuccessProfileNeeded
        } else {
            _uiState.value = AuthUiState.SuccessProfileExists
        }
    }

    private fun isValidEmail(email: String): Boolean {
        val emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$".toRegex()
        return email.isNotBlank() && emailRegex.matches(email)
    }
}
