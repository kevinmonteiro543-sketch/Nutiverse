package com.monteirogames.nutriverse.data.repository

import com.google.android.gms.tasks.Task
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.FirebaseUser
import com.monteirogames.nutriverse.domain.model.AuthenticatedUser
import com.monteirogames.nutriverse.domain.repository.AuthRepository
import com.monteirogames.nutriverse.domain.repository.AuthResult
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class FirebaseAuthRepository(
    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()
) : AuthRepository {

    override val currentUserFlow: Flow<String?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            trySend(auth.currentUser?.uid)
        }
        firebaseAuth.addAuthStateListener(listener)
        trySend(firebaseAuth.currentUser?.uid)
        awaitClose {
            firebaseAuth.removeAuthStateListener(listener)
        }
    }

    override fun getCurrentUserId(): String? {
        return firebaseAuth.currentUser?.uid
    }

    override fun getCurrentUser(): AuthenticatedUser? {
        return firebaseAuth.currentUser?.toDomain()
    }

    override fun observeAuthState(): Flow<AuthenticatedUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            trySend(auth.currentUser?.toDomain())
        }
        firebaseAuth.addAuthStateListener(listener)
        trySend(firebaseAuth.currentUser?.toDomain())
        awaitClose {
            firebaseAuth.removeAuthStateListener(listener)
        }
    }

    override suspend fun signIn(email: String, password: String): AuthResult<String> {
        val trimmedEmail = email.trim()
        if (trimmedEmail.isEmpty() || password.isEmpty()) {
            return AuthResult.Error("Email and password cannot be empty")
        }

        return try {
            val result = firebaseAuth.signInWithEmailAndPassword(trimmedEmail, password).awaitResult()
            val user = result.user
            if (user != null) {
                AuthResult.Success(user.uid)
            } else {
                AuthResult.Error("Sign in failed. User not found.")
            }
        } catch (e: Exception) {
            AuthResult.Error(mapFirebaseException(e))
        }
    }

    override suspend fun signUp(email: String, password: String): AuthResult<String> {
        val trimmedEmail = email.trim()
        if (trimmedEmail.isEmpty() || password.isEmpty()) {
            return AuthResult.Error("Email and password cannot be empty")
        }
        if (password.length < 6) {
            return AuthResult.Error("Password must be at least 6 characters long")
        }

        return try {
            val result = firebaseAuth.createUserWithEmailAndPassword(trimmedEmail, password).awaitResult()
            val user = result.user
            if (user != null) {
                AuthResult.Success(user.uid)
            } else {
                AuthResult.Error("Registration failed. Could not create user.")
            }
        } catch (e: Exception) {
            AuthResult.Error(mapFirebaseException(e))
        }
    }

    override suspend fun sendPasswordResetEmail(email: String): AuthResult<Unit> {
        val trimmedEmail = email.trim()
        if (trimmedEmail.isEmpty()) {
            return AuthResult.Error("Please enter your email address to reset your password")
        }

        return try {
            firebaseAuth.sendPasswordResetEmail(trimmedEmail).awaitResult()
            AuthResult.Success(Unit)
        } catch (e: Exception) {
            AuthResult.Error(mapFirebaseException(e))
        }
    }

    override suspend fun signOut() {
        firebaseAuth.signOut()
    }

    private fun FirebaseUser.toDomain(): AuthenticatedUser {
        return AuthenticatedUser(
            uid = uid,
            email = email,
            displayName = displayName,
            isEmailVerified = isEmailVerified
        )
    }

    private fun mapFirebaseException(exception: Exception): String {
        return when (exception) {
            is FirebaseAuthInvalidUserException -> {
                when (exception.errorCode) {
                    "ERROR_USER_NOT_FOUND" -> "No account found with this email."
                    "ERROR_USER_DISABLED" -> "This account has been disabled."
                    else -> "No account found with this email."
                }
            }
            is FirebaseAuthInvalidCredentialsException -> {
                when (exception.errorCode) {
                    "ERROR_INVALID_EMAIL" -> "Please enter a valid email address."
                    "ERROR_WRONG_PASSWORD" -> "Incorrect password. Please try again."
                    else -> "Incorrect email or password."
                }
            }
            is FirebaseAuthUserCollisionException -> {
                "An account with this email address already exists. Please sign in instead."
            }
            is FirebaseAuthWeakPasswordException -> {
                exception.reason ?: "The password is too weak. Please use at least 6 characters."
            }
            is FirebaseTooManyRequestsException -> {
                "Too many attempts. Please wait a moment and try again."
            }
            is FirebaseNetworkException -> {
                "Network error. Please check your internet connection."
            }
            else -> {
                val message = exception.localizedMessage ?: exception.message ?: ""
                when {
                    message.contains("network", ignoreCase = true) ->
                        "Network error. Please check your internet connection."
                    message.contains("password", ignoreCase = true) && message.contains("invalid", ignoreCase = true) ->
                        "Incorrect password. Please try again."
                    message.contains("no user", ignoreCase = true) || message.contains("user-not-found", ignoreCase = true) ->
                        "No account found with this email."
                    message.contains("email-already-in-use", ignoreCase = true) ->
                        "An account with this email address already exists."
                    message.isNotBlank() ->
                        message
                    else ->
                        "Authentication error. Please try again."
                }
            }
        }
    }

    companion object {
        suspend fun <T> Task<T>.awaitResult(): T = suspendCancellableCoroutine { cont ->
            addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    cont.resume(task.result)
                } else {
                    cont.resumeWithException(task.exception ?: RuntimeException("Unknown Firebase operation error"))
                }
            }
        }
    }
}
