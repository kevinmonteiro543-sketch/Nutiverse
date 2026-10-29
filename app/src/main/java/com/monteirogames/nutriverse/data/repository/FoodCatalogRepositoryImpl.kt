package com.monteirogames.nutriverse.data.repository

import com.monteirogames.nutriverse.data.catalog.BuiltInFoodCatalog
import com.monteirogames.nutriverse.domain.model.CatalogFoodItem
import com.monteirogames.nutriverse.domain.repository.FoodCatalogRepository

class FoodCatalogRepositoryImpl : FoodCatalogRepository {

    override fun getAllFoods(): List<CatalogFoodItem> {
        return BuiltInFoodCatalog.foods
    }

    override fun searchFoods(query: String, category: String?): List<CatalogFoodItem> {
        val trimmedQuery = query.trim().lowercase()
        return BuiltInFoodCatalog.foods.filter { food ->
            val matchesCategory = category.isNullOrBlank() || category.equals("All", ignoreCase = true) || food.category.equals(category, ignoreCase = true)
            val matchesQuery = trimmedQuery.isEmpty() ||
                    food.name.lowercase().contains(trimmedQuery) ||
                    food.category.lowercase().contains(trimmedQuery) ||
                    (food.description?.lowercase()?.contains(trimmedQuery) == true)
            matchesCategory && matchesQuery
        }
    }

    override fun getCategories(): List<String> {
        return BuiltInFoodCatalog.categories
    }

    override fun getFoodById(id: String): CatalogFoodItem? {
        return BuiltInFoodCatalog.foods.find { it.id == id }
    }
}
