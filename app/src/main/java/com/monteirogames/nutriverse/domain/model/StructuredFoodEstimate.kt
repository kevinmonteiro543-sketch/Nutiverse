package com.monteirogames.nutriverse.domain.model

data class StructuredFoodEstimate(
    val name: String,
    val servingDescription: String?,
    val servingQuantity: Float?,
    val calories: Float?,
    val protein: Float?,
    val carbs: Float?,
    val fat: Float?,
    val confidenceScore: Float?,
    val notes: String?
)
