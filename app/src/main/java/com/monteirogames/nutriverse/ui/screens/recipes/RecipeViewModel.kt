package com.monteirogames.nutriverse.ui.screens.recipes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.monteirogames.nutriverse.domain.model.FoodItem
import com.monteirogames.nutriverse.domain.model.MealEntry
import com.monteirogames.nutriverse.domain.model.PantryItem
import com.monteirogames.nutriverse.domain.model.Recipe
import com.monteirogames.nutriverse.domain.model.RecipeSuggestion
import com.monteirogames.nutriverse.domain.model.UserProfile
import com.monteirogames.nutriverse.domain.usecase.AuthUseCases
import com.monteirogames.nutriverse.domain.usecase.DailyNutrition
import com.monteirogames.nutriverse.domain.usecase.MealTrackerUseCases
import com.monteirogames.nutriverse.domain.usecase.NutritionTargets
import com.monteirogames.nutriverse.domain.usecase.PantryUseCases
import com.monteirogames.nutriverse.domain.usecase.RecipeUseCases
import com.monteirogames.nutriverse.domain.usecase.GroceryUseCases
import com.monteirogames.nutriverse.domain.usecase.UserProfileUseCases
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.UUID

data class RecipeListUiState(
    val isLoading: Boolean = true,
    val allRecipes: List<Recipe> = emptyList(),
    val filteredRecipes: List<Recipe> = emptyList(),
    val suggestions: List<RecipeSuggestion> = emptyList(),
    val suggestionMap: Map<String, RecipeSuggestion> = emptyMap(),
    val searchQuery: String = "",
    val selectedCategory: String = "All",
    val pantryItemsCount: Int = 0,
    val userProfile: UserProfile? = null,
    val dailyTargets: NutritionTargets? = null,
    val consumedNutrition: DailyNutrition? = null
)

