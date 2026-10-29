package com.monteirogames.nutriverse.domain.model

data class FirestoreDietPlanMeal(
    val id: String = "",
    val dayNumber: Int = 1,
    val mealType: String = "",
    val name: String = "",
    val calories: Float = 0f,
    val protein: Float = 0f,
    val carbs: Float = 0f,
    val fat: Float = 0f,
    val prepTip: String = ""
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "id" to id,
        "dayNumber" to dayNumber,
        "mealType" to mealType,
        "name" to name,
        "calories" to calories.toDouble(),
        "protein" to protein.toDouble(),
        "carbs" to carbs.toDouble(),
        "fat" to fat.toDouble(),
        "prepTip" to prepTip
    )

    companion object {
        fun fromMap(map: Map<String, Any?>): FirestoreDietPlanMeal {
            return FirestoreDietPlanMeal(
                id = map["id"] as? String ?: "",
                dayNumber = (map["dayNumber"] as? Number)?.toInt() ?: 1,
                mealType = map["mealType"] as? String ?: "Meal",
                name = map["name"] as? String ?: "Nutritious Meal",
                calories = (map["calories"] as? Number)?.toFloat() ?: 0f,
                protein = (map["protein"] as? Number)?.toFloat() ?: 0f,
                carbs = (map["carbs"] as? Number)?.toFloat() ?: 0f,
                fat = (map["fat"] as? Number)?.toFloat() ?: 0f,
                prepTip = map["prepTip"] as? String ?: ""
            )
        }
    }
}

/**
 * Firestore DTO representing a Diet Plan document stored at `users/{FirebaseUID}/dietPlans/{dietPlanId}`.
 * Stores the plan metadata along with nested meals as a single aggregate document.
 */
data class FirestoreDietPlanDocument(
    val id: String,
    val userId: String = "",
    val name: String = "",
    val targetCalories: Float = 0f,
    val targetProtein: Float = 0f,
    val targetCarbs: Float = 0f,
    val targetFat: Float = 0f,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isActive: Boolean = true,
    val isDeleted: Boolean = false,
    val schemaVersion: Int = CURRENT_SCHEMA_VERSION,
    val meals: List<FirestoreDietPlanMeal> = emptyList()
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "id" to id,
        "userId" to userId,
        "name" to name,
        "targetCalories" to targetCalories.toDouble(),
        "targetProtein" to targetProtein.toDouble(),
        "targetCarbs" to targetCarbs.toDouble(),
        "targetFat" to targetFat.toDouble(),
        "createdAt" to createdAt,
        "updatedAt" to updatedAt,
        "isActive" to isActive,
        "isDeleted" to isDeleted,
        "schemaVersion" to schemaVersion,
        "meals" to meals.map { it.toMap() }
    )

    fun toDietPlan(): DietPlan {
        val daysList = meals
            .groupBy { it.dayNumber }
            .toSortedMap()
            .map { (dayNum, mealList) ->
                DayPlan(
                    dayNumber = dayNum,
                    meals = mealList.map { m ->
                        DietPlanMeal(
                            id = m.id.ifBlank { java.util.UUID.randomUUID().toString() },
                            dayNumber = m.dayNumber,
                            mealType = m.mealType,
                            name = m.name,
                            calories = m.calories,
                            protein = m.protein,
                            carbs = m.carbs,
                            fat = m.fat,
                            prepTip = m.prepTip
                        )
                    }
                )
            }

        return DietPlan(
            id = id,
            userId = userId,
            name = name.ifBlank { "7-Day AI Meal Plan" },
            targetCalories = targetCalories,
            targetProtein = targetProtein,
            targetCarbs = targetCarbs,
            targetFat = targetFat,
            createdAt = createdAt,
            days = daysList,
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
        ): FirestoreDietPlanDocument {
            val mealsList = mutableListOf<FirestoreDietPlanMeal>()
            val rawMeals = map["meals"]
            if (rawMeals is List<*>) {
                for (item in rawMeals) {
                    if (item is Map<*, *>) {
                        mealsList.add(FirestoreDietPlanMeal.fromMap(item as Map<String, Any?>))
                    }
                }
            }

            val docId = map["id"] as? String ?: fallbackId
            val uid = map["userId"] as? String ?: fallbackUserId
            val name = map["name"] as? String ?: "7-Day AI Meal Plan"

            val targetCalories = (map["targetCalories"] as? Number)?.toFloat()
                ?: (map["targetCalories"] as? String)?.toFloatOrNull()
                ?: 0f
            val targetProtein = (map["targetProtein"] as? Number)?.toFloat()
                ?: (map["targetProtein"] as? String)?.toFloatOrNull()
                ?: 0f
            val targetCarbs = (map["targetCarbs"] as? Number)?.toFloat()
                ?: (map["targetCarbs"] as? String)?.toFloatOrNull()
                ?: 0f
            val targetFat = (map["targetFat"] as? Number)?.toFloat()
                ?: (map["targetFat"] as? String)?.toFloatOrNull()
                ?: 0f

            val createdAt = (map["createdAt"] as? Number)?.toLong()
                ?: (map["createdAt"] as? String)?.toLongOrNull()
                ?: System.currentTimeMillis()
            val updatedAt = (map["updatedAt"] as? Number)?.toLong()
                ?: (map["updatedAt"] as? String)?.toLongOrNull()
                ?: createdAt

            val isActive = when (val active = map["isActive"]) {
                is Boolean -> active
                is String -> active.toBooleanStrictOrNull() ?: true
                else -> true
            }

            val isDeleted = when (val del = map["isDeleted"]) {
                is Boolean -> del
                is String -> del.toBooleanStrictOrNull() ?: false
                else -> false
            }

            val schemaVer = (map["schemaVersion"] as? Number)?.toInt() ?: CURRENT_SCHEMA_VERSION

            return FirestoreDietPlanDocument(
                id = docId,
                userId = uid,
                name = name,
                targetCalories = targetCalories,
                targetProtein = targetProtein,
                targetCarbs = targetCarbs,
                targetFat = targetFat,
                createdAt = createdAt,
                updatedAt = updatedAt,
                isActive = isActive,
                isDeleted = isDeleted,
                schemaVersion = schemaVer,
                meals = mealsList
            )
        }
    }
}

fun DietPlan.toFirestoreDocument(
    userId: String = this.userId,
    isDeleted: Boolean = false
): FirestoreDietPlanDocument {
    val flattenedMeals = days.flatMap { day ->
        day.meals.map { meal ->
            FirestoreDietPlanMeal(
                id = meal.id,
                dayNumber = day.dayNumber,
                mealType = meal.mealType,
                name = meal.name,
                calories = meal.calories,
                protein = meal.protein,
                carbs = meal.carbs,
                fat = meal.fat,
                prepTip = meal.prepTip
            )
        }
    }

    return FirestoreDietPlanDocument(
        id = id,
        userId = userId,
        name = name,
        targetCalories = targetCalories,
        targetProtein = targetProtein,
        targetCarbs = targetCarbs,
        targetFat = targetFat,
        createdAt = createdAt,
        updatedAt = if (updatedAt > 0L) updatedAt else createdAt,
        isActive = true,
        isDeleted = isDeleted,
        schemaVersion = FirestoreDietPlanDocument.CURRENT_SCHEMA_VERSION,
        meals = flattenedMeals
    )
}
