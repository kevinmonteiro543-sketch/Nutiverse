package com.monteirogames.nutriverse.domain.usecase

import com.monteirogames.nutriverse.domain.model.BudgetPeriodHelper
import com.monteirogames.nutriverse.domain.model.NotificationSettings
import com.monteirogames.nutriverse.domain.model.NotificationType
import com.monteirogames.nutriverse.domain.model.PendingNotification
import com.monteirogames.nutriverse.domain.repository.BudgetRepository
import com.monteirogames.nutriverse.domain.repository.ExpenseRepository
import com.monteirogames.nutriverse.domain.repository.GroceryRepository
import com.monteirogames.nutriverse.domain.repository.MealRepository
import com.monteirogames.nutriverse.domain.repository.NotificationRepository
import com.monteirogames.nutriverse.domain.repository.PantryRepository
import com.monteirogames.nutriverse.domain.repository.UserProfileRepository
import com.monteirogames.nutriverse.notifications.NotificationChannels
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import java.util.Calendar

class GetNotificationSettingsUseCase(
    private val notificationRepository: NotificationRepository
) {
    operator fun invoke(userId: String): Flow<NotificationSettings> =
        notificationRepository.getNotificationSettings(userId)

    suspend fun getOnce(userId: String): NotificationSettings =
        notificationRepository.getNotificationSettingsOnce(userId)
}

class UpdateNotificationSettingsUseCase(
    private val notificationRepository: NotificationRepository
) {
    suspend operator fun invoke(settings: NotificationSettings) {
        notificationRepository.saveNotificationSettings(settings)
    }
}

