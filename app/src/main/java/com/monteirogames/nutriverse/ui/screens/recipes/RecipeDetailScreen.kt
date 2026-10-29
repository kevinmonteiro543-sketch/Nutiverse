package com.monteirogames.nutriverse.ui.screens.recipes

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Kitchen
import androidx.compose.material.icons.outlined.RemoveCircleOutline
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.monteirogames.nutriverse.domain.model.Recipe
import com.monteirogames.nutriverse.domain.model.RecipeSuggestion
import com.monteirogames.nutriverse.ui.viewmodel.createViewModel
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipeDetailScreen(
    recipeId: String,
    onNavigateBack: () -> Unit,
    onNavigateToMeals: (() -> Unit)? = null,
    onNavigateToGroceryPlanner: (() -> Unit)? = null
) {
    val viewModel = createViewModel { app ->
        RecipeViewModel(
            recipeUseCases = app.recipeUseCases,
            mealTrackerUseCases = app.mealTrackerUseCases,
            authUseCases = app.authUseCases,
            userProfileUseCases = app.userProfileUseCases,
            pantryUseCases = app.pantryUseCases,
            groceryUseCases = app.groceryUseCases
        )
    }

    val recipeFlow = remember(recipeId) { viewModel.getRecipeById(recipeId) }
    val recipe by recipeFlow.collectAsStateWithLifecycle(initialValue = null)
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val suggestion = uiState.suggestionMap[recipeId]

    var currentServings by remember(recipe) {
        mutableIntStateOf(recipe?.servings ?: 1)
    }

    var showLogMealDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(recipe?.name ?: "Recipe Details") },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("recipe_detail_back_button")
                    ) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    recipe?.let { currentRecipe ->
                        IconButton(
                            onClick = {
                                viewModel.toggleFavorite(currentRecipe.id, currentRecipe.isFavorite)
                            },
                            modifier = Modifier.testTag("recipe_detail_favorite_button")
                        ) {
                            Icon(
                                imageVector = if (currentRecipe.isFavorite) Icons.Default.Favorite else Icons.Outlined.FavoriteBorder,
                                contentDescription = if (currentRecipe.isFavorite) "Remove favorite" else "Favorite",
                                tint = if (currentRecipe.isFavorite) Color(0xFFE91E63) else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            )
        },
        bottomBar = {
            recipe?.let { currentRecipe ->
                Surface(
                    shadowElevation = 8.dp,
                    tonalElevation = 2.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(modifier = Modifier.padding(16.dp)) {
                        Button(
                            onClick = { showLogMealDialog = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("log_meal_button")
                        ) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Log Meal to Tracker")
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        if (recipe == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            val currentRecipe = recipe!!
            val baseServings = currentRecipe.servings.coerceAtLeast(1)
            val scaleRatio = currentServings.toFloat() / baseServings.toFloat()

            val scaledCalories = (currentRecipe.calories * scaleRatio).roundToInt()
            val scaledProtein = (currentRecipe.protein * scaleRatio).roundToInt()
            val scaledCarbs = (currentRecipe.carbs * scaleRatio).roundToInt()
            val scaledFat = (currentRecipe.fat * scaleRatio).roundToInt()

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header Visual Banner
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primaryContainer,
                                    MaterialTheme.colorScheme.tertiaryContainer
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(76.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Restaurant,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(42.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            AssistChip(
                                onClick = {},
                                label = { Text("Prep: ${currentRecipe.prepTimeMinutes}m") }
                            )
                            AssistChip(
                                onClick = {},
                                label = { Text("Cook: ${currentRecipe.cookTimeMinutes}m") }
                            )
                            AssistChip(
                                onClick = {},
                                label = { Text(currentRecipe.difficulty) }
                            )
                        }
                    }
                }

                Column(modifier = Modifier.padding(16.dp)) {
                    // Recipe Name & Description
                    Text(
                        text = currentRecipe.name,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = currentRecipe.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Tags
                    if (currentRecipe.dietaryTags.isNotEmpty() || currentRecipe.allergyTags.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            currentRecipe.dietaryTags.take(3).forEach { tag ->
                                SuggestionChip(
                                    onClick = {},
                                    label = { Text(tag, style = MaterialTheme.typography.labelSmall) }
                                )
                            }
                            currentRecipe.allergyTags.take(2).forEach { tag ->
                                SuggestionChip(
                                    onClick = {},
                                    label = { Text(tag, style = MaterialTheme.typography.labelSmall) },
                                    colors = SuggestionChipDefaults.suggestionChipColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                                    )
                                )
                            }
                        }
                    }

                    // Smart Match Breakdown Card
                    if (suggestion != null) {
                        Spacer(modifier = Modifier.height(16.dp))
                        SmartMatchBreakdownCard(suggestion = suggestion)
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Servings Adjustment Card
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "Servings",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Nutrition scales proportionally",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    FilledTonalIconButton(
                                        onClick = { if (currentServings > 1) currentServings-- },
                                        enabled = currentServings > 1,
                                        modifier = Modifier.testTag("decrement_servings_button")
                                    ) {
                                        Icon(imageVector = Icons.Default.Remove, contentDescription = "Decrease")
                                    }
                                    Text(
                                        text = "$currentServings",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp)
                                    )
                                    FilledTonalIconButton(
                                        onClick = { if (currentServings < 12) currentServings++ },
                                        modifier = Modifier.testTag("increment_servings_button")
                                    ) {
                                        Icon(imageVector = Icons.Default.Add, contentDescription = "Increase")
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Scaled Nutrition Totals
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                MacroBox(label = "Calories", value = "$scaledCalories kcal")
                                MacroBox(label = "Protein", value = "${scaledProtein}g")
                                MacroBox(label = "Carbs", value = "${scaledCarbs}g")
                                MacroBox(label = "Fat", value = "${scaledFat}g")
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Ingredients Section with Pantry Status
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Ingredients",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        if (suggestion != null) {
                            Text(
                                text = "${suggestion.matchedPantryCount}/${suggestion.totalIngredientsCount} in pantry",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedButton(
                        onClick = {
                            viewModel.addMissingIngredientsToGrocery(currentRecipe) { count ->
                                coroutineScope.launch {
                                    val result = snackbarHostState.showSnackbar(
                                        message = if (count > 0) {
                                            "Added $count missing ingredients to your Grocery Planner!"
                                        } else {
                                            "All ingredients are already in your pantry or grocery list!"
                                        },
                                        actionLabel = if (count > 0 && onNavigateToGroceryPlanner != null) "View List" else null
                                    )
                                    if (result == SnackbarResult.ActionPerformed) {
                                        onNavigateToGroceryPlanner?.invoke()
                                    }
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .testTag("add_missing_ingredients_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddShoppingCart,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (suggestion != null && suggestion.missingIngredients.isNotEmpty()) {
                                "Add ${suggestion.missingIngredients.size} Missing to Grocery"
                            } else {
                                "Add Missing Ingredients to Grocery"
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            currentRecipe.ingredients.forEachIndexed { index, ing ->
                                val scaledQty = (ing.quantity * scaleRatio)
                                val formattedQty = if (scaledQty % 1f == 0f) {
                                    scaledQty.toInt().toString()
                                } else {
                                    String.format("%.1f", scaledQty)
                                }

                                val isInPantry = suggestion?.matchedIngredients?.contains(ing.name) == true
                                val isExpiring = suggestion?.expiringIngredients?.contains(ing.name) == true

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        if (suggestion != null) {
                                            if (isInPantry) {
                                                Icon(
                                                    imageVector = Icons.Outlined.CheckCircle,
                                                    contentDescription = "In Pantry",
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            } else {
                                                Icon(
                                                    imageVector = Icons.Outlined.RemoveCircleOutline,
                                                    contentDescription = "Missing",
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(8.dp))
                                        }

                                        Column {
                                            Text(
                                                text = ing.name,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Medium
                                            )
                                            if (isExpiring) {
                                                Text(
                                                    text = "Expiring soon in pantry",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.error,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }

                                    Text(
                                        text = "$formattedQty ${ing.unit}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                if (index < currentRecipe.ingredients.lastIndex) {
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Instructions Section
                    Text(
                        text = "Instructions",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        currentRecipe.instructions.forEach { step ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.Top
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "${step.stepNumber}",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = step.instructionText,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))
                }
            }

            // Log Meal Dialog
            if (showLogMealDialog) {
                LogRecipeMealDialog(
                    recipeName = currentRecipe.name,
                    initialServings = currentServings,
                    scaledCalories = scaledCalories,
                    scaledProtein = scaledProtein,
                    scaledCarbs = scaledCarbs,
                    scaledFat = scaledFat,
                    onDismiss = { showLogMealDialog = false },
                    onConfirm = { chosenServings, mealType ->
                        viewModel.logRecipeMeal(
                            recipe = currentRecipe,
                            servings = chosenServings,
                            mealType = mealType,
                            onSuccess = {
                                showLogMealDialog = false
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar(
                                        message = "Logged ${currentRecipe.name} ($chosenServings serv.) to $mealType!"
                                    )
                                }
                            }
                        )
                    }
                )
            }
        }
    }
}

@Composable
private fun SmartMatchBreakdownCard(suggestion: RecipeSuggestion) {
    ElevatedCard(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Smart Recommendation",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Based on your pantry, targets & preferences",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (suggestion.matchScore >= 75) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Text(
                        text = "${suggestion.matchScore}% Match",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Score breakdown pills
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                ScoreColumn(
                    label = "Pantry",
                    value = "${(suggestion.pantryScore * 2).roundToInt()}%",
                    sub = "${suggestion.matchedPantryCount}/${suggestion.totalIngredientsCount} items"
                )
                ScoreColumn(
                    label = "Diet Fit",
                    value = "${(suggestion.dietScore * 5).roundToInt()}%",
                    sub = "Safe & aligned"
                )
                ScoreColumn(
                    label = "Target Fit",
                    value = "${(suggestion.nutritionScore * 5).roundToInt()}%",
                    sub = "Calories & macros"
                )
                ScoreColumn(
                    label = "Goal Fit",
                    value = "${(suggestion.goalScore * 10).roundToInt()}%",
                    sub = "Satiety & goals"
                )
            }

            // Highlights
            if (suggestion.highlights.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(10.dp))
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    suggestion.highlights.forEach { highlight ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = highlight,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ScoreColumn(label: String, value: String, sub: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = sub,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
        )
    }
}

@Composable
private fun MacroBox(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LogRecipeMealDialog(
    recipeName: String,
    initialServings: Int,
    scaledCalories: Int,
    scaledProtein: Int,
    scaledCarbs: Int,
    scaledFat: Int,
    onDismiss: () -> Unit,
    onConfirm: (servings: Int, mealType: String) -> Unit
) {
    val mealTypes = listOf("Breakfast", "Lunch", "Dinner", "Snack")
    var selectedMealType by remember { mutableStateOf(mealTypes[1]) }
    var servings by remember { mutableIntStateOf(initialServings) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Log Meal") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = recipeName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "Select meal type:",
                    style = MaterialTheme.typography.labelMedium
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    mealTypes.forEach { type ->
                        FilterChip(
                            selected = selectedMealType == type,
                            onClick = { selectedMealType = type },
                            label = { Text(type, style = MaterialTheme.typography.labelSmall) },
                            modifier = Modifier.testTag("meal_type_chip_$type")
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Servings:")
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { if (servings > 1) servings-- }) {
                            Icon(imageVector = Icons.Default.Remove, contentDescription = "Decrease")
                        }
                        Text("$servings", fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp))
                        IconButton(onClick = { if (servings < 12) servings++ }) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = "Increase")
                        }
                    }
                }

                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("$scaledCalories kcal", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                        Text("${scaledProtein}g Protein", style = MaterialTheme.typography.bodySmall)
                        Text("${scaledCarbs}g Carbs", style = MaterialTheme.typography.bodySmall)
                        Text("${scaledFat}g Fat", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(servings, selectedMealType) },
                modifier = Modifier.testTag("confirm_log_meal_button")
            ) {
                Text("Log to Tracker")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
