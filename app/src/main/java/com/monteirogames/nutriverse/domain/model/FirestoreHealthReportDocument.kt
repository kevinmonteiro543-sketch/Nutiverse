package com.monteirogames.nutriverse.domain.model

data class FirestoreHealthReportResult(
    val id: String = "",
    val reportId: String = "",
    val testName: String = "",
    val value: String? = null,
    val unit: String? = null,
    val referenceRange: String? = null,
    val interpretation: String? = null,
    val status: String = "UNKNOWN",
    val userNote: String? = null
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "id" to id,
        "reportId" to reportId,
        "testName" to testName,
        "value" to value,
        "unit" to unit,
        "referenceRange" to referenceRange,
        "interpretation" to interpretation,
        "status" to status,
        "userNote" to userNote
    )

    companion object {
        fun fromMap(map: Map<String, Any?>): FirestoreHealthReportResult {
            return FirestoreHealthReportResult(
                id = map["id"] as? String ?: "",
                reportId = map["reportId"] as? String ?: "",
                testName = map["testName"] as? String ?: "Diagnostic Test",
                value = map["value"] as? String,
                unit = map["unit"] as? String,
                referenceRange = map["referenceRange"] as? String,
                interpretation = map["interpretation"] as? String,
                status = when ((map["status"] as? String)?.trim()?.uppercase()) {
                    "LOW" -> "LOW"
                    "NORMAL" -> "NORMAL"
                    "HIGH" -> "HIGH"
                    else -> "UNKNOWN"
                },
                userNote = map["userNote"] as? String
            )
        }
    }
}

/**
 * Firestore DTO representing a Health Report document stored at `users/{FirebaseUID}/healthReports/{healthReportId}`.
 * Stores the structured report summary and nested test results in a single aggregate document.
 * Note: Raw device-local images are strictly excluded from cloud synchronization for user privacy.
 */
data class FirestoreHealthReportDocument(
    val id: String,
    val userId: String = "",
    val reportName: String = "",
    val reportType: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val summary: String = "",
    val observations: List<String> = emptyList(),
    val questionsForDoctor: List<String> = emptyList(),
    val disclaimer: String = "",
    val results: List<FirestoreHealthReportResult> = emptyList(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isDeleted: Boolean = false,
    val schemaVersion: Int = CURRENT_SCHEMA_VERSION
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "id" to id,
        "userId" to userId,
        "reportName" to reportName,
        "reportType" to reportType,
        "createdAt" to createdAt,
        "summary" to summary,
        "observations" to observations,
        "questionsForDoctor" to questionsForDoctor,
        "disclaimer" to disclaimer,
        "results" to results.map { it.toMap() },
        "updatedAt" to updatedAt,
        "isDeleted" to isDeleted,
        "schemaVersion" to schemaVersion
    )

    fun toHealthReport(): HealthReport {
        return HealthReport(
            id = id,
            userId = userId,
            reportName = reportName.ifBlank { "Health Report" },
            reportType = reportType.ifBlank { "General Report" },
            createdAt = createdAt,
            summary = summary,
            observations = observations,
            questionsForDoctor = questionsForDoctor,
            disclaimer = disclaimer,
            results = results.map { res ->
                HealthReportResult(
                    id = res.id.ifBlank { java.util.UUID.randomUUID().toString() },
                    reportId = id,
                    testName = res.testName,
                    value = res.value,
                    unit = res.unit,
                    referenceRange = res.referenceRange,
                    interpretation = res.interpretation,
                    status = HealthResultStatus.fromString(res.status),
                    userNote = res.userNote
                )
            },
            updatedAt = updatedAt
        )
    }

    companion object {
        const val CURRENT_SCHEMA_VERSION = 1

        @Suppress("UNCHECKED_CAST")
        fun fromMap(
            map: Map<String, Any?>,
            fallbackId: String = "",
            fallbackUserId: String = ""
        ): FirestoreHealthReportDocument {
            val resultsList = mutableListOf<FirestoreHealthReportResult>()
            val rawResults = map["results"]
            if (rawResults is List<*>) {
                for (item in rawResults) {
                    if (item is Map<*, *>) {
                        resultsList.add(FirestoreHealthReportResult.fromMap(item as Map<String, Any?>))
                    }
                }
            }

            val obsList = when (val obs = map["observations"]) {
                is List<*> -> obs.filterIsInstance<String>()
                is String -> listOf(obs)
                else -> emptyList()
            }

            val qList = when (val q = map["questionsForDoctor"]) {
                is List<*> -> q.filterIsInstance<String>()
                is String -> listOf(q)
                else -> emptyList()
            }

            val docId = map["id"] as? String ?: fallbackId
            val uid = map["userId"] as? String ?: fallbackUserId
            val reportName = map["reportName"] as? String ?: "Health Report"
            val reportType = map["reportType"] as? String ?: "General Report"
            val summary = map["summary"] as? String ?: ""
            val disclaimer = map["disclaimer"] as? String ?: ""

            val createdAt = (map["createdAt"] as? Number)?.toLong()
                ?: (map["createdAt"] as? String)?.toLongOrNull()
                ?: System.currentTimeMillis()
            val updatedAt = (map["updatedAt"] as? Number)?.toLong()
                ?: (map["updatedAt"] as? String)?.toLongOrNull()
                ?: createdAt

            val isDeleted = when (val del = map["isDeleted"]) {
                is Boolean -> del
                is String -> del.toBooleanStrictOrNull() ?: false
                else -> false
            }

            val schemaVer = (map["schemaVersion"] as? Number)?.toInt() ?: CURRENT_SCHEMA_VERSION

            return FirestoreHealthReportDocument(
                id = docId,
                userId = uid,
                reportName = reportName,
                reportType = reportType,
                createdAt = createdAt,
                summary = summary,
                observations = obsList,
                questionsForDoctor = qList,
                disclaimer = disclaimer,
                results = resultsList,
                updatedAt = updatedAt,
                isDeleted = isDeleted,
                schemaVersion = schemaVer
            )
        }
    }
}

fun HealthReport.toFirestoreDocument(
    userId: String = this.userId,
    isDeleted: Boolean = false
): FirestoreHealthReportDocument {
    val resultDtos = results.map { res ->
        FirestoreHealthReportResult(
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

    return FirestoreHealthReportDocument(
        id = id,
        userId = userId,
        reportName = reportName,
        reportType = reportType,
        createdAt = createdAt,
        summary = summary,
        observations = observations,
        questionsForDoctor = questionsForDoctor,
        disclaimer = disclaimer,
        results = resultDtos,
        updatedAt = if (updatedAt > 0L) updatedAt else createdAt,
        isDeleted = isDeleted,
        schemaVersion = FirestoreHealthReportDocument.CURRENT_SCHEMA_VERSION
    )
}
