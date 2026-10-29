package com.monteirogames.nutriverse.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "diet_plan_meal",
    foreignKeys = [
        ForeignKey(
            entity = DietPlanEntity::class,
            parentColumns = ["id"],
            childColumns = ["planId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["planId"])]
)
data class DietPlanMealEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val planId: String,
    val dayNumber: Int, // 1 to 7
    val mealType: String, // Breakfast, Lunch, Dinner, Snack
    val name: String,
    val calories: Float,
    val protein: Float,
    val carbs: Float,
    val fat: Float,
    val prepTip: String
)
