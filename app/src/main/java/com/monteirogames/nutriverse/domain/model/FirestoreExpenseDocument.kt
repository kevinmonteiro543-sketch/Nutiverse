package com.monteirogames.nutriverse.domain.model

/**
 * Firestore DTO representing a food expense stored at `users/{FirebaseUID}/expenses/{expenseId}`.
 */
data class FirestoreExpenseDocument(
    val id: String,
    val userId: String,
    val name: String = "",
    val amount: Double = 0.0,
    val category: String = "Groceries",
    val date: Long = System.currentTimeMillis(),
    val notes: String? = null,
    val groceryItemId: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isDeleted: Boolean = false,
    val schemaVersion: Int = CURRENT_SCHEMA_VERSION
) {
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "id" to id,
            "userId" to userId,
            "name" to name,
            "amount" to amount,
            "category" to category,
            "date" to date,
            "notes" to notes,
            "groceryItemId" to groceryItemId,
            "createdAt" to createdAt,
            "updatedAt" to updatedAt,
            "isDeleted" to isDeleted,
            "schemaVersion" to schemaVersion
        )
    }

    companion object {
        const val CURRENT_SCHEMA_VERSION = 1

        fun fromMap(map: Map<String, Any?>, fallbackId: String = "", fallbackUserId: String = ""): FirestoreExpenseDocument {
            val isDeletedVal = when (val del = map["isDeleted"] ?: map["deleted"]) {
                is Boolean -> del
                is String -> del.equals("true", ignoreCase = true)
                is Number -> del.toInt() != 0
                else -> false
            }

            return FirestoreExpenseDocument(
                id = map["id"] as? String ?: fallbackId,
                userId = map["userId"] as? String ?: fallbackUserId,
                name = map["name"] as? String ?: "",
                amount = (map["amount"] as? Number)?.toDouble() ?: 0.0,
                category = map["category"] as? String ?: "Groceries",
                date = (map["date"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                notes = map["notes"] as? String,
                groceryItemId = map["groceryItemId"] as? String,
                createdAt = (map["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                updatedAt = (map["updatedAt"] as? Number)?.toLong() ?: 0L,
                isDeleted = isDeletedVal,
                schemaVersion = (map["schemaVersion"] as? Number)?.toInt() ?: CURRENT_SCHEMA_VERSION
            )
        }
    }
}

fun FoodExpense.toFirestoreDocument(isDeleted: Boolean = false): FirestoreExpenseDocument {
    val validUpdatedAt = if (updatedAt > 0L) updatedAt else createdAt
    return FirestoreExpenseDocument(
        id = id,
        userId = userId,
        name = name,
        amount = amount,
        category = category,
        date = date,
        notes = notes,
        groceryItemId = groceryItemId,
        createdAt = createdAt,
        updatedAt = validUpdatedAt,
        isDeleted = isDeleted,
        schemaVersion = FirestoreExpenseDocument.CURRENT_SCHEMA_VERSION
    )
}

fun FirestoreExpenseDocument.toFoodExpense(): FoodExpense {
    return FoodExpense(
        id = id,
        userId = userId,
        name = name,
        amount = amount,
        category = category,
        date = date,
        notes = notes,
        groceryItemId = groceryItemId,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}
