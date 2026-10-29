package com.monteirogames.nutriverse.domain.model

/**
 * Firestore DTO representing the profile state stored at `users/{FirebaseUID}`.
 * Maps cleanly between the local Room UserProfile and Firestore without exposing internal Room details.
 */
data class FirestoreProfileDocument(
    val uid: String,
    val name: String = "",
    val age: Int = 0,
    val gender: String = "",
    val heightCm: Float = 0f,
    val weightKg: Float = 0f,
    val goal: String = "",
    val dietaryPreference: String = "",
    val allergies: String = "",
    val activityLevel: String = "",
    val workoutFrequency: String = "",
    val workoutIntensity: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val schemaVersion: Int = CURRENT_SCHEMA_VERSION
) {
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "uid" to uid,
            "name" to name,
            "age" to age,
            "gender" to gender,
            "heightCm" to heightCm.toDouble(),
            "weightKg" to weightKg.toDouble(),
            "goal" to goal,
            "dietaryPreference" to dietaryPreference,
            "allergies" to allergies,
            "activityLevel" to activityLevel,
            "workoutFrequency" to workoutFrequency,
            "workoutIntensity" to workoutIntensity,
            "createdAt" to createdAt,
            "updatedAt" to updatedAt,
            "schemaVersion" to schemaVersion
        )
    }

    companion object {
        const val CURRENT_SCHEMA_VERSION = 1

        fun fromMap(map: Map<String, Any?>, fallbackUid: String = ""): FirestoreProfileDocument {
            return FirestoreProfileDocument(
                uid = map["uid"] as? String ?: fallbackUid,
                name = map["name"] as? String ?: "",
                age = (map["age"] as? Number)?.toInt() ?: 0,
                gender = map["gender"] as? String ?: "",
                heightCm = (map["heightCm"] as? Number)?.toFloat() ?: 0f,
                weightKg = (map["weightKg"] as? Number)?.toFloat() ?: 0f,
                goal = map["goal"] as? String ?: "",
                dietaryPreference = map["dietaryPreference"] as? String ?: "",
                allergies = map["allergies"] as? String ?: "",
                activityLevel = map["activityLevel"] as? String ?: "",
                workoutFrequency = map["workoutFrequency"] as? String ?: "",
                workoutIntensity = map["workoutIntensity"] as? String ?: "",
                createdAt = (map["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                updatedAt = (map["updatedAt"] as? Number)?.toLong() ?: 0L,
                schemaVersion = (map["schemaVersion"] as? Number)?.toInt() ?: CURRENT_SCHEMA_VERSION
            )
        }
    }
}

fun UserProfile.toFirestoreDocument(createdAt: Long = updatedAt): FirestoreProfileDocument {
    val validUpdatedAt = if (updatedAt > 0L) updatedAt else System.currentTimeMillis()
    return FirestoreProfileDocument(
        uid = userId,
        name = name,
        age = age,
        gender = gender,
        heightCm = heightCm,
        weightKg = weightKg,
        goal = goal,
        dietaryPreference = dietaryPreference,
        allergies = allergies,
        activityLevel = activityLevel,
        workoutFrequency = workoutFrequency,
        workoutIntensity = workoutIntensity,
        createdAt = if (createdAt > 0L) createdAt else validUpdatedAt,
        updatedAt = validUpdatedAt
    )
}

fun FirestoreProfileDocument.toUserProfile(): UserProfile {
    return UserProfile(
        userId = uid,
        name = name,
        age = age,
        gender = gender,
        heightCm = heightCm,
        weightKg = weightKg,
        goal = goal,
        dietaryPreference = dietaryPreference,
        allergies = allergies,
        activityLevel = activityLevel,
        workoutFrequency = workoutFrequency,
        workoutIntensity = workoutIntensity,
        updatedAt = updatedAt
    )
}
