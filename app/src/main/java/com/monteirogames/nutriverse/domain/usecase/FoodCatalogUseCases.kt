package com.monteirogames.nutriverse.domain.usecase

import com.monteirogames.nutriverse.domain.model.CatalogFoodItem
import com.monteirogames.nutriverse.domain.model.FoodItem
import com.monteirogames.nutriverse.domain.model.MealEntry
import com.monteirogames.nutriverse.domain.repository.FoodCatalogRepository
import java.util.UUID

class SearchCatalogFoodsUseCase(
    private val repository: FoodCatalogRepository
) {
    operator fun invoke(query: String = "", category: String? = null): List<CatalogFoodItem> {
        return repository.searchFoods(query, category)
    }
}

class GetCatalogCategoriesUseCase(
    private val repository: FoodCatalogRepository
) {
    operator fun invoke(): List<String> {
        return repository.getCategories()
    }
}

class GetCatalogFoodByIdUseCase(
    private val repository: FoodCatalogRepository
) {
    operator fun invoke(id: String): CatalogFoodItem? {
        return repository.getFoodById(id)
    }
}

class AddCatalogFoodToMealUseCase(
    private val mealTrackerUseCases: MealTrackerUseCases
) {
    suspend operator fun invoke(
        userId: String,
        catalogFood: CatalogFoodItem,
        quantityInGrams: Float,
        mealType: String,
        date: Long = System.currentTimeMillis()
    ): Result<MealEntry> {
        if (quantityInGrams <= 0f) {
            return Result.failure(IllegalArgumentException("Quantity must be greater than 0g"))
        }
        if (quantityInGrams > 5000f) {
            return Result.failure(IllegalArgumentException("Quantity cannot exceed 5000g"))
        }

        val foodItem = FoodItem(
            id = UUID.randomUUID().toString(),
            name = catalogFood.name,
            calories = catalogFood.caloriesPer100g,
            protein = catalogFood.proteinPer100g,
            carbs = catalogFood.carbsPer100g,
            fat = catalogFood.fatPer100g,
            servingSize = 100f,
            servingUnit = "g"
        )

        val mealEntry = MealEntry(
            id = UUID.randomUUID().toString(),
            userId = userId,
            foodItem = foodItem,
            date = date,
            mealType = mealType,
            quantity = quantityInGrams,
            loggedAt = System.currentTimeMillis()
        )

        mealTrackerUseCases.addMeal(mealEntry)
        return Result.success(mealEntry)
    }
}

data class FoodCatalogUseCases(
    val searchFoods: SearchCatalogFoodsUseCase,
    val getCategories: GetCatalogCategoriesUseCase,
    val getFoodById: GetCatalogFoodByIdUseCase,
    val addCatalogFoodToMeal: AddCatalogFoodToMealUseCase
)
