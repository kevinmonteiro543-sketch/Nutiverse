package com.monteirogames.nutriverse.domain.model

import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object BudgetPeriodHelper {
    const val CURRENCY_SYMBOL = "₹"

    fun formatCurrency(amount: Double): String {
        val format = NumberFormat.getNumberInstance(Locale("en", "IN"))
        format.minimumFractionDigits = 0
        format.maximumFractionDigits = 2
        return "$CURRENCY_SYMBOL${format.format(amount)}"
    }

    /**
     * Calculates the start and end timestamp (inclusive) for the current calendar week.
     * Uses Monday as the start of the week.
     */
    fun getCurrentWeekBounds(currentTimeMillis: Long = System.currentTimeMillis()): Pair<Long, Long> {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = currentTimeMillis
            firstDayOfWeek = Calendar.MONDAY
            set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startOfWeek = calendar.timeInMillis

        calendar.add(Calendar.DAY_OF_WEEK, 6)
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        val endOfWeek = calendar.timeInMillis

        return Pair(startOfWeek, endOfWeek)
    }

    /**
     * Calculates the start and end timestamp (inclusive) for the current calendar month.
     */
    fun getCurrentMonthBounds(currentTimeMillis: Long = System.currentTimeMillis()): Pair<Long, Long> {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = currentTimeMillis
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startOfMonth = calendar.timeInMillis

        val lastDay = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
        calendar.set(Calendar.DAY_OF_MONTH, lastDay)
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        val endOfMonth = calendar.timeInMillis

        return Pair(startOfMonth, endOfMonth)
    }

    /**
     * Formats start and end dates as a readable range, e.g., "Oct 1 - Oct 31, 2026"
     */
    fun formatDateRange(startDate: Long, endDate: Long): String {
        val df = SimpleDateFormat("MMM d", Locale.getDefault())
        val yf = SimpleDateFormat("yyyy", Locale.getDefault())
        val startStr = df.format(Date(startDate))
        val endStr = df.format(Date(endDate))
        val yearStr = yf.format(Date(endDate))
        return "$startStr – $endStr, $yearStr"
    }

    /**
     * Returns the date bounds for the given period.
     */
    fun getBoundsForPeriod(period: String, currentTimeMillis: Long = System.currentTimeMillis()): Pair<Long, Long> {
        return if (period.equals(BudgetPeriod.WEEKLY.name, ignoreCase = true)) {
            getCurrentWeekBounds(currentTimeMillis)
        } else {
            getCurrentMonthBounds(currentTimeMillis)
        }
    }
}
