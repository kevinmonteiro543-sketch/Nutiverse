package com.monteirogames.nutriverse.domain.repository

import com.monteirogames.nutriverse.domain.model.HealthReport
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

interface HealthReportRepository {
    fun getReportsForUser(userId: String): Flow<List<HealthReport>>
    suspend fun getAllReportsList(userId: String): List<HealthReport> = getReportsForUser(userId).first()
    suspend fun getReportById(reportId: String, userId: String): HealthReport?
    suspend fun saveReport(report: HealthReport)
    suspend fun deleteReport(reportId: String, userId: String)
}
