package com.monteirogames.nutriverse.domain.model

data class ScannedProduct(
    val barcode: String,
    val name: String,
    val brand: String?,
    val imageUrl: String?,
    val servingSize: String?,
    val ingredients: String?,
    val allergens: String?,
    val nutriscore: String?,
    val energyKcal: Float?,
    val protein: Float?,
    val carbs: Float?,
    val fat: Float?
)
