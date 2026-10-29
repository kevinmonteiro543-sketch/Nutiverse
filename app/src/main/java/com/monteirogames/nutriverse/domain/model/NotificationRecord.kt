package com.monteirogames.nutriverse.domain.model

data class PendingNotification(
    val id: Int,
    val userId: String,
    val type: NotificationType,
    val deduplicationKey: String,
    val title: String,
    val message: String,
    val channelId: String,
    val timestamp: Long = System.currentTimeMillis()
)

data class NotificationRecord(
    val id: String,
    val userId: String,
    val deduplicationKey: String,
    val type: String,
    val sentAt: Long
)
