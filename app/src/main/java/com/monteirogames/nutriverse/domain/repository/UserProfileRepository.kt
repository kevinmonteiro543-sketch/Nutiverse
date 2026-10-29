package com.monteirogames.nutriverse.domain.repository

import com.monteirogames.nutriverse.domain.model.UserProfile
import kotlinx.coroutines.flow.Flow

interface UserProfileRepository {
    fun getUserProfile(userId: String): Flow<UserProfile?>
    suspend fun saveProfile(profile: UserProfile)
    suspend fun clearProfile()
}