class RecipeViewModel(
    private val recipeUseCases: RecipeUseCases,
    private val mealTrackerUseCases: MealTrackerUseCases,
    private val authUseCases: AuthUseCases,
    private val userProfileUseCases: UserProfileUseCases? = null,
    private val pantryUseCases: PantryUseCases? = null,
    private val groceryUseCases: GroceryUseCases? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(RecipeListUiState())
    val uiState: StateFlow<RecipeListUiState> = _uiState.asStateFlow()

    val categories = listOf(
        "All",
        "Smart Suggestions",
        "High Protein",
        "Vegetarian",
        "Indian",
        "Quick",
        "Breakfast",
        "Lunch",
        "Dinner",
        "Snacks",
        "Favorites"
    )

    init {
        // Seed initial recipes if database is empty on start
        viewModelScope.launch {
            recipeUseCases.seedRecipes()
        }

        val userId = authUseCases.getCurrentUserId()
        if (userId != null && userProfileUseCases != null && pantryUseCases != null) {
            val todayStart = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis

            viewModelScope.launch {
                combine(
                    recipeUseCases.getAllRecipes(),
                    userProfileUseCases.getUserProfile(userId),
                    pantryUseCases.observePantryItems(userId),
                    mealTrackerUseCases.calculateDailyNutrition(userId, todayStart)
                ) { recipes, profile, pantryItems, dailyConsumed ->
                    val targets = if (profile != null) {
                        userProfileUseCases.calculateNutritionTargets(profile)
                    } else null

                    val suggestions = recipeUseCases.getSmartRecipeSuggestions(
                        recipes = recipes,
                        userProfile = profile,
                        pantryItems = pantryItems,
                        consumedNutrition = dailyConsumed,
                        dailyTargets = targets
                    )

                    SuggestionBundle(
                        recipes = recipes,
                        profile = profile,
                        pantryItems = pantryItems,
                        dailyConsumed = dailyConsumed,
                        targets = targets,
                        suggestions = suggestions
                    )
                }.collectLatest { bundle ->
                    val current = _uiState.value
                    val suggestionMap = bundle.suggestions.associateBy { it.recipe.id }
                    val filtered = applyFilter(
                        recipes = bundle.recipes,
                        suggestions = bundle.suggestions,
                        query = current.searchQuery,
                        category = current.selectedCategory
                    )
                    _uiState.value = current.copy(
                        isLoading = false,
                        allRecipes = bundle.recipes,
                        suggestions = bundle.suggestions,
                        suggestionMap = suggestionMap,
                        filteredRecipes = filtered,
                        pantryItemsCount = bundle.pantryItems.size,
                        userProfile = bundle.profile,
                        dailyTargets = bundle.targets,
                        consumedNutrition = bundle.dailyConsumed
                    )
                }
            }
        } else {
            // Fallback if userId or profile/pantry not injected
            viewModelScope.launch {
                recipeUseCases.getAllRecipes().collectLatest { recipes ->
                    val current = _uiState.value
                    val filtered = applyFilter(recipes, emptyList(), current.searchQuery, current.selectedCategory)
                    _uiState.value = current.copy(
                        isLoading = false,
                        allRecipes = recipes,
                        filteredRecipes = filtered
                    )
                }
            }
        }
    }

    private data class SuggestionBundle(
        val recipes: List<Recipe>,
        val profile: UserProfile?,
        val pantryItems: List<PantryItem>,
        val dailyConsumed: DailyNutrition,
        val targets: NutritionTargets?,
        val suggestions: List<RecipeSuggestion>
    )

    fun onSearchQueryChange(query: String) {
        val current = _uiState.value
        val filtered = applyFilter(current.allRecipes, current.suggestions, query, current.selectedCategory)
        _uiState.value = current.copy(
            searchQuery = query,
            filteredRecipes = filtered
        )
    }

    fun onCategorySelect(category: String) {
        val current = _uiState.value
        val filtered = applyFilter(current.allRecipes, current.suggestions, current.searchQuery, category)
        _uiState.value = current.copy(
            selectedCategory = category,
            filteredRecipes = filtered
        )
    }

    fun toggleFavorite(recipeId: String, currentFavorite: Boolean) {
        viewModelScope.launch {
            recipeUseCases.toggleRecipeFavorite(recipeId, !currentFavorite)
        }
    }

    fun getRecipeById(recipeId: String): Flow<Recipe?> {
        return recipeUseCases.getRecipeById(recipeId)
    }

    fun getSuggestionForRecipe(recipeId: String): RecipeSuggestion? {
        return _uiState.value.suggestionMap[recipeId]
    }

    fun addMissingIngredientsToGrocery(
        recipe: Recipe,
        onSuccess: (count: Int) -> Unit
    ) {
        val userId = authUseCases.getCurrentUserId() ?: return
        val gUseCases = groceryUseCases ?: return
        val pUseCases = pantryUseCases ?: return

        viewModelScope.launch {
            val pantryItems = pUseCases.observePantryItems(userId).first()
            val addedCount = gUseCases.addMissingRecipeIngredients(userId, recipe, pantryItems)
            onSuccess(addedCount)
        }
    }

    fun logRecipeMeal(
        recipe: Recipe,
        servings: Int,
        mealType: String,
        onSuccess: () -> Unit
    ) {
        val userId = authUseCases.getCurrentUserId() ?: return
        val baseServings = recipe.servings.coerceAtLeast(1).toFloat()
        val ratio = servings.toFloat() / baseServings

        val foodItem = FoodItem(
            id = UUID.randomUUID().toString(),
            name = recipe.name,
            calories = recipe.calories * ratio,
            protein = recipe.protein * ratio,
            carbs = recipe.carbs * ratio,
            fat = recipe.fat * ratio,
            servingSize = servings.toFloat(),
            servingUnit = if (servings == 1) "serving" else "servings"
        )

        val mealEntry = MealEntry(
            id = UUID.randomUUID().toString(),
            userId = userId,
            foodItem = foodItem,
            date = System.currentTimeMillis(),
            mealType = mealType,
            quantity = servings.toFloat(),
            loggedAt = System.currentTimeMillis()
        )

        viewModelScope.launch {
            mealTrackerUseCases.addMeal(mealEntry)
            onSuccess()
        }
    }

    private fun applyFilter(
        recipes: List<Recipe>,
        suggestions: List<RecipeSuggestion>,
        query: String,
        category: String
    ): List<Recipe> {
        val baseList = if (category == "Smart Suggestions") {
            suggestions.map { it.recipe }
        } else {
            recipes
        }

        return baseList.filter { recipe ->
            val matchesCategory = when (category) {
                "All" -> true
                "Smart Suggestions" -> true // baseList is already suggestions
                "Favorites" -> recipe.isFavorite
                "High Protein" -> recipe.dietaryTags.any { it.equals("High Protein", ignoreCase = true) }
                "Vegetarian" -> recipe.dietaryTags.any { it.equals("Vegetarian", ignoreCase = true) }
                "Indian" -> recipe.dietaryTags.any { it.equals("Indian", ignoreCase = true) }
                "Quick" -> recipe.dietaryTags.any { it.equals("Quick", ignoreCase = true) } || (recipe.prepTimeMinutes + recipe.cookTimeMinutes) <= 20
                "Breakfast" -> recipe.dietaryTags.any { it.equals("Breakfast", ignoreCase = true) }
                "Lunch" -> recipe.dietaryTags.any { it.equals("Lunch", ignoreCase = true) }
                "Dinner" -> recipe.dietaryTags.any { it.equals("Dinner", ignoreCase = true) }
                "Snacks" -> recipe.dietaryTags.any { it.equals("Snacks", ignoreCase = true) }
                else -> true
            }

            val matchesQuery = if (query.isBlank()) {
                true
            } else {
                val q = query.trim().lowercase()
                recipe.name.lowercase().contains(q) ||
                recipe.description.lowercase().contains(q) ||
                recipe.dietaryTags.any { it.lowercase().contains(q) } ||
                recipe.ingredients.any { it.name.lowercase().contains(q) }
            }

            matchesCategory && matchesQuery
        }
    }
}
