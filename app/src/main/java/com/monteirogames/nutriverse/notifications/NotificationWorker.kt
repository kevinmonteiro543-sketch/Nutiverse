package com.monteirogames.nutriverse.notifications

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.monteirogames.nutriverse.NutriVerseApplication

class NotificationWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as? NutriVerseApplication ?: return Result.success()

        val userId = app.authUseCases.getCurrentUserId() ?: return Result.success()

        try {
            val evaluateUseCase = app.notificationUseCases.evaluateNotifications
            val pendingNotifications = evaluateUseCase(userId)

            for (notification in pendingNotifications) {
                val shown = NotificationHelper.showNotification(applicationContext, notification)
                if (shown) {
                    evaluateUseCase.markNotificationSent(notification)
                }
            }
        } catch (_: Exception) {
            return Result.retry()
        }

        return Result.success()
    }
}
