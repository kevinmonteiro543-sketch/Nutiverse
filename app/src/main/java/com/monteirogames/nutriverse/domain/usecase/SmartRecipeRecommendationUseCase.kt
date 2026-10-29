package com.monteirogames.nutriverse.domain.usecase

import com.monteirogames.nutriverse.domain.model.PantryItem
import com.monteirogames.nutriverse.domain.model.Recipe
import com.monteirogames.nutriverse.domain.model.RecipeSuggestion
import com.monteirogames.nutriverse.domain.model.UserProfile
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

class SmartRecipeRecommendationUseCase {

    operator fun invoke(
        recipes: List<Recipe>,
        userProfile: UserProfile?,
        pantryItems: List<PantryItem>,
        consumedNutrition: DailyNutrition?,
        dailyTargets: NutritionTargets?
    ): List<RecipeSuggestion> {
        if (recipes.isEmpty()) return emptyList()

        val now = System.currentTimeMillis()
        val expiringThresholdMs = now + 4L * 24 * 60 * 60 * 1000L // 4 days

        // Map pantry items with normalized name
        val pantryItemMap = pantryItems.associate { item ->
            normalizeFoodName(item.foodItem.name) to item
        }
        val pantryKeys = pantryItemMap.keys.toList()

        // Parse user allergies
        val userAllergies = parseAllergies(userProfile?.allergies ?: "")

        val suggestions = mutableListOf<RecipeSuggestion>()

        for (recipe in recipes) {
            // 1. ALLERGY SAFETY (Mandatory Exclusion)
            val allergyConflict = checkAllergyConflict(recipe, userAllergies)
            if (allergyConflict != null) {
                // Strict exclusion: never recommend recipes with allergy conflict
                continue
            }

            // 2. PANTRY MATCH (50% = max 50 points)
            val totalIngredients = recipe.ingredients.size.coerceAtLeast(1)
            val matchedIngredients = mutableListOf<String>()
            val missingIngredients = mutableListOf<String>()
            val expiringUsed = mutableListOf<String>()

            for (ingredient in recipe.ingredients) {
                val ingName = ingredient.name.trim()
                val normIng = normalizeFoodName(ingName)

                val matchedKey = pantryKeys.firstOrNull { pKey ->
                    isIngredientMatch(normIng, pKey)
                }

                if (matchedKey != null) {
                    matchedIngredients.add(ingName)
                    val pItem = pantryItemMap[matchedKey]
                    if (pItem != null && pItem.expiryDate in 1..expiringThresholdMs) {
                        expiringUsed.add(ingName)
                    }
                } else {
                    missingIngredients.add(ingName)
                }
            }

            val pantryRatio = matchedIngredients.size.toFloat() / totalIngredients.toFloat()
            var pantryScore = pantryRatio * 50f
            // Bonus for using expiring ingredients (up to +3 within 50 cap)
            if (expiringUsed.isNotEmpty()) {
                pantryScore = min(50f, pantryScore + (expiringUsed.size * 2f))
            }

            // 3. DIET COMPATIBILITY (20% = max 20 points)
            val dietEval = evaluateDietCompatibility(recipe, userProfile?.dietaryPreference)
            if (dietEval.isExcluded) {
                // Excluded by dietary constraint (e.g. Vegetarian given meat recipe)
                continue
            }

            // 4. NUTRITION FIT (20% = max 20 points)
            val nutritionEval = evaluateNutritionFit(recipe, dailyTargets, consumedNutrition)

            // 5. GOAL FIT (10% = max 10 points)
            val goalEval = evaluateGoalFit(recipe, userProfile?.goal)

            val totalScore = (pantryScore + dietEval.score + nutritionEval.score + goalEval.score)
                .roundToInt()
                .coerceIn(0, 100)

            val highlights = mutableListOf<String>()
            if (matchedIngredients.isNotEmpty()) {
                if (matchedIngredients.size == totalIngredients) {
                    highlights.add("All ingredients in pantry!")
                } else {
                    highlights.add("${matchedIngredients.size} of $totalIngredients in pantry")
                }
            }
            if (expiringUsed.isNotEmpty()) {
                highlights.add("Uses expiring: ${expiringUsed.joinToString(", ")}")
            }
            if (!dietEval.highlight.isNullOrBlank()) {
                highlights.add(dietEval.highlight)
            }
            if (!nutritionEval.highlight.isNullOrBlank()) {
                highlights.add(nutritionEval.highlight)
            }
            if (!goalEval.highlight.isNullOrBlank()) {
                highlights.add(goalEval.highlight)
            }

            suggestions.add(
                RecipeSuggestion(
                    recipe = recipe,
                    matchScore = totalScore,
                    pantryScore = pantryScore,
                    dietScore = dietEval.score,
                    nutritionScore = nutritionEval.score,
                    goalScore = goalEval.score,
                    matchedPantryCount = matchedIngredients.size,
                    totalIngredientsCount = totalIngredients,
                    matchedIngredients = matchedIngredients,
                    missingIngredients = missingIngredients,
                    expiringIngredients = expiringUsed,
                    highlights = highlights
                )
            )
        }

        // Return sorted by highest match score, then highest pantry match, then shortest cooking time
        return suggestions.sortedWith(
            compareByDescending<RecipeSuggestion> { it.matchScore }
                .thenByDescending { it.matchedPantryCount }
                .thenBy { it.recipe.prepTimeMinutes + it.recipe.cookTimeMinutes }
        )
    }

