package com.monteirogames.nutriverse.ui.screens.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.monteirogames.nutriverse.domain.model.WeeklyReview
import com.monteirogames.nutriverse.domain.model.WeeklyReviewMetrics
import com.monteirogames.nutriverse.domain.usecase.AuthUseCases
import com.monteirogames.nutriverse.domain.usecase.InsufficientReviewDataException
import com.monteirogames.nutriverse.domain.usecase.WeeklyReviewUseCases
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

data class WeeklyAiReviewUiState(
    val isLoading: Boolean = false,
    val isGenerating: Boolean = false,
    val currentReview: WeeklyReview? = null,
    val previewMetrics: WeeklyReviewMetrics? = null,
    val historyReviews: List<WeeklyReview> = emptyList(),
    val selectedHistoryReview: WeeklyReview? = null,
    val errorMessage: String? = null,
    val insufficientDataMessage: String? = null,
    val canGenerateReview: Boolean = false
) {
    /**
     * The active review to display (either user-selected from history, or the latest generated review).
     */
    val activeReview: WeeklyReview?
        get() = selectedHistoryReview ?: currentReview

    /**
     * The active metrics to display (from active review if present, otherwise preview calculated metrics).
     */
    val activeMetrics: WeeklyReviewMetrics?
        get() = activeReview?.metrics ?: previewMetrics
}

class WeeklyAiReviewViewModel(
    private val authUseCases: AuthUseCases,
    private val weeklyReviewUseCases: WeeklyReviewUseCases
) : ViewModel() {

    private val _uiState = MutableStateFlow(WeeklyAiReviewUiState(isLoading = true))
    val uiState: StateFlow<WeeklyAiReviewUiState> = _uiState.asStateFlow()

    private var currentUserId: String = "guest_user"

    init {
        viewModelScope.launch {
            authUseCases.getCurrentUserFlow().collectLatest { userEmail ->
                val uid = authUseCases.getCurrentUserId() ?: userEmail ?: "guest_user"
                currentUserId = uid
                observeHistory(uid)
                loadInitialState(uid)
            }
        }
    }

    private fun observeHistory(userId: String) {
        viewModelScope.launch {
            weeklyReviewUseCases.getWeeklyReviewHistory(userId).collectLatest { history ->
                _uiState.value = _uiState.value.copy(historyReviews = history)
            }
        }
    }

    fun loadInitialState(userId: String = currentUserId) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            try {
                val metrics = weeklyReviewUseCases.calculateMetrics(userId)
                val canGenerate = metrics.trackedDays >= 2

                // Check for existing saved review for this exact 7-day period
                val existing = weeklyReviewUseCases.generateWeeklyReview(
                    userId = userId,
                    forceRegenerate = false
                ).getOrNull()

                val insufficientMsg = if (!canGenerate && existing == null) {
                    "Track at least 2 days of meals from the past 7 days to generate your weekly AI review."
                } else null

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    previewMetrics = metrics,
                    currentReview = existing,
                    canGenerateReview = canGenerate,
                    insufficientDataMessage = insufficientMsg
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = e.localizedMessage ?: "Failed to load nutrition metrics."
                )
            }
        }
    }

    fun generateReview(forceRegenerate: Boolean = true) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isGenerating = true,
                errorMessage = null,
                insufficientDataMessage = null
            )
            val result = weeklyReviewUseCases.generateWeeklyReview(
                userId = currentUserId,
                forceRegenerate = forceRegenerate
            )

            result.fold(
                onSuccess = { review ->
                    _uiState.value = _uiState.value.copy(
                        isGenerating = false,
                        currentReview = review,
                        selectedHistoryReview = null,
                        previewMetrics = review.metrics,
                        canGenerateReview = true,
                        errorMessage = null,
                        insufficientDataMessage = null
                    )
                },
                onFailure = { error ->
                    val isInsufficient = error is InsufficientReviewDataException
                    _uiState.value = _uiState.value.copy(
                        isGenerating = false,
                        insufficientDataMessage = if (isInsufficient) error.message else null,
                        errorMessage = if (!isInsufficient) (error.localizedMessage ?: "Analysis failed") else null
                    )
                }
            )
        }
    }

    fun selectHistoryReview(review: WeeklyReview?) {
        _uiState.value = _uiState.value.copy(selectedHistoryReview = review)
    }

    fun deleteReview(id: String) {
        viewModelScope.launch {
            weeklyReviewUseCases.deleteWeeklyReview(id)
            if (_uiState.value.selectedHistoryReview?.id == id) {
                _uiState.value = _uiState.value.copy(selectedHistoryReview = null)
            }
            if (_uiState.value.currentReview?.id == id) {
                _uiState.value = _uiState.value.copy(currentReview = null)
                loadInitialState()
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
}
