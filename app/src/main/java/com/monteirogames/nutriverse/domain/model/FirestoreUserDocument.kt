package com.monteirogames.nutriverse.domain.model

/**
 * Minimal user metadata document stored in Firestore at `users/{uid}`.
 * Establishes cloud identity foundation without duplicating the local Room UserProfile.
 */
data class FirestoreUserDocument(
    val uid: String,
    val email: String? = null,
    val displayName: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val schemaVersion: Int = CURRENT_SCHEMA_VERSION
) {
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "uid" to uid,
            "email" to email,
            "displayName" to displayName,
            "createdAt" to createdAt,
            "updatedAt" to updatedAt,
            "schemaVersion" to schemaVersion
        )
    }

    companion object {
        const val CURRENT_SCHEMA_VERSION = 1

        fun fromMap(map: Map<String, Any?>): FirestoreUserDocument {
            return FirestoreUserDocument(
                uid = map["uid"] as? String ?: "",
                email = map["email"] as? String,
                displayName = map["displayName"] as? String,
                createdAt = (map["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                updatedAt = (map["updatedAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                schemaVersion = (map["schemaVersion"] as? Number)?.toInt() ?: CURRENT_SCHEMA_VERSION
            )
        }
    }
}
