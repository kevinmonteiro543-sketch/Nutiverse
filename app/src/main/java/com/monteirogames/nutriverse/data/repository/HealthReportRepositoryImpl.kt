package com.monteirogames.nutriverse.data.repository

import com.monteirogames.nutriverse.data.local.dao.HealthReportDao
import com.monteirogames.nutriverse.data.local.dao.SyncTombstoneDao
import com.monteirogames.nutriverse.data.local.entity.HealthReportEntity
import com.monteirogames.nutriverse.data.local.entity.HealthReportResultEntity
import com.monteirogames.nutriverse.data.local.entity.HealthReportWithResults
import com.monteirogames.nutriverse.data.local.entity.SyncTombstoneEntity
import com.monteirogames.nutriverse.domain.model.HealthReport
import com.monteirogames.nutriverse.domain.model.HealthReportResult
import com.monteirogames.nutriverse.domain.model.HealthResultStatus
import com.monteirogames.nutriverse.domain.repository.HealthReportRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray

class HealthReportRepositoryImpl(
    private val healthReportDao: HealthReportDao,
    private val syncTombstoneDao: SyncTombstoneDao? = null
) : HealthReportRepository {

    override fun getReportsForUser(userId: String): Flow<List<HealthReport>> {
        return healthReportDao.getReportsForUser(userId).map { list ->
            list.map { it.toDomain() }
        }
    }

    override suspend fun getAllReportsList(userId: String): List<HealthReport> {
        return healthReportDao.getAllReportsListForUser(userId).map { it.toDomain() }
    }

    override suspend fun getReportById(reportId: String, userId: String): HealthReport? {
        return healthReportDao.getReportById(reportId, userId)?.toDomain()
    }

    override suspend fun saveReport(report: HealthReport) {
        val timestamp = if (report.updatedAt > 0L) report.updatedAt else System.currentTimeMillis()
        val reportWithTimestamp = report.copy(updatedAt = timestamp)
        val (reportEntity, resultEntities) = reportWithTimestamp.toEntities()
        healthReportDao.insertFullReport(reportEntity, resultEntities)
        syncTombstoneDao?.deleteTombstone("healthReport", report.id)
    }

    override suspend fun deleteReport(reportId: String, userId: String) {
        healthReportDao.deleteResultsForReport(reportId)
        healthReportDao.deleteReportById(reportId, userId)
        if (userId.isNotBlank()) {
            syncTombstoneDao?.insertTombstone(
                SyncTombstoneEntity(
                    id = reportId,
                    entityType = "healthReport",
                    userId = userId,
                    deletedAt = System.currentTimeMillis()
                )
            )
        }
    }
}

fun HealthReportWithResults.toDomain(): HealthReport {
    val obsList = mutableListOf<String>()
    try {
        val array = JSONArray(report.observationsJson)
        for (i in 0 until array.length()) {
            val s = array.optString(i)
            if (!s.isNullOrBlank()) obsList.add(s)
        }
    } catch (_: Exception) {}

    val questionsList = mutableListOf<String>()
    try {
        val array = JSONArray(report.questionsJson)
        for (i in 0 until array.length()) {
            val s = array.optString(i)
            if (!s.isNullOrBlank()) questionsList.add(s)
        }
    } catch (_: Exception) {}

    return HealthReport(
        id = report.id,
        userId = report.userId,
        reportName = report.reportName,
        reportType = report.reportType,
        createdAt = report.createdAt,
        summary = report.summary,
        observations = obsList,
        questionsForDoctor = questionsList,
        disclaimer = report.disclaimer,
        results = results.map { it.toDomain() },
        updatedAt = if (report.updatedAt > 0L) report.updatedAt else report.createdAt
    )
}

fun HealthReportResultEntity.toDomain() = HealthReportResult(
    id = id,
    reportId = reportId,
    testName = testName,
    value = value,
    unit = unit,
    referenceRange = referenceRange,
    interpretation = interpretation,
    status = HealthResultStatus.fromString(status),
    userNote = userNote
)

fun HealthReport.toEntities(): Pair<HealthReportEntity, List<HealthReportResultEntity>> {
    val timestamp = if (updatedAt > 0L) updatedAt else createdAt
    val reportEntity = HealthReportEntity(
        id = id,
        userId = userId,
        reportName = reportName,
        reportType = reportType,
        createdAt = createdAt,
        summary = summary,
        observationsJson = JSONArray(observations).toString(),
        questionsJson = JSONArray(questionsForDoctor).toString(),
        disclaimer = disclaimer,
        updatedAt = timestamp
    )

    val resultEntities = results.map { res ->
        HealthReportResultEntity(
            id = res.id,
            reportId = id,
            testName = res.testName,
            value = res.value,
            unit = res.unit,
            referenceRange = res.referenceRange,
            interpretation = res.interpretation,
            status = res.status.name,
            userNote = res.userNote
        )
    }

    return Pair(reportEntity, resultEntities)
}
