package com.monteirogames.nutriverse.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.monteirogames.nutriverse.data.local.entity.HealthReportEntity
import com.monteirogames.nutriverse.data.local.entity.HealthReportResultEntity
import com.monteirogames.nutriverse.data.local.entity.HealthReportWithResults
import kotlinx.coroutines.flow.Flow

@Dao
interface HealthReportDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReport(report: HealthReportEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertResults(results: List<HealthReportResultEntity>)

    @Query("DELETE FROM health_report WHERE id = :reportId AND userId = :userId")
    suspend fun deleteReportById(reportId: String, userId: String)

    @Query("DELETE FROM health_report WHERE id = :reportId")
    suspend fun deleteReportById(reportId: String)

    @Query("DELETE FROM health_report_result WHERE reportId = :reportId")
    suspend fun deleteResultsForReport(reportId: String)

    @Transaction
    @Query("SELECT * FROM health_report WHERE userId = :userId ORDER BY createdAt DESC")
    fun getReportsForUser(userId: String): Flow<List<HealthReportWithResults>>

    @Transaction
    @Query("SELECT * FROM health_report WHERE userId = :userId ORDER BY createdAt DESC")
    suspend fun getAllReportsListForUser(userId: String): List<HealthReportWithResults>

    @Transaction
    @Query("SELECT * FROM health_report WHERE id = :reportId AND userId = :userId LIMIT 1")
    suspend fun getReportById(reportId: String, userId: String): HealthReportWithResults?

    @Transaction
    @Query("SELECT * FROM health_report WHERE id = :reportId LIMIT 1")
    suspend fun getReportByIdOnce(reportId: String): HealthReportWithResults?

    @Transaction
    suspend fun insertFullReport(report: HealthReportEntity, results: List<HealthReportResultEntity>) {
        insertReport(report)
        deleteResultsForReport(report.id)
        if (results.isNotEmpty()) {
            insertResults(results)
        }
    }
}
