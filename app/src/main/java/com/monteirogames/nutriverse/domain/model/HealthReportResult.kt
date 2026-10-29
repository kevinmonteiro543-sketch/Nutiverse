package com.monteirogames.nutriverse.domain.model

data class HealthReportResult(
    val id: String,
    val reportId: String,
    val testName: String,
    val value: String?,
    val unit: String?,
    val referenceRange: String?,
    val interpretation: String?,
    val status: HealthResultStatus,
    val userNote: String? = null
)
