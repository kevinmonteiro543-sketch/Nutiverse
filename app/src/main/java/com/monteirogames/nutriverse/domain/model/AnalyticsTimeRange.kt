package com.monteirogames.nutriverse.domain.model

enum class AnalyticsTimeRange(val days: Int, val label: String) {
    LAST_7_DAYS(7, "7 Days"),
    LAST_30_DAYS(30, "30 Days")
}
