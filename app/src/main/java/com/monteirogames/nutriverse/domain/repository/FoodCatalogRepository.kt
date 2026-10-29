package com.monteirogames.nutriverse.domain.repository

import com.monteirogames.nutriverse.domain.model.CatalogFoodItem

interface FoodCatalogRepository {
    fun getAllFoods(): List<CatalogFoodItem>
    fun searchFoods(query: String, category: String?): List<CatalogFoodItem>
    fun getCategories(): List<String>
    fun getFoodById(id: String): CatalogFoodItem?
}
