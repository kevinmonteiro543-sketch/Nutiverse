package com.monteirogames.nutriverse.data.repository

import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.SetOptions
import com.monteirogames.nutriverse.domain.model.FirestoreCollections
import com.monteirogames.nutriverse.domain.model.FirestoreUserDocument
import com.monteirogames.nutriverse.domain.repository.AuthRepository
import com.monteirogames.nutriverse.domain.repository.FirestoreResult
import com.monteirogames.nutriverse.domain.repository.FirestoreUserRepository
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class FirestoreUserRepositoryImpl(
    private val authRepository: AuthRepository,
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) : FirestoreUserRepository {

    override suspend fun getCurrentUserDocument(): FirestoreResult<FirestoreUserDocument?> {
        val currentUid = authRepository.getCurrentUserId()
            ?: return FirestoreResult.Error("No authenticated user found. Please sign in.")
        return getUserDocument(currentUid)
    }

    override suspend fun createOrUpdateUserDocument(document: FirestoreUserDocument): FirestoreResult<Unit> {
        val currentUid = authRepository.getCurrentUserId()
            ?: return FirestoreResult.Error("No authenticated user found. Please sign in.")

        if (document.uid.isBlank()) {
            return FirestoreResult.Error("Invalid document: UID cannot be empty.")
        }

        if (document.uid != currentUid) {
            return FirestoreResult.Error("Security violation: Cannot write document for another user identity.")
        }

        return try {
            firestore.collection(FirestoreCollections.USERS)
                .document(document.uid)
                .set(document.toMap(), SetOptions.merge())
                .awaitResult()
            FirestoreResult.Success(Unit)
        } catch (e: Exception) {
            FirestoreResult.Error(mapFirestoreException(e), e)
        }
    }

    override suspend fun getUserDocument(uid: String): FirestoreResult<FirestoreUserDocument?> {
        val currentUid = authRepository.getCurrentUserId()
            ?: return FirestoreResult.Error("No authenticated user found. Please sign in.")

        if (uid.isBlank()) {
            return FirestoreResult.Error("User ID cannot be blank.")
        }

        if (uid != currentUid) {
            return FirestoreResult.Error("Security violation: Cannot access document of another user.")
        }

        return try {
            val snapshot = firestore.collection(FirestoreCollections.USERS)
                .document(uid)
                .get()
                .awaitResult()

            if (!snapshot.exists()) {
                FirestoreResult.Success(null)
            } else {
                val data = snapshot.data ?: emptyMap()
                FirestoreResult.Success(FirestoreUserDocument.fromMap(data))
            }
        } catch (e: Exception) {
            FirestoreResult.Error(mapFirestoreException(e), e)
        }
    }

    private fun mapFirestoreException(e: Exception): String {
        return when (e) {
            is FirebaseFirestoreException -> {
                when (e.code) {
                    FirebaseFirestoreException.Code.PERMISSION_DENIED ->
                        "Access denied. You do not have permission to access this cloud data."
                    FirebaseFirestoreException.Code.UNAVAILABLE ->
                        "Cloud database is currently unavailable. Please check your network connection."
                    FirebaseFirestoreException.Code.UNAUTHENTICATED ->
                        "User is not authenticated. Please sign in again."
                    FirebaseFirestoreException.Code.DEADLINE_EXCEEDED ->
                        "Cloud operation timed out. Please try again later."
                    else -> "Cloud database operation failed: ${e.code.name}"
                }
            }
            else -> e.localizedMessage ?: "An unexpected cloud error occurred."
        }
    }

    companion object {
        suspend fun <T> Task<T>.awaitResult(): T = suspendCancellableCoroutine { continuation ->
            addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    continuation.resume(task.result)
                } else {
                    continuation.resumeWithException(
                        task.exception ?: RuntimeException("Unknown Firestore operation failure")
                    )
                }
            }
        }
    }
}
