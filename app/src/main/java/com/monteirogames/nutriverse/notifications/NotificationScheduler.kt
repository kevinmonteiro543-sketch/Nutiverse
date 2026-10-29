package com.monteirogames.nutriverse.notifications

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object NotificationScheduler {
    private const val PERIODIC_WORK_NAME = "nutriverse_periodic_notifications"
    private const val IMMEDIATE_WORK_NAME = "nutriverse_immediate_notifications"

    fun schedulePeriodicEvaluation(context: Context) {
        try {
            val workRequest = PeriodicWorkRequestBuilder<NotificationWorker>(
                1, TimeUnit.HOURS,
                15, TimeUnit.MINUTES // flex window
            ).setConstraints(
                Constraints.Builder()
                    .setRequiresBatteryNotLow(false)
                    .build()
            ).build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                PERIODIC_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                workRequest
            )
        } catch (_: Exception) {
            // Safely ignored in test/Robolectric environments where WorkManager is not initialized
        }
    }

    fun triggerImmediateEvaluation(context: Context) {
        try {
            val workRequest = OneTimeWorkRequestBuilder<NotificationWorker>()
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                IMMEDIATE_WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                workRequest
            )
        } catch (_: Exception) {
            // Safely ignored in test environments
        }
    }

    fun cancelAllScheduledWork(context: Context) {
        try {
            WorkManager.getInstance(context).cancelUniqueWork(PERIODIC_WORK_NAME)
            WorkManager.getInstance(context).cancelUniqueWork(IMMEDIATE_WORK_NAME)
        } catch (_: Exception) {
            // Safely ignored in test environments
        }
    }
}
