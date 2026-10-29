package com.monteirogames.nutriverse.domain.model

enum class BudgetPeriod {
    WEEKLY,
    MONTHLY
}

data class Budget(
    val id: String,
    val userId: String,
    val amount: Double,
    val period: String = BudgetPeriod.MONTHLY.name, // "WEEKLY", "MONTHLY"
    val startDate: Long,
    val endDate: Long,
    val createdAt: Long = System.currentTimeMillis(),
    val isActive: Boolean = true,
    val updatedAt: Long = createdAt
)

data class BudgetSummary(
    val budget: Budget?,
    val totalBudget: Double,
    val totalSpent: Double,
    val remaining: Double,
    val percentageUsed: Float,
    val isOverBudget: Boolean,
    val overBudgetAmount: Double,
    val periodLabel: String,
    val periodRangeLabel: String
)
