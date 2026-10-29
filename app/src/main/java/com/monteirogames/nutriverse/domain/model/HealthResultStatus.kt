package com.monteirogames.nutriverse.domain.model

enum class HealthResultStatus {
    LOW,
    NORMAL,
    HIGH,
    UNKNOWN;

    companion object {
        fun fromString(value: String?): HealthResultStatus {
            return when (value?.trim()?.uppercase()) {
                "LOW" -> LOW
                "NORMAL" -> NORMAL
                "HIGH" -> HIGH
                else -> UNKNOWN
            }
        }
    }
}
