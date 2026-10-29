package com.monteirogames.nutriverse.ui.screens.mealtracker

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.monteirogames.nutriverse.domain.model.CalculatedNutrition
import com.monteirogames.nutriverse.domain.model.CatalogFoodItem
import com.monteirogames.nutriverse.domain.model.CatalogServingPreset
import com.monteirogames.nutriverse.domain.usecase.AuthUseCases
import com.monteirogames.nutriverse.domain.usecase.FoodCatalogUseCases
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

data class FoodCatalogUiState(
    val searchQuery: String = "",
    val selectedCategory: String? = null,
    val categories: List<String> = emptyList(),
    val filteredFoods: List<CatalogFoodItem> = emptyList(),
    val selectedFood: CatalogFoodItem? = null,
    val quantityInput: String = "100",
    val selectedMealType: String = "Lunch",
    val calculatedNutrition: CalculatedNutrition = CalculatedNutrition(0f, 0f, 0f, 0f),
    val isAdding: Boolean = false,
    val isAddedSuccess: Boolean = false,
    val errorMessage: String? = null
)

class FoodCatalogViewModel(
    private val authUseCases: AuthUseCases,
    private val foodCatalogUseCases: FoodCatalogUseCases
) : ViewModel() {

    private val _uiState = MutableStateFlow(FoodCatalogUiState())
    val uiState: StateFlow<FoodCatalogUiState> = _uiState.asStateFlow()

    private var currentUserId: String = "guest_user"

    init {
        val cats = listOf("All") + foodCatalogUseCases.getCategories()
        val allFoods = foodCatalogUseCases.searchFoods()
        _uiState.value = _uiState.value.copy(
            categories = cats,
            filteredFoods = allFoods
        )

        viewModelScope.launch {
            authUseCases.getCurrentUserFlow().collectLatest { userEmail ->
                currentUserId = authUseCases.getCurrentUserId() ?: userEmail ?: "guest_user"
            }
        }
    }

    fun onQueryChanged(query: String) {
        val cat = _uiState.value.selectedCategory?.takeIf { !it.equals("All", ignoreCase = true) }
        val filtered = foodCatalogUseCases.searchFoods(query, cat)
        _uiState.value = _uiState.value.copy(
            searchQuery = query,
            filteredFoods = filtered
        )
    }

    fun onCategorySelected(category: String?) {
        val effectiveCat = if (category.isNullOrBlank() || category.equals("All", ignoreCase = true)) null else category
        val filtered = foodCatalogUseCases.searchFoods(_uiState.value.searchQuery, effectiveCat)
        _uiState.value = _uiState.value.copy(
            selectedCategory = category,
            filteredFoods = filtered
        )
    }

    fun onFoodSelected(food: CatalogFoodItem?) {
        if (food == null) {
            _uiState.value = _uiState.value.copy(
                selectedFood = null,
                calculatedNutrition = CalculatedNutrition(0f, 0f, 0f, 0f)
            )
            return
        }

        val defaultGrams = food.defaultQuantityGrams
        val calculated = food.calculateNutrition(defaultGrams)
        _uiState.value = _uiState.value.copy(
            selectedFood = food,
            quantityInput = if (defaultGrams % 1f == 0f) defaultGrams.toInt().toString() else defaultGrams.toString(),
            calculatedNutrition = calculated,
            errorMessage = null
        )
    }

    fun onQuantityChanged(input: String) {
        val food = _uiState.value.selectedFood
        val grams = input.toFloatOrNull() ?: 0f
        val calculated = food?.calculateNutrition(grams) ?: CalculatedNutrition(0f, 0f, 0f, 0f)
        _uiState.value = _uiState.value.copy(
            quantityInput = input,
            calculatedNutrition = calculated
        )
    }

    fun onPresetSelected(preset: CatalogServingPreset) {
        val food = _uiState.value.selectedFood
        val grams = preset.grams
        val calculated = food?.calculateNutrition(grams) ?: CalculatedNutrition(0f, 0f, 0f, 0f)
        _uiState.value = _uiState.value.copy(
            quantityInput = if (grams % 1f == 0f) grams.toInt().toString() else grams.toString(),
            calculatedNutrition = calculated
        )
    }

    fun onMealTypeSelected(mealType: String) {
        _uiState.value = _uiState.value.copy(selectedMealType = mealType)
    }

    fun addMealToTracker(date: Long = System.currentTimeMillis()) {
        val food = _uiState.value.selectedFood ?: return
        val grams = _uiState.value.quantityInput.toFloatOrNull()

        if (grams == null || grams <= 0f) {
            _uiState.value = _uiState.value.copy(errorMessage = "Please enter a valid quantity in grams (greater than 0).")
            return
        }
        if (grams > 5000f) {
            _uiState.value = _uiState.value.copy(errorMessage = "Quantity cannot exceed 5000g.")
            return
        }

        if (_uiState.value.isAdding) return // Guard against duplicate multi-taps

        _uiState.value = _uiState.value.copy(isAdding = true, errorMessage = null)

        viewModelScope.launch {
            val result = foodCatalogUseCases.addCatalogFoodToMeal(
                userId = currentUserId,
                catalogFood = food,
                quantityInGrams = grams,
                mealType = _uiState.value.selectedMealType,
                date = date
            )

            result.fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(
                        isAdding = false,
                        isAddedSuccess = true,
                        selectedFood = null
                    )
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isAdding = false,
                        errorMessage = error.localizedMessage ?: "Failed to add meal"
                    )
                }
            )
        }
    }

    fun clearSuccess() {
        _uiState.value = _uiState.value.copy(isAddedSuccess = false)
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
}
