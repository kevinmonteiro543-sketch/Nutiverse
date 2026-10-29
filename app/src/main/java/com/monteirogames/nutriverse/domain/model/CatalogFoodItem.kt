package com.monteirogames.nutriverse.domain.model

data class CatalogServingPreset(
    val label: String,
    val grams: Float
)

data class CalculatedNutrition(
    val calories: Float,
    val protein: Float,
    val carbs: Float,
    val fat: Float
)

data class CatalogFoodItem(
    val id: String,
    val name: String,
    val category: String,
    val caloriesPer100g: Float,
    val proteinPer100g: Float,
    val carbsPer100g: Float,
    val fatPer100g: Float,
    val defaultQuantityGrams: Float = 100f,
    val servingUnitName: String = "g",
    val presets: List<CatalogServingPreset> = emptyList(),
    val isCooked: Boolean = true,
    val description: String? = null
) {
    /**
     * Deterministic calculation:
     * calculatedValue = valuePer100g * quantityInGrams / 100
     * Preserves internal numeric precision.
     */
    fun calculateNutrition(quantityInGrams: Float): CalculatedNutrition {
        val safeGrams = if (quantityInGrams > 0f) quantityInGrams else 0f
        val factor = safeGrams / 100f
        return CalculatedNutrition(
            calories = caloriesPer100g * factor,
            protein = proteinPer100g * factor,
            carbs = carbsPer100g * factor,
            fat = fatPer100g * factor
        )
    }
}