    private data class DietEvalResult(
        val score: Float,
        val highlight: String?,
        val isExcluded: Boolean = false
    )

    private fun evaluateDietCompatibility(recipe: Recipe, preference: String?): DietEvalResult {
        if (preference.isNullOrBlank() || preference.equals("None", ignoreCase = true) || preference.equals("No Preference", ignoreCase = true)) {
            return DietEvalResult(score = 15f, highlight = null)
        }

        val pref = preference.trim().lowercase()
        val tagsLower = recipe.dietaryTags.map { it.lowercase() }
        val recipeText = (recipe.name + " " + recipe.description + " " + recipe.ingredients.joinToString(" ") { it.name }).lowercase()

        val isVegetarianRecipe = tagsLower.contains("vegetarian") || tagsLower.contains("vegan")
        val isNonVeg = recipeText.contains("chicken") || recipeText.contains("meat") || recipeText.contains("fish") ||
                recipeText.contains("salmon") || recipeText.contains("tuna") || recipeText.contains("beef") ||
                recipeText.contains("pork") || recipeText.contains("shrimp")

        return when {
            pref.contains("vegetarian") -> {
                if (isNonVeg) {
                    DietEvalResult(score = 0f, highlight = null, isExcluded = true)
                } else if (isVegetarianRecipe) {
                    DietEvalResult(score = 20f, highlight = "Matches Vegetarian diet")
                } else {
                    DietEvalResult(score = 12f, highlight = null)
                }
            }
            pref.contains("vegan") -> {
                val hasAnimal = isNonVeg || recipeText.contains("paneer") || recipeText.contains("cheese") ||
                        recipeText.contains("milk") || recipeText.contains("yogurt") || recipeText.contains("egg") ||
                        recipeText.contains("butter")
                if (hasAnimal) {
                    DietEvalResult(score = 0f, highlight = null, isExcluded = true)
                } else {
                    DietEvalResult(score = 20f, highlight = "100% Plant-based / Vegan")
                }
            }
            pref.contains("high protein") -> {
                if (recipe.protein >= 35f || tagsLower.contains("high protein")) {
                    DietEvalResult(score = 20f, highlight = "High protein recipe (${recipe.protein.roundToInt()}g)")
                } else if (recipe.protein >= 25f) {
                    DietEvalResult(score = 15f, highlight = "Good protein source")
                } else {
                    DietEvalResult(score = 10f, highlight = null)
                }
            }
            pref.contains("keto") || pref.contains("low carb") -> {
                if (recipe.carbs <= 15f) {
                    DietEvalResult(score = 20f, highlight = "Keto / Low-Carb friendly")
                } else if (recipe.carbs <= 25f) {
                    DietEvalResult(score = 14f, highlight = "Moderate carbs (${recipe.carbs.roundToInt()}g)")
                } else {
                    DietEvalResult(score = 6f, highlight = null)
                }
            }
            pref.contains("pescatarian") -> {
                val isRedMeatOrPoultry = recipeText.contains("chicken") || recipeText.contains("meat") ||
                        recipeText.contains("beef") || recipeText.contains("pork")
                if (isRedMeatOrPoultry) {
                    DietEvalResult(score = 0f, highlight = null, isExcluded = true)
                } else {
                    DietEvalResult(score = 20f, highlight = "Pescatarian friendly")
                }
            }
            else -> DietEvalResult(score = 16f, highlight = null)
        }
    }

