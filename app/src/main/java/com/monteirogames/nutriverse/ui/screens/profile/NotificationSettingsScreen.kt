package com.monteirogames.nutriverse.ui.screens.profile

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.monteirogames.nutriverse.notifications.NotificationHelper
import com.monteirogames.nutriverse.ui.viewmodel.createViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationSettingsScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val viewModel = createViewModel { app ->
        NotificationSettingsViewModel(
            authUseCases = app.authUseCases,
            notificationUseCases = app.notificationUseCases
        )
    }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    // Check permission on resume/compose
    LaunchedEffect(Unit) {
        viewModel.updatePermissionState(NotificationHelper.hasNotificationPermission(context))
    }

    LaunchedEffect(uiState.infoMessage) {
        uiState.infoMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearInfoMessage()
        }
    }

    // Permission launcher for Android 13+
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        viewModel.updatePermissionState(isGranted)
        if (isGranted) {
            viewModel.triggerBackgroundEvaluation(context)
        }
    }

    var timePickerTarget by remember { mutableStateOf<Pair<String, String>?>(null) } // mealType to currentTime

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Notification Settings",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("notification_settings_back_button")
                    ) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
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
                contentPadding = PaddingValues(top = 8.dp, bottom = 48.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header Explanation Banner
                item {
                    Text(
                        text = "NutriVerse can remind you about planned meals, daily nutrition, pantry expiry, groceries, and your food budget.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Android 13+ Permission Banner
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !uiState.hasPermission) {
                    item {
                        PermissionRequiredCard(
                            onRequestPermission = {
                                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            }
                        )
                    }
                }

                // Master Toggle Card
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth().testTag("master_notification_card"),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (uiState.settings.notificationsEnabled) {
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant
                            }
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (uiState.settings.notificationsEnabled) Icons.Default.NotificationsActive else Icons.Default.NotificationsOff,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "Allow Notifications",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = if (uiState.settings.notificationsEnabled) "All reminders active" else "All reminders paused",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Switch(
                                checked = uiState.settings.notificationsEnabled,
                                onCheckedChange = { isChecked ->
                                    if (isChecked && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !uiState.hasPermission) {
                                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    }
                                    viewModel.updateMasterToggle(isChecked, context)
                                },
                                modifier = Modifier.testTag("master_notification_switch")
                            )
                        }
                    }
                }

                // Sub-Settings (Only active when master switch is ON)
                item {
                    Text(
                        text = "Notification Types",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                // 1. Meal Reminders Card
                item {
                    NotificationTypeCard(
                        icon = Icons.Outlined.Restaurant,
                        iconColor = Color(0xFFFF9800),
                        title = "Meal Reminders",
                        description = "Remind you around planned meal times if the meal has not been logged.",
                        enabled = uiState.settings.notificationsEnabled,
                        checked = uiState.settings.mealRemindersEnabled,
                        onCheckedChange = { viewModel.updateMealReminders(it) },
                        testTag = "meal_reminders_switch"
                    ) {
                        AnimatedVisibility(visible = uiState.settings.notificationsEnabled && uiState.settings.mealRemindersEnabled) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Reminder Times",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold
                                )

                                MealTimeRow(
                                    mealName = "Breakfast",
                                    time = uiState.settings.breakfastTime,
                                    onClick = { timePickerTarget = Pair("Breakfast", uiState.settings.breakfastTime) },
                                    testTag = "breakfast_time_row"
                                )
                                MealTimeRow(
                                    mealName = "Lunch",
                                    time = uiState.settings.lunchTime,
                                    onClick = { timePickerTarget = Pair("Lunch", uiState.settings.lunchTime) },
                                    testTag = "lunch_time_row"
                                )
                                MealTimeRow(
                                    mealName = "Dinner",
                                    time = uiState.settings.dinnerTime,
                                    onClick = { timePickerTarget = Pair("Dinner", uiState.settings.dinnerTime) },
                                    testTag = "dinner_time_row"
                                )
                            }
                        }
                    }
                }

                // 2. Nutrition Reminders Card
                item {
                    NotificationTypeCard(
                        icon = Icons.Outlined.FitnessCenter,
                        iconColor = Color(0xFF2196F3),
                        title = "Nutrition & Protein Check",
                        description = "Gentle evening reminder when you're significantly below your daily protein target.",
                        enabled = uiState.settings.notificationsEnabled,
                        checked = uiState.settings.nutritionRemindersEnabled,
                        onCheckedChange = { viewModel.updateNutritionReminders(it) },
                        testTag = "nutrition_reminders_switch"
                    )
                }

                // 3. Pantry Expiry Card
                item {
                    NotificationTypeCard(
                        icon = Icons.Outlined.Kitchen,
                        iconColor = Color(0xFF4CAF50),
                        title = "Pantry Expiry Alerts",
                        description = "Alerts 3 days before, 1 day before, and when items in your pantry expire.",
                        enabled = uiState.settings.notificationsEnabled,
                        checked = uiState.settings.pantryExpiryEnabled,
                        onCheckedChange = { viewModel.updatePantryExpiry(it) },
                        testTag = "pantry_expiry_switch"
                    )
                }

                // 4. Grocery Reminders Card
                item {
                    NotificationTypeCard(
                        icon = Icons.Outlined.ShoppingCart,
                        iconColor = Color(0xFF9C27B0),
                        title = "Grocery Planner Reminders",
                        description = "Daily reminder when you have pending unpurchased items on your grocery list.",
                        enabled = uiState.settings.notificationsEnabled,
                        checked = uiState.settings.groceryRemindersEnabled,
                        onCheckedChange = { viewModel.updateGroceryReminders(it) },
                        testTag = "grocery_reminders_switch"
                    )
                }

                // 5. Budget Warnings Card
                item {
                    NotificationTypeCard(
                        icon = Icons.Outlined.AccountBalanceWallet,
                        iconColor = Color(0xFF009688),
                        title = "Food Budget Warnings",
                        description = "Alerts when spending reaches 75%, 90%, and 100% of your current food budget.",
                        enabled = uiState.settings.notificationsEnabled,
                        checked = uiState.settings.budgetWarningsEnabled,
                        onCheckedChange = { viewModel.updateBudgetWarnings(it) },
                        testTag = "budget_warnings_switch"
                    )
                }

                // Test Notification Action
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { viewModel.sendTestNotification(context) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("send_test_notification_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Send Test Notification")
                    }
                }
            }
        }
    }

    // Material 3 Time Picker Dialog
    timePickerTarget?.let { (mealType, currentTime) ->
        val parts = currentTime.split(":")
        val initialHour = parts.getOrNull(0)?.toIntOrNull() ?: 8
        val initialMinute = parts.getOrNull(1)?.toIntOrNull() ?: 0

        val timePickerState = rememberTimePickerState(
            initialHour = initialHour,
            initialMinute = initialMinute,
            is24Hour = true
        )

        Dialog(onDismissRequest = { timePickerTarget = null }) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp,
                modifier = Modifier.padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Set $mealType Time",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.align(Alignment.Start)
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    TimePicker(state = timePickerState)

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { timePickerTarget = null }) {
                            Text("Cancel")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                val formatted = String.format("%02d:%02d", timePickerState.hour, timePickerState.minute)
                                viewModel.updateMealTime(mealType, formatted)
                                timePickerTarget = null
                            },
                            modifier = Modifier.testTag("save_meal_time_button")
                        ) {
                            Text("Save")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PermissionRequiredCard(onRequestPermission: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("permission_required_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.NotificationsNone,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.tertiary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Notification Permission Needed",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Android requires your permission to display reminders for meals, expiring groceries, and budget updates.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onTertiaryContainer
            )
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = onRequestPermission,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("grant_notification_permission_button")
            ) {
                Text("Enable Notifications")
            }
        }
    }
}

@Composable
private fun NotificationTypeCard(
    icon: ImageVector,
    iconColor: Color,
    title: String,
    description: String,
    enabled: Boolean,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String,
    content: @Composable () -> Unit = {}
) {
    val cardAlpha = if (enabled) 1f else 0.4f

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .background(iconColor.copy(alpha = 0.15f * cardAlpha), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = iconColor.copy(alpha = cardAlpha),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = cardAlpha)
                        )
                        Text(
                            text = description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = cardAlpha)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                Switch(
                    checked = checked && enabled,
                    onCheckedChange = onCheckedChange,
                    enabled = enabled,
                    modifier = Modifier.testTag(testTag)
                )
            }

            content()
        }
    }
}

@Composable
private fun MealTimeRow(
    mealName: String,
    time: String,
    onClick: () -> Unit,
    testTag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp, horizontal = 4.dp)
            .testTag(testTag),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Outlined.AccessTime,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = mealName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
        }
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.padding(2.dp)
        ) {
            Text(
                text = time,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
            )
        }
    }
}
