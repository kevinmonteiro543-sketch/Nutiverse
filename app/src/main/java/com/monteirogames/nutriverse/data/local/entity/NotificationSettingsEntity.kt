package com.monteirogames.nutriverse.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notification_settings")
data class NotificationSettingsEntity(
    @PrimaryKey
    val userId: String,
    val notificationsEnabled: Boolean,
    val mealRemindersEnabled: Boolean,
    val nutritionRemindersEnabled: Boolean,
    val pantryExpiryEnabled: Boolean,
    val groceryRemindersEnabled: Boolean,
    val budgetWarningsEnabled: Boolean,
    val breakfastTime: String,
    val lunchTime: String,
    val dinnerTime: String
)