    private data class NutritionEvalResult(
        val score: Float,
        val highlight: String?
    )

    private fun evaluateNutritionFit(
        recipe: Recipe,
        targets: NutritionTargets?,
        consumed: DailyNutrition?
    ): NutritionEvalResult {
        if (targets == null || targets.calories <= 0f) {
            return NutritionEvalResult(score = 14f, highlight = null)
        }

        val remainingCalories = max(0f, targets.calories - (consumed?.calories ?: 0f))
        val remainingProtein = max(0f, targets.protein - (consumed?.protein ?: 0f))

        var score = 0f
        var highlight: String? = null

        // 1. Calorie fit (max 10 pts)
        if (remainingCalories > 150f) {
            val calRatio = recipe.calories / remainingCalories
            if (calRatio in 0.25f..0.85f) {
                score += 10f
                highlight = "Fits calorie budget"
            } else if (calRatio < 0.25f) {
                score += 7f
            } else if (calRatio <= 1.1f) {
                score += 6f
                highlight = "Uses remaining calories"
            } else {
                score += 3f
            }
        } else {
            // Very low remaining calories: prefer snack / light meal
            if (recipe.calories <= 250f) {
                score += 10f
                highlight = "Light meal for low calorie budget"
            } else {
                score += 3f
            }
        }

        // 2. Protein fit (max 10 pts)
        if (remainingProtein > 5f) {
            val proteinCoverRatio = (recipe.protein / remainingProtein).coerceIn(0f, 1.2f)
            if (proteinCoverRatio >= 0.5f) {
                score += 10f
                val pct = min(100, (proteinCoverRatio * 100).roundToInt())
                highlight = "Fills $pct% of remaining protein"
            } else if (proteinCoverRatio >= 0.25f) {
                score += 7f
            } else {
                score += 4f
            }
        } else {
            // Protein target already met
            score += 8f
        }

        return NutritionEvalResult(score = score.coerceIn(0f, 20f), highlight = highlight)
    }

    private data class GoalEvalResult(
        val score: Float,
        val highlight: String?
    )

    private fun evaluateGoalFit(recipe: Recipe, goal: String?): GoalEvalResult {
        if (goal.isNullOrBlank()) return GoalEvalResult(score = 8f, highlight = null)

        val g = goal.trim().lowercase()
        return when {
            g.contains("lose") -> {
                if (recipe.calories <= 480f && recipe.protein >= 24f) {
                    GoalEvalResult(score = 10f, highlight = "Great for Weight Loss")
                } else if (recipe.calories <= 550f) {
                    GoalEvalResult(score = 8f, highlight = null)
                } else {
                    GoalEvalResult(score = 5f, highlight = null)
                }
            }
            g.contains("muscle") || g.contains("gain") -> {
                if (recipe.protein >= 30f && recipe.calories >= 450f) {
                    GoalEvalResult(score = 10f, highlight = "Optimal for Muscle Building")
                } else if (recipe.protein >= 22f) {
                    GoalEvalResult(score = 8f, highlight = null)
                } else {
                    GoalEvalResult(score = 5f, highlight = null)
                }
            }
            g.contains("maintain") -> {
                if (recipe.calories in 380f..620f) {
                    GoalEvalResult(score = 10f, highlight = "Balanced for Maintenance")
                } else {
                    GoalEvalResult(score = 7f, highlight = null)
                }
            }
            else -> GoalEvalResult(score = 8f, highlight = null)
        }
    }

