package com.monteirogames.nutriverse.ui.screens.mealtracker

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.monteirogames.nutriverse.domain.model.ScannedProduct
import com.monteirogames.nutriverse.domain.usecase.ScannerUseCases
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class ProductResultUiState {
    object Loading : ProductResultUiState()
    data class Success(val product: ScannedProduct) : ProductResultUiState()
    data class Error(val message: String) : ProductResultUiState()
}

class ProductResultViewModel(
    private val scannerUseCases: ScannerUseCases
) : ViewModel() {

    private val _uiState = MutableStateFlow<ProductResultUiState>(ProductResultUiState.Loading)
    val uiState: StateFlow<ProductResultUiState> = _uiState.asStateFlow()

    fun loadProduct(barcode: String) {
        viewModelScope.launch {
            _uiState.value = ProductResultUiState.Loading
            val result = scannerUseCases.getProductFromBarcode(barcode)
            result.onSuccess { product ->
                _uiState.value = ProductResultUiState.Success(product)
            }.onFailure { e ->
                _uiState.value = ProductResultUiState.Error(e.message ?: "Failed to load product.")
            }
        }
    }
}
