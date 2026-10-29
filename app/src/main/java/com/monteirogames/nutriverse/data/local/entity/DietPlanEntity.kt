package com.monteirogames.nutriverse.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "diet_plan")
data class DietPlanEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val userId: String,
    val name: String,
    val targetCalories: Float,
    val targetProtein: Float,
    val targetCarbs: Float,
    val targetFat: Float,
    val createdAt: Long = System.currentTimeMillis(),
    val isActive: Boolean = true,
    val updatedAt: Long = 0L
)
