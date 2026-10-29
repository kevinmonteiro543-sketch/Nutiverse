package com.monteirogames.nutriverse.domain.usecase

import com.monteirogames.nutriverse.domain.model.FirestoreUserDocument
import com.monteirogames.nutriverse.domain.model.SyncState
import com.monteirogames.nutriverse.domain.repository.FirestoreResult
import com.monteirogames.nutriverse.domain.repository.FirestoreUserRepository
import com.monteirogames.nutriverse.domain.repository.SyncRepository
import kotlinx.coroutines.flow.StateFlow

class GetSyncStateUseCase(private val syncRepository: SyncRepository) {
    operator fun invoke(): SyncState {
        return syncRepository.getSyncState()
    }
}

class ObserveSyncStateUseCase(private val syncRepository: SyncRepository) {
    operator fun invoke(): StateFlow<SyncState> {
        return syncRepository.syncState
    }
}

class GetCurrentUserDocumentUseCase(private val firestoreUserRepository: FirestoreUserRepository) {
    suspend operator fun invoke(): FirestoreResult<FirestoreUserDocument?> {
        return firestoreUserRepository.getCurrentUserDocument()
    }
}

class CreateOrUpdateUserDocumentUseCase(private val firestoreUserRepository: FirestoreUserRepository) {
    suspend operator fun invoke(document: FirestoreUserDocument): FirestoreResult<Unit> {
        return firestoreUserRepository.createOrUpdateUserDocument(document)
    }
}

class SyncAllUseCase(private val syncRepository: SyncRepository) {
    suspend operator fun invoke(): Result<Unit> {
        return syncRepository.syncAll()
    }
}

data class SyncUseCases(
    val getSyncState: GetSyncStateUseCase,
    val observeSyncState: ObserveSyncStateUseCase,
    val getCurrentUserDocument: GetCurrentUserDocumentUseCase,
    val createOrUpdateUserDocument: CreateOrUpdateUserDocumentUseCase,
    val syncAll: SyncAllUseCase
)

