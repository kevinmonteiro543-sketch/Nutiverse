package com.monteirogames.nutriverse.domain.model

import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

object HealthReportAnalysisParser {

    const val DEFAULT_DISCLAIMER =
        "Informational only. This analysis is not a medical diagnosis or medical advice. Discuss your report and any concerns with a qualified healthcare professional."

    fun parse(
        jsonString: String,
        userId: String,
        reportId: String = UUID.randomUUID().toString(),
        createdAt: Long = System.currentTimeMillis()
    ): Result<HealthReport> {
        val trimmed = jsonString.trim()
        if (trimmed.isEmpty()) {
            return Result.failure(IllegalArgumentException("Empty AI analysis response"))
        }

        // Clean any accidental markdown code fences
        val cleanJson = if (trimmed.startsWith("```")) {
            trimmed.substringAfter("\n").substringBeforeLast("```").trim()
        } else {
            trimmed
        }

        return try {
            val root = JSONObject(cleanJson)

            val reportType = root.optString("reportType", "Laboratory Health Report")
                .ifBlank { "Laboratory Health Report" }
            val summary = root.optString("summary", "Analysis completed.")
                .ifBlank { "Analysis completed." }

            val resultsList = mutableListOf<HealthReportResult>()
            val resultsArray = root.optJSONArray("results")
            if (resultsArray != null) {
                for (i in 0 until resultsArray.length()) {
                    val item = resultsArray.optJSONObject(i) ?: continue
                    val testName = item.optString("testName", "Unspecified Test")
                        .ifBlank { "Unspecified Test" }

                    val value = if (item.has("value") && !item.isNull("value")) {
                        item.optString("value").trim().ifBlank { null }
                    } else null

                    val unit = if (item.has("unit") && !item.isNull("unit")) {
                        item.optString("unit").trim().ifBlank { null }
                    } else null

                    val referenceRange = if (item.has("referenceRange") && !item.isNull("referenceRange")) {
                        item.optString("referenceRange").trim().ifBlank { null }
                    } else null

                    val interpretation = if (item.has("interpretation") && !item.isNull("interpretation")) {
                        item.optString("interpretation").trim().ifBlank { null }
                    } else null

                    val statusString = item.optString("status", "UNKNOWN")
                    val status = HealthResultStatus.fromString(statusString)

                    resultsList.add(
                        HealthReportResult(
                            id = UUID.randomUUID().toString(),
                            reportId = reportId,
                            testName = testName,
                            value = value,
                            unit = unit,
                            referenceRange = referenceRange,
                            interpretation = interpretation,
                            status = status,
                            userNote = null
                        )
                    )
                }
            }

            val observationsList = parseStringList(root.optJSONArray("observations"))
            val questionsList = parseStringList(root.optJSONArray("questionsForDoctor"))

            val disclaimer = root.optString("disclaimer", DEFAULT_DISCLAIMER)
                .ifBlank { DEFAULT_DISCLAIMER }

            val report = HealthReport(
                id = reportId,
                userId = userId,
                reportName = "$reportType (${resultsList.size} tests)",
                reportType = reportType,
                createdAt = createdAt,
                summary = summary,
                observations = observationsList,
                questionsForDoctor = questionsList,
                disclaimer = disclaimer,
                results = resultsList
            )

            Result.success(report)
        } catch (e: Exception) {
            Result.failure(IllegalArgumentException("Malformed or unparseable AI analysis response: ${e.message}", e))
        }
    }

    private fun parseStringList(array: JSONArray?): List<String> {
        if (array == null) return emptyList()
        val list = mutableListOf<String>()
        for (i in 0 until array.length()) {
            val s = array.optString(i)?.trim()
            if (!s.isNullOrBlank()) {
                list.add(s)
            }
        }
        return list
    }
}
