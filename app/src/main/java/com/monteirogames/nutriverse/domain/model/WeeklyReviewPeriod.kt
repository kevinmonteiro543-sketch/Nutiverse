package com.monteirogames.nutriverse.domain.model

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * Deterministic representation of the previous 7 completed calendar days.
 * Today is strictly excluded.
 */
data class WeeklyReviewPeriod(
    val startDate: String,       // ISO format: "yyyy-MM-dd"
    val endDate: String,         // ISO format: "yyyy-MM-dd"
    val startTimestamp: Long,   // 00:00:00.000 of start day
    val endTimestamp: Long,     // 23:59:59.999 of end day
    val displayRange: String    // Formatted for UI, e.g. "Sep 29 – Oct 5, 2026"
)

object WeeklyReviewPeriodHelper {

    fun calculatePreviousWeekPeriod(currentTimeMillis: Long = System.currentTimeMillis()): WeeklyReviewPeriod {
        val isoFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val monthDayFormat = SimpleDateFormat("MMM d", Locale.getDefault())
        val yearFormat = SimpleDateFormat("yyyy", Locale.getDefault())

        // End date = Yesterday (23:59:59.999)
        val endCal = Calendar.getInstance().apply {
            timeInMillis = currentTimeMillis
            add(Calendar.DAY_OF_YEAR, -1)
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }
        val endTimestamp = endCal.timeInMillis
        val endDateStr = isoFormat.format(endCal.time)
        val endLabel = monthDayFormat.format(endCal.time)
        val endYear = yearFormat.format(endCal.time)

        // Start date = End date minus 6 days (00:00:00.000) -> total 7 calendar days
        val startCal = Calendar.getInstance().apply {
            timeInMillis = endTimestamp
            add(Calendar.DAY_OF_YEAR, -6)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startTimestamp = startCal.timeInMillis
        val startDateStr = isoFormat.format(startCal.time)
        val startLabel = monthDayFormat.format(startCal.time)
        val startYear = yearFormat.format(startCal.time)

        val displayRange = if (startYear == endYear) {
            "$startLabel – $endLabel, $endYear"
        } else {
            "$startLabel, $startYear – $endLabel, $endYear"
        }

        return WeeklyReviewPeriod(
            startDate = startDateStr,
            endDate = endDateStr,
            startTimestamp = startTimestamp,
            endTimestamp = endTimestamp,
            displayRange = displayRange
        )
    }
}
