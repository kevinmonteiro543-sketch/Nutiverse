package com.monteirogames.nutriverse.ui.screens.mealtracker

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.monteirogames.nutriverse.domain.model.FoodItem
import com.monteirogames.nutriverse.domain.model.MealEntry
import com.monteirogames.nutriverse.domain.usecase.AuthUseCases
import com.monteirogames.nutriverse.domain.usecase.MealTrackerUseCases
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

class AddMealViewModel(
    private val authUseCases: AuthUseCases,
    private val mealTrackerUseCases: MealTrackerUseCases
) : ViewModel() {

    private val _isSaved = MutableStateFlow(false)
    val isSaved: StateFlow<Boolean> = _isSaved.asStateFlow()
    
    private val _initialMeal = MutableStateFlow<MealEntry?>(null)
    val initialMeal: StateFlow<MealEntry?> = _initialMeal.asStateFlow()
    
    private var editingMealId: String? = null
    
    fun loadMeal(mealId: String?) {
        if (mealId == null) return
        val userId = authUseCases.getCurrentUserId() ?: return
        editingMealId = mealId
        
        viewModelScope.launch {
            val meal = mealTrackerUseCases.getMeal(mealId, userId)
            _initialMeal.value = meal
        }
    }

    fun saveMeal(
        foodName: String,
        calories: String,
        protein: String,
        carbs: String,
        fat: String,
        servingSize: String,
        servingUnit: String,
        quantity: String,
        mealType: String
    ) {
        val userId = authUseCases.getCurrentUserId() ?: return
        
        val initial = _initialMeal.value
        val foodId = initial?.foodItem?.id ?: UUID.randomUUID().toString()
        val mealId = initial?.id ?: UUID.randomUUID().toString()
        val date = initial?.date ?: System.currentTimeMillis()
        val loggedAt = initial?.loggedAt ?: System.currentTimeMillis()
        
        val foodItem = FoodItem(
            id = foodId,
            name = foodName,
            calories = calories.toFloatOrNull() ?: 0f,
            protein = protein.toFloatOrNull() ?: 0f,
            carbs = carbs.toFloatOrNull() ?: 0f,
            fat = fat.toFloatOrNull() ?: 0f,
            servingSize = servingSize.toFloatOrNull() ?: 100f,
            servingUnit = servingUnit.ifBlank { "g" }
        )
        val mealEntry = MealEntry(
            id = mealId,
            userId = userId,
            foodItem = foodItem,
            date = date,
            mealType = mealType,
            quantity = quantity.toFloatOrNull() ?: 100f,
            loggedAt = loggedAt
        )

        viewModelScope.launch {
            if (editingMealId != null) {
                mealTrackerUseCases.updateMeal(mealEntry)
            } else {
                mealTrackerUseCases.addMeal(mealEntry)
            }
            _isSaved.value = true
        }
    }
}
