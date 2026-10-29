package com.monteirogames.nutriverse.domain.model

data class FirestoreRecipeIngredient(
    val id: String = "",
    val name: String = "",
    val quantity: Float = 0f,
    val unit: String = ""
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "id" to id,
        "name" to name,
        "quantity" to quantity.toDouble(),
        "unit" to unit
    )

    companion object {
        fun fromMap(map: Map<String, Any?>): FirestoreRecipeIngredient {
            return FirestoreRecipeIngredient(
                id = map["id"] as? String ?: "",
                name = map["name"] as? String ?: "",
                quantity = (map["quantity"] as? Number)?.toFloat() ?: 0f,
                unit = map["unit"] as? String ?: ""
            )
        }
    }
}

data class FirestoreRecipeInstruction(
    val id: String = "",
    val stepNumber: Int = 1,
    val instructionText: String = ""
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "id" to id,
        "stepNumber" to stepNumber,
        "instructionText" to instructionText
    )

    companion object {
        fun fromMap(map: Map<String, Any?>): FirestoreRecipeInstruction {
            return FirestoreRecipeInstruction(
                id = map["id"] as? String ?: "",
                stepNumber = (map["stepNumber"] as? Number)?.toInt() ?: 1,
                instructionText = map["instructionText"] as? String ?: ""
            )
        }
    }
}

/**
 * Firestore DTO representing a recipe stored at `users/{FirebaseUID}/recipes/{recipeId}`.
 * Follows the single document design with nested ingredients and instructions.
 */
