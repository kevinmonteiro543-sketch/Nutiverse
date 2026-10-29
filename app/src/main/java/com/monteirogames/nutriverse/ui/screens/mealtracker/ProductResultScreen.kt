package com.monteirogames.nutriverse.ui.screens.mealtracker

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.monteirogames.nutriverse.domain.model.ScannedProduct
import com.monteirogames.nutriverse.ui.viewmodel.createViewModel

@Composable
fun ProductResultScreen(
    barcode: String,
    onNavigateBack: () -> Unit,
    onNavigateToScanAgain: () -> Unit,
    onAddToPantry: (ScannedProduct) -> Unit
) {
    val viewModel = createViewModel { app ->
        ProductResultViewModel(app.scannerUseCases)
    }

    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(barcode) {
        viewModel.loadProduct(barcode)
    }

    Scaffold { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            when (val state = uiState) {
                is ProductResultUiState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                is ProductResultUiState.Success -> {
                    ProductDetails(
                        product = state.product,
                        onScanAgain = onNavigateToScanAgain,
                        onAddToPantry = { onAddToPantry(state.product) },
                        onCancel = onNavigateBack
                    )
                }
                is ProductResultUiState.Error -> {
                    ErrorState(
                        message = state.message,
                        onScanAgain = onNavigateToScanAgain,
                        onManualEntry = { onAddToPantry(
                            ScannedProduct(barcode = barcode, name = "", brand = null, imageUrl = null, servingSize = null, ingredients = null, allergens = null, nutriscore = null, energyKcal = null, protein = null, carbs = null, fat = null)
                        ) },
                        onCancel = onNavigateBack
                    )
                }
            }
        }
    }
}

@Composable
fun ProductDetails(
    product: ScannedProduct,
    onScanAgain: () -> Unit,
    onAddToPantry: () -> Unit,
    onCancel: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (!product.imageUrl.isNullOrEmpty()) {
            AsyncImage(
                model = product.imageUrl,
                contentDescription = "Product Image",
                modifier = Modifier.size(200.dp).padding(16.dp)
            )
        }
        
        Text(product.name, style = MaterialTheme.typography.headlineMedium)
        if (!product.brand.isNullOrEmpty()) {
            Text(product.brand, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text("Barcode: ${product.barcode}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Nutrition Information", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                if (!product.servingSize.isNullOrEmpty()) {
                    Text("Serving Size: ${product.servingSize}", style = MaterialTheme.typography.bodyMedium)
                }
                Text("Calories: ${product.energyKcal?.toString() ?: "N/A"} kcal", style = MaterialTheme.typography.bodyMedium)
                Text("Protein: ${product.protein?.toString() ?: "N/A"} g", style = MaterialTheme.typography.bodyMedium)
                Text("Carbs: ${product.carbs?.toString() ?: "N/A"} g", style = MaterialTheme.typography.bodyMedium)
                Text("Fat: ${product.fat?.toString() ?: "N/A"} g", style = MaterialTheme.typography.bodyMedium)
                
                if (!product.nutriscore.isNullOrEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Nutri-Score: ${product.nutriscore.uppercase()}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        if (!product.ingredients.isNullOrEmpty()) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Ingredients", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(product.ingredients, style = MaterialTheme.typography.bodyMedium)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        if (!product.allergens.isNullOrEmpty()) {
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Allergens", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onErrorContainer)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(product.allergens, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onErrorContainer)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        Spacer(modifier = Modifier.height(16.dp))
        
        Button(onClick = onAddToPantry, modifier = Modifier.fillMaxWidth()) {
            Text("Add to Pantry")
        }
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedButton(onClick = onScanAgain, modifier = Modifier.fillMaxWidth()) {
            Text("Scan Another Item")
        }
        Spacer(modifier = Modifier.height(8.dp))
        TextButton(onClick = onCancel, modifier = Modifier.fillMaxWidth()) {
            Text("Cancel")
        }
        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
fun ErrorState(
    message: String,
    onScanAgain: () -> Unit,
    onManualEntry: () -> Unit,
    onCancel: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Product Not Found", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))
        Text(message, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.error)
        Spacer(modifier = Modifier.height(24.dp))
        
        Button(onClick = onManualEntry, modifier = Modifier.fillMaxWidth()) {
            Text("Enter Manually")
        }
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedButton(onClick = onScanAgain, modifier = Modifier.fillMaxWidth()) {
            Text("Scan Again")
        }
        Spacer(modifier = Modifier.height(8.dp))
        TextButton(onClick = onCancel, modifier = Modifier.fillMaxWidth()) {
            Text("Cancel")
        }
    }
}
