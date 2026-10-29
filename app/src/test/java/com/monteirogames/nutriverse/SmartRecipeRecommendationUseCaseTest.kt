package com.monteirogames.nutriverse

import com.monteirogames.nutriverse.domain.model.FoodItem
import com.monteirogames.nutriverse.domain.model.PantryItem
import com.monteirogames.nutriverse.domain.model.Recipe
import com.monteirogames.nutriverse.domain.model.RecipeIngredient
import com.monteirogames.nutriverse.domain.model.UserProfile
import com.monteirogames.nutriverse.domain.usecase.DailyNutrition
import com.monteirogames.nutriverse.domain.usecase.NutritionTargets
import com.monteirogames.nutriverse.domain.usecase.SmartRecipeRecommendationUseCase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SmartRecipeRecommendationUseCaseTest {

    private val useCase = SmartRecipeRecommendationUseCase()

    private val chickenRecipe = Recipe(
        id = "r_chicken",
        name = "Grilled Chicken Breast with Rice",
        description = "Lean chicken breast with steamed jasmine rice.",
        prepTimeMinutes = 10,
        cookTimeMinutes = 20,
        servings = 1,
        calories = 500f,
        protein = 45f,
        carbs = 45f,
        fat = 10f,
        difficulty = "Easy",
        dietaryTags = listOf("High Protein", "Dinner"),
        allergyTags = listOf("Gluten-Free", "Dairy-Free"),
        ingredients = listOf(
            RecipeIngredient(id = "i1", recipeId = "r_chicken", name = "Chicken Breast", quantity = 200f, unit = "g"),
            RecipeIngredient(id = "i2", recipeId = "r_chicken", name = "Jasmine Rice", quantity = 100f, unit = "g")
        )
    )

    private val paneerRecipe = Recipe(
        id = "r_paneer",
        name = "Paneer Tikka Bowl",
        description = "Cottage cheese cubes with bell peppers and spices.",
        prepTimeMinutes = 15,
        cookTimeMinutes = 15,
        servings = 1,
        calories = 420f,
        protein = 26f,
        carbs = 20f,
        fat = 24f,
        difficulty = "Easy",
        dietaryTags = listOf("Vegetarian", "Indian", "High Protein"),
        allergyTags = listOf("Gluten-Free", "Nut-Free"),
        ingredients = listOf(
            RecipeIngredient(id = "i3", recipeId = "r_paneer", name = "Paneer (Cottage Cheese)", quantity = 200f, unit = "g"),
            RecipeIngredient(id = "i4", recipeId = "r_paneer", name = "Bell Peppers", quantity = 100f, unit = "g")
        )
    )

    private val peanutRecipe = Recipe(
        id = "r_peanut",
        name = "Peanut Butter Oats",
        description = "Oatmeal topped with crunchy peanut butter.",
        prepTimeMinutes = 5,
        cookTimeMinutes = 5,
        servings = 1,
        calories = 380f,
        protein = 16f,
        carbs = 42f,
        fat = 16f,
        difficulty = "Easy",
        dietaryTags = listOf("Vegetarian", "Quick", "Breakfast"),
        allergyTags = listOf("Contains Peanuts"),
        ingredients = listOf(
            RecipeIngredient(id = "i5", recipeId = "r_peanut", name = "Rolled Oats", quantity = 60f, unit = "g"),
            RecipeIngredient(id = "i6", recipeId = "r_peanut", name = "Peanut Butter", quantity = 30f, unit = "g")
        )
    )

    @Test
    fun allergySafety_mandatoryExclusion_excludesPeanutRecipeForPeanutAllergy() {
        val userProfile = UserProfile(
            userId = "user_1",
            name = "Alex",
            age = 28,
            gender = "Male",
            heightCm = 178f,
            weightKg = 75f,
            goal = "Maintain weight",
            dietaryPreference = "Vegetarian",
            allergies = "Peanuts",
            activityLevel = "Moderately active"
        )

        val recommendations = useCase(
            recipes = listOf(chickenRecipe, paneerRecipe, peanutRecipe),
            userProfile = userProfile,
            pantryItems = emptyList(),
            consumedNutrition = null,
            dailyTargets = NutritionTargets(2200f, 135f, 250f, 65f)
        )

        // Peanut recipe must be excluded due to allergy
        assertFalse(recommendations.any { it.recipe.id == "r_peanut" })
        // Chicken recipe must be excluded due to Vegetarian preference
        assertFalse(recommendations.any { it.recipe.id == "r_chicken" })
        // Only paneer recipe should be recommended
        assertEquals(1, recommendations.size)
        assertEquals("r_paneer", recommendations.first().recipe.id)
    }

    @Test
    fun pantryMatching_boostsScoreWhenPantryIngredientsAvailable() {
        val userProfile = UserProfile(
            userId = "user_1",
            name = "Alex",
            age = 28,
            gender = "Male",
            heightCm = 178f,
            weightKg = 75f,
            goal = "Build muscle",
            dietaryPreference = "High Protein",
            allergies = "None",
            activityLevel = "Very active"
        )

        val foodChicken = FoodItem("f1", "Chicken Breast", 165f, 31f, 0f, 3.6f, 100f, "g")
        val foodRice = FoodItem("f2", "Jasmine Rice", 130f, 2.7f, 28f, 0.3f, 100f, "g")

        val pantryItems = listOf(
            PantryItem("p1", "user_1", foodChicken, 500f, "g", expiryDate = System.currentTimeMillis() + 86400000L * 2, dateAdded = System.currentTimeMillis()),
            PantryItem("p2", "user_1", foodRice, 1000f, "g", expiryDate = System.currentTimeMillis() + 86400000L * 30, dateAdded = System.currentTimeMillis())
        )

        val recommendations = useCase(
            recipes = listOf(chickenRecipe, paneerRecipe),
            userProfile = userProfile,
            pantryItems = pantryItems,
            consumedNutrition = DailyNutrition(0f, 0f, 0f, 0f),
            dailyTargets = NutritionTargets(2500f, 150f, 300f, 70f)
        )

        val chickenSuggestion = recommendations.first { it.recipe.id == "r_chicken" }
        // Both chicken and rice are in the pantry (100% pantry match)
        assertEquals(2, chickenSuggestion.matchedPantryCount)
        assertEquals(2, chickenSuggestion.totalIngredientsCount)
        assertTrue(chickenSuggestion.pantryScore >= 45f)
        assertTrue(chickenSuggestion.matchScore >= 80)
        // Check expiring ingredients detected
        assertTrue(chickenSuggestion.expiringIngredients.contains("Chicken Breast"))
    }
}
