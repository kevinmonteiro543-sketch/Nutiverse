package com.monteirogames.nutriverse.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "health_report_result",
    foreignKeys = [
        ForeignKey(
            entity = HealthReportEntity::class,
            parentColumns = ["id"],
            childColumns = ["reportId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["reportId"])
    ]
)
data class HealthReportResultEntity(
    @PrimaryKey
    val id: String,
    val reportId: String,
    val testName: String,
    val value: String?,
    val unit: String?,
    val referenceRange: String?,
    val interpretation: String?,
    val status: String,
    val userNote: String?
)
