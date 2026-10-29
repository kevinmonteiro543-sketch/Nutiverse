package com.monteirogames.nutriverse.ui.screens.profile

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.monteirogames.nutriverse.domain.model.DayPlan
import com.monteirogames.nutriverse.domain.model.DietPlan
import com.monteirogames.nutriverse.domain.model.DietPlanMeal
import com.monteirogames.nutriverse.domain.model.UserProfile
import com.monteirogames.nutriverse.domain.usecase.NutritionTargets
import com.monteirogames.nutriverse.ui.viewmodel.createViewModel
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DietPlannerScreen(
    onNavigateBack: () -> Unit
) {
    val viewModel = createViewModel { app ->
        DietPlannerViewModel(
            userProfileUseCases = app.userProfileUseCases,
            dietPlannerUseCases = app.dietPlannerUseCases,
            authUseCases = app.authUseCases
        )
    }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var editingMeal by remember { mutableStateOf<Pair<Int, DietPlanMeal>?>(null) }

    BackHandler {
        if (uiState is DietPlannerUiState.Reviewing) {
            viewModel.cancelReview()
        } else {
            onNavigateBack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("7-Day AI Diet Planner") },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            if (uiState is DietPlannerUiState.Reviewing) {
                                viewModel.cancelReview()
                            } else {
                                onNavigateBack()
                            }
                        },
                        modifier = Modifier.testTag("diet_planner_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (val state = uiState) {
                is DietPlannerUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(modifier = Modifier.testTag("diet_planner_loading"))
                    }
                }

                is DietPlannerUiState.IncompleteProfile -> {
                    IncompleteProfileView(
                        message = state.message,
                        onNavigateBack = onNavigateBack
                    )
                }

                is DietPlannerUiState.Generating -> {
                    GeneratingView(
                        targets = state.nutritionTargets,
                        preference = state.userProfile.dietaryPreference
                    )
                }

                is DietPlannerUiState.Reviewing -> {
                    ReviewPlanView(
                        userProfile = state.userProfile,
                        targets = state.nutritionTargets,
                        draftPlan = state.draftPlan,
                        selectedDay = state.selectedDay,
                        isSaving = state.isSaving,
                        onSelectDay = { viewModel.selectDay(it) },
                        onEditMeal = { dayNum, meal -> editingMeal = Pair(dayNum, meal) },
                        onSavePlan = { viewModel.savePlan() },
                        onDiscard = { viewModel.cancelReview() }
                    )
                }

                is DietPlannerUiState.Ready -> {
                    ReadyPlanView(
                        userProfile = state.userProfile,
                        targets = state.nutritionTargets,
                        activePlan = state.activePlan,
                        selectedDay = state.selectedDay,
                        onSelectDay = { viewModel.selectDay(it) },
                        onGeneratePlan = { viewModel.generate7DayPlan() }
                    )
                }

                is DietPlannerUiState.Error -> {
                    ErrorPlanView(
                        message = state.message,
                        onDismiss = { viewModel.dismissError() },
                        onRetry = { viewModel.generate7DayPlan() }
                    )
                }
            }
        }
    }

    editingMeal?.let { (dayNum, meal) ->
        EditMealDialog(
            dayNumber = dayNum,
            meal = meal,
            onDismiss = { editingMeal = null },
            onConfirm = { updated ->
                viewModel.updateMeal(dayNum, updated)
                editingMeal = null
            }
        )
    }
}

@Composable
private fun IncompleteProfileView(
    message: String,
    onNavigateBack: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Profile Incomplete",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = onNavigateBack,
                    modifier = Modifier.testTag("back_to_profile_button")
                ) {
                    Text("Go Back to Profile")
                }
            }
        }
    }
}

@Composable
private fun GeneratingView(
    targets: NutritionTargets,
    preference: String
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = "Crafting Your 7-Day Plan...",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Matching your daily target of ${targets.calories.roundToInt()} kcal (${targets.protein.roundToInt()}g protein, ${targets.carbs.roundToInt()}g carbs, ${targets.fat.roundToInt()}g fat).",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                if (preference.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Respecting preference: $preference",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }
    }
}

