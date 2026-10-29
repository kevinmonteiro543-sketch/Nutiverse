package com.monteirogames.nutriverse.domain.usecase

import com.monteirogames.nutriverse.domain.model.PantryItem
import com.monteirogames.nutriverse.domain.model.SyncState
import com.monteirogames.nutriverse.domain.repository.PantrySyncRepository
import kotlinx.coroutines.flow.StateFlow

class SyncPantryUseCase(private val repository: PantrySyncRepository) {
    suspend operator fun invoke(): Result<Int> {
        return repository.syncPantry()
    }
}

class UploadPantryItemUseCase(private val repository: PantrySyncRepository) {
    suspend operator fun invoke(item: PantryItem): Result<Unit> {
        return repository.uploadPantryItem(item)
    }
}

class DeletePantryItemFromCloudUseCase(private val repository: PantrySyncRepository) {
    suspend operator fun invoke(itemId: String): Result<Unit> {
        return repository.deletePantryItemInCloud(itemId)
    }
}

class ObservePantrySyncStateUseCase(private val repository: PantrySyncRepository) {
    operator fun invoke(): StateFlow<SyncState> {
        return repository.syncState
    }
}

data class PantrySyncUseCases(
    val syncPantry: SyncPantryUseCase,
    val uploadPantryItem: UploadPantryItemUseCase,
    val deletePantryItemFromCloud: DeletePantryItemFromCloudUseCase,
    val observePantrySyncState: ObservePantrySyncStateUseCase
)
