package com.monteirogames.nutriverse.domain.model

data class FoodItem(
    val id: String,
    val name: String,
    val calories: Float,
    val protein: Float,
    val carbs: Float,
    val fat: Float,
    val servingSize: Float,
    val servingUnit: String,
    val barcode: String? = null
)
