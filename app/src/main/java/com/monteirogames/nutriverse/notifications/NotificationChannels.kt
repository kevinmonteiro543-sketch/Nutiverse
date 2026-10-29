package com.monteirogames.nutriverse.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build

object NotificationChannels {
    const val CHANNEL_MEAL = "nutriverse_meal_reminders"
    const val CHANNEL_NUTRITION = "nutriverse_nutrition"
    const val CHANNEL_PANTRY = "nutriverse_pantry"
    const val CHANNEL_GROCERY = "nutriverse_grocery"
    const val CHANNEL_BUDGET = "nutriverse_budget"

    fun createChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val channels = listOf(
                NotificationChannel(
                    CHANNEL_MEAL,
                    "NutriVerse Meal Reminders",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "Reminders for planned meals"
                },
                NotificationChannel(
                    CHANNEL_NUTRITION,
                    "NutriVerse Nutrition",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "Daily protein and nutrition target reminders"
                },
                NotificationChannel(
                    CHANNEL_PANTRY,
                    "NutriVerse Pantry",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "Food expiration alerts"
                },
                NotificationChannel(
                    CHANNEL_GROCERY,
                    "NutriVerse Grocery",
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = "Grocery list reminders"
                },
                NotificationChannel(
                    CHANNEL_BUDGET,
                    "NutriVerse Budget",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "Food budget warnings"
                }
            )

            notificationManager.createNotificationChannels(channels)
        }
    }
}
