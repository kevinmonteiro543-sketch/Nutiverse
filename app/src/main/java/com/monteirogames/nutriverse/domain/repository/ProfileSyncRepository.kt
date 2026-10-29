package com.monteirogames.nutriverse.domain.repository

import com.monteirogames.nutriverse.domain.model.SyncState
import com.monteirogames.nutriverse.domain.model.UserProfile
import kotlinx.coroutines.flow.StateFlow

/**
 * Dedicated repository abstraction for UserProfile cloud synchronization.
 * Synchronizes only the user profile between local Room and Firestore users/{FirebaseUID}.
 */
interface ProfileSyncRepository {
    val syncState: StateFlow<SyncState>
    fun getSyncState(): SyncState

    suspend fun syncProfile(): Result<UserProfile?>
    suspend fun uploadProfile(profile: UserProfile): Result<Unit>
    suspend fun downloadProfile(): Result<UserProfile?>
}