@Composable
private fun ReviewPlanView(
    userProfile: UserProfile,
    targets: NutritionTargets,
    draftPlan: DietPlan,
    selectedDay: Int,
    isSaving: Boolean,
    onSelectDay: (Int) -> Unit,
    onEditMeal: (Int, DietPlanMeal) -> Unit,
    onSavePlan: () -> Unit,
    onDiscard: () -> Unit
) {
    val currentDayPlan = draftPlan.days.find { it.dayNumber == selectedDay }

    Column(modifier = Modifier.fillMaxSize()) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onTertiaryContainer
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Review & Customize (Estimated Values)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                    Text(
                        text = "AI nutrition estimates. You can edit any meal before saving.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                }
            }
        }

        currentDayPlan?.let { day ->
            DayMacroTargetComparisonCard(
                dayNumber = day.dayNumber,
                dayPlan = day,
                targets = targets
            )
        }

        ScrollableTabRow(
            selectedTabIndex = (selectedDay - 1).coerceIn(0, 6),
            edgePadding = 16.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            (1..7).forEach { dayNum ->
                Tab(
                    selected = selectedDay == dayNum,
                    onClick = { onSelectDay(dayNum) },
                    text = { Text("Day $dayNum") },
                    modifier = Modifier.testTag("review_tab_day_$dayNum")
                )
            }
        }

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (currentDayPlan != null) {
                items(currentDayPlan.meals, key = { it.id }) { meal ->
                    MealItemCard(
                        meal = meal,
                        isEditable = true,
                        onEdit = { onEditMeal(selectedDay, meal) }
                    )
                }
            } else {
                item {
                    Text(
                        text = "No meals generated for Day $selectedDay.",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }

        Surface(
            shadowElevation = 8.dp,
            tonalElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onDiscard,
                    enabled = !isSaving,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("discard_plan_button")
                ) {
                    Text("Discard")
                }
                Button(
                    onClick = onSavePlan,
                    enabled = !isSaving,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("save_plan_button")
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Saving...")
                    } else {
                        Icon(imageVector = Icons.Default.Check, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Save Plan")
                    }
                }
            }
        }
    }
}

