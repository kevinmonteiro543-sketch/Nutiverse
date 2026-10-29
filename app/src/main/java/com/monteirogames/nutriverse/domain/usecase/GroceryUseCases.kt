package com.monteirogames.nutriverse.domain.usecase

import com.monteirogames.nutriverse.domain.model.FoodItem
import com.monteirogames.nutriverse.domain.model.GroceryItem
import com.monteirogames.nutriverse.domain.model.PantryItem
import com.monteirogames.nutriverse.domain.model.Recipe
import com.monteirogames.nutriverse.domain.repository.GroceryRepository
import com.monteirogames.nutriverse.domain.repository.PantryRepository
import kotlinx.coroutines.flow.Flow
import java.util.UUID

class GetGroceryItemsUseCase(private val repository: GroceryRepository) {
    operator fun invoke(userId: String): Flow<List<GroceryItem>> = repository.getGroceryItems(userId)
}

class AddGroceryItemUseCase(private val repository: GroceryRepository) {
    suspend operator fun invoke(item: GroceryItem) {
        repository.addGroceryItem(item)
    }
}

class UpdateGroceryItemUseCase(private val repository: GroceryRepository) {
    suspend operator fun invoke(item: GroceryItem) {
        repository.updateGroceryItem(item)
    }
}

class DeleteGroceryItemUseCase(private val repository: GroceryRepository) {
    suspend operator fun invoke(item: GroceryItem) {
        repository.deleteGroceryItem(item)
    }
}

class ToggleGroceryPurchasedUseCase(private val repository: GroceryRepository) {
    suspend operator fun invoke(id: String, userId: String, isPurchased: Boolean) {
        repository.togglePurchased(id, userId, isPurchased)
    }
}

class ClearPurchasedGroceryUseCase(private val repository: GroceryRepository) {
    suspend operator fun invoke(userId: String) {
        repository.clearPurchasedItems(userId)
    }
}

class AddMissingRecipeIngredientsUseCase(private val repository: GroceryRepository) {
    suspend operator fun invoke(
        userId: String,
        recipe: Recipe,
        pantryItems: List<PantryItem>
    ): Int {
        return repository.addMissingRecipeIngredients(userId, recipe, pantryItems)
    }
}

class AddGroceryItemToPantryUseCase(private val pantryRepository: PantryRepository) {
    suspend operator fun invoke(groceryItem: GroceryItem) {
        val foodItem = FoodItem(
            id = UUID.randomUUID().toString(),
            name = groceryItem.name.trim(),
            calories = 100f, // reasonable baseline serving
            protein = 2f,
            carbs = 10f,
            fat = 1f,
            servingSize = 1f,
            servingUnit = groceryItem.unit
        )
        val pantryItem = PantryItem(
            id = UUID.randomUUID().toString(),
            userId = groceryItem.userId,
            foodItem = foodItem,
            quantity = groceryItem.quantity,
            unit = groceryItem.unit,
            expiryDate = System.currentTimeMillis() + 7L * 24 * 60 * 60 * 1000L, // 7 days default
            dateAdded = System.currentTimeMillis()
        )
        pantryRepository.savePantryItem(pantryItem)
    }
}

data class GroceryUseCases(
    val getGroceryItems: GetGroceryItemsUseCase,
    val addGroceryItem: AddGroceryItemUseCase,
    val updateGroceryItem: UpdateGroceryItemUseCase,
    val deleteGroceryItem: DeleteGroceryItemUseCase,
    val toggleGroceryPurchased: ToggleGroceryPurchasedUseCase,
    val clearPurchasedGrocery: ClearPurchasedGroceryUseCase,
    val addMissingRecipeIngredients: AddMissingRecipeIngredientsUseCase,
    val addGroceryItemToPantry: AddGroceryItemToPantryUseCase
)
