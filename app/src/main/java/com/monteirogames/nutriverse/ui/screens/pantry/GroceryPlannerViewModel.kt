package com.monteirogames.nutriverse.ui.screens.pantry

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.monteirogames.nutriverse.domain.model.GroceryCategoryHelper
import com.monteirogames.nutriverse.domain.model.GroceryItem
import com.monteirogames.nutriverse.domain.usecase.AuthUseCases
import com.monteirogames.nutriverse.domain.usecase.GroceryUseCases
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.UUID

enum class GroceryFilterStatus {
    ALL, REMAINING, PURCHASED
}

data class GroceryPlannerUiState(
    val isLoading: Boolean = true,
    val allItems: List<GroceryItem> = emptyList(),
    val filteredItems: List<GroceryItem> = emptyList(),
    val groupedItems: Map<String, List<GroceryItem>> = emptyMap(),
    val searchQuery: String = "",
    val filterStatus: GroceryFilterStatus = GroceryFilterStatus.ALL,
    val selectedCategory: String = "All",
    val remainingCount: Int = 0,
    val purchasedCount: Int = 0
)

class GroceryPlannerViewModel(
    private val authUseCases: AuthUseCases,
    private val groceryUseCases: GroceryUseCases
) : ViewModel() {

    private val _uiState = MutableStateFlow(GroceryPlannerUiState())
    val uiState: StateFlow<GroceryPlannerUiState> = _uiState.asStateFlow()

    private val currentUserId: String?
        get() = authUseCases.getCurrentUserId()

    init {
        loadGroceryItems()
    }

    private fun loadGroceryItems() {
        val userId = currentUserId ?: return
        viewModelScope.launch {
            groceryUseCases.getGroceryItems(userId).collectLatest { items ->
                val current = _uiState.value
                val remaining = items.count { !it.isPurchased }
                val purchased = items.count { it.isPurchased }
                val filtered = filterAndGroup(
                    items = items,
                    search = current.searchQuery,
                    status = current.filterStatus,
                    category = current.selectedCategory
                )

                _uiState.value = current.copy(
                    isLoading = false,
                    allItems = items,
                    filteredItems = filtered,
                    groupedItems = groupByCategory(filtered),
                    remainingCount = remaining,
                    purchasedCount = purchased
                )
            }
        }
    }

    fun onSearchQueryChange(query: String) {
        val current = _uiState.value
        val filtered = filterAndGroup(current.allItems, query, current.filterStatus, current.selectedCategory)
        _uiState.value = current.copy(
            searchQuery = query,
            filteredItems = filtered,
            groupedItems = groupByCategory(filtered)
        )
    }

    fun onFilterStatusChange(status: GroceryFilterStatus) {
        val current = _uiState.value
        val filtered = filterAndGroup(current.allItems, current.searchQuery, status, current.selectedCategory)
        _uiState.value = current.copy(
            filterStatus = status,
            filteredItems = filtered,
            groupedItems = groupByCategory(filtered)
        )
    }

    fun onCategorySelect(category: String) {
        val current = _uiState.value
        val filtered = filterAndGroup(current.allItems, current.searchQuery, current.filterStatus, category)
        _uiState.value = current.copy(
            selectedCategory = category,
            filteredItems = filtered,
            groupedItems = groupByCategory(filtered)
        )
    }

    fun togglePurchased(item: GroceryItem) {
        val userId = currentUserId ?: return
        viewModelScope.launch {
            groceryUseCases.toggleGroceryPurchased(item.id, userId, !item.isPurchased)
        }
    }

    fun addItem(name: String, quantity: Float, unit: String, category: String) {
        val userId = currentUserId ?: return
        if (name.isBlank()) return

        val finalCategory = if (category.isNotBlank() && category != "Auto") {
            category
        } else {
            GroceryCategoryHelper.inferCategory(name)
        }

        val item = GroceryItem(
            id = UUID.randomUUID().toString(),
            userId = userId,
            name = name.trim(),
            quantity = quantity.coerceAtLeast(0.1f),
            unit = unit.trim().ifBlank { "pcs" },
            category = finalCategory,
            isPurchased = false,
            source = "MANUAL",
            createdAt = System.currentTimeMillis()
        )

        viewModelScope.launch {
            groceryUseCases.addGroceryItem(item)
        }
    }

    fun updateItem(item: GroceryItem) {
        viewModelScope.launch {
            groceryUseCases.updateGroceryItem(item)
        }
    }

    fun deleteItem(item: GroceryItem) {
        viewModelScope.launch {
            groceryUseCases.deleteGroceryItem(item)
        }
    }

    fun clearPurchasedItems() {
        val userId = currentUserId ?: return
        viewModelScope.launch {
            groceryUseCases.clearPurchasedGrocery(userId)
        }
    }

    fun addToPantry(item: GroceryItem, onSuccess: () -> Unit) {
        viewModelScope.launch {
            groceryUseCases.addGroceryItemToPantry(item)
            onSuccess()
        }
    }

    private fun filterAndGroup(
        items: List<GroceryItem>,
        search: String,
        status: GroceryFilterStatus,
        category: String
    ): List<GroceryItem> {
        return items.filter { item ->
            val matchesStatus = when (status) {
                GroceryFilterStatus.ALL -> true
                GroceryFilterStatus.REMAINING -> !item.isPurchased
                GroceryFilterStatus.PURCHASED -> item.isPurchased
            }

            val matchesCategory = if (category == "All") true else item.category.equals(category, ignoreCase = true)

            val matchesSearch = if (search.isBlank()) {
                true
            } else {
                val q = search.trim().lowercase()
                item.name.lowercase().contains(q) || item.category.lowercase().contains(q)
            }

            matchesStatus && matchesCategory && matchesSearch
        }
    }

    private fun groupByCategory(items: List<GroceryItem>): Map<String, List<GroceryItem>> {
        val orderedCategories = GroceryCategoryHelper.CATEGORIES
        val result = mutableMapOf<String, MutableList<GroceryItem>>()

        for (item in items) {
            val cat = if (item.category in orderedCategories) item.category else "Other"
            result.getOrPut(cat) { mutableListOf() }.add(item)
        }

        // Return sorted according to category order
        return orderedCategories.mapNotNull { cat ->
            result[cat]?.let { cat to it.toList() }
        }.toMap()
    }
}
