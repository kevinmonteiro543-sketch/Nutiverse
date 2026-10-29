package com.monteirogames.nutriverse.data.repository

import com.monteirogames.nutriverse.data.local.dao.UserProfileDao
import com.monteirogames.nutriverse.data.local.entity.UserProfileEntity
import com.monteirogames.nutriverse.domain.model.UserProfile
import com.monteirogames.nutriverse.domain.repository.UserProfileRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class UserProfileRepositoryImpl(
    private val dao: UserProfileDao
) : UserProfileRepository {
    
    override fun getUserProfile(userId: String): Flow<UserProfile?> {
        return dao.getUserProfile(userId).map { entity ->
            entity?.toDomain()
        }
    }

    override suspend fun saveProfile(profile: UserProfile) {
        dao.insertProfile(profile.toEntity())
    }

    override suspend fun clearProfile() {
        dao.clearAll()
    }
}

fun UserProfileEntity.toDomain() = UserProfile(
    userId = userId,
    name = name,
    age = age,
    gender = gender,
    heightCm = heightCm,
    weightKg = weightKg,
    goal = goal,
    dietaryPreference = dietaryPreference,
    allergies = allergies,
    activityLevel = activityLevel,
    workoutFrequency = workoutFrequency,
    workoutIntensity = workoutIntensity,
    updatedAt = updatedAt
)

fun UserProfile.toEntity() = UserProfileEntity(
    userId = userId,
    name = name,
    age = age,
    gender = gender,
    heightCm = heightCm,
    weightKg = weightKg,
    goal = goal,
    dietaryPreference = dietaryPreference,
    allergies = allergies,
    activityLevel = activityLevel,
    workoutFrequency = workoutFrequency,
    workoutIntensity = workoutIntensity,
    updatedAt = if (updatedAt > 0L) updatedAt else System.currentTimeMillis()
)
