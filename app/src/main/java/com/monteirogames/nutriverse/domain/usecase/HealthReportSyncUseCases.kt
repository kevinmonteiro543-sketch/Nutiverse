package com.monteirogames.nutriverse.domain.usecase

import com.monteirogames.nutriverse.domain.model.HealthReport
import com.monteirogames.nutriverse.domain.model.SyncState
import com.monteirogames.nutriverse.domain.repository.HealthReportSyncRepository
import kotlinx.coroutines.flow.StateFlow

class SyncHealthReportsUseCase(private val repository: HealthReportSyncRepository) {
    suspend operator fun invoke(): Result<Int> = repository.syncHealthReports()
}

class UploadHealthReportUseCase(private val repository: HealthReportSyncRepository) {
    suspend operator fun invoke(report: HealthReport): Result<Unit> = repository.uploadHealthReport(report)
}

class DeleteHealthReportFromCloudUseCase(private val repository: HealthReportSyncRepository) {
    suspend operator fun invoke(reportId: String): Result<Unit> = repository.deleteHealthReportInCloud(reportId)
}

class ObserveHealthReportSyncStateUseCase(private val repository: HealthReportSyncRepository) {
    operator fun invoke(): StateFlow<SyncState> = repository.syncState
}

data class HealthReportSyncUseCases(
    val syncHealthReports: SyncHealthReportsUseCase,
    val uploadHealthReport: UploadHealthReportUseCase,
    val deleteHealthReportFromCloud: DeleteHealthReportFromCloudUseCase,
    val observeHealthReportSyncState: ObserveHealthReportSyncStateUseCase
)
