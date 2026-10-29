package com.monteirogames.nutriverse.ui.screens.mealtracker

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.monteirogames.nutriverse.domain.model.StructuredFoodEstimate
import com.monteirogames.nutriverse.domain.usecase.AnalyzeFoodImageUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

import com.monteirogames.nutriverse.domain.usecase.AuthUseCases
import com.monteirogames.nutriverse.domain.usecase.MealTrackerUseCases
import com.monteirogames.nutriverse.domain.model.FoodItem
import com.monteirogames.nutriverse.domain.model.MealEntry
import java.util.UUID

sealed class AiFoodRecognitionState {
    object Idle : AiFoodRecognitionState()
    object Analyzing : AiFoodRecognitionState()
    data class Success(val estimate: StructuredFoodEstimate) : AiFoodRecognitionState()
    data class Error(val message: String) : AiFoodRecognitionState()
}

class AiFoodRecognitionViewModel(
    private val analyzeFoodImageUseCase: AnalyzeFoodImageUseCase,
    private val mealTrackerUseCases: MealTrackerUseCases,
    private val authUseCases: AuthUseCases
) : ViewModel() {

    private val _uiState = MutableStateFlow<AiFoodRecognitionState>(AiFoodRecognitionState.Idle)
    val uiState: StateFlow<AiFoodRecognitionState> = _uiState.asStateFlow()

    private val _selectedImage = MutableStateFlow<Bitmap?>(null)
    val selectedImage: StateFlow<Bitmap?> = _selectedImage.asStateFlow()

    fun onImageSelected(bitmap: Bitmap?) {
        _selectedImage.value = bitmap
        if (bitmap != null) {
            _uiState.value = AiFoodRecognitionState.Idle
        }
    }

    fun analyzeImage() {
        val bitmap = _selectedImage.value ?: return
        
        _uiState.value = AiFoodRecognitionState.Analyzing
        
        viewModelScope.launch {
            val scaledBitmap = scaleDown(bitmap, 800f, true)
            val result = analyzeFoodImageUseCase(scaledBitmap)
            
            result.onSuccess { estimate ->
                _uiState.value = AiFoodRecognitionState.Success(estimate)
            }.onFailure { error ->
                _uiState.value = AiFoodRecognitionState.Error(error.message ?: "Failed to analyze image")
            }
        }
    }

    fun resetState() {
        _uiState.value = AiFoodRecognitionState.Idle
        _selectedImage.value = null
    }

    fun saveEstimateAsMeal(estimate: StructuredFoodEstimate) {
        val userId = authUseCases.getCurrentUserId() ?: return
        
        val foodItem = FoodItem(
            id = UUID.randomUUID().toString(),
            name = estimate.name,
            calories = estimate.calories ?: 0f,
            protein = estimate.protein ?: 0f,
            carbs = estimate.carbs ?: 0f,
            fat = estimate.fat ?: 0f,
            servingSize = estimate.servingQuantity ?: 1f,
            servingUnit = estimate.servingDescription ?: "serving"
        )
        
        val mealEntry = MealEntry(
            id = UUID.randomUUID().toString(),
            userId = userId,
            foodItem = foodItem,
            date = System.currentTimeMillis(),
            mealType = "Snack", // Default, could be expanded later
            quantity = 1f, // The serving quantity is inside foodItem
            loggedAt = System.currentTimeMillis()
        )
        
        viewModelScope.launch {
            mealTrackerUseCases.addMeal(mealEntry)
        }
    }

    private fun scaleDown(realImage: Bitmap, maxImageSize: Float, filter: Boolean): Bitmap {
        val ratio = Math.min(
            maxImageSize / realImage.width,
            maxImageSize / realImage.height
        )
        if (ratio >= 1.0) return realImage
        val width = Math.round(ratio * realImage.width)
        val height = Math.round(ratio * realImage.height)
        return Bitmap.createScaledBitmap(realImage, width, height, filter)
    }
}
