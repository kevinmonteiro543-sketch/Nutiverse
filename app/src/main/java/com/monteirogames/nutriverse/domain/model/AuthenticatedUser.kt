package com.monteirogames.nutriverse.domain.model

data class AuthenticatedUser(
    val uid: String,
    val email: String?,
    val displayName: String? = null,
    val isEmailVerified: Boolean = false
)
