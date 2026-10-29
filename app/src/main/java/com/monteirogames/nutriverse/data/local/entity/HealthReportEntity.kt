package com.monteirogames.nutriverse.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "health_report",
    indices = [
        Index(value = ["userId"]),
        Index(value = ["userId", "createdAt"])
    ]
)
data class HealthReportEntity(
    @PrimaryKey
    val id: String,
    val userId: String,
    val reportName: String,
    val reportType: String,
    val createdAt: Long,
    val summary: String,
    val observationsJson: String,
    val questionsJson: String,
    val disclaimer: String,
    val updatedAt: Long = 0L
)
