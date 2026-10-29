package com.monteirogames.nutriverse.domain.repository

import com.monteirogames.nutriverse.domain.model.FirestoreUserDocument

sealed class FirestoreResult<out T> {
    data class Success<out T>(val data: T) : FirestoreResult<T>()
    data class Error(val message: String, val cause: Throwable? = null) : FirestoreResult<Nothing>()
}

interface FirestoreUserRepository {
    suspend fun getCurrentUserDocument(): FirestoreResult<FirestoreUserDocument?>
    suspend fun createOrUpdateUserDocument(document: FirestoreUserDocument): FirestoreResult<Unit>
    suspend fun getUserDocument(uid: String): FirestoreResult<FirestoreUserDocument?>
}
