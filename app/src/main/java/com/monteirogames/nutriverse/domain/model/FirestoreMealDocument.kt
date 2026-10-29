package com.monteirogames.nutriverse.domain.model

/**
 * Firestore DTO representing a meal entry stored at `users/{FirebaseUID}/meals/{mealId}`.
 * Maps cleanly between the local Room MealEntry and Firestore without exposing internal Room details.
 */
data class FirestoreMealDocument(
    val id: String,
    val userId: String,
    val date: Long = System.currentTimeMillis(),
    val mealType: String = "Breakfast",
    val quantity: Float = 100f,
    val loggedAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isDeleted: Boolean = false,
    val schemaVersion: Int = CURRENT_SCHEMA_VERSION,
    // Embedded FoodItem data required to fully reconstruct MealEntry locally
    val foodItemId: String = "",
    val foodName: String = "",
    val calories: Float = 0f,
    val protein: Float = 0f,
    val carbs: Float = 0f,
    val fat: Float = 0f,
    val servingSize: Float = 100f,
    val servingUnit: String = "g",
    val barcode: String? = null
) {
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "id" to id,
            "userId" to userId,
            "date" to date,
            "mealType" to mealType,
            "quantity" to quantity.toDouble(),
            "loggedAt" to loggedAt,
            "updatedAt" to updatedAt,
            "isDeleted" to isDeleted,
            "schemaVersion" to schemaVersion,
            // Top-level food item attributes
            "foodItemId" to foodItemId,
            "foodName" to foodName,
            "calories" to calories.toDouble(),
            "protein" to protein.toDouble(),
            "carbs" to carbs.toDouble(),
            "fat" to fat.toDouble(),
            "servingSize" to servingSize.toDouble(),
            "servingUnit" to servingUnit,
            "barcode" to barcode,
            // Nested object format for flexible forward/backward schema compatibility
            "foodItem" to mapOf(
                "id" to foodItemId,
                "name" to foodName,
                "calories" to calories.toDouble(),
                "protein" to protein.toDouble(),
                "carbs" to carbs.toDouble(),
                "fat" to fat.toDouble(),
                "servingSize" to servingSize.toDouble(),
                "servingUnit" to servingUnit,
                "barcode" to barcode
            )
        )
    }

    companion object {
        const val CURRENT_SCHEMA_VERSION = 1

        @Suppress("UNCHECKED_CAST")
        fun fromMap(map: Map<String, Any?>, fallbackId: String = "", fallbackUserId: String = ""): FirestoreMealDocument {
            val nestedFood = map["foodItem"] as? Map<String, Any?>

            val foodId = (nestedFood?.get("id") as? String)
                ?: (map["foodItemId"] as? String)
                ?: fallbackId

            val name = (nestedFood?.get("name") as? String)
                ?: (map["foodName"] as? String)
                ?: (map["name"] as? String)
                ?: "Food Item"

            val cal = (nestedFood?.get("calories") as? Number)?.toFloat()
                ?: (map["calories"] as? Number)?.toFloat()
                ?: 0f

            val prot = (nestedFood?.get("protein") as? Number)?.toFloat()
                ?: (map["protein"] as? Number)?.toFloat()
                ?: 0f

            val carbsVal = (nestedFood?.get("carbs") as? Number)?.toFloat()
                ?: (map["carbs"] as? Number)?.toFloat()
                ?: 0f

            val fatVal = (nestedFood?.get("fat") as? Number)?.toFloat()
                ?: (map["fat"] as? Number)?.toFloat()
                ?: 0f

            val servSize = (nestedFood?.get("servingSize") as? Number)?.toFloat()
                ?: (map["servingSize"] as? Number)?.toFloat()
                ?: 100f

            val servUnit = (nestedFood?.get("servingUnit") as? String)
                ?: (map["servingUnit"] as? String)
                ?: "g"

            val barcodeVal = (nestedFood?.get("barcode") as? String)
                ?: (map["barcode"] as? String)

            val isDeletedVal = when (val del = map["isDeleted"] ?: map["deleted"]) {
                is Boolean -> del
                is String -> del.equals("true", ignoreCase = true)
                is Number -> del.toInt() != 0
                else -> false
            }

            return FirestoreMealDocument(
                id = map["id"] as? String ?: fallbackId,
                userId = map["userId"] as? String ?: fallbackUserId,
                date = (map["date"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                mealType = map["mealType"] as? String ?: "Breakfast",
                quantity = (map["quantity"] as? Number)?.toFloat() ?: 100f,
                loggedAt = (map["loggedAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                updatedAt = (map["updatedAt"] as? Number)?.toLong() ?: 0L,
                isDeleted = isDeletedVal,
                schemaVersion = (map["schemaVersion"] as? Number)?.toInt() ?: CURRENT_SCHEMA_VERSION,
                foodItemId = foodId,
                foodName = name,
                calories = cal,
                protein = prot,
                carbs = carbsVal,
                fat = fatVal,
                servingSize = servSize,
                servingUnit = servUnit,
                barcode = barcodeVal
            )
        }
    }
}

fun MealEntry.toFirestoreDocument(isDeleted: Boolean = false): FirestoreMealDocument {
    val validUpdatedAt = if (updatedAt > 0L) updatedAt else loggedAt
    return FirestoreMealDocument(
        id = id,
        userId = userId,
        date = date,
        mealType = mealType,
        quantity = quantity,
        loggedAt = loggedAt,
        updatedAt = validUpdatedAt,
        isDeleted = isDeleted,
        schemaVersion = FirestoreMealDocument.CURRENT_SCHEMA_VERSION,
        foodItemId = foodItem.id,
        foodName = foodItem.name,
        calories = foodItem.calories,
        protein = foodItem.protein,
        carbs = foodItem.carbs,
        fat = foodItem.fat,
        servingSize = foodItem.servingSize,
        servingUnit = foodItem.servingUnit,
        barcode = foodItem.barcode
    )
}

fun FirestoreMealDocument.toMealEntry(): MealEntry {
    return MealEntry(
        id = id,
        userId = userId,
        foodItem = FoodItem(
            id = foodItemId.ifBlank { id },
            name = foodName.ifBlank { "Meal Item" },
            calories = calories,
            protein = protein,
            carbs = carbs,
            fat = fat,
            servingSize = servingSize,
            servingUnit = servingUnit,
            barcode = barcode
        ),
        date = date,
        mealType = mealType,
        quantity = quantity,
        loggedAt = loggedAt,
        updatedAt = updatedAt
    )
}

fun FirestoreMealDocument.toMeal(): Meal = toMealEntry()
