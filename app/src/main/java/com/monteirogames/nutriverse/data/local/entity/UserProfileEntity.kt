package com.monteirogames.nutriverse.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_profile")
data class UserProfileEntity(
    @PrimaryKey
    val userId: String,
    val name: String,
    val age: Int,
    val gender: String,
    val heightCm: Float,
    val weightKg: Float,
    val goal: String, // Lose Weight, Maintain Weight, Gain Weight
    val dietaryPreference: String,
    val allergies: String,
    val activityLevel: String,
    val workoutFrequency: String = "",
    val workoutIntensity: String = "",
    val updatedAt: Long = 0L
)
