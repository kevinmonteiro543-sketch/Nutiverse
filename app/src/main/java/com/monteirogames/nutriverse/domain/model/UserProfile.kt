package com.monteirogames.nutriverse.domain.model

data class UserProfile(
    val userId: String,
    val name: String,
    val age: Int,
    val gender: String,
    val heightCm: Float,
    val weightKg: Float,
    val goal: String,
    val dietaryPreference: String,
    val allergies: String,
    val activityLevel: String,
    val workoutFrequency: String = "",
    val workoutIntensity: String = "",
    val updatedAt: Long = 0L
)
