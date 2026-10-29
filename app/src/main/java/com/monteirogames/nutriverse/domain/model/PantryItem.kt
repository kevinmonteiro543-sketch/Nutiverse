package com.monteirogames.nutriverse.domain.model

data class PantryItem(
    val id: String,
    val userId: String,
    val foodItem: FoodItem,
    val quantity: Float,
    val unit: String,
    val expiryDate: Long,
    val dateAdded: Long,
    val updatedAt: Long = dateAdded
)
