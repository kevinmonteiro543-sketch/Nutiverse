package com.monteirogames.nutriverse.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "notification_record",
    indices = [
        Index(value = ["userId"]),
        Index(value = ["userId", "deduplicationKey"], unique = true)
    ]
)
data class NotificationRecordEntity(
    @PrimaryKey
    val id: String,
    val userId: String,
    val deduplicationKey: String,
    val type: String,
    val sentAt: Long
)
