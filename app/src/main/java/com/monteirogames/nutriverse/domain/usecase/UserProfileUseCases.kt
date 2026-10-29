package com.monteirogames.nutriverse.domain.usecase

import com.monteirogames.nutriverse.domain.model.UserProfile
import com.monteirogames.nutriverse.domain.repository.UserProfileRepository
import kotlinx.coroutines.flow.Flow

class GetUserProfileUseCase(private val repository: UserProfileRepository) {
    operator fun invoke(userId: String): Flow<UserProfile?> {
        return repository.getUserProfile(userId)
    }
}

class SaveUserProfileUseCase(private val repository: UserProfileRepository) {
    suspend operator fun invoke(profile: UserProfile) {
        repository.saveProfile(profile)
    }
}

class ClearUserProfileUseCase(private val repository: UserProfileRepository) {
    suspend operator fun invoke() {
        repository.clearProfile()
    }
}

data class UserProfileUseCases(
    val getUserProfile: GetUserProfileUseCase,
    val saveUserProfile: SaveUserProfileUseCase,
    val clearUserProfile: ClearUserProfileUseCase,
    val calculateNutritionTargets: CalculateNutritionTargetsUseCase
)
