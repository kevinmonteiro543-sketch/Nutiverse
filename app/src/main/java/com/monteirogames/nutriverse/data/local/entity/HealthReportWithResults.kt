package com.monteirogames.nutriverse.data.local.entity

import androidx.room.Embedded
import androidx.room.Relation

data class HealthReportWithResults(
    @Embedded
    val report: HealthReportEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "reportId"
    )
    val results: List<HealthReportResultEntity>
)
