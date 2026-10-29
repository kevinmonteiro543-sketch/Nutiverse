package com.monteirogames.nutriverse.data.repository

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.monteirogames.nutriverse.domain.model.AuthenticatedUser
import com.monteirogames.nutriverse.domain.repository.AuthRepository
import com.monteirogames.nutriverse.domain.repository.AuthResult
import kotlinx.coroutines.flow.Flow

/**
 * Backward-compatible implementation of [AuthRepository] delegating to [FirebaseAuthRepository].
 */
class AuthRepositoryImpl(
    context: Context? = null,
    private val delegate: FirebaseAuthRepository = FirebaseAuthRepository()
) : AuthRepository by delegate
