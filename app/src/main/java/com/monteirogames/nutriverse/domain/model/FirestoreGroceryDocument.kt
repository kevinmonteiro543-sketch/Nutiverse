package com.monteirogames.nutriverse.domain.model

/**
 * Firestore DTO representing a grocery item stored at `users/{FirebaseUID}/grocery/{groceryItemId}`.
 */
data class FirestoreGroceryDocument(
    val id: String,
    val userId: String,
    val name: String = "",
    val quantity: Float = 1f,
    val unit: String = "pcs",
    val category: String = "Other",
    val isPurchased: Boolean = false,
    val source: String = "MANUAL",
    val sourceRecipeId: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val purchasedAt: Long? = null,
    val updatedAt: Long = System.currentTimeMillis(),
    val isDeleted: Boolean = false,
    val schemaVersion: Int = CURRENT_SCHEMA_VERSION
) {
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "id" to id,
            "userId" to userId,
            "name" to name,
            "quantity" to quantity.toDouble(),
            "unit" to unit,
            "category" to category,
            "isPurchased" to isPurchased,
            "source" to source,
            "sourceRecipeId" to sourceRecipeId,
            "createdAt" to createdAt,
            "purchasedAt" to purchasedAt,
            "updatedAt" to updatedAt,
            "isDeleted" to isDeleted,
            "schemaVersion" to schemaVersion
        )
    }

    companion object {
        const val CURRENT_SCHEMA_VERSION = 1

        fun fromMap(map: Map<String, Any?>, fallbackId: String = "", fallbackUserId: String = ""): FirestoreGroceryDocument {
            val isPurchasedVal = when (val p = map["isPurchased"]) {
                is Boolean -> p
                is String -> p.equals("true", ignoreCase = true)
                is Number -> p.toInt() != 0
                else -> false
            }

            val isDeletedVal = when (val del = map["isDeleted"] ?: map["deleted"]) {
                is Boolean -> del
                is String -> del.equals("true", ignoreCase = true)
                is Number -> del.toInt() != 0
                else -> false
            }

            return FirestoreGroceryDocument(
                id = map["id"] as? String ?: fallbackId,
                userId = map["userId"] as? String ?: fallbackUserId,
                name = map["name"] as? String ?: "",
                quantity = (map["quantity"] as? Number)?.toFloat() ?: 1f,
                unit = map["unit"] as? String ?: "pcs",
                category = map["category"] as? String ?: "Other",
                isPurchased = isPurchasedVal,
                source = map["source"] as? String ?: "MANUAL",
                sourceRecipeId = map["sourceRecipeId"] as? String,
                createdAt = (map["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                purchasedAt = (map["purchasedAt"] as? Number)?.toLong(),
                updatedAt = (map["updatedAt"] as? Number)?.toLong() ?: 0L,
                isDeleted = isDeletedVal,
                schemaVersion = (map["schemaVersion"] as? Number)?.toInt() ?: CURRENT_SCHEMA_VERSION
            )
        }
    }
}

fun GroceryItem.toFirestoreDocument(isDeleted: Boolean = false): FirestoreGroceryDocument {
    val validUpdatedAt = if (updatedAt > 0L) updatedAt else createdAt
    return FirestoreGroceryDocument(
        id = id,
        userId = userId,
        name = name,
        quantity = quantity,
        unit = unit,
        category = category,
        isPurchased = isPurchased,
        source = source,
        sourceRecipeId = sourceRecipeId,
        createdAt = createdAt,
        purchasedAt = purchasedAt,
        updatedAt = validUpdatedAt,
        isDeleted = isDeleted,
        schemaVersion = FirestoreGroceryDocument.CURRENT_SCHEMA_VERSION
    )
}

fun FirestoreGroceryDocument.toGroceryItem(): GroceryItem {
    return GroceryItem(
        id = id,
        userId = userId,
        name = name,
        quantity = quantity,
        unit = unit,
        category = category,
        isPurchased = isPurchased,
        source = source,
        sourceRecipeId = sourceRecipeId,
        createdAt = createdAt,
        purchasedAt = purchasedAt,
        updatedAt = updatedAt
    )
}
