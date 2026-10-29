package com.monteirogames.nutriverse.domain.repository

import com.monteirogames.nutriverse.domain.model.NotificationSettings
import kotlinx.coroutines.flow.Flow

interface NotificationRepository {
    fun getNotificationSettings(userId: String): Flow<NotificationSettings>
    suspend fun getNotificationSettingsOnce(userId: String): NotificationSettings
    suspend fun saveNotificationSettings(settings: NotificationSettings)
    suspend fun isNotificationSent(userId: String, deduplicationKey: String): Boolean
    suspend fun recordNotificationSent(userId: String, deduplicationKey: String, type: String)
    suspend fun clearUserRecords(userId: String)
}
