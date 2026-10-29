package com.monteirogames.nutriverse.domain.usecase

import com.monteirogames.nutriverse.domain.model.AuthenticatedUser
import com.monteirogames.nutriverse.domain.repository.AuthRepository
import com.monteirogames.nutriverse.domain.repository.AuthResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

class SignInUseCase(private val repository: AuthRepository) {
    suspend operator fun invoke(email: String, password: String): AuthResult<String> {
        return repository.signIn(email, password)
    }
}

class SignUpUseCase(private val repository: AuthRepository) {
    suspend operator fun invoke(email: String, password: String): AuthResult<String> {
        return repository.signUp(email, password)
    }
}

class SignOutUseCase(private val repository: AuthRepository) {
    suspend operator fun invoke() {
        repository.signOut()
    }
}

class GetCurrentUserIdUseCase(private val repository: AuthRepository) {
    operator fun invoke(): String? {
        return repository.getCurrentUserId()
    }
}

class GetCurrentUserFlowUseCase(private val repository: AuthRepository) {
    operator fun invoke(): Flow<String?> {
        return repository.currentUserFlow
    }
}

class SendPasswordResetEmailUseCase(private val repository: AuthRepository) {
    suspend operator fun invoke(email: String): AuthResult<Unit> {
        return repository.sendPasswordResetEmail(email)
    }
}

class GetCurrentUserUseCase(private val repository: AuthRepository) {
    operator fun invoke(): AuthenticatedUser? {
        return repository.getCurrentUser()
    }
}

class ObserveAuthStateUseCase(private val repository: AuthRepository) {
    operator fun invoke(): Flow<AuthenticatedUser?> {
        return repository.observeAuthState()
    }
}

data class AuthUseCases(
    val signIn: SignInUseCase,
    val signUp: SignUpUseCase,
    val signOut: SignOutUseCase,
    val getCurrentUserId: GetCurrentUserIdUseCase,
    val getCurrentUserFlow: GetCurrentUserFlowUseCase,
    val sendPasswordResetEmail: SendPasswordResetEmailUseCase,
    val getCurrentUser: GetCurrentUserUseCase,
    val observeAuthState: ObserveAuthStateUseCase
) {
    constructor(
        signIn: SignInUseCase,
        signUp: SignUpUseCase,
        signOut: SignOutUseCase,
        getCurrentUserId: GetCurrentUserIdUseCase,
        getCurrentUserFlow: GetCurrentUserFlowUseCase
    ) : this(
        signIn = signIn,
        signUp = signUp,
        signOut = signOut,
        getCurrentUserId = getCurrentUserId,
        getCurrentUserFlow = getCurrentUserFlow,
        sendPasswordResetEmail = SendPasswordResetEmailUseCase(object : AuthRepository {
            override val currentUserFlow: Flow<String?> get() = getCurrentUserFlow()
            override fun getCurrentUserId(): String? = getCurrentUserId()
            override fun getCurrentUser(): AuthenticatedUser? = null
            override fun observeAuthState(): Flow<AuthenticatedUser?> = emptyFlow()
            override suspend fun signIn(email: String, password: String) = signIn(email, password)
            override suspend fun signUp(email: String, password: String) = signUp(email, password)
            override suspend fun sendPasswordResetEmail(email: String) = AuthResult.Error("Not configured")
            override suspend fun signOut() = signOut()
        }),
        getCurrentUser = GetCurrentUserUseCase(object : AuthRepository {
            override val currentUserFlow: Flow<String?> get() = getCurrentUserFlow()
            override fun getCurrentUserId(): String? = getCurrentUserId()
            override fun getCurrentUser(): AuthenticatedUser? = null
            override fun observeAuthState(): Flow<AuthenticatedUser?> = emptyFlow()
            override suspend fun signIn(email: String, password: String) = signIn(email, password)
            override suspend fun signUp(email: String, password: String) = signUp(email, password)
            override suspend fun sendPasswordResetEmail(email: String) = AuthResult.Error("Not configured")
            override suspend fun signOut() = signOut()
        }),
        observeAuthState = ObserveAuthStateUseCase(object : AuthRepository {
            override val currentUserFlow: Flow<String?> get() = getCurrentUserFlow()
            override fun getCurrentUserId(): String? = getCurrentUserId()
            override fun getCurrentUser(): AuthenticatedUser? = null
            override fun observeAuthState(): Flow<AuthenticatedUser?> = emptyFlow()
            override suspend fun signIn(email: String, password: String) = signIn(email, password)
            override suspend fun signUp(email: String, password: String) = signUp(email, password)
            override suspend fun sendPasswordResetEmail(email: String) = AuthResult.Error("Not configured")
            override suspend fun signOut() = signOut()
        })
    )
}