class EvaluateNotificationsUseCase(
    private val notificationRepository: NotificationRepository,
    private val mealRepository: MealRepository,
    private val userProfileRepository: UserProfileRepository,
    private val calculateNutritionTargetsUseCase: CalculateNutritionTargetsUseCase,
    private val pantryRepository: PantryRepository,
    private val groceryRepository: GroceryRepository,
    private val budgetRepository: BudgetRepository,
    private val expenseRepository: ExpenseRepository,
    private val calculateBudgetStatusUseCase: CalculateBudgetStatusUseCase
) {

    suspend operator fun invoke(
        userId: String,
        currentTimeMillis: Long = System.currentTimeMillis()
    ): List<PendingNotification> {
        val settings = notificationRepository.getNotificationSettingsOnce(userId)
        if (!settings.notificationsEnabled) {
            return emptyList()
        }

        val pending = mutableListOf<PendingNotification>()

        val calendar = Calendar.getInstance().apply { timeInMillis = currentTimeMillis }
        val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
        val currentMinute = calendar.get(Calendar.MINUTE)
        val dateStr = String.format(
            "%04d-%02d-%02d",
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH) + 1,
            calendar.get(Calendar.DAY_OF_MONTH)
        )

        // 1. MEAL REMINDERS
        if (settings.mealRemindersEnabled) {
            evaluateMealReminders(userId, settings, currentHour, currentMinute, currentTimeMillis, dateStr, pending)
        }

        // 2. NUTRITION / PROTEIN REMINDERS
        if (settings.nutritionRemindersEnabled) {
            evaluateNutritionReminder(userId, currentHour, currentTimeMillis, dateStr, pending)
        }

        // 3. PANTRY EXPIRY REMINDERS
        if (settings.pantryExpiryEnabled) {
            evaluatePantryExpiry(userId, currentTimeMillis, dateStr, pending)
        }

        // 4. GROCERY REMINDERS
        if (settings.groceryRemindersEnabled) {
            evaluateGroceryReminder(userId, dateStr, pending)
        }

        // 5. BUDGET WARNINGS
        if (settings.budgetWarningsEnabled) {
            evaluateBudgetWarnings(userId, currentTimeMillis, pending)
        }

        return pending
    }

    private suspend fun evaluateMealReminders(
        userId: String,
        settings: NotificationSettings,
        currentHour: Int,
        currentMinute: Int,
        currentTimeMillis: Long,
        dateStr: String,
        pending: MutableList<PendingNotification>
    ) {
        val todayMeals = mealRepository.getMealsForDate(userId, currentTimeMillis).firstOrNull() ?: emptyList()
        val loggedMealTypes = todayMeals.map { it.mealType.lowercase() }.toSet()

        val mealConfigs = listOf(
            Triple("Breakfast", settings.breakfastTime, 1001),
            Triple("Lunch", settings.lunchTime, 1002),
            Triple("Dinner", settings.dinnerTime, 1003)
        )

        for ((mealName, timeStr, notifId) in mealConfigs) {
            val (mealHour, mealMin) = parseTime(timeStr)
            val isWindowActive = isTimeInWindow(currentHour, currentMinute, mealHour, mealMin, windowHours = 3)

            if (isWindowActive && !loggedMealTypes.contains(mealName.lowercase())) {
                val deduplicationKey = "meal_${mealName.lowercase()}_$dateStr"
                if (!notificationRepository.isNotificationSent(userId, deduplicationKey)) {
                    pending.add(
                        PendingNotification(
                            id = notifId,
                            userId = userId,
                            type = NotificationType.MEAL,
                            deduplicationKey = deduplicationKey,
                            title = "$mealName reminder",
                            message = "You haven't logged your ${mealName.lowercase()} yet.",
                            channelId = NotificationChannels.CHANNEL_MEAL
                        )
                    )
                }
            }
        }
    }

    private suspend fun evaluateNutritionReminder(
        userId: String,
        currentHour: Int,
        currentTimeMillis: Long,
        dateStr: String,
        pending: MutableList<PendingNotification>
    ) {
        // Late in the day (6:00 PM / 18:00 or later)
        if (currentHour < 18) return

        val deduplicationKey = "nutrition_$dateStr"
        if (notificationRepository.isNotificationSent(userId, deduplicationKey)) return

        val profile = userProfileRepository.getUserProfile(userId).firstOrNull() ?: return
        val targets = calculateNutritionTargetsUseCase(profile)
        val todayMeals = mealRepository.getMealsForDate(userId, currentTimeMillis).firstOrNull() ?: emptyList()

        var totalProtein = 0f
        for (meal in todayMeals) {
            val ratio = meal.quantity / meal.foodItem.servingSize
            totalProtein += meal.foodItem.protein * ratio
        }

        if (targets.protein > 0) {
            val proteinPercent = ((totalProtein / targets.protein) * 100).toInt()
            if (proteinPercent < 75) {
                pending.add(
                    PendingNotification(
                        id = 2001,
                        userId = userId,
                        type = NotificationType.NUTRITION,
                        deduplicationKey = deduplicationKey,
                        title = "Protein check",
                        message = "You're at ${proteinPercent}% of your protein target today. A protein-rich meal could help you close the gap.",
                        channelId = NotificationChannels.CHANNEL_NUTRITION
                    )
                )
            }
        }
    }

    private suspend fun evaluatePantryExpiry(
        userId: String,
        currentTimeMillis: Long,
        dateStr: String,
        pending: MutableList<PendingNotification>
    ) {
        val pantryItems = pantryRepository.getPantryItems(userId).firstOrNull() ?: emptyList()

        for (item in pantryItems) {
            val diffMillis = item.expiryDate - currentTimeMillis
            val diffDays = (diffMillis / (24 * 60 * 60 * 1000L)).toInt()

            val (threshold, title, message) = when {
                diffMillis < 0 -> {
                    Triple(
                        "expired",
                        "Pantry alert",
                        "You have expired pantry items that may need attention: ${item.foodItem.name}."
                    )
                }
                diffDays <= 1 -> {
                    Triple(
                        "1day",
                        "Pantry item expiring soon",
                        "${item.foodItem.name} expires tomorrow."
                    )
                }
                diffDays in 2..3 -> {
                    Triple(
                        "3days",
                        "Food expiring soon",
                        "${item.foodItem.name} expires in $diffDays days."
                    )
                }
                else -> null
            } ?: continue

            val deduplicationKey = "pantry_${item.id}_${threshold}_$dateStr"
            if (!notificationRepository.isNotificationSent(userId, deduplicationKey)) {
                pending.add(
                    PendingNotification(
                        id = (item.id.hashCode() and 0x7FFFFFFF) % 10000 + 3000,
                        userId = userId,
                        type = NotificationType.PANTRY,
                        deduplicationKey = deduplicationKey,
                        title = title,
                        message = message,
                        channelId = NotificationChannels.CHANNEL_PANTRY
                    )
                )
            }
        }
    }

    private suspend fun evaluateGroceryReminder(
        userId: String,
        dateStr: String,
        pending: MutableList<PendingNotification>
    ) {
        val groceryItems = groceryRepository.getGroceryItems(userId).firstOrNull() ?: emptyList()
        val unpurchasedCount = groceryItems.count { !it.isPurchased }

        if (unpurchasedCount > 0) {
            val deduplicationKey = "grocery_$dateStr"
            if (!notificationRepository.isNotificationSent(userId, deduplicationKey)) {
                pending.add(
                    PendingNotification(
                        id = 4001,
                        userId = userId,
                        type = NotificationType.GROCERY,
                        deduplicationKey = deduplicationKey,
                        title = "Grocery reminder",
                        message = "You have $unpurchasedCount item${if (unpurchasedCount > 1) "s" else ""} waiting on your grocery list.",
                        channelId = NotificationChannels.CHANNEL_GROCERY
                    )
                )
            }
        }
    }

    private suspend fun evaluateBudgetWarnings(
        userId: String,
        currentTimeMillis: Long,
        pending: MutableList<PendingNotification>
    ) {
        val activeBudget = budgetRepository.getActiveBudget(userId).firstOrNull() ?: return
        if (activeBudget.amount <= 0.0) return

        val expenses = expenseRepository.getExpenses(userId).firstOrNull() ?: emptyList()
        val summary = calculateBudgetStatusUseCase.calculate(activeBudget, expenses, currentTimeMillis)

        val percentage = summary.percentageUsed

        // Check threshold crossing: 100%+, 90%, 75%
        val thresholdLevel = when {
            percentage >= 100f -> 100
            percentage >= 90f -> 90
            percentage >= 75f -> 75
            else -> 0
        }

        if (thresholdLevel > 0) {
            val deduplicationKey = "budget_${activeBudget.id}_${thresholdLevel}_${activeBudget.startDate}"
            if (!notificationRepository.isNotificationSent(userId, deduplicationKey)) {
                val (title, message) = when (thresholdLevel) {
                    100 -> Pair("Budget warning", "You've reached your food budget for this period.")
                    90 -> Pair("Budget warning", "You're getting close to your food budget limit (${percentage.toInt()}% used).")
                    else -> Pair("Budget update", "You're at ${percentage.toInt()}% of your food budget.")
                }

                pending.add(
                    PendingNotification(
                        id = 5000 + thresholdLevel,
                        userId = userId,
                        type = NotificationType.BUDGET,
                        deduplicationKey = deduplicationKey,
                        title = title,
                        message = message,
                        channelId = NotificationChannels.CHANNEL_BUDGET
                    )
                )
            }
        }
    }

    suspend fun markNotificationSent(notification: PendingNotification) {
        notificationRepository.recordNotificationSent(
            userId = notification.userId,
            deduplicationKey = notification.deduplicationKey,
            type = notification.type.name
        )
    }

    companion object {
        fun parseTime(timeStr: String): Pair<Int, Int> {
            val parts = timeStr.split(":")
            val hour = parts.getOrNull(0)?.toIntOrNull() ?: 8
            val min = parts.getOrNull(1)?.toIntOrNull() ?: 0
            return Pair(hour, min)
        }

        fun isTimeInWindow(
            currentHour: Int,
            currentMinute: Int,
            targetHour: Int,
            targetMinute: Int,
            windowHours: Int = 3
        ): Boolean {
            val currentTotalMin = currentHour * 60 + currentMinute
            val targetTotalMin = targetHour * 60 + targetMinute
            val endTotalMin = targetTotalMin + windowHours * 60
            return currentTotalMin in targetTotalMin..endTotalMin
        }
    }
}

data class NotificationUseCases(
    val getNotificationSettings: GetNotificationSettingsUseCase,
    val updateNotificationSettings: UpdateNotificationSettingsUseCase,
    val evaluateNotifications: EvaluateNotificationsUseCase
)
