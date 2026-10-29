package com.monteirogames.nutriverse.ui.screens.profile.health

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.monteirogames.nutriverse.domain.model.HealthReport
import com.monteirogames.nutriverse.domain.model.UserProfile
import com.monteirogames.nutriverse.domain.usecase.AuthUseCases
import com.monteirogames.nutriverse.domain.usecase.HealthReportUseCases
import com.monteirogames.nutriverse.domain.usecase.UserProfileUseCases
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

data class HealthReportUiState(
    val selectedImage: Bitmap? = null,
    val isAnalyzing: Boolean = false,
    val currentReport: HealthReport? = null,
    val historyReports: List<HealthReport> = emptyList(),
    val selectedHistoryReport: HealthReport? = null,
    val isSaved: Boolean = false,
    val infoMessage: String? = null,
    val errorMessage: String? = null
)

class HealthReportAnalysisViewModel(
    private val authUseCases: AuthUseCases,
    private val userProfileUseCases: UserProfileUseCases,
    private val healthReportUseCases: HealthReportUseCases
) : ViewModel() {

    private val _uiState = MutableStateFlow(HealthReportUiState())
    val uiState: StateFlow<HealthReportUiState> = _uiState.asStateFlow()

    private var currentUserId: String? = null

    init {
        viewModelScope.launch {
            authUseCases.getCurrentUserFlow().collectLatest { user ->
                val userId = user ?: authUseCases.getCurrentUserId() ?: "guest_user"
                currentUserId = userId
                observeHistory(userId)
            }
        }
    }

    private fun observeHistory(userId: String) {
        viewModelScope.launch {
            healthReportUseCases.getHealthReportHistory(userId).collectLatest { list ->
                _uiState.value = _uiState.value.copy(historyReports = list)
            }
        }
    }

    fun onImageSelected(bitmap: Bitmap) {
        _uiState.value = _uiState.value.copy(
            selectedImage = bitmap,
            errorMessage = null
        )
    }

    fun clearImage() {
        _uiState.value = _uiState.value.copy(
            selectedImage = null,
            errorMessage = null
        )
    }

    fun analyzeReport() {
        val bitmap = _uiState.value.selectedImage ?: return
        val userId = currentUserId ?: authUseCases.getCurrentUserId() ?: "guest_user"

        if (_uiState.value.isAnalyzing) return

        _uiState.value = _uiState.value.copy(
            isAnalyzing = true,
            errorMessage = null
        )

        viewModelScope.launch {
            val userProfile: UserProfile? = userProfileUseCases.getUserProfile(userId).firstOrNull()

            val result = healthReportUseCases.analyzeHealthReport(
                bitmap = bitmap,
                userId = userId,
                userProfile = userProfile
            )

            result.onSuccess { report ->
                _uiState.value = _uiState.value.copy(
                    isAnalyzing = false,
                    currentReport = report,
                    isSaved = false,
                    errorMessage = null
                )
            }.onFailure { err ->
                _uiState.value = _uiState.value.copy(
                    isAnalyzing = false,
                    errorMessage = err.localizedMessage ?: "Failed to analyze report."
                )
            }
        }
    }

    fun updateResultValue(resultId: String, newValue: String) {
        val current = _uiState.value.currentReport ?: return
        val updatedResults = current.results.map { res ->
            if (res.id == resultId) res.copy(value = newValue.ifBlank { null }) else res
        }
        _uiState.value = _uiState.value.copy(
            currentReport = current.copy(results = updatedResults),
            isSaved = false
        )
    }

    fun updateResultNote(resultId: String, newNote: String) {
        val current = _uiState.value.currentReport ?: return
        val updatedResults = current.results.map { res ->
            if (res.id == resultId) res.copy(userNote = newNote.ifBlank { null }) else res
        }
        _uiState.value = _uiState.value.copy(
            currentReport = current.copy(results = updatedResults),
            isSaved = false
        )
    }

    fun removeResult(resultId: String) {
        val current = _uiState.value.currentReport ?: return
        val updatedResults = current.results.filter { it.id != resultId }
        _uiState.value = _uiState.value.copy(
            currentReport = current.copy(results = updatedResults),
            isSaved = false
        )
    }

    fun updateReportName(newName: String) {
        val current = _uiState.value.currentReport ?: return
        _uiState.value = _uiState.value.copy(
            currentReport = current.copy(reportName = newName.ifBlank { current.reportName }),
            isSaved = false
        )
    }

    fun saveCurrentReport() {
        if (_uiState.value.isSaved) return
        val current = _uiState.value.currentReport ?: return
        viewModelScope.launch {
            try {
                healthReportUseCases.saveHealthReport(current)
                _uiState.value = _uiState.value.copy(
                    isSaved = true,
                    infoMessage = "Report analysis saved successfully."
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Failed to save analysis: ${e.localizedMessage}"
                )
            }
        }
    }

    fun discardCurrentReport() {
        _uiState.value = _uiState.value.copy(
            currentReport = null,
            selectedImage = null,
            isSaved = false
        )
    }

    fun viewHistoryReport(report: HealthReport) {
        _uiState.value = _uiState.value.copy(selectedHistoryReport = report)
    }

    fun closeHistoryReport() {
        _uiState.value = _uiState.value.copy(selectedHistoryReport = null)
    }

    fun deleteHistoryReport(reportId: String) {
        val userId = currentUserId ?: authUseCases.getCurrentUserId() ?: "guest_user"
        viewModelScope.launch {
            try {
                healthReportUseCases.deleteHealthReport(reportId, userId)
                if (_uiState.value.selectedHistoryReport?.id == reportId) {
                    _uiState.value = _uiState.value.copy(selectedHistoryReport = null)
                }
                _uiState.value = _uiState.value.copy(infoMessage = "Report deleted.")
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Failed to delete report: ${e.localizedMessage}"
                )
            }
        }
    }

    fun clearInfoMessage() {
        _uiState.value = _uiState.value.copy(infoMessage = null)
    }

    fun clearErrorMessage() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
}
