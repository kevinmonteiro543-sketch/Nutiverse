package com.monteirogames.nutriverse.domain.usecase

import android.graphics.Bitmap
import com.monteirogames.nutriverse.domain.model.HealthReport
import com.monteirogames.nutriverse.domain.model.UserProfile
import com.monteirogames.nutriverse.domain.repository.HealthReportAnalysisRepository
import com.monteirogames.nutriverse.domain.repository.HealthReportRepository
import kotlinx.coroutines.flow.Flow

class AnalyzeHealthReportUseCase(
    private val analysisRepository: HealthReportAnalysisRepository
) {
    suspend operator fun invoke(
        bitmap: Bitmap,
        userId: String,
        userProfile: UserProfile?
    ): Result<HealthReport> {
        return analysisRepository.analyzeReportImage(bitmap, userId, userProfile)
    }
}

class GetHealthReportHistoryUseCase(
    private val reportRepository: HealthReportRepository
) {
    operator fun invoke(userId: String): Flow<List<HealthReport>> {
        return reportRepository.getReportsForUser(userId)
    }
}

class GetHealthReportUseCase(
    private val reportRepository: HealthReportRepository
) {
    suspend operator fun invoke(reportId: String, userId: String): HealthReport? {
        return reportRepository.getReportById(reportId, userId)
    }
}

class SaveHealthReportUseCase(
    private val reportRepository: HealthReportRepository
) {
    suspend operator fun invoke(report: HealthReport) {
        reportRepository.saveReport(report)
    }
}

class DeleteHealthReportUseCase(
    private val reportRepository: HealthReportRepository
) {
    suspend operator fun invoke(reportId: String, userId: String) {
        reportRepository.deleteReport(reportId, userId)
    }
}

data class HealthReportUseCases(
    val analyzeHealthReport: AnalyzeHealthReportUseCase,
    val getHealthReportHistory: GetHealthReportHistoryUseCase,
    val getHealthReport: GetHealthReportUseCase,
    val saveHealthReport: SaveHealthReportUseCase,
    val deleteHealthReport: DeleteHealthReportUseCase
)
