package com.monteirogames.nutriverse.domain.repository

import com.monteirogames.nutriverse.domain.model.HealthReport
import com.monteirogames.nutriverse.domain.model.SyncState
import kotlinx.coroutines.flow.StateFlow

interface HealthReportSyncRepository {
    val syncState: StateFlow<SyncState>
    fun getSyncState(): SyncState
    suspend fun syncHealthReports(): Result<Int>
    suspend fun uploadHealthReport(report: HealthReport): Result<Unit>
    suspend fun deleteHealthReportInCloud(reportId: String): Result<Unit>
}
