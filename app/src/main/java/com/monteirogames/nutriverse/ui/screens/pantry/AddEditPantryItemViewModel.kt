package com.monteirogames.nutriverse.ui.screens.pantry

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.monteirogames.nutriverse.domain.model.FoodItem
import com.monteirogames.nutriverse.domain.model.PantryItem
import com.monteirogames.nutriverse.domain.usecase.AuthUseCases
import com.monteirogames.nutriverse.domain.usecase.PantryUseCases
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

class AddEditPantryItemViewModel(
    private val authUseCases: AuthUseCases,
    private val pantryUseCases: PantryUseCases
) : ViewModel() {

    private val _isSaved = MutableStateFlow(false)
    val isSaved: StateFlow<Boolean> = _isSaved.asStateFlow()

    private val _initialItem = MutableStateFlow<PantryItem?>(null)
    val initialItem: StateFlow<PantryItem?> = _initialItem.asStateFlow()
    
    private var editingItemId: String? = null

    fun loadItem(itemId: String?) {
        if (itemId == null) return
        val userId = authUseCases.getCurrentUserId() ?: return
        editingItemId = itemId
        
        viewModelScope.launch {
            val item = pantryUseCases.getPantryItem(itemId, userId)
            _initialItem.value = item
        }
    }

    fun saveItem(
        foodName: String,
        quantity: String,
        unit: String,
        expiryDaysFromNow: String,
        calories: Float? = null,
        protein: Float? = null,
        carbs: Float? = null,
        fat: Float? = null,
        barcode: String? = null
    ) {
        val userId = authUseCases.getCurrentUserId() ?: return
        
        val initial = _initialItem.value
        val foodId = initial?.foodItem?.id ?: UUID.randomUUID().toString()
        val itemId = initial?.id ?: UUID.randomUUID().toString()
        val dateAdded = initial?.dateAdded ?: System.currentTimeMillis()
        
        val foodItem = FoodItem(
            id = foodId,
            name = foodName,
            calories = calories ?: initial?.foodItem?.calories ?: 0f,
            protein = protein ?: initial?.foodItem?.protein ?: 0f,
            carbs = carbs ?: initial?.foodItem?.carbs ?: 0f,
            fat = fat ?: initial?.foodItem?.fat ?: 0f,
            servingSize = initial?.foodItem?.servingSize ?: 100f,
            servingUnit = initial?.foodItem?.servingUnit ?: "g",
            barcode = barcode ?: initial?.foodItem?.barcode
        )
        
        val expiryMillis = if (expiryDaysFromNow.isNotBlank() && initial == null) {
            val days = expiryDaysFromNow.toLongOrNull() ?: 7L
            System.currentTimeMillis() + (days * 24 * 60 * 60 * 1000)
        } else if (expiryDaysFromNow.isNotBlank() && initial != null) {
            val days = expiryDaysFromNow.toLongOrNull() ?: 7L
            System.currentTimeMillis() + (days * 24 * 60 * 60 * 1000)
        } else {
            initial?.expiryDate ?: (System.currentTimeMillis() + (7L * 24 * 60 * 60 * 1000))
        }
        
        val pantryItem = PantryItem(
            id = itemId,
            userId = userId,
            foodItem = foodItem,
            quantity = quantity.toFloatOrNull() ?: 1f,
            unit = unit.ifBlank { "pcs" },
            expiryDate = expiryMillis,
            dateAdded = dateAdded
        )

        viewModelScope.launch {
            if (editingItemId != null) {
                pantryUseCases.updatePantryItem(pantryItem)
            } else {
                pantryUseCases.addPantryItem(pantryItem)
            }
            _isSaved.value = true
        }
    }
}
