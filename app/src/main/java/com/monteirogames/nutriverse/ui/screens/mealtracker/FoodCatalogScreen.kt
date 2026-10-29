package com.monteirogames.nutriverse.ui.screens.mealtracker

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.monteirogames.nutriverse.domain.model.CatalogFoodItem
import com.monteirogames.nutriverse.domain.model.CatalogServingPreset
import com.monteirogames.nutriverse.ui.viewmodel.createViewModel
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FoodCatalogScreen(
    onNavigateBack: () -> Unit,
    onMealAdded: () -> Unit = onNavigateBack
) {
    val viewModel = createViewModel { app ->
        FoodCatalogViewModel(
            authUseCases = app.authUseCases,
            foodCatalogUseCases = app.foodCatalogUseCases
        )
    }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.isAddedSuccess) {
        if (uiState.isAddedSuccess) {
            snackbarHostState.showSnackbar("Meal logged successfully!")
            viewModel.clearSuccess()
            onMealAdded()
        }
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { error ->
            snackbarHostState.showSnackbar(error)
            viewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Food Database",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Common Everyday & Indian Foods",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("food_catalog_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Search Input
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = { viewModel.onQueryChanged(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .testTag("food_catalog_search_input"),
                placeholder = { Text("Search roti, dal, rice, paneer, egg, fruit...") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search"
                    )
                },
                trailingIcon = {
                    if (uiState.searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.onQueryChanged("") }) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Clear search"
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            // Category Filter Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                uiState.categories.forEach { category ->
                    val isSelected = if (category.equals("All", ignoreCase = true)) {
                        uiState.selectedCategory == null || uiState.selectedCategory.equals("All", ignoreCase = true)
                    } else {
                        uiState.selectedCategory.equals(category, ignoreCase = true)
                    }

                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            viewModel.onCategorySelected(if (category.equals("All", ignoreCase = true)) null else category)
                        },
                        label = { Text(category) },
                        modifier = Modifier.testTag("category_chip_${category.lowercase().replace(" ", "_")}")
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Foods Count Header
            Text(
                text = "${uiState.filteredFoods.size} foods available",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )

            // Food List
            if (uiState.filteredFoods.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.SearchOff,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.outline
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No matching foods found",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Try adjusting your search terms or clearing the category filter.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        OutlinedButton(
                            onClick = {
                                viewModel.onQueryChanged("")
                                viewModel.onCategorySelected(null)
                            }
                        ) {
                            Text("Reset Filters")
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(
                        items = uiState.filteredFoods,
                        key = { it.id }
                    ) { food ->
                        CatalogFoodItemCard(
                            food = food,
                            onSelect = { viewModel.onFoodSelected(food) }
                        )
                    }
                }
            }
        }
    }

    // Food Quantity & Nutrition Dialog
    if (uiState.selectedFood != null) {
        val selectedFood = uiState.selectedFood!!
        FoodPortionDialog(
            food = selectedFood,
            quantityInput = uiState.quantityInput,
            calculatedNutrition = uiState.calculatedNutrition,
            selectedMealType = uiState.selectedMealType,
            isAdding = uiState.isAdding,
            onQuantityChanged = { viewModel.onQuantityChanged(it) },
            onPresetSelected = { viewModel.onPresetSelected(it) },
            onMealTypeSelected = { viewModel.onMealTypeSelected(it) },
            onConfirmAdd = { viewModel.addMealToTracker() },
            onDismiss = { viewModel.onFoodSelected(null) }
        )
    }
}

