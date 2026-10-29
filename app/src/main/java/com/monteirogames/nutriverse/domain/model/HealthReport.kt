package com.monteirogames.nutriverse.domain.model

data class HealthReport(
    val id: String,
    val userId: String,
    val reportName: String,
    val reportType: String,
    val createdAt: Long,
    val summary: String,
    val observations: List<String>,
    val questionsForDoctor: List<String>,
    val disclaimer: String,
    val results: List<HealthReportResult>,
    val updatedAt: Long = createdAt
)
