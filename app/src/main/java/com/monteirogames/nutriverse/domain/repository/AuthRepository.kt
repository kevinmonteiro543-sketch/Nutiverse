package com.monteirogames.nutriverse.domain.repository

import com.monteirogames.nutriverse.domain.model.AuthenticatedUser
import kotlinx.coroutines.flow.Flow

sealed class AuthResult<out T> {
    data class Success<out T>(val data: T) : AuthResult<T>()
    data class Error(val message: String) : AuthResult<Nothing>()
    object Loading : AuthResult<Nothing>()
}

interface AuthRepository {
    val currentUserFlow: Flow<String?>
    fun getCurrentUserId(): String?
    fun getCurrentUser(): AuthenticatedUser?
    fun observeAuthState(): Flow<AuthenticatedUser?>
    suspend fun signIn(email: String, password: String): AuthResult<String>
    suspend fun signUp(email: String, password: String): AuthResult<String>
    suspend fun sendPasswordResetEmail(email: String): AuthResult<Unit>
    suspend fun signOut()
}