@Composable
private fun ReadyPlanView(
    userProfile: UserProfile,
    targets: NutritionTargets,
    activePlan: DietPlan?,
    selectedDay: Int,
    onSelectDay: (Int) -> Unit,
    onGeneratePlan: () -> Unit
) {
    val currentDayPlan = activePlan?.days?.find { it.dayNumber == selectedDay }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        TargetSummaryCard(
            profile = userProfile,
            targets = targets
        )

        Spacer(modifier = Modifier.height(12.dp))

        if (activePlan == null) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Restaurant,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No Active 7-Day Plan",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Tap below to generate a tailored 7-day meal plan respecting your calorie targets, dietary preference (${if (userProfile.dietaryPreference.isNotBlank()) userProfile.dietaryPreference else "None"}), and allergies.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = onGeneratePlan,
                        modifier = Modifier.testTag("generate_7_day_plan_button")
                    ) {
                        Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Generate 7-Day Plan")
                    }
                }
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Active Plan: Day $selectedDay",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                OutlinedButton(
                    onClick = onGeneratePlan,
                    modifier = Modifier.testTag("regenerate_plan_button")
                ) {
                    Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Regenerate")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            currentDayPlan?.let { day ->
                DayMacroTargetComparisonCard(
                    dayNumber = day.dayNumber,
                    dayPlan = day,
                    targets = targets
                )
            }

            ScrollableTabRow(
                selectedTabIndex = (selectedDay - 1).coerceIn(0, 6),
                edgePadding = 8.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                (1..7).forEach { dayNum ->
                    Tab(
                        selected = selectedDay == dayNum,
                        onClick = { onSelectDay(dayNum) },
                        text = { Text("Day $dayNum") }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (currentDayPlan != null) {
                    items(currentDayPlan.meals, key = { it.id }) { meal ->
                        MealItemCard(
                            meal = meal,
                            isEditable = false,
                            onEdit = {}
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TargetSummaryCard(
    profile: UserProfile,
    targets: NutritionTargets
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Daily Nutrition Targets",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Goal: ${profile.goal}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                MacroChip(label = "Calories", value = "${targets.calories.roundToInt()} kcal")
                MacroChip(label = "Protein", value = "${targets.protein.roundToInt()}g")
                MacroChip(label = "Carbs", value = "${targets.carbs.roundToInt()}g")
                MacroChip(label = "Fat", value = "${targets.fat.roundToInt()}g")
            }
            if (profile.dietaryPreference.isNotBlank() || profile.allergies.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = buildString {
                        if (profile.dietaryPreference.isNotBlank()) append("Diet: ${profile.dietaryPreference}. ")
                        if (profile.allergies.isNotBlank()) append("Allergies: ${profile.allergies}.")
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun DayMacroTargetComparisonCard(
    dayNumber: Int,
    dayPlan: DayPlan,
    targets: NutritionTargets
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "Day $dayNumber Estimated Totals (vs Target):",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Cal: ${dayPlan.totalCalories.roundToInt()} / ${targets.calories.roundToInt()}",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = "P: ${dayPlan.totalProtein.roundToInt()} / ${targets.protein.roundToInt()}g",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = "C: ${dayPlan.totalCarbs.roundToInt()} / ${targets.carbs.roundToInt()}g",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = "F: ${dayPlan.totalFat.roundToInt()} / ${targets.fat.roundToInt()}g",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
private fun MealItemCard(
    meal: DietPlanMeal,
    isEditable: Boolean,
    onEdit: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SuggestionChip(
                    onClick = {},
                    label = { Text(meal.mealType, style = MaterialTheme.typography.labelSmall) }
                )
                if (isEditable) {
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.testTag("edit_meal_${meal.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Meal",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Text(
                text = meal.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "${meal.calories.roundToInt()} kcal (est.)",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "P: ${meal.protein.roundToInt()}g",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "C: ${meal.carbs.roundToInt()}g",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "F: ${meal.fat.roundToInt()}g",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (meal.prepTip.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Tip: ${meal.prepTip}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun MacroChip(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun ErrorPlanView(
    message: String,
    onDismiss: () -> Unit,
    onRetry: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Plan Generation Error",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
                Spacer(modifier = Modifier.height(24.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(onClick = onDismiss) {
                        Text("Dismiss")
                    }
                    Button(
                        onClick = onRetry,
                        modifier = Modifier.testTag("retry_generate_button")
                    ) {
                        Text("Try Again")
                    }
                }
            }
        }
    }
}

@Composable
private fun EditMealDialog(
    dayNumber: Int,
    meal: DietPlanMeal,
    onDismiss: () -> Unit,
    onConfirm: (DietPlanMeal) -> Unit
) {
    var name by remember { mutableStateOf(meal.name) }
    var mealType by remember { mutableStateOf(meal.mealType) }
    var caloriesText by remember { mutableStateOf(meal.calories.roundToInt().toString()) }
    var proteinText by remember { mutableStateOf(meal.protein.roundToInt().toString()) }
    var carbsText by remember { mutableStateOf(meal.carbs.roundToInt().toString()) }
    var fatText by remember { mutableStateOf(meal.fat.roundToInt().toString()) }
    var prepTip by remember { mutableStateOf(meal.prepTip) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Day $dayNumber Meal") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Meal Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("edit_meal_name")
                )

                OutlinedTextField(
                    value = mealType,
                    onValueChange = { mealType = it },
                    label = { Text("Meal Type (Breakfast, Lunch, Dinner, Snack)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = caloriesText,
                        onValueChange = { caloriesText = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Calories") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f).testTag("edit_meal_calories")
                    )
                    OutlinedTextField(
                        value = proteinText,
                        onValueChange = { proteinText = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Protein (g)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = carbsText,
                        onValueChange = { carbsText = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Carbs (g)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = fatText,
                        onValueChange = { fatText = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Fat (g)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = prepTip,
                    onValueChange = { prepTip = it },
                    label = { Text("Preparation Tip") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val updated = meal.copy(
                        name = name.ifBlank { meal.name },
                        mealType = mealType.ifBlank { meal.mealType },
                        calories = caloriesText.toFloatOrNull() ?: meal.calories,
                        protein = proteinText.toFloatOrNull() ?: meal.protein,
                        carbs = carbsText.toFloatOrNull() ?: meal.carbs,
                        fat = fatText.toFloatOrNull() ?: meal.fat,
                        prepTip = prepTip
                    )
                    onConfirm(updated)
                },
                modifier = Modifier.testTag("confirm_edit_meal_button")
            ) {
                Text("Save Changes")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
