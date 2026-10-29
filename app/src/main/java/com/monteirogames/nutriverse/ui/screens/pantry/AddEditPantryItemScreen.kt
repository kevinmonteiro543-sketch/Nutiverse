package com.monteirogames.nutriverse.ui.screens.pantry

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.monteirogames.nutriverse.ui.viewmodel.createViewModel

@Composable
fun AddEditPantryItemScreen(
    itemId: String? = null,
    initialName: String? = null,
    initialCalories: Float? = null,
    initialProtein: Float? = null,
    initialCarbs: Float? = null,
    initialFat: Float? = null,
    initialBarcode: String? = null,
    onNavigateBack: () -> Unit
) {
    val viewModel = createViewModel { app -> 
        AddEditPantryItemViewModel(app.authUseCases, app.pantryUseCases) 
    }
    
    val isSaved by viewModel.isSaved.collectAsState()
    val initialItem by viewModel.initialItem.collectAsState()

    LaunchedEffect(itemId) {
        viewModel.loadItem(itemId)
    }

    LaunchedEffect(isSaved) {
        if (isSaved) {
            onNavigateBack()
        }
    }

    var foodName by remember { mutableStateOf(initialName ?: "") }
    var quantity by remember { mutableStateOf("") }
    var unit by remember { mutableStateOf("") }
    var expiryDays by remember { mutableStateOf("") }

    LaunchedEffect(initialItem) {
        initialItem?.let { item ->
            foodName = item.foodItem.name
            quantity = item.quantity.toString()
            unit = item.unit
            val current = System.currentTimeMillis()
            val diff = item.expiryDate - current
            val days = diff / (1000 * 60 * 60 * 24)
            expiryDays = Math.max(0, days).toString()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(if (itemId != null) "Edit Pantry Item" else "Add Pantry Item", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = foodName,
            onValueChange = { foodName = it },
            label = { Text("Item Name") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(value = quantity, onValueChange = { quantity = it }, label = { Text("Quantity") }, modifier = Modifier.weight(1f))
            OutlinedTextField(value = unit, onValueChange = { unit = it }, label = { Text("Unit") }, modifier = Modifier.weight(1f))
        }
        Spacer(modifier = Modifier.height(8.dp))
        
        OutlinedTextField(
            value = expiryDays,
            onValueChange = { expiryDays = it },
            label = { Text("Expires in (days)") },
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            OutlinedButton(onClick = onNavigateBack, modifier = Modifier.weight(1f)) {
                Text("Cancel")
            }
            Button(
                onClick = {
                    viewModel.saveItem(
                        foodName = foodName,
                        quantity = quantity,
                        unit = unit,
                        expiryDaysFromNow = expiryDays,
                        calories = initialCalories,
                        protein = initialProtein,
                        carbs = initialCarbs,
                        fat = initialFat,
                        barcode = initialBarcode
                    )
                },
                modifier = Modifier.weight(1f)
            ) {
                Text(if (itemId != null) "Update" else "Save Item")
            }
        }
    }
}
