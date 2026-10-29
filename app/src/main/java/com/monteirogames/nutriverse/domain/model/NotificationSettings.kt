package com.monteirogames.nutriverse.domain.model

data class NotificationSettings(
    val userId: String,
    val notificationsEnabled: Boolean = true,
    val mealRemindersEnabled: Boolean = true,
    val nutritionRemindersEnabled: Boolean = true,
    val pantryExpiryEnabled: Boolean = true,
    val groceryRemindersEnabled: Boolean = true,
    val budgetWarningsEnabled: Boolean = true,
    val breakfastTime: String = "08:00",
    val lunchTime: String = "13:00",
    val dinnerTime: String = "20:00"
)
