package com.monteirogames.nutriverse.domain.usecase

import com.monteirogames.nutriverse.domain.model.PantryItem
import com.monteirogames.nutriverse.domain.repository.PantryRepository
import kotlinx.coroutines.flow.Flow

class AddPantryItemUseCase(private val repository: PantryRepository) {
    suspend operator fun invoke(pantryItem: PantryItem) {
        repository.savePantryItem(pantryItem)
    }
}

class UpdatePantryItemUseCase(private val repository: PantryRepository) {
    suspend operator fun invoke(pantryItem: PantryItem) {
        repository.updatePantryItem(pantryItem)
    }
}

class DeletePantryItemUseCase(private val repository: PantryRepository) {
    suspend operator fun invoke(pantryItem: PantryItem) {
        repository.deletePantryItem(pantryItem)
    }
}

class GetPantryItemUseCase(private val repository: PantryRepository) {
    suspend operator fun invoke(id: String, userId: String): PantryItem? {
        return repository.getPantryItemById(id, userId)
    }
}

class ObservePantryItemsUseCase(private val repository: PantryRepository) {
    operator fun invoke(userId: String): Flow<List<PantryItem>> {
        return repository.getPantryItems(userId)
    }
}

data class PantryUseCases(
    val addPantryItem: AddPantryItemUseCase,
    val updatePantryItem: UpdatePantryItemUseCase,
    val deletePantryItem: DeletePantryItemUseCase,
    val getPantryItem: GetPantryItemUseCase,
    val observePantryItems: ObservePantryItemsUseCase
)
