package com.monteirogames.nutriverse.ui.screens.profile

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.monteirogames.nutriverse.domain.model.NotificationSettings
import com.monteirogames.nutriverse.domain.model.NotificationType
import com.monteirogames.nutriverse.domain.model.PendingNotification
import com.monteirogames.nutriverse.domain.usecase.AuthUseCases
import com.monteirogames.nutriverse.domain.usecase.NotificationUseCases
import com.monteirogames.nutriverse.notifications.NotificationChannels
import com.monteirogames.nutriverse.notifications.NotificationHelper
import com.monteirogames.nutriverse.notifications.NotificationScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

data class NotificationSettingsUiState(
    val isLoading: Boolean = true,
    val settings: NotificationSettings = NotificationSettings(userId = ""),
    val hasPermission: Boolean = false,
    val infoMessage: String? = null
)

class NotificationSettingsViewModel(
    private val authUseCases: AuthUseCases,
    private val notificationUseCases: NotificationUseCases
) : ViewModel() {

    private val _uiState = MutableStateFlow(NotificationSettingsUiState())
    val uiState: StateFlow<NotificationSettingsUiState> = _uiState.asStateFlow()

    private var currentUserId: String = ""

    init {
        loadSettings()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            authUseCases.getCurrentUserFlow().collectLatest { user ->
                val userId = user ?: authUseCases.getCurrentUserId() ?: "guest_user"
                currentUserId = userId

                notificationUseCases.getNotificationSettings(userId).collectLatest { settings ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        settings = settings
                    )
                }
            }
        }
    }

    fun updatePermissionState(hasPermission: Boolean) {
        _uiState.value = _uiState.value.copy(hasPermission = hasPermission)
    }

    fun updateMasterToggle(enabled: Boolean, context: Context) {
        val updated = _uiState.value.settings.copy(notificationsEnabled = enabled)
        saveSettings(updated)

        if (enabled) {
            NotificationScheduler.schedulePeriodicEvaluation(context)
            NotificationScheduler.triggerImmediateEvaluation(context)
        } else {
            NotificationScheduler.cancelAllScheduledWork(context)
        }
    }

    fun updateMealReminders(enabled: Boolean) {
        saveSettings(_uiState.value.settings.copy(mealRemindersEnabled = enabled))
    }

    fun updateNutritionReminders(enabled: Boolean) {
        saveSettings(_uiState.value.settings.copy(nutritionRemindersEnabled = enabled))
    }

    fun updatePantryExpiry(enabled: Boolean) {
        saveSettings(_uiState.value.settings.copy(pantryExpiryEnabled = enabled))
    }

    fun updateGroceryReminders(enabled: Boolean) {
        saveSettings(_uiState.value.settings.copy(groceryRemindersEnabled = enabled))
    }

    fun updateBudgetWarnings(enabled: Boolean) {
        saveSettings(_uiState.value.settings.copy(budgetWarningsEnabled = enabled))
    }

    fun updateMealTime(mealType: String, timeStr: String) {
        val current = _uiState.value.settings
        val updated = when (mealType.lowercase()) {
            "breakfast" -> current.copy(breakfastTime = timeStr)
            "lunch" -> current.copy(lunchTime = timeStr)
            "dinner" -> current.copy(dinnerTime = timeStr)
            else -> current
        }
        saveSettings(updated)
    }

    fun sendTestNotification(context: Context) {
        val testNotification = PendingNotification(
            id = 9999,
            userId = currentUserId,
            type = NotificationType.MEAL,
            deduplicationKey = "test_${System.currentTimeMillis()}",
            title = "NutriVerse Reminder",
            message = "Personalized reminders are configured and active!",
            channelId = NotificationChannels.CHANNEL_MEAL
        )
        val sent = NotificationHelper.showNotification(context, testNotification)
        _uiState.value = _uiState.value.copy(
            infoMessage = if (sent) "Test notification sent!" else "Could not send. Check permission."
        )
    }

    fun triggerBackgroundEvaluation(context: Context) {
        NotificationScheduler.triggerImmediateEvaluation(context)
    }

    fun clearInfoMessage() {
        _uiState.value = _uiState.value.copy(infoMessage = null)
    }

    private fun saveSettings(settings: NotificationSettings) {
        viewModelScope.launch {
            notificationUseCases.updateNotificationSettings(settings)
        }
    }
}
