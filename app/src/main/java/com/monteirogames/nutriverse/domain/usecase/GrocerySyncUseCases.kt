package com.monteirogames.nutriverse.domain.usecase

import com.monteirogames.nutriverse.domain.model.GroceryItem
import com.monteirogames.nutriverse.domain.model.SyncState
import com.monteirogames.nutriverse.domain.repository.GrocerySyncRepository
import kotlinx.coroutines.flow.StateFlow

class SyncGroceryUseCase(private val repository: GrocerySyncRepository) {
    suspend operator fun invoke(): Result<Int> = repository.syncGrocery()
}

class UploadGroceryItemUseCase(private val repository: GrocerySyncRepository) {
    suspend operator fun invoke(item: GroceryItem): Result<Unit> = repository.uploadGroceryItem(item)
}

class DeleteGroceryItemFromCloudUseCase(private val repository: GrocerySyncRepository) {
    suspend operator fun invoke(itemId: String): Result<Unit> = repository.deleteGroceryItemInCloud(itemId)
}

class ObserveGrocerySyncStateUseCase(private val repository: GrocerySyncRepository) {
    operator fun invoke(): StateFlow<SyncState> = repository.syncState
}

data class GrocerySyncUseCases(
    val syncGrocery: SyncGroceryUseCase,
    val uploadGroceryItem: UploadGroceryItemUseCase,
    val deleteGroceryItemFromCloud: DeleteGroceryItemFromCloudUseCase,
    val observeGrocerySyncState: ObserveGrocerySyncStateUseCase
)
