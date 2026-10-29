package com.monteirogames.nutriverse.ui.screens.budget

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
import androidx.compose.material.icons.outlined.*
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.monteirogames.nutriverse.domain.model.BudgetPeriod
import com.monteirogames.nutriverse.domain.model.BudgetPeriodHelper
import com.monteirogames.nutriverse.domain.model.BudgetSummary
import com.monteirogames.nutriverse.domain.model.ExpenseCategoryHelper
import com.monteirogames.nutriverse.domain.model.FoodExpense
import com.monteirogames.nutriverse.ui.viewmodel.createViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetScreen(
    onNavigateBack: () -> Unit
) {
    val viewModel = createViewModel { app ->
        BudgetViewModel(
            authUseCases = app.authUseCases,
            budgetUseCases = app.budgetUseCases,
            expenseUseCases = app.expenseUseCases
        )
    }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    var showSetBudgetDialog by remember { mutableStateOf(false) }
    var showAddExpenseDialog by remember { mutableStateOf(false) }
    var expenseToEdit by remember { mutableStateOf<FoodExpense?>(null) }
    var expenseToDelete by remember { mutableStateOf<FoodExpense?>(null) }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { error ->
            snackbarHostState.showSnackbar(error)
            viewModel.clearErrorMessage()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Budget",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("budget_back_button")
                    ) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showSetBudgetDialog = true },
                        modifier = Modifier.testTag("budget_settings_button")
                    ) {
                        Icon(imageVector = Icons.Default.Tune, contentDescription = "Set Budget")
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddExpenseDialog = true },
                icon = { Icon(imageVector = Icons.Default.Add, contentDescription = null) },
                text = { Text("Add Expense") },
                modifier = Modifier.testTag("add_expense_fab")
            )
        }
    ) { paddingValues ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(top = 8.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Budget Summary Card
                item {
                    BudgetSummaryCard(
                        summary = uiState.summary,
                        onSetBudgetClick = { showSetBudgetDialog = true }
                    )
                }

                // Filter Row (Current Period vs All Time)
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Spending Breakdown",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = uiState.filterMode == "CURRENT_PERIOD",
                                onClick = { viewModel.setFilterMode("CURRENT_PERIOD") },
                                label = { Text("Period") },
                                modifier = Modifier.testTag("filter_period_chip")
                            )
                            FilterChip(
                                selected = uiState.filterMode == "ALL_TIME",
                                onClick = { viewModel.setFilterMode("ALL_TIME") },
                                label = { Text("All Time") },
                                modifier = Modifier.testTag("filter_all_time_chip")
                            )
                        }
                    }
                }

                // Category Spending Card (if there's spending)
                if (uiState.categorySpending.isNotEmpty()) {
                    item {
                        CategoryBreakdownCard(
                            categorySpending = uiState.categorySpending,
                            totalSpending = uiState.categorySpending.values.sum()
                        )
                    }
                }

                // Expense History Header
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Expense History (${uiState.filteredExpenses.size})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        if (uiState.filteredExpenses.isNotEmpty()) {
                            Text(
                                text = "Total: ${BudgetPeriodHelper.formatCurrency(uiState.filteredExpenses.sumOf { it.amount })}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                // Expense Items
                if (uiState.filteredExpenses.isEmpty()) {
                    item {
                        EmptyExpensesCard(
                            onAddExpenseClick = { showAddExpenseDialog = true }
                        )
                    }
                } else {
                    items(uiState.filteredExpenses, key = { it.id }) { expense ->
                        ExpenseItemRow(
                            expense = expense,
                            onEditClick = { expenseToEdit = expense },
                            onDeleteClick = { expenseToDelete = expense }
                        )
                    }
                }
            }
        }
    }

    // Set Budget Dialog
    if (showSetBudgetDialog) {
        SetBudgetDialog(
            currentBudget = uiState.summary.budget,
            onDismiss = { showSetBudgetDialog = false },
            onConfirm = { amount, period ->
                viewModel.setBudget(amount, period)
                showSetBudgetDialog = false
            }
        )
    }

    // Add Expense Dialog
    if (showAddExpenseDialog) {
        AddEditExpenseDialog(
            expense = null,
            onDismiss = { showAddExpenseDialog = false },
            onConfirm = { name, amount, category, date, notes ->
                viewModel.addExpense(name, amount, category, date, notes)
                showAddExpenseDialog = false
            }
        )
    }

    // Edit Expense Dialog
    expenseToEdit?.let { expense ->
        AddEditExpenseDialog(
            expense = expense,
            onDismiss = { expenseToEdit = null },
            onConfirm = { name, amount, category, date, notes ->
                viewModel.updateExpense(
                    expense.copy(
                        name = name,
                        amount = amount,
                        category = category,
                        date = date,
                        notes = notes
                    )
                )
                expenseToEdit = null
            }
        )
    }

    // Delete Confirmation Dialog
    expenseToDelete?.let { expense ->
        AlertDialog(
            onDismissRequest = { expenseToDelete = null },
            title = { Text("Delete Expense") },
            text = { Text("Are you sure you want to delete '${expense.name}' (${BudgetPeriodHelper.formatCurrency(expense.amount)})?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteExpense(expense)
                        expenseToDelete = null
                    },
                    modifier = Modifier.testTag("confirm_delete_expense_button")
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { expenseToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun BudgetSummaryCard(
    summary: BudgetSummary,
    onSetBudgetClick: () -> Unit
) {
    val isOverBudget = summary.isOverBudget
    val hasBudget = summary.budget != null

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("budget_summary_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = when {
                !hasBudget -> MaterialTheme.colorScheme.surfaceVariant
                isOverBudget -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
                summary.percentageUsed > 80f -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.6f)
                else -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // Period label & change button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = summary.periodLabel,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (summary.periodRangeLabel.isNotBlank()) {
                        Text(
                            text = summary.periodRangeLabel,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                OutlinedButton(
                    onClick = onSetBudgetClick,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("set_budget_action_button")
                ) {
                    Text(if (hasBudget) "Edit Budget" else "Set Budget")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (!hasBudget) {
                Text(
                    text = "No active budget set for this period. Tap 'Set Budget' to establish a weekly or monthly food spending goal.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Total Spent this month: ${BudgetPeriodHelper.formatCurrency(summary.totalSpent)}",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold
                )
            } else {
                // Spending vs Budget Main Display
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Column {
                        Text(
                            text = "Spent",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = BudgetPeriodHelper.formatCurrency(summary.totalSpent),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "Budget: ${BudgetPeriodHelper.formatCurrency(summary.totalBudget)}",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${String.format(Locale.getDefault(), "%.0f", summary.percentageUsed)}% used",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isOverBudget) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Progress Indicator
                val progressFraction = (summary.percentageUsed / 100f).coerceIn(0f, 1f)
                LinearProgressIndicator(
                    progress = { progressFraction },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(CircleShape),
                    color = when {
                        isOverBudget -> MaterialTheme.colorScheme.error
                        summary.percentageUsed > 80f -> MaterialTheme.colorScheme.tertiary
                        else -> MaterialTheme.colorScheme.primary
                    },
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Status Banner / Remaining text
                if (isOverBudget) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.error.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "${BudgetPeriodHelper.formatCurrency(summary.overBudgetAmount)} over budget",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Remaining:",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = BudgetPeriodHelper.formatCurrency(summary.remaining),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryBreakdownCard(
    categorySpending: Map<String, Double>,
    totalSpending: Double
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("category_breakdown_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "By Category",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )

            val sortedCategories = categorySpending.entries.sortedByDescending { it.value }

            sortedCategories.forEach { (category, amount) ->
                val ratio = if (totalSpending > 0) (amount / totalSpending).toFloat() else 0f
                val percent = (ratio * 100).toInt()

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = getCategoryEmoji(category),
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = category,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        Text(
                            text = "${BudgetPeriodHelper.formatCurrency(amount)} ($percent%)",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    LinearProgressIndicator(
                        progress = { ratio.coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(CircleShape),
                        color = getCategoryColor(category),
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun ExpenseItemRow(
    expense: FoodExpense,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val dateFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
    val dateStr = dateFormat.format(Date(expense.date))

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("expense_card_${expense.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // Category icon avatar
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .background(getCategoryColor(expense.category).copy(alpha = 0.15f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = getCategoryEmoji(expense.category),
                        style = MaterialTheme.typography.titleMedium
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = expense.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = expense.category,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "•",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = dateStr,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (!expense.notes.isNullOrBlank()) {
                        Text(
                            text = expense.notes,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = BudgetPeriodHelper.formatCurrency(expense.amount),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                IconButton(onClick = onEditClick) {
                    Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit Expense", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = onDeleteClick) {
                    Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete Expense", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
private fun EmptyExpensesCard(onAddExpenseClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("empty_expenses_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.ReceiptLong,
                contentDescription = null,
                modifier = Modifier.size(56.dp),
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "No Expenses Recorded",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Track your food and grocery spending by tapping 'Add Expense' below.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onAddExpenseClick,
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Log Food Expense")
            }
        }
    }
}

@Composable
private fun SetBudgetDialog(
    currentBudget: com.monteirogames.nutriverse.domain.model.Budget?,
    onDismiss: () -> Unit,
    onConfirm: (amount: Double, period: String) -> Unit
) {
    var amountText by remember { mutableStateOf(currentBudget?.amount?.toString()?.removeSuffix(".0") ?: "") }
    var selectedPeriod by remember {
        mutableStateOf(currentBudget?.period ?: BudgetPeriod.MONTHLY.name)
    }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (currentBudget != null) "Edit Food Budget" else "Set Food Budget") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    text = "Choose your budget timeframe and enter your total spending target.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Period selection
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedPeriod == BudgetPeriod.MONTHLY.name,
                        onClick = { selectedPeriod = BudgetPeriod.MONTHLY.name },
                        label = { Text("Monthly") },
                        modifier = Modifier.weight(1f).testTag("period_monthly_chip")
                    )
                    FilterChip(
                        selected = selectedPeriod == BudgetPeriod.WEEKLY.name,
                        onClick = { selectedPeriod = BudgetPeriod.WEEKLY.name },
                        label = { Text("Weekly") },
                        modifier = Modifier.weight(1f).testTag("period_weekly_chip")
                    )
                }

                // Amount input
                OutlinedTextField(
                    value = amountText,
                    onValueChange = {
                        amountText = it.filter { char -> char.isDigit() || char == '.' }
                        errorMessage = null
                    },
                    label = { Text("Budget Amount (${BudgetPeriodHelper.CURRENCY_SYMBOL})") },
                    placeholder = { Text("e.g. 5000") },
                    leadingIcon = { Text(BudgetPeriodHelper.CURRENCY_SYMBOL, style = MaterialTheme.typography.titleMedium) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    isError = errorMessage != null,
                    supportingText = { errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) } },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("budget_amount_input"),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amount = amountText.toDoubleOrNull()
                    if (amount == null || amount <= 0.0) {
                        errorMessage = "Enter a valid amount greater than 0"
                        return@Button
                    }
                    onConfirm(amount, selectedPeriod)
                },
                modifier = Modifier.testTag("save_budget_button")
            ) {
                Text("Save Budget")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun AddEditExpenseDialog(
    expense: FoodExpense?,
    onDismiss: () -> Unit,
    onConfirm: (name: String, amount: Double, category: String, date: Long, notes: String?) -> Unit
) {
    var name by remember { mutableStateOf(expense?.name ?: "") }
    var amountText by remember { mutableStateOf(expense?.amount?.toString()?.removeSuffix(".0") ?: "") }
    var selectedCategory by remember {
        mutableStateOf(expense?.category ?: "Groceries")
    }
    var notes by remember { mutableStateOf(expense?.notes ?: "") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (expense == null) "Add Expense" else "Edit Expense") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        if (expense == null && it.isNotBlank()) {
                            selectedCategory = ExpenseCategoryHelper.inferCategory(it)
                        }
                        errorMessage = null
                    },
                    label = { Text("Item / Place Name") },
                    placeholder = { Text("e.g. Vegetables, Milk, Cafe dinner") },
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    isError = errorMessage != null && name.isBlank(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("expense_name_input"),
                    singleLine = true
                )

                OutlinedTextField(
                    value = amountText,
                    onValueChange = {
                        amountText = it.filter { char -> char.isDigit() || char == '.' }
                        errorMessage = null
                    },
                    label = { Text("Amount (${BudgetPeriodHelper.CURRENCY_SYMBOL})") },
                    placeholder = { Text("e.g. 250") },
                    leadingIcon = { Text(BudgetPeriodHelper.CURRENCY_SYMBOL, style = MaterialTheme.typography.titleMedium) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("expense_amount_input"),
                    singleLine = true
                )

                Text(
                    text = "Category",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(ExpenseCategoryHelper.ALL_CATEGORIES) { category ->
                        FilterChip(
                            selected = selectedCategory == category,
                            onClick = { selectedCategory = category },
                            label = { Text("${getCategoryEmoji(category)} $category") },
                            modifier = Modifier.testTag("category_chip_$category")
                        )
                    }
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Optional Notes") },
                    placeholder = { Text("e.g. Purchased at local supermarket") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 2
                )

                if (errorMessage != null) {
                    Text(
                        text = errorMessage!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isBlank()) {
                        errorMessage = "Please enter an expense name"
                        return@Button
                    }
                    val amount = amountText.toDoubleOrNull()
                    if (amount == null || amount <= 0.0) {
                        errorMessage = "Enter a valid amount greater than 0"
                        return@Button
                    }
                    onConfirm(
                        name.trim(),
                        amount,
                        selectedCategory,
                        expense?.date ?: System.currentTimeMillis(),
                        notes.ifBlank { null }
                    )
                },
                modifier = Modifier.testTag("save_expense_button")
            ) {
                Text(if (expense == null) "Add Expense" else "Save Changes")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

private fun getCategoryEmoji(category: String): String {
    return when (category) {
        "Groceries" -> "🛒"
        "Fruits & Vegetables" -> "🥦"
        "Dairy" -> "🥛"
        "Meat & Seafood" -> "🥩"
        "Snacks" -> "🥨"
        "Beverages" -> "🥤"
        "Eating Out" -> "🍽️"
        else -> "🏷️"
    }
}

private fun getCategoryColor(category: String): Color {
    return when (category) {
        "Groceries" -> Color(0xFF4CAF50)
        "Fruits & Vegetables" -> Color(0xFF8BC34A)
        "Dairy" -> Color(0xFF03A9F4)
        "Meat & Seafood" -> Color(0xFFE91E63)
        "Snacks" -> Color(0xFFFF9800)
        "Beverages" -> Color(0xFF9C27B0)
        "Eating Out" -> Color(0xFFFF5722)
        else -> Color(0xFF607D8B)
    }
}
