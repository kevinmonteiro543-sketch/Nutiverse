package com.monteirogames.nutriverse.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "budget",
    indices = [
        Index(value = ["userId"]),
        Index(value = ["userId", "isActive"])
    ]
)
data class BudgetEntity(
    @PrimaryKey
    val id: String,
    val userId: String,
    val amount: Double,
    val period: String,
    val startDate: Long,
    val endDate: Long,
    val createdAt: Long,
    val isActive: Boolean,
    val updatedAt: Long = 0L
)
