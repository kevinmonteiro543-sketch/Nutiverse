package com.monteirogames.nutriverse.domain.model

/**
 * Firestore DTO representing budget configuration stored at `users/{FirebaseUID}/budget/{budgetId}`.
 */
data class FirestoreBudgetDocument(
    val id: String,
    val userId: String,
    val amount: Double = 0.0,
    val period: String = "MONTHLY",
    val startDate: Long = 0L,
    val endDate: Long = 0L,
    val createdAt: Long = System.currentTimeMillis(),
    val isActive: Boolean = true,
    val updatedAt: Long = System.currentTimeMillis(),
    val isDeleted: Boolean = false,
    val schemaVersion: Int = CURRENT_SCHEMA_VERSION
) {
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "id" to id,
            "userId" to userId,
            "amount" to amount,
            "period" to period,
            "startDate" to startDate,
            "endDate" to endDate,
            "createdAt" to createdAt,
            "isActive" to isActive,
            "updatedAt" to updatedAt,
            "isDeleted" to isDeleted,
            "schemaVersion" to schemaVersion
        )
    }

    companion object {
        const val CURRENT_SCHEMA_VERSION = 1

        fun fromMap(map: Map<String, Any?>, fallbackId: String = "", fallbackUserId: String = ""): FirestoreBudgetDocument {
            val isActiveVal = when (val a = map["isActive"]) {
                is Boolean -> a
                is String -> a.equals("true", ignoreCase = true)
                is Number -> a.toInt() != 0
                else -> true
            }

            val isDeletedVal = when (val del = map["isDeleted"] ?: map["deleted"]) {
                is Boolean -> del
                is String -> del.equals("true", ignoreCase = true)
                is Number -> del.toInt() != 0
                else -> false
            }

            return FirestoreBudgetDocument(
                id = map["id"] as? String ?: fallbackId,
                userId = map["userId"] as? String ?: fallbackUserId,
                amount = (map["amount"] as? Number)?.toDouble() ?: 0.0,
                period = map["period"] as? String ?: "MONTHLY",
                startDate = (map["startDate"] as? Number)?.toLong() ?: 0L,
                endDate = (map["endDate"] as? Number)?.toLong() ?: 0L,
                createdAt = (map["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                isActive = isActiveVal,
                updatedAt = (map["updatedAt"] as? Number)?.toLong() ?: 0L,
                isDeleted = isDeletedVal,
                schemaVersion = (map["schemaVersion"] as? Number)?.toInt() ?: CURRENT_SCHEMA_VERSION
            )
        }
    }
}

fun Budget.toFirestoreDocument(isDeleted: Boolean = false): FirestoreBudgetDocument {
    val validUpdatedAt = if (updatedAt > 0L) updatedAt else createdAt
    return FirestoreBudgetDocument(
        id = id,
        userId = userId,
        amount = amount,
        period = period,
        startDate = startDate,
        endDate = endDate,
        createdAt = createdAt,
        isActive = isActive,
        updatedAt = validUpdatedAt,
        isDeleted = isDeleted,
        schemaVersion = FirestoreBudgetDocument.CURRENT_SCHEMA_VERSION
    )
}

fun FirestoreBudgetDocument.toBudget(): Budget {
    return Budget(
        id = id,
        userId = userId,
        amount = amount,
        period = period,
        startDate = startDate,
        endDate = endDate,
        createdAt = createdAt,
        isActive = isActive,
        updatedAt = updatedAt
    )
}
