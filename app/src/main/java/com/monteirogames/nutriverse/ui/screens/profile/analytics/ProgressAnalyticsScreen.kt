package com.monteirogames.nutriverse.ui.screens.profile.analytics

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.monteirogames.nutriverse.domain.model.AnalyticsTimeRange
import com.monteirogames.nutriverse.domain.model.DailyNutritionRecord
import com.monteirogames.nutriverse.domain.model.ProgressAnalyticsData
import com.monteirogames.nutriverse.ui.viewmodel.createViewModel
import kotlin.math.max
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProgressAnalyticsScreen(
    onNavigateBack: () -> Unit
) {
    val viewModel = createViewModel { app ->
        ProgressAnalyticsViewModel(
            authUseCases = app.authUseCases,
            progressAnalyticsUseCases = app.progressAnalyticsUseCases
        )
    }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { error ->
            snackbarHostState.showSnackbar(error)
            viewModel.clearErrorMessage()
        }
    }

    Scaffold(
        modifier = Modifier.testTag("progress_analytics_screen"),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Progress Analytics",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("analytics_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to Profile"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        if (uiState.isLoading && uiState.analyticsData == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(modifier = Modifier.testTag("analytics_loading_indicator"))
            }
        } else {
            val data = uiState.analyticsData
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(bottom = 32.dp)
            ) {
                // Time Range Selector
                item {
                    TimeRangeSegmentedControl(
                        selectedRange = uiState.timeRange,
                        onRangeSelected = { viewModel.setTimeRange(it) }
                    )
                }

                if (data == null || !data.hasSufficientData) {
                    item {
                        EmptyAnalyticsCard(
                            data = data,
                            timeRange = uiState.timeRange
                        )
                    }
                } else {
                    // Overview header card
                    item {
                        TrackingOverviewCard(data = data)
                    }

                    // Nutrition Summary Card
                    item {
                        NutritionSummaryCard(data = data)
                    }

                    // Calorie Analytics & Trend Chart
                    item {
                        CalorieAnalyticsCard(data = data)
                    }

                    // Macronutrient Analytics
                    item {
                        MacronutrientAnalyticsCard(data = data)
                    }

                    // Adherence Cards (Protein + Calorie Range)
                    item {
                        AdherenceMetricsCard(data = data)
                    }

                    // Deterministic Insights
                    item {
                        InsightsCard(insights = data.insights)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimeRangeSegmentedControl(
    selectedRange: AnalyticsTimeRange,
    onRangeSelected: (AnalyticsTimeRange) -> Unit
) {
    SingleChoiceSegmentedButtonRow(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("analytics_time_range_selector")
    ) {
        AnalyticsTimeRange.entries.forEachIndexed { index, range ->
            SegmentedButton(
                shape = SegmentedButtonDefaults.itemShape(
                    index = index,
                    count = AnalyticsTimeRange.entries.size
                ),
                onClick = { onRangeSelected(range) },
                selected = range == selectedRange,
                modifier = Modifier.testTag("time_range_${range.name.lowercase()}")
            ) {
                Text(range.label, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
fun TrackingOverviewCard(data: ProgressAnalyticsData) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("tracking_overview_card"),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Tracked Days",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = "${data.trackedDaysCount} of ${data.totalCalendarDays} calendar days recorded",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                }
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(54.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        val pct = ((data.trackedDaysCount.toFloat() / data.totalCalendarDays) * 100f).roundToInt()
                        Text(
                            text = "$pct%",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            LinearProgressIndicator(
                progress = { (data.trackedDaysCount.toFloat() / data.totalCalendarDays).coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(CircleShape),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f)
            )

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Tracked days represent days with meal entries. Untracked days are not counted as failed days.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
fun NutritionSummaryCard(data: ProgressAnalyticsData) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("nutrition_summary_card"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Nutrition Summary",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Average daily intake vs configured target",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SummaryMetricItem(
                    label = "Calories",
                    value = "${data.averageCalories.roundToInt()}",
                    target = "${data.targetCalories.roundToInt()} kcal",
                    modifier = Modifier.weight(1f)
                )
                SummaryMetricItem(
                    label = "Protein",
                    value = "${data.averageProtein.roundToInt()}g",
                    target = "${data.targetProtein.roundToInt()}g",
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SummaryMetricItem(
                    label = "Carbs",
                    value = "${data.averageCarbs.roundToInt()}g",
                    target = "${data.targetCarbs.roundToInt()}g",
                    modifier = Modifier.weight(1f)
                )
                SummaryMetricItem(
                    label = "Fat",
                    value = "${data.averageFat.roundToInt()}g",
                    target = "${data.targetFat.roundToInt()}g",
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun SummaryMetricItem(
    label: String,
    value: String,
    target: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Target: $target",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun CalorieAnalyticsCard(data: ProgressAnalyticsData) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("calorie_analytics_card")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Calorie Trend",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Daily intake and target reference",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Text(
                        text = "Target: ${data.targetCalories.roundToInt()} kcal",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Calorie trend chart
            CalorieTrendChart(
                records = data.dailyRecords,
                targetCalories = data.targetCalories
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Extreme days and quick comparisons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                data.highestCalorieDay?.let { high ->
                    StatPill(
                        label = "Highest",
                        value = "${high.dayOfWeek}, ${high.dateLabel}",
                        subValue = "${high.calories.roundToInt()} kcal",
                        icon = Icons.Default.TrendingUp,
                        modifier = Modifier.weight(1f)
                    )
                }
                data.lowestCalorieDay?.let { low ->
                    StatPill(
                        label = "Lowest",
                        value = "${low.dayOfWeek}, ${low.dateLabel}",
                        subValue = "${low.calories.roundToInt()} kcal",
                        icon = Icons.Default.TrendingDown,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
fun CalorieTrendChart(
    records: List<DailyNutritionRecord>,
    targetCalories: Float
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val tertiaryColor = MaterialTheme.colorScheme.tertiary
    val outlineColor = MaterialTheme.colorScheme.outlineVariant
    val targetLineColor = MaterialTheme.colorScheme.error

    val maxCalorie = max(
        targetCalories * 1.25f,
        (records.maxOfOrNull { it.calories } ?: 0f) * 1.15f
    ).coerceAtLeast(1000f)

    val scrollState = rememberScrollState()
    val isWide = records.size > 14
    val chartWidth = if (isWide) (records.size * 32).dp else 320.dp

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .then(if (isWide) Modifier.horizontalScroll(scrollState) else Modifier)
            .testTag("calorie_chart_canvas")
    ) {
        Canvas(
            modifier = Modifier
                .width(if (isWide) chartWidth else 320.dp)
                .fillMaxHeight()
                .padding(top = 16.dp, bottom = 24.dp, start = 8.dp, end = 8.dp)
        ) {
            val chartHeight = size.height
            val availableWidth = size.width
            val barCount = records.size
            val barSpacing = availableWidth / barCount
            val barWidth = (barSpacing * 0.65f).coerceIn(12f, 32f)

            // Draw target line
            val targetY = chartHeight - (targetCalories / maxCalorie) * chartHeight
            if (targetY in 0f..chartHeight) {
                drawLine(
                    color = targetLineColor,
                    start = Offset(0f, targetY),
                    end = Offset(availableWidth, targetY),
                    strokeWidth = 2f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f)
                )
            }

            // Draw bars
            records.forEachIndexed { index, record ->
                val centerX = index * barSpacing + barSpacing / 2f
                val left = centerX - barWidth / 2f

                if (record.hasData) {
                    val barHeight = (record.calories / maxCalorie) * chartHeight
                    val top = chartHeight - barHeight

                    val barColor = when {
                        record.isCalorieTargetInRange -> primaryColor
                        record.calories > targetCalories * 1.1f -> tertiaryColor
                        else -> secondaryColor
                    }

                    drawRoundRect(
                        color = barColor,
                        topLeft = Offset(left, top),
                        size = Size(barWidth, barHeight),
                        cornerRadius = CornerRadius(6f, 6f)
                    )
                } else {
                    // Empty state dashed placeholder bar
                    val placeholderHeight = 12f
                    val top = chartHeight - placeholderHeight
                    drawRoundRect(
                        color = outlineColor,
                        topLeft = Offset(left, top),
                        size = Size(barWidth, placeholderHeight),
                        cornerRadius = CornerRadius(4f, 4f)
                    )
                }

                // Day of week / date label
                val label = if (records.size <= 7) record.dayOfWeek else record.dateLabel.split(" ").lastOrNull() ?: ""
                drawContext.canvas.nativeCanvas.apply {
                    val paint = android.graphics.Paint().apply {
                        color = outlineColor.toArgb()
                        textSize = 24f
                        textAlign = android.graphics.Paint.Align.CENTER
                        isAntiAlias = true
                    }
                    drawText(label, centerX, chartHeight + 22f, paint)
                }
            }
        }
    }
}

@Composable
fun StatPill(
    label: String,
    value: String,
    subValue: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = subValue,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun MacronutrientAnalyticsCard(data: ProgressAnalyticsData) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("macronutrient_analytics_card")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Macronutrient Breakdown",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Average intake compared to daily targets",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            MacroProgressRow(
                name = "Protein",
                current = data.averageProtein,
                target = data.targetProtein,
                unit = "g",
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(12.dp))

            MacroProgressRow(
                name = "Carbohydrates",
                current = data.averageCarbs,
                target = data.targetCarbs,
                unit = "g",
                color = MaterialTheme.colorScheme.secondary
            )

            Spacer(modifier = Modifier.height(12.dp))

            MacroProgressRow(
                name = "Fat",
                current = data.averageFat,
                target = data.targetFat,
                unit = "g",
                color = MaterialTheme.colorScheme.tertiary
            )
        }
    }
}

@Composable
fun MacroProgressRow(
    name: String,
    current: Float,
    target: Float,
    unit: String,
    color: Color
) {
    val percent = if (target > 0f) ((current / target) * 100f).roundToInt() else 0
    val progress = if (target > 0f) (current / target).coerceIn(0f, 1f) else 0f

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "${current.roundToInt()}$unit / ${target.roundToInt()}$unit ($percent%)",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(CircleShape),
            color = color,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
    }
}

@Composable
fun AdherenceMetricsCard(data: ProgressAnalyticsData) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("adherence_metrics_card"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Target Adherence",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Consistency across ${data.trackedDaysCount} tracked days",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Protein Adherence
            AdherenceItem(
                title = "Protein Target Reached",
                countText = "${data.proteinAdherenceDays} of ${data.trackedDaysCount} tracked days",
                percent = data.proteinAdherencePercent,
                explanation = "Days where protein intake reached at least your daily target of ${data.targetProtein.roundToInt()}g.",
                color = MaterialTheme.colorScheme.primary
            )

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 12.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)
            )

            // Calorie Target Range
            AdherenceItem(
                title = "Calorie Target Range (90%–110%)",
                countText = "${data.calorieRangeDays} of ${data.trackedDaysCount} tracked days",
                percent = data.calorieRangePercent,
                explanation = "Days within 90%–110% of your configured NutriVerse goal (~${data.targetCalories.roundToInt()} kcal/day).",
                color = MaterialTheme.colorScheme.secondary
            )
        }
    }
}

@Composable
fun AdherenceItem(
    title: String,
    countText: String,
    percent: Float,
    explanation: String,
    color: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = countText,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = color
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = explanation,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Surface(
            shape = CircleShape,
            color = color.copy(alpha = 0.15f),
            modifier = Modifier.size(52.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = "${percent.roundToInt()}%",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
            }
        }
    }
}

@Composable
fun InsightsCard(insights: List<String>) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("insights_card")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Lightbulb,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Insights & Consistency",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            insights.forEachIndexed { index, insight ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Box(
                        modifier = Modifier
                            .padding(top = 6.dp)
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = insight,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
fun EmptyAnalyticsCard(
    data: ProgressAnalyticsData?,
    timeRange: AnalyticsTimeRange
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("empty_analytics_card"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Outlined.Analytics,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "No Meal Records in ${timeRange.label}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Progress Analytics is built entirely from your real logged meals. Log your breakfast, lunch, or dinner in the Meal Tracker to see your calorie trends, macronutrient adherence, and consistency metrics.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (data != null && data.targetCalories > 0f) {
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Configured Daily Goals:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    GoalChip(label = "Calories", value = "${data.targetCalories.roundToInt()} kcal")
                    GoalChip(label = "Protein", value = "${data.targetProtein.roundToInt()}g")
                    GoalChip(label = "Carbs", value = "${data.targetCarbs.roundToInt()}g")
                    GoalChip(label = "Fat", value = "${data.targetFat.roundToInt()}g")
                }
            }
        }
    }
}

@Composable
fun GoalChip(label: String, value: String) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}