data class FirestoreRecipeDocument(
    val id: String,
    val userId: String = "",
    val name: String = "",
    val description: String = "",
    val imageUrl: String = "",
    val prepTimeMinutes: Int = 0,
    val cookTimeMinutes: Int = 0,
    val servings: Int = 1,
    val calories: Float = 0f,
    val protein: Float = 0f,
    val carbs: Float = 0f,
    val fat: Float = 0f,
    val difficulty: String = "Easy",
    val dietaryTags: List<String> = emptyList(),
    val allergyTags: List<String> = emptyList(),
    val ingredients: List<FirestoreRecipeIngredient> = emptyList(),
    val instructions: List<FirestoreRecipeInstruction> = emptyList(),
    val isFavorite: Boolean = false,
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
            "description" to description,
            "imageUrl" to imageUrl,
            "prepTimeMinutes" to prepTimeMinutes,
            "cookTimeMinutes" to cookTimeMinutes,
            "servings" to servings,
            "calories" to calories.toDouble(),
            "protein" to protein.toDouble(),
            "carbs" to carbs.toDouble(),
            "fat" to fat.toDouble(),
            "difficulty" to difficulty,
            "dietaryTags" to dietaryTags,
            "allergyTags" to allergyTags,
            "ingredients" to ingredients.map { it.toMap() },
            "instructions" to instructions.map { it.toMap() },
            "isFavorite" to isFavorite,
            "createdAt" to createdAt,
            "updatedAt" to updatedAt,
            "isDeleted" to isDeleted,
            "schemaVersion" to schemaVersion
        )
    }

    companion object {
        const val CURRENT_SCHEMA_VERSION = 1

        @Suppress("UNCHECKED_CAST")
        fun fromMap(map: Map<String, Any?>, fallbackId: String = "", fallbackUserId: String = ""): FirestoreRecipeDocument {
            val dietary = when (val dt = map["dietaryTags"]) {
                is List<*> -> dt.filterIsInstance<String>()
                is String -> dt.split(",").map { it.trim() }.filter { it.isNotBlank() }
                else -> emptyList()
            }

            val allergy = when (val at = map["allergyTags"]) {
                is List<*> -> at.filterIsInstance<String>()
                is String -> at.split(",").map { it.trim() }.filter { it.isNotBlank() }
                else -> emptyList()
            }

            val rawIngs = map["ingredients"] as? List<*>
            val ings = rawIngs?.mapNotNull { item ->
                (item as? Map<String, Any?>)?.let { FirestoreRecipeIngredient.fromMap(it) }
            } ?: emptyList()

            val rawInst = map["instructions"] as? List<*>
            val insts = rawInst?.mapNotNull { item ->
                (item as? Map<String, Any?>)?.let { FirestoreRecipeInstruction.fromMap(it) }
            } ?: emptyList()

            val isDeletedVal = when (val del = map["isDeleted"] ?: map["deleted"]) {
                is Boolean -> del
                is String -> del.equals("true", ignoreCase = true)
                is Number -> del.toInt() != 0
                else -> false
            }

            return FirestoreRecipeDocument(
                id = map["id"] as? String ?: fallbackId,
                userId = map["userId"] as? String ?: fallbackUserId,
                name = map["name"] as? String ?: "",
                description = map["description"] as? String ?: "",
                imageUrl = map["imageUrl"] as? String ?: "",
                prepTimeMinutes = (map["prepTimeMinutes"] as? Number)?.toInt() ?: 0,
                cookTimeMinutes = (map["cookTimeMinutes"] as? Number)?.toInt() ?: 0,
                servings = (map["servings"] as? Number)?.toInt() ?: 1,
                calories = (map["calories"] as? Number)?.toFloat() ?: 0f,
                protein = (map["protein"] as? Number)?.toFloat() ?: 0f,
                carbs = (map["carbs"] as? Number)?.toFloat() ?: 0f,
                fat = (map["fat"] as? Number)?.toFloat() ?: 0f,
                difficulty = map["difficulty"] as? String ?: "Easy",
                dietaryTags = dietary,
                allergyTags = allergy,
                ingredients = ings,
                instructions = insts,
                isFavorite = map["isFavorite"] as? Boolean ?: false,
                createdAt = (map["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                updatedAt = (map["updatedAt"] as? Number)?.toLong() ?: 0L,
                isDeleted = isDeletedVal,
                schemaVersion = (map["schemaVersion"] as? Number)?.toInt() ?: CURRENT_SCHEMA_VERSION
            )
        }
    }
}

fun Recipe.toFirestoreDocument(userId: String = "", isDeleted: Boolean = false): FirestoreRecipeDocument {
    val validUpdatedAt = if (updatedAt > 0L) updatedAt else createdAt
    return FirestoreRecipeDocument(
        id = id,
        userId = userId,
        name = name,
        description = description,
        imageUrl = imageUrl,
        prepTimeMinutes = prepTimeMinutes,
        cookTimeMinutes = cookTimeMinutes,
        servings = servings,
        calories = calories,
        protein = protein,
        carbs = carbs,
        fat = fat,
        difficulty = difficulty,
        dietaryTags = dietaryTags,
        allergyTags = allergyTags,
        ingredients = ingredients.map {
            FirestoreRecipeIngredient(
                id = it.id,
                name = it.name,
                quantity = it.quantity,
                unit = it.unit
            )
        },
        instructions = instructions.map {
            FirestoreRecipeInstruction(
                id = it.id,
                stepNumber = it.stepNumber,
                instructionText = it.instructionText
            )
        },
        isFavorite = isFavorite,
        createdAt = createdAt,
        updatedAt = validUpdatedAt,
        isDeleted = isDeleted,
        schemaVersion = FirestoreRecipeDocument.CURRENT_SCHEMA_VERSION
    )
}

fun FirestoreRecipeDocument.toRecipe(): Recipe {
    return Recipe(
        id = id,
        name = name,
        description = description,
        imageUrl = imageUrl,
        prepTimeMinutes = prepTimeMinutes,
        cookTimeMinutes = cookTimeMinutes,
        servings = servings,
        calories = calories,
        protein = protein,
        carbs = carbs,
        fat = fat,
        difficulty = difficulty,
        dietaryTags = dietaryTags,
        allergyTags = allergyTags,
        ingredients = ingredients.map {
            RecipeIngredient(
                id = it.id.ifBlank { java.util.UUID.randomUUID().toString() },
                recipeId = id,
                name = it.name,
                quantity = it.quantity,
                unit = it.unit
            )
        },
        instructions = instructions.map {
            RecipeInstruction(
                id = it.id.ifBlank { java.util.UUID.randomUUID().toString() },
                recipeId = id,
                stepNumber = it.stepNumber,
                instructionText = it.instructionText
            )
        },
        isFavorite = isFavorite,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}