@Composable
fun CatalogFoodItemCard(
    food: CatalogFoodItem,
    onSelect: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect() }
            .testTag("catalog_food_item_${food.id}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = food.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer
                        ) {
                            Text(
                                text = food.category,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (food.isCooked) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = if (food.isCooked) "Cooked" else "Raw",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (food.isCooked) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Button(
                    onClick = onSelect,
                    modifier = Modifier.padding(start = 8.dp),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text("Select", fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Nutrition per 100g breakdown
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                NutritionChip("Energy", "${food.caloriesPer100g.roundToInt()} kcal", MaterialTheme.colorScheme.primary, Modifier.weight(1f))
                NutritionChip("Protein", "${food.proteinPer100g}g", MaterialTheme.colorScheme.secondary, Modifier.weight(1f))
                NutritionChip("Carbs", "${food.carbsPer100g}g", MaterialTheme.colorScheme.tertiary, Modifier.weight(1f))
                NutritionChip("Fat", "${food.fatPer100g}g", MaterialTheme.colorScheme.outline, Modifier.weight(1f))
            }

            if (!food.description.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = food.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun NutritionChip(
    label: String,
    value: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = accentColor
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FoodPortionDialog(
    food: CatalogFoodItem,
    quantityInput: String,
    calculatedNutrition: com.monteirogames.nutriverse.domain.model.CalculatedNutrition,
    selectedMealType: String,
    isAdding: Boolean,
    onQuantityChanged: (String) -> Unit,
    onPresetSelected: (CatalogServingPreset) -> Unit,
    onMealTypeSelected: (String) -> Unit,
    onConfirmAdd: () -> Unit,
    onDismiss: () -> Unit
) {
    val mealTypes = listOf("Breakfast", "Lunch", "Dinner", "Snack")
    val defaultPresets = listOf(
        CatalogServingPreset("50g", 50f),
        CatalogServingPreset("100g", 100f),
        CatalogServingPreset("150g", 150f),
        CatalogServingPreset("200g", 200f)
    )

    val allPresets = if (food.presets.isNotEmpty()) food.presets + defaultPresets.filter { dp -> food.presets.none { it.grams == dp.grams } } else defaultPresets

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    text = food.name,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${food.category} • ${if (food.isCooked) "Cooked" else "Raw"}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Presets
                Text(
                    text = "Quick Portion Presets",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    allPresets.forEach { preset ->
                        val isPresetActive = quantityInput.toFloatOrNull() == preset.grams
                        OutlinedButton(
                            onClick = { onPresetSelected(preset) },
                            shape = RoundedCornerShape(8.dp),
                            colors = if (isPresetActive) {
                                ButtonDefaults.outlinedButtonColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            } else ButtonDefaults.outlinedButtonColors(),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.testTag("preset_button_${preset.grams.toInt()}")
                        ) {
                            Text(preset.label, style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }

                // Quantity Input
                OutlinedTextField(
                    value = quantityInput,
                    onValueChange = onQuantityChanged,
                    label = { Text("Quantity in grams (${food.servingUnitName})") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("catalog_quantity_input"),
                    trailingIcon = { Text("g", modifier = Modifier.padding(end = 12.dp)) },
                    singleLine = true
                )

                // Calculated Nutrition Card (Live Updates)
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("catalog_calculated_nutrition_card"),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "Calculated Nutrition (${quantityInput.ifBlank { "0" }}g)",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Calories", style = MaterialTheme.typography.labelSmall)
                                Text(
                                    text = "${calculatedNutrition.calories.roundToInt()} kcal",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Column {
                                Text("Protein", style = MaterialTheme.typography.labelSmall)
                                Text(
                                    text = "${String.format("%.1f", calculatedNutrition.protein)}g",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Column {
                                Text("Carbs", style = MaterialTheme.typography.labelSmall)
                                Text(
                                    text = "${String.format("%.1f", calculatedNutrition.carbs)}g",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Column {
                                Text("Fat", style = MaterialTheme.typography.labelSmall)
                                Text(
                                    text = "${String.format("%.1f", calculatedNutrition.fat)}g",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                // Meal Type Selection
                Text(
                    text = "Select Meal Type",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("catalog_meal_type_selector"),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    mealTypes.forEach { type ->
                        val isSelected = selectedMealType.equals(type, ignoreCase = true)
                        FilterChip(
                            selected = isSelected,
                            onClick = { onMealTypeSelected(type) },
                            label = { Text(type, style = MaterialTheme.typography.labelSmall) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirmAdd,
                enabled = !isAdding && (quantityInput.toFloatOrNull() ?: 0f) > 0f,
                modifier = Modifier.testTag("catalog_add_to_tracker_button")
            ) {
                if (isAdding) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                }
                Text("Add to Tracker")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
