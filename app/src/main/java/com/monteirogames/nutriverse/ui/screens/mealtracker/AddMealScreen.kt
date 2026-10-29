package com.monteirogames.nutriverse.ui.screens.mealtracker

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.monteirogames.nutriverse.data.catalog.BuiltInFoodCatalog
import com.monteirogames.nutriverse.ui.viewmodel.createViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddMealScreen(
    mealId: String? = null,
    onNavigateBack: () -> Unit
) {
    val viewModel = createViewModel { app -> 
        AddMealViewModel(app.authUseCases, app.mealTrackerUseCases) 
    }
    
    val isSaved by viewModel.isSaved.collectAsState()
    val initialMeal by viewModel.initialMeal.collectAsState()

    LaunchedEffect(mealId) {
        viewModel.loadMeal(mealId)
    }

    LaunchedEffect(isSaved) {
        if (isSaved) {
            onNavigateBack()
        }
    }

    var foodName by remember { mutableStateOf("") }
    var calories by remember { mutableStateOf("") }
    var protein by remember { mutableStateOf("") }
    var carbs by remember { mutableStateOf("") }
    var fat by remember { mutableStateOf("") }
    var servingSize by remember { mutableStateOf("100") }
    var servingUnit by remember { mutableStateOf("g") }
    var quantity by remember { mutableStateOf("100") }
    
    val mealTypes = listOf("Breakfast", "Lunch", "Dinner", "Snack")
    var expanded by remember { mutableStateOf(false) }
    var selectedMealType by remember { mutableStateOf(mealTypes[0]) }

    LaunchedEffect(initialMeal) {
        initialMeal?.let { meal ->
            foodName = meal.foodItem.name
            calories = meal.foodItem.calories.toString()
            protein = meal.foodItem.protein.toString()
            carbs = meal.foodItem.carbs.toString()
            fat = meal.foodItem.fat.toString()
            servingSize = meal.foodItem.servingSize.toString()
            servingUnit = meal.foodItem.servingUnit
            quantity = meal.quantity.toString()
            selectedMealType = meal.mealType
        }
    }

    var showCatalogPicker by remember { mutableStateOf(false) }
    var catalogSearchQuery by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(if (mealId != null) "Edit Meal" else "Add Meal", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(12.dp))

        // Quick Autofill from built-in database
        OutlinedButton(
            onClick = { showCatalogPicker = true },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("add_meal_autofill_button"),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.MenuBook, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Autofill from Built-in Food Database")
        }
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = foodName,
            onValueChange = { foodName = it },
            label = { Text("Food Name") },
            modifier = Modifier.fillMaxWidth().testTag("add_meal_food_name_input")
        )
        Spacer(modifier = Modifier.height(8.dp))

        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded }
        ) {
            OutlinedTextField(
                value = selectedMealType,
                onValueChange = {},
                readOnly = true,
                label = { Text("Meal Type") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier.fillMaxWidth().menuAnchor()
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                mealTypes.forEach { selectionOption ->
                    DropdownMenuItem(
                        text = { Text(selectionOption) },
                        onClick = {
                            selectedMealType = selectionOption
                            expanded = false
                        }
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        
        Text("Nutritional Info (per serving)", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(value = servingSize, onValueChange = { servingSize = it }, label = { Text("Serving Size") }, modifier = Modifier.weight(1f))
            OutlinedTextField(value = servingUnit, onValueChange = { servingUnit = it }, label = { Text("Unit (g, ml, cup)") }, modifier = Modifier.weight(1f))
        }
        Spacer(modifier = Modifier.height(8.dp))
        
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(value = calories, onValueChange = { calories = it }, label = { Text("Calories") }, modifier = Modifier.weight(1f))
            OutlinedTextField(value = protein, onValueChange = { protein = it }, label = { Text("Protein (g)") }, modifier = Modifier.weight(1f))
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(value = carbs, onValueChange = { carbs = it }, label = { Text("Carbs (g)") }, modifier = Modifier.weight(1f))
            OutlinedTextField(value = fat, onValueChange = { fat = it }, label = { Text("Fat (g)") }, modifier = Modifier.weight(1f))
        }
        Spacer(modifier = Modifier.height(16.dp))
        
        Text("Log Amount", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))
        
        OutlinedTextField(
            value = quantity,
            onValueChange = { quantity = it },
            label = { Text("Quantity Logged ($servingUnit)") },
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            OutlinedButton(onClick = onNavigateBack, modifier = Modifier.weight(1f)) {
                Text("Cancel")
            }
            Button(
                onClick = {
                    viewModel.saveMeal(
                        foodName, calories, protein, carbs, fat, servingSize, servingUnit, quantity, selectedMealType
                    )
                },
                modifier = Modifier.weight(1f)
            ) {
                Text(if (mealId != null) "Update" else "Save Meal")
            }
        }
    }

    if (showCatalogPicker) {
        val filteredCatalog = remember(catalogSearchQuery) {
            val q = catalogSearchQuery.trim().lowercase()
            if (q.isEmpty()) BuiltInFoodCatalog.foods else {
                BuiltInFoodCatalog.foods.filter {
                    it.name.lowercase().contains(q) || it.category.lowercase().contains(q)
                }
            }
        }

        AlertDialog(
            onDismissRequest = { showCatalogPicker = false },
            title = { Text("Select Food from Catalog", fontWeight = FontWeight.Bold) },
            text = {
                Column(modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp)) {
                    OutlinedTextField(
                        value = catalogSearchQuery,
                        onValueChange = { catalogSearchQuery = it },
                        placeholder = { Text("Search food...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                    )

                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(filteredCatalog) { item ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        foodName = item.name
                                        calories = item.caloriesPer100g.toString()
                                        protein = item.proteinPer100g.toString()
                                        carbs = item.carbsPer100g.toString()
                                        fat = item.fatPer100g.toString()
                                        servingSize = "100"
                                        servingUnit = "g"
                                        quantity = item.defaultQuantityGrams.toInt().toString()
                                        showCatalogPicker = false
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp).fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(item.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                        Text("${item.category} • ${if (item.isCooked) "Cooked" else "Raw"}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                    }
                                    Text("${item.caloriesPer100g.toInt()} kcal / 100g", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showCatalogPicker = false }) {
                    Text("Close")
                }
            }
        )
    }
}

