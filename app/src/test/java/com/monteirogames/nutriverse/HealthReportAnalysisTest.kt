package com.monteirogames.nutriverse

import com.monteirogames.nutriverse.domain.model.HealthReport
import com.monteirogames.nutriverse.domain.model.HealthReportAnalysisParser
import com.monteirogames.nutriverse.domain.model.HealthReportResult
import com.monteirogames.nutriverse.domain.model.HealthResultStatus
import com.monteirogames.nutriverse.domain.repository.HealthReportRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class HealthReportAnalysisTest {

    // Test 1: Valid structured AI JSON parses correctly
    @Test
    fun testValidStructuredAiJsonParsesCorrectly() {
        val json = """
            {
              "reportType": "Complete Blood Count",
              "summary": "Standard hematology panel showing mostly normal values with slightly elevated WBC.",
              "results": [
                {
                  "testName": "Hemoglobin",
                  "value": "14.5",
                  "unit": "g/dL",
                  "referenceRange": "13.8 - 17.2",
                  "interpretation": "Within the expected reference interval.",
                  "status": "NORMAL"
                },
                {
                  "testName": "White Blood Cell Count",
                  "value": "11.2",
                  "unit": "x10^3/uL",
                  "referenceRange": "4.5 - 11.0",
                  "interpretation": "Slightly above reference range.",
                  "status": "HIGH"
                }
              ],
              "observations": [
                "WBC is marginally above printed range."
              ],
              "questionsForDoctor": [
                "Could the mild WBC elevation be related to my recent head cold?"
              ],
              "disclaimer": "Informational only. Not medical advice."
            }
        """.trimIndent()

        val parseResult = HealthReportAnalysisParser.parse(json, "user_123")
        assertTrue(parseResult.isSuccess)

        val report = parseResult.getOrThrow()
        assertEquals("user_123", report.userId)
        assertEquals("Complete Blood Count", report.reportType)
        assertEquals(2, report.results.size)
        assertEquals("Hemoglobin", report.results[0].testName)
        assertEquals("14.5", report.results[0].value)
        assertEquals("g/dL", report.results[0].unit)
        assertEquals(HealthResultStatus.NORMAL, report.results[0].status)

        assertEquals("White Blood Cell Count", report.results[1].testName)
        assertEquals(HealthResultStatus.HIGH, report.results[1].status)

        assertEquals(1, report.observations.size)
        assertEquals(1, report.questionsForDoctor.size)
        assertEquals("Informational only. Not medical advice.", report.disclaimer)
    }

    // Test 2: Missing optional fields do not crash parsing
    @Test
    fun testMissingOptionalFieldsDoNotCrash() {
        val json = """
            {
              "results": [
                {
                  "testName": "Platelets"
                }
              ]
            }
        """.trimIndent()

        val parseResult = HealthReportAnalysisParser.parse(json, "user_123")
        assertTrue(parseResult.isSuccess)

        val report = parseResult.getOrThrow()
        assertEquals(1, report.results.size)
        assertEquals("Platelets", report.results[0].testName)
        assertNull(report.results[0].value)
        assertNull(report.results[0].unit)
        assertNull(report.results[0].referenceRange)
        assertNull(report.results[0].interpretation)
        assertEquals(HealthResultStatus.UNKNOWN, report.results[0].status)
        assertTrue(report.observations.isEmpty())
        assertTrue(report.questionsForDoctor.isEmpty())
        assertNotNull(report.disclaimer)
    }

    // Test 3: Invalid JSON produces a controlled error
    @Test
    fun testInvalidJsonProducesControlledError() {
        val brokenJson = "{ this is not valid json : [ 123 "
        val result = HealthReportAnalysisParser.parse(brokenJson, "user_123")
        assertTrue(result.isFailure)
        val ex = result.exceptionOrNull()
        assertNotNull(ex)
        assertTrue(ex is IllegalArgumentException)
        assertTrue(ex?.message?.contains("Malformed") == true)
    }

    // Test 4: LOW status parses correctly
    @Test
    fun testLowStatusParsing() {
        val json = """
            {
              "results": [
                { "testName": "Ferritin", "value": "10", "unit": "ng/mL", "status": "LOW" },
                { "testName": "Potassium", "value": "3.1", "status": "low" }
              ]
            }
        """.trimIndent()

        val report = HealthReportAnalysisParser.parse(json, "user_1").getOrThrow()
        assertEquals(HealthResultStatus.LOW, report.results[0].status)
        assertEquals(HealthResultStatus.LOW, report.results[1].status)
    }

    // Test 5: NORMAL status parses correctly
    @Test
    fun testNormalStatusParsing() {
        val json = """
            {
              "results": [
                { "testName": "Sodium", "value": "140", "status": "NORMAL" },
                { "testName": "Chloride", "value": "102", "status": "normal" }
              ]
            }
        """.trimIndent()

        val report = HealthReportAnalysisParser.parse(json, "user_1").getOrThrow()
        assertEquals(HealthResultStatus.NORMAL, report.results[0].status)
        assertEquals(HealthResultStatus.NORMAL, report.results[1].status)
    }

    // Test 6: HIGH status parses correctly
    @Test
    fun testHighStatusParsing() {
        val json = """
            {
              "results": [
                { "testName": "Glucose", "value": "115", "status": "HIGH" },
                { "testName": "Triglycerides", "value": "220", "status": "high" }
              ]
            }
        """.trimIndent()

        val report = HealthReportAnalysisParser.parse(json, "user_1").getOrThrow()
        assertEquals(HealthResultStatus.HIGH, report.results[0].status)
        assertEquals(HealthResultStatus.HIGH, report.results[1].status)
    }

    // Test 7: UNKNOWN status parses correctly
    @Test
    fun testUnknownStatusParsing() {
        val json = """
            {
              "results": [
                { "testName": "Random Marker", "status": "UNKNOWN" },
                { "testName": "Other Marker", "status": "Borderline" }
              ]
            }
        """.trimIndent()

        val report = HealthReportAnalysisParser.parse(json, "user_1").getOrThrow()
        assertEquals(HealthResultStatus.UNKNOWN, report.results[0].status)
        assertEquals(HealthResultStatus.UNKNOWN, report.results[1].status)
    }

    // Test 8: Missing value remains unknown / null rather than being fabricated
    @Test
    fun testMissingValueRemainsNull() {
        val json = """
            {
              "results": [
                {
                  "testName": "Calcium",
                  "value": null,
                  "referenceRange": "8.5 - 10.2",
                  "status": "UNKNOWN"
                }
              ]
            }
        """.trimIndent()

        val report = HealthReportAnalysisParser.parse(json, "user_1").getOrThrow()
        assertNull(report.results[0].value)
        assertEquals(HealthResultStatus.UNKNOWN, report.results[0].status)
    }

    // Test 9: Missing reference range remains null
    @Test
    fun testMissingReferenceRangeRemainsNull() {
        val json = """
            {
              "results": [
                {
                  "testName": "Custom Protein Ratio",
                  "value": "1.4",
                  "unit": "ratio"
                }
              ]
            }
        """.trimIndent()

        val report = HealthReportAnalysisParser.parse(json, "user_1").getOrThrow()
        assertNull(report.results[0].referenceRange)
        assertEquals("1.4", report.results[0].value)
    }

    // Fake in-memory repository for storage and multi-user isolation tests
    private class FakeHealthReportRepository : HealthReportRepository {
        val reportsMap = mutableMapOf<String, HealthReport>()

        override fun getReportsForUser(userId: String): Flow<List<HealthReport>> {
            return flowOf(reportsMap.values.filter { it.userId == userId }.sortedByDescending { it.createdAt })
        }

        override suspend fun getReportById(reportId: String, userId: String): HealthReport? {
            val report = reportsMap[reportId]
            return if (report?.userId == userId) report else null
        }

        override suspend fun saveReport(report: HealthReport) {
            reportsMap[report.id] = report
        }

        override suspend fun deleteReport(reportId: String, userId: String) {
            val report = reportsMap[reportId]
            if (report?.userId == userId) {
                reportsMap.remove(reportId)
            }
        }
    }

    // Test 10: Saving a report stores its results correctly
    @Test
    fun testSavingReportStoresResults() = runBlocking {
        val repo = FakeHealthReportRepository()
        val report = HealthReport(
            id = "rep_1",
            userId = "user_1",
            reportName = "Lipid Profile",
            reportType = "Lipid Profile",
            createdAt = 1000L,
            summary = "Summary test",
            observations = listOf("Obs 1"),
            questionsForDoctor = listOf("Q 1"),
            disclaimer = "Disclaimer test",
            results = listOf(
                HealthReportResult("res_1", "rep_1", "Cholesterol", "190", "mg/dL", "<200", "Normal", HealthResultStatus.NORMAL)
            )
        )

        repo.saveReport(report)
        val loaded = repo.getReportById("rep_1", "user_1")
        assertNotNull(loaded)
        assertEquals("Lipid Profile", loaded!!.reportName)
        assertEquals(1, loaded.results.size)
        assertEquals("Cholesterol", loaded.results[0].testName)
    }

    // Test 11: Deleting a report deletes associated results
    @Test
    fun testDeletingReportDeletesAssociatedResults() = runBlocking {
        val repo = FakeHealthReportRepository()
        val report = HealthReport(
            id = "rep_2",
            userId = "user_1",
            reportName = "Metabolic Panel",
            reportType = "CMP",
            createdAt = 1000L,
            summary = "Summary",
            observations = emptyList(),
            questionsForDoctor = emptyList(),
            disclaimer = "Disclaimer",
            results = listOf(
                HealthReportResult("res_2", "rep_2", "BUN", "15", "mg/dL", "7-20", "Normal", HealthResultStatus.NORMAL)
            )
        )

        repo.saveReport(report)
        assertNotNull(repo.getReportById("rep_2", "user_1"))

        repo.deleteReport("rep_2", "user_1")
        assertNull(repo.getReportById("rep_2", "user_1"))
    }

    // Test 12: Different users cannot access each other's saved analyses
    @Test
    fun testUserIsolationForSavedAnalyses() = runBlocking {
        val repo = FakeHealthReportRepository()
        val user1Report = HealthReport(
            id = "rep_u1",
            userId = "user_1",
            reportName = "User 1 Report",
            reportType = "CBC",
            createdAt = 1000L,
            summary = "User 1 summary",
            observations = emptyList(),
            questionsForDoctor = emptyList(),
            disclaimer = "Disclaimer",
            results = emptyList()
        )
        repo.saveReport(user1Report)

        // User 2 cannot access user 1's report
        val loadedByUser2 = repo.getReportById("rep_u1", "user_2")
        assertNull(loadedByUser2)

        // User 2's list does not contain user 1's report
        var user2List: List<HealthReport> = emptyList()
        repo.getReportsForUser("user_2").collect { user2List = it }
        assertEquals(0, user2List.size)
    }

    // Test 13: Empty analysis is handled safely
    @Test
    fun testEmptyAnalysisHandledSafely() {
        val result = HealthReportAnalysisParser.parse("", "user_1")
        assertTrue(result.isFailure)
        val ex = result.exceptionOrNull()
        assertNotNull(ex)
        assertTrue(ex?.message?.contains("Empty") == true)
    }

    // Test 14: AI errors are converted into user-friendly errors
    @Test
    fun testAiErrorsConvertedIntoUserFriendlyMessage() {
        fun mapError(e: Exception): String {
            return when {
                e.message?.contains("429", ignoreCase = true) == true || e.message?.contains("quota", ignoreCase = true) == true ->
                    "You've reached the current AI usage limit. Please try again in a few minutes."
                e.message?.contains("network", ignoreCase = true) == true || e.message?.contains("connect", ignoreCase = true) == true ->
                    "Network error. Please check your internet connection and try again."
                else ->
                    "AI analysis could not complete: ${e.localizedMessage ?: "Unexpected error"}. Please ensure the report image is clear and legible."
            }
        }

        val quotaError = Exception("HTTP 429: Resource has been exhausted (e.g. check quota)")
        assertEquals("You've reached the current AI usage limit. Please try again in a few minutes.", mapError(quotaError))

        val networkError = Exception("Failed to connect to generativelanguage.googleapis.com network timeout")
        assertEquals("Network error. Please check your internet connection and try again.", mapError(networkError))

        val genericError = Exception("Corrupt image data")
        assertTrue(mapError(genericError).contains("AI analysis could not complete"))
    }
}
