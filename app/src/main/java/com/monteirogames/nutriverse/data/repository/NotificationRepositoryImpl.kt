package com.monteirogames.nutriverse.data.repository

import com.monteirogames.nutriverse.data.local.dao.NotificationDao
import com.monteirogames.nutriverse.data.local.entity.NotificationRecordEntity
import com.monteirogames.nutriverse.data.local.entity.NotificationSettingsEntity
import com.monteirogames.nutriverse.domain.model.NotificationSettings
import com.monteirogames.nutriverse.domain.repository.NotificationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

class NotificationRepositoryImpl(
    private val notificationDao: NotificationDao
) : NotificationRepository {

    override fun getNotificationSettings(userId: String): Flow<NotificationSettings> {
        return notificationDao.getNotificationSettings(userId).map { entity ->
            entity?.toDomain() ?: defaultSettings(userId)
        }
    }

    override suspend fun getNotificationSettingsOnce(userId: String): NotificationSettings {
        return notificationDao.getNotificationSettingsOnce(userId)?.toDomain() ?: defaultSettings(userId)
    }

    override suspend fun saveNotificationSettings(settings: NotificationSettings) {
        notificationDao.saveNotificationSettings(settings.toEntity())
    }

    override suspend fun isNotificationSent(userId: String, deduplicationKey: String): Boolean {
        return notificationDao.isNotificationSent(userId, deduplicationKey)
    }

    override suspend fun recordNotificationSent(userId: String, deduplicationKey: String, type: String) {
        val record = NotificationRecordEntity(
            id = UUID.randomUUID().toString(),
            userId = userId,
            deduplicationKey = deduplicationKey,
            type = type,
            sentAt = System.currentTimeMillis()
        )
        notificationDao.recordNotificationSent(record)
    }

    override suspend fun clearUserRecords(userId: String) {
        notificationDao.clearUserRecords(userId)
    }

    private fun defaultSettings(userId: String): NotificationSettings {
        return NotificationSettings(
            userId = userId,
            notificationsEnabled = true,
            mealRemindersEnabled = true,
            nutritionRemindersEnabled = true,
            pantryExpiryEnabled = true,
            groceryRemindersEnabled = true,
            budgetWarningsEnabled = true,
            breakfastTime = "08:00",
            lunchTime = "13:00",
            dinnerTime = "20:00"
        )
    }

    private fun NotificationSettingsEntity.toDomain(): NotificationSettings {
        return NotificationSettings(
            userId = userId,
            notificationsEnabled = notificationsEnabled,
            mealRemindersEnabled = mealRemindersEnabled,
            nutritionRemindersEnabled = nutritionRemindersEnabled,
            pantryExpiryEnabled = pantryExpiryEnabled,
            groceryRemindersEnabled = groceryRemindersEnabled,
            budgetWarningsEnabled = budgetWarningsEnabled,
            breakfastTime = breakfastTime,
            lunchTime = lunchTime,
            dinnerTime = dinnerTime
        )
    }

    private fun NotificationSettings.toEntity(): NotificationSettingsEntity {
        return NotificationSettingsEntity(
            userId = userId,
            notificationsEnabled = notificationsEnabled,
            mealRemindersEnabled = mealRemindersEnabled,
            nutritionRemindersEnabled = nutritionRemindersEnabled,
            pantryExpiryEnabled = pantryExpiryEnabled,
            groceryRemindersEnabled = groceryRemindersEnabled,
            budgetWarningsEnabled = budgetWarningsEnabled,
            breakfastTime = breakfastTime,
            lunchTime = lunchTime,
            dinnerTime = dinnerTime
        )
    }
}
