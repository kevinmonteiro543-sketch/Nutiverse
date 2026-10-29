package com.monteirogames.nutriverse.ui.screens.pantry

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Kitchen
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.monteirogames.nutriverse.domain.model.GroceryCategoryHelper
import com.monteirogames.nutriverse.domain.model.GroceryItem
import com.monteirogames.nutriverse.ui.viewmodel.createViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroceryPlannerScreen(
    onNavigateBack: () -> Unit,
    onNavigateToBudget: (() -> Unit)? = null
) {
    val viewModel = createViewModel { app ->
        GroceryPlannerViewModel(
            authUseCases = app.authUseCases,
            groceryUseCases = app.groceryUseCases
        )
    }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    var showAddItemDialog by remember { mutableStateOf(false) }
    var itemToEdit by remember { mutableStateOf<GroceryItem?>(null) }
    var showClearPurchasedDialog by remember { mutableStateOf(false) }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Grocery Planner",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${uiState.remainingCount} remaining • ${uiState.purchasedCount} purchased",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("grocery_back_button")
                    ) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (onNavigateToBudget != null) {
                        IconButton(
                            onClick = onNavigateToBudget,
                            modifier = Modifier.testTag("grocery_budget_button")
                        ) {
                            Icon(imageVector = Icons.Default.AccountBalanceWallet, contentDescription = "Budget")
                        }
                    }
                    if (uiState.purchasedCount > 0) {
                        TextButton(
                            onClick = { showClearPurchasedDialog = true },
                            modifier = Modifier.testTag("clear_purchased_button")
                        ) {
                            Text("Clear Done", color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddItemDialog = true },
                icon = { Icon(imageVector = Icons.Default.Add, contentDescription = null) },
                text = { Text("Add Item") },
                modifier = Modifier.testTag("add_grocery_item_fab")
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Search Bar
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = { viewModel.onSearchQueryChange(it) },
                placeholder = { Text("Search groceries or categories...") },
                leadingIcon = {
                    Icon(imageVector = Icons.Default.Search, contentDescription = "Search")
                },
                trailingIcon = {
                    if (uiState.searchQuery.isNotBlank()) {
                        IconButton(onClick = { viewModel.onSearchQueryChange("") }) {
                            Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .testTag("grocery_search_input")
            )

            // Status Filter Tabs (All, Remaining, Purchased)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = uiState.filterStatus == GroceryFilterStatus.ALL,
                    onClick = { viewModel.onFilterStatusChange(GroceryFilterStatus.ALL) },
                    label = { Text("All (${uiState.allItems.size})") },
                    modifier = Modifier.testTag("filter_all")
                )
                FilterChip(
                    selected = uiState.filterStatus == GroceryFilterStatus.REMAINING,
                    onClick = { viewModel.onFilterStatusChange(GroceryFilterStatus.REMAINING) },
                    label = { Text("Remaining (${uiState.remainingCount})") },
                    modifier = Modifier.testTag("filter_remaining")
                )
                FilterChip(
                    selected = uiState.filterStatus == GroceryFilterStatus.PURCHASED,
                    onClick = { viewModel.onFilterStatusChange(GroceryFilterStatus.PURCHASED) },
                    label = { Text("Purchased (${uiState.purchasedCount})") },
                    modifier = Modifier.testTag("filter_purchased")
                )
            }

            // Category Chips Row
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                val categoryOptions = listOf("All") + GroceryCategoryHelper.CATEGORIES
                items(categoryOptions) { cat ->
                    FilterChip(
                        selected = uiState.selectedCategory == cat,
                        onClick = { viewModel.onCategorySelect(cat) },
                        label = { Text(cat) },
                        modifier = Modifier.testTag("filter_category_$cat")
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Main Content / States
            if (uiState.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(modifier = Modifier.testTag("grocery_loading_indicator"))
                }
            } else if (uiState.filteredItems.isEmpty()) {
                EmptyGroceryView(
                    isSearchActive = uiState.searchQuery.isNotBlank(),
                    onAddItem = { showAddItemDialog = true }
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    uiState.groupedItems.forEach { (category, itemsInCategory) ->
                        if (itemsInCategory.isNotEmpty()) {
                            item(key = "header_$category") {
                                CategorySectionHeader(
                                    category = category,
                                    count = itemsInCategory.size
                                )
                            }

                            items(itemsInCategory, key = { it.id }) { item ->
                                GroceryItemCard(
                                    item = item,
                                    onTogglePurchased = { viewModel.togglePurchased(item) },
                                    onAddToPantry = {
                                        viewModel.addToPantry(item) {
                                            coroutineScope.launch {
                                                snackbarHostState.showSnackbar("Added \"${item.name}\" to your Pantry!")
                                            }
                                        }
                                    },
                                    onEdit = { itemToEdit = item },
                                    onDelete = { viewModel.deleteItem(item) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Add Item Dialog
    if (showAddItemDialog) {
        AddEditGroceryItemDialog(
            initialItem = null,
            onDismiss = { showAddItemDialog = false },
            onSave = { name, quantity, unit, category ->
                viewModel.addItem(name, quantity, unit, category)
                showAddItemDialog = false
                coroutineScope.launch {
                    snackbarHostState.showSnackbar("Added \"$name\" to grocery list")
                }
            }
        )
    }

    // Edit Item Dialog
    itemToEdit?.let { item ->
        AddEditGroceryItemDialog(
            initialItem = item,
            onDismiss = { itemToEdit = null },
            onSave = { name, quantity, unit, category ->
                viewModel.updateItem(
                    item.copy(
                        name = name,
                        quantity = quantity,
                        unit = unit,
                        category = category
                    )
                )
                itemToEdit = null
                coroutineScope.launch {
                    snackbarHostState.showSnackbar("Updated \"$name\"")
                }
            }
        )
    }

    // Clear Purchased Confirmation Dialog
    if (showClearPurchasedDialog) {
        AlertDialog(
            onDismissRequest = { showClearPurchasedDialog = false },
            title = { Text("Clear Purchased Items") },
            text = { Text("Are you sure you want to remove all purchased items from your grocery list?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearPurchasedItems()
                        showClearPurchasedDialog = false
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar("Cleared purchased items")
                        }
                    },
                    modifier = Modifier.testTag("confirm_clear_purchased_button")
                ) {
                    Text("Clear", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearPurchasedDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun CategorySectionHeader(category: String, count: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = category,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer
        ) {
            Text(
                text = "$count",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
            )
        }
    }
}

@Composable
private fun GroceryItemCard(
    item: GroceryItem,
    onTogglePurchased: () -> Unit,
    onAddToPantry: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    ElevatedCard(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = if (item.isPurchased) {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        elevation = CardDefaults.elevatedCardElevation(
            defaultElevation = if (item.isPurchased) 0.dp else 2.dp
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onEdit)
            .testTag("grocery_item_${item.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = item.isPurchased,
                onCheckedChange = { onTogglePurchased() },
                modifier = Modifier.testTag("checkbox_${item.id}")
            )

            Spacer(modifier = Modifier.width(8.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = if (item.isPurchased) FontWeight.Normal else FontWeight.SemiBold,
                    textDecoration = if (item.isPurchased) TextDecoration.LineThrough else TextDecoration.None,
                    color = if (item.isPurchased) {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(2.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val qtyStr = if (item.quantity % 1f == 0f) {
                        item.quantity.toInt().toString()
                    } else {
                        String.format("%.1f", item.quantity)
                    }
                    Text(
                        text = "$qtyStr ${item.unit}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (item.source == "RECIPE") {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer
                        ) {
                            Text(
                                text = "Recipe",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                            )
                        }
                    }
                }
            }

            // If purchased: Provide "Add to Pantry" button
            AnimatedVisibility(visible = item.isPurchased) {
                FilledTonalButton(
                    onClick = onAddToPantry,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.testTag("add_to_pantry_${item.id}")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Kitchen,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Pantry", style = MaterialTheme.typography.labelSmall)
                }
            }

            IconButton(
                onClick = onDelete,
                modifier = Modifier.testTag("delete_grocery_${item.id}")
            ) {
                Icon(
                    imageVector = Icons.Outlined.Delete,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun EmptyGroceryView(
    isSearchActive: Boolean,
    onAddItem: () -> Unit
) {
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
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.ShoppingCart,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(44.dp)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = if (isSearchActive) "No Matching Items" else "No groceries yet",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = if (isSearchActive) {
                    "Try adjusting your search query or clear filters."
                } else {
                    "Add ingredients manually or add missing ingredients from a recipe."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Spacer(modifier = Modifier.height(20.dp))
            Button(
                onClick = onAddItem,
                modifier = Modifier.testTag("empty_state_add_button")
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Add Item")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddEditGroceryItemDialog(
    initialItem: GroceryItem?,
    onDismiss: () -> Unit,
    onSave: (name: String, quantity: Float, unit: String, category: String) -> Unit
) {
    var name by remember { mutableStateOf(initialItem?.name ?: "") }
    var quantityStr by remember {
        mutableStateOf(
            initialItem?.let {
                if (it.quantity % 1f == 0f) it.quantity.toInt().toString() else it.quantity.toString()
            } ?: "1"
        )
    }
    var unit by remember { mutableStateOf(initialItem?.unit ?: "pcs") }
    var selectedCategory by remember {
        mutableStateOf(initialItem?.category ?: GroceryCategoryHelper.inferCategory(name))
    }

    // Auto-update category as user types name if category wasn't manually overridden
    LaunchedEffect(name) {
        if (initialItem == null && name.isNotBlank()) {
            selectedCategory = GroceryCategoryHelper.inferCategory(name)
        }
    }

    var isNameError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (initialItem == null) "Add Grocery Item" else "Edit Grocery Item")
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        if (it.isNotBlank()) isNameError = false
                    },
                    label = { Text("Item Name *") },
                    placeholder = { Text("e.g. Greek Yogurt, Spinach, Chicken") },
                    isError = isNameError,
                    supportingText = if (isNameError) {
                        { Text("Item name is required") }
                    } else null,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("grocery_input_name")
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = quantityStr,
                        onValueChange = { quantityStr = it },
                        label = { Text("Quantity") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("grocery_input_quantity")
                    )

                    OutlinedTextField(
                        value = unit,
                        onValueChange = { unit = it },
                        label = { Text("Unit") },
                        placeholder = { Text("pcs, g, kg, ml") },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("grocery_input_unit")
                    )
                }

                Text(
                    text = "Category (Inferred: $selectedCategory):",
                    style = MaterialTheme.typography.labelMedium
                )

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(GroceryCategoryHelper.CATEGORIES) { cat ->
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick = { selectedCategory = cat },
                            label = { Text(cat, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isBlank()) {
                        isNameError = true
                        return@Button
                    }
                    val qty = quantityStr.toFloatOrNull() ?: 1f
                    onSave(name, qty, unit, selectedCategory)
                },
                modifier = Modifier.testTag("save_grocery_button")
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
