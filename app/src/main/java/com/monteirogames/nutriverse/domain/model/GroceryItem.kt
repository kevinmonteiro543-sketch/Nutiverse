package com.monteirogames.nutriverse.domain.model

data class GroceryItem(
    val id: String,
    val userId: String,
    val name: String,
    val quantity: Float,
    val unit: String,
    val category: String, // "Produce", "Dairy", "Meat & Seafood", "Grains", "Snacks", "Beverages", "Pantry", "Other"
    val isPurchased: Boolean = false,
    val source: String = "MANUAL", // "MANUAL", "RECIPE", "DIET_PLAN"
    val sourceRecipeId: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val purchasedAt: Long? = null,
    val updatedAt: Long = createdAt
)
