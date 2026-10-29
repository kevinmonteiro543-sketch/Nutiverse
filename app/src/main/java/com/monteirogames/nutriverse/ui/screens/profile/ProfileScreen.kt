package com.monteirogames.nutriverse.ui.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.monteirogames.nutriverse.BuildConfig
import com.monteirogames.nutriverse.domain.model.SyncStatus
import com.monteirogames.nutriverse.notifications.NotificationScheduler
import com.monteirogames.nutriverse.ui.viewmodel.createViewModel
import com.monteirogames.nutriverse.util.AppCheckDebugHelper
import kotlinx.coroutines.launch

@Composable
fun ProfileScreen(
    onNavigateToDietPlanner: () -> Unit,
    onNavigateToNotificationSettings: () -> Unit,
    onNavigateToBudget: () -> Unit = {},
    onNavigateToProgressAnalytics: () -> Unit = {},
    onNavigateToHealthReportAnalysis: () -> Unit = {},
    onNavigateToWeeklyReview: () -> Unit = {},
    onLogout: () -> Unit
) {
    val viewModel = createViewModel { app -> 
        ProfileViewModel(app.authUseCases, app.profileSyncUseCases) 
    }
    
    val context = LocalContext.current
    var showDebugDialog by remember { mutableStateOf(false) }
    val syncState by viewModel.syncState?.collectAsState() ?: remember { mutableStateOf(null) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // User Header Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Profile Avatar",
                            modifier = Modifier.size(40.dp),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "NutriVerse Member",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Healthy Nutrition & Wellness",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    // Cloud Sync Status Pill
                    Spacer(modifier = Modifier.height(12.dp))
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.testTag("profile_sync_status_indicator")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            val statusText = when (syncState?.status) {
                                SyncStatus.SYNCING -> "Syncing profile..."
                                SyncStatus.SUCCESS -> "Profile synced with cloud"
                                SyncStatus.ERROR -> "Offline (saved locally)"
                                SyncStatus.IDLE, null -> "Cloud backup ready"
                            }
                            val icon = when (syncState?.status) {
                                SyncStatus.SYNCING -> Icons.Default.Sync
                                SyncStatus.SUCCESS -> Icons.Default.CloudDone
                                SyncStatus.ERROR -> Icons.Default.CloudOff
                                SyncStatus.IDLE, null -> Icons.Default.CloudQueue
                            }
                            Icon(
                                imageVector = icon,
                                contentDescription = "Sync status",
                                tint = when (syncState?.status) {
                                    SyncStatus.ERROR -> MaterialTheme.colorScheme.error
                                    SyncStatus.SUCCESS -> MaterialTheme.colorScheme.primary
                                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = statusText,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // Section 1: AI & Health Intelligence
        item {
            Text(
                text = "AI & Health Intelligence",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 4.dp, top = 4.dp)
            )
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column {
                    ProfileMenuRow(
                        icon = Icons.Default.AutoAwesome,
                        iconTint = MaterialTheme.colorScheme.primary,
                        title = "Weekly AI Review",
                        subtitle = "7-day personalized nutrition coaching",
                        onClick = onNavigateToWeeklyReview
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                    ProfileMenuRow(
                        icon = Icons.Default.MedicalServices,
                        iconTint = MaterialTheme.colorScheme.primary,
                        title = "Health Report Analysis",
                        subtitle = "Analyze medical tests & blood reports",
                        onClick = onNavigateToHealthReportAnalysis
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                    ProfileMenuRow(
                        icon = Icons.Default.Psychology,
                        iconTint = MaterialTheme.colorScheme.primary,
                        title = "AI Diet Planner",
                        subtitle = "Tailored caloric and macronutrient plans",
                        onClick = onNavigateToDietPlanner
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                    ProfileMenuRow(
                        icon = Icons.Default.Insights,
                        iconTint = MaterialTheme.colorScheme.primary,
                        title = "Progress Analytics",
                        subtitle = "Nutrition trends, calorie adherence & charts",
                        onClick = onNavigateToProgressAnalytics
                    )
                }
            }
        }

        // Section 2: Lifestyle & Management
        item {
            Text(
                text = "Lifestyle & Management",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 4.dp, top = 4.dp)
            )
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column {
                    ProfileMenuRow(
                        icon = Icons.Default.AccountBalanceWallet,
                        iconTint = MaterialTheme.colorScheme.tertiary,
                        title = "Food Budget Tracker",
                        subtitle = "Manage grocery spending limits & expenses",
                        onClick = onNavigateToBudget
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                    ProfileMenuRow(
                        icon = Icons.Default.Notifications,
                        iconTint = MaterialTheme.colorScheme.secondary,
                        title = "Notification Settings",
                        subtitle = "Reminders for meals & pantry expiration",
                        onClick = onNavigateToNotificationSettings
                    )
                }
            }
        }

        // Section 3: Account & Session
        item {
            Text(
                text = "Account",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 4.dp, top = 4.dp)
            )
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column {
                    ProfileMenuRow(
                        icon = Icons.AutoMirrored.Filled.Logout,
                        iconTint = MaterialTheme.colorScheme.error,
                        title = "Logout",
                        subtitle = "Sign out from this device",
                        titleColor = MaterialTheme.colorScheme.error,
                        onClick = {
                            NotificationScheduler.cancelAllScheduledWork(context)
                            viewModel.logout()
                            onLogout()
                        }
                    )
                }
            }
        }

        // Developer Section (when BuildConfig.DEBUG)
        if (BuildConfig.DEBUG) {
            item {
                OutlinedButton(
                    onClick = { showDebugDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Developer: App Check Token")
                }
            }
        }
    }

    if (showDebugDialog) {
        AppCheckDebugDialog(onDismiss = { showDebugDialog = false })
    }
}

@Composable
private fun ProfileMenuRow(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    subtitle: String,
    titleColor: Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(iconTint.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = titleColor
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
fun AppCheckDebugDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val coroutineScope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(true) }
    var token by remember { mutableStateOf<String?>(null) }
    var copied by remember { mutableStateOf(false) }

    fun loadToken() {
        coroutineScope.launch {
            isLoading = true
            copied = false
            token = AppCheckDebugHelper.getDebugToken(context)
            isLoading = false
        }
    }

    LaunchedEffect(Unit) {
        loadToken()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("App Check Debug Token") },
        text = {
            if (isLoading) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(16.dp))
                    Text("Retrieving SDK debug token...", style = MaterialTheme.typography.bodyMedium)
                }
            } else if (token != null) {
                val currentToken = token!!
                Column {
                    Text(
                        "Official SDK Debug Secret:",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    SelectionContainer {
                        Text(
                            text = currentToken,
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                                .padding(12.dp)
                        )
                    }
                    if (copied) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Copied to clipboard!",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            } else {
                Text(
                    "Failed to retrieve debug token from logcat. Ensure the app has initialized Firebase App Check.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
            }
        },
        confirmButton = {
            if (token != null) {
                TextButton(onClick = {
                    clipboardManager.setText(AnnotatedString(token!!))
                    copied = true
                }) {
                    Text("Copy")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}
