package com.monteirogames.nutriverse.domain.usecase

import com.monteirogames.nutriverse.domain.model.SyncState
import com.monteirogames.nutriverse.domain.model.UserProfile
import com.monteirogames.nutriverse.domain.repository.ProfileSyncRepository
import kotlinx.coroutines.flow.StateFlow

class SyncProfileUseCase(private val repository: ProfileSyncRepository) {
    suspend operator fun invoke(): Result<UserProfile?> {
        return repository.syncProfile()
    }
}

class ObserveProfileSyncStateUseCase(private val repository: ProfileSyncRepository) {
    operator fun invoke(): StateFlow<SyncState> {
        return repository.syncState
    }
}

data class ProfileSyncUseCases(
    val syncProfile: SyncProfileUseCase,
    val observeProfileSyncState: ObserveProfileSyncStateUseCase
)
