package com.monteirogames.nutriverse.ui.screens.pantry

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.monteirogames.nutriverse.domain.model.PantryItem
import com.monteirogames.nutriverse.domain.usecase.AuthUseCases
import com.monteirogames.nutriverse.domain.usecase.PantryUseCases
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

data class PantryUiState(
    val isLoading: Boolean = true,
    val items: List<PantryItem> = emptyList()
)

class PantryViewModel(
    private val authUseCases: AuthUseCases,
    private val pantryUseCases: PantryUseCases
) : ViewModel() {

    private val _uiState = MutableStateFlow(PantryUiState())
    val uiState: StateFlow<PantryUiState> = _uiState.asStateFlow()

    init {
        loadPantry()
    }

    private fun loadPantry() {
        val userId = authUseCases.getCurrentUserId() ?: return
        
        viewModelScope.launch {
            pantryUseCases.observePantryItems(userId).collectLatest { items ->
                _uiState.value = _uiState.value.copy(items = items, isLoading = false)
            }
        }
    }

    fun deleteItem(item: PantryItem) {
        viewModelScope.launch {
            pantryUseCases.deletePantryItem(item)
        }
    }
}
