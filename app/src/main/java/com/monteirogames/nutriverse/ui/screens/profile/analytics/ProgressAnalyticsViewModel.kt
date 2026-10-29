package com.monteirogames.nutriverse.ui.screens.profile.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.monteirogames.nutriverse.domain.model.AnalyticsTimeRange
import com.monteirogames.nutriverse.domain.model.ProgressAnalyticsData
import com.monteirogames.nutriverse.domain.usecase.AuthUseCases
import com.monteirogames.nutriverse.domain.usecase.ProgressAnalyticsUseCases
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

data class ProgressAnalyticsUiState(
    val isLoading: Boolean = true,
    val timeRange: AnalyticsTimeRange = AnalyticsTimeRange.LAST_7_DAYS,
    val analyticsData: ProgressAnalyticsData? = null,
    val errorMessage: String? = null
)

class ProgressAnalyticsViewModel(
    private val authUseCases: AuthUseCases,
    private val progressAnalyticsUseCases: ProgressAnalyticsUseCases
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProgressAnalyticsUiState())
    val uiState: StateFlow<ProgressAnalyticsUiState> = _uiState.asStateFlow()

    private var currentUserId: String? = null
    private var observeJob: Job? = null

    init {
        viewModelScope.launch {
            authUseCases.getCurrentUserFlow().collectLatest { user ->
                val userId = user ?: authUseCases.getCurrentUserId() ?: "guest_user"
                currentUserId = userId
                observeAnalytics(userId, _uiState.value.timeRange)
            }
        }
    }

    fun setTimeRange(timeRange: AnalyticsTimeRange) {
        if (_uiState.value.timeRange == timeRange) return
        _uiState.value = _uiState.value.copy(
            timeRange = timeRange,
            isLoading = true
        )
        currentUserId?.let { userId ->
            observeAnalytics(userId, timeRange)
        }
    }

    private fun observeAnalytics(userId: String, timeRange: AnalyticsTimeRange) {
        observeJob?.cancel()
        observeJob = viewModelScope.launch {
            try {
                progressAnalyticsUseCases.calculateProgressAnalytics(userId, timeRange).collectLatest { data ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        timeRange = timeRange,
                        analyticsData = data,
                        errorMessage = null
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = e.localizedMessage ?: "Failed to calculate analytics"
                )
            }
        }
    }

    fun clearErrorMessage() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
}