    private fun checkAllergyConflict(recipe: Recipe, userAllergies: List<String>): String? {
        if (userAllergies.isEmpty()) return null

        val recipeIngredientsText = recipe.ingredients.joinToString(" ") { it.name }.lowercase()
        val recipeNameAndDesc = "${recipe.name} ${recipe.description}".lowercase()
        val allergyTagsLower = recipe.allergyTags.map { it.lowercase() }

        for (allergy in userAllergies) {
            when (allergy) {
                "peanut", "peanuts" -> {
                    if (recipeIngredientsText.contains("peanut") || recipeNameAndDesc.contains("peanut")) {
                        return "Contains peanuts"
                    }
                }
                "nut", "nuts", "tree nut", "tree nuts" -> {
                    val nutTerms = listOf("nut", "almond", "walnut", "cashew", "pistachio", "hazelnut", "pecan")
                    if (nutTerms.any { recipeIngredientsText.contains(it) || recipeNameAndDesc.contains(it) }) {
                        return "Contains tree nuts"
                    }
                }
                "dairy", "milk", "lactose" -> {
                    val dairyTerms = listOf("dairy", "milk", "cheese", "paneer", "yogurt", "butter", "cream", "curd", "whey")
                    val isDairyFree = allergyTagsLower.contains("dairy-free")
                    if (!isDairyFree && dairyTerms.any { recipeIngredientsText.contains(it) || recipeNameAndDesc.contains(it) }) {
                        return "Contains dairy"
                    }
                }
                "gluten", "wheat" -> {
                    val glutenTerms = listOf("gluten", "wheat", "flour", "bread", "pasta", "barley", "rye")
                    val isGlutenFree = allergyTagsLower.contains("gluten-free")
                    if (!isGlutenFree && glutenTerms.any { recipeIngredientsText.contains(it) || recipeNameAndDesc.contains(it) }) {
                        return "Contains gluten"
                    }
                }
                "egg", "eggs" -> {
                    if (recipeIngredientsText.contains("egg") || recipeNameAndDesc.contains("egg")) {
                        return "Contains eggs"
                    }
                }
                "shellfish", "seafood" -> {
                    val shellfishTerms = listOf("shellfish", "shrimp", "prawn", "crab", "lobster", "oyster", "clam", "scallop")
                    if (shellfishTerms.any { recipeIngredientsText.contains(it) || recipeNameAndDesc.contains(it) }) {
                        return "Contains shellfish"
                    }
                }
                "fish" -> {
                    val fishTerms = listOf("fish", "salmon", "tuna", "cod", "tilapia", "trout")
                    if (fishTerms.any { recipeIngredientsText.contains(it) || recipeNameAndDesc.contains(it) }) {
                        return "Contains fish"
                    }
                }
                "soy", "soya" -> {
                    val soyTerms = listOf("soy", "soya", "tofu", "edamame")
                    if (soyTerms.any { recipeIngredientsText.contains(it) || recipeNameAndDesc.contains(it) }) {
                        return "Contains soy"
                    }
                }
                else -> {
                    if (recipeIngredientsText.contains(allergy) || recipeNameAndDesc.contains(allergy)) {
                        return "Contains $allergy"
                    }
                }
            }
        }

        return null
    }

    private fun parseAllergies(raw: String): List<String> {
        if (raw.isBlank() || raw.equals("None", ignoreCase = true) || raw.equals("No", ignoreCase = true)) {
            return emptyList()
        }
        return raw.split(",", ";", "/")
            .map { it.trim().lowercase() }
            .filter { it.isNotEmpty() && it != "none" && it != "no allergies" }
    }

    private fun normalizeFoodName(name: String): String {
        return name.lowercase()
            .replace(Regex("[^a-z0-9\\s]"), " ")
            .split("\\s+".toRegex())
            .filter { it.length > 2 && it !in STOP_WORDS }
            .joinToString(" ")
    }

    private fun isIngredientMatch(normIng: String, normPantry: String): Boolean {
        if (normIng.isBlank() || normPantry.isBlank()) return false
        if (normIng == normPantry) return true
        if (normIng.contains(normPantry) || normPantry.contains(normIng)) return true

        // Match individual words (e.g. "quinoa" matches "cooked quinoa", "paneer" matches "paneer cottage cheese")
        val ingTokens = normIng.split(" ").filter { it.length >= 3 }
        val pantryTokens = normPantry.split(" ").filter { it.length >= 3 }

        for (itoken in ingTokens) {
            val isStem = itoken.removeSuffix("s").removeSuffix("es")
            for (ptoken in pantryTokens) {
                val pStem = ptoken.removeSuffix("s").removeSuffix("es")
                if (isStem == pStem || (isStem.length >= 4 && pStem.contains(isStem)) || (pStem.length >= 4 && isStem.contains(pStem))) {
                    return true
                }
            }
        }
        return false
    }

    companion object {
        private val STOP_WORDS = setOf(
            "fresh", "cooked", "raw", "chopped", "diced", "sliced", "ground",
            "organic", "whole", "powder", "paste", "puree", "sauce", "and", "the",
            "with", "for", "cup", "cups", "tbsp", "tsp", "gram", "grams", "medium", "small", "large"
        )
    }
}
