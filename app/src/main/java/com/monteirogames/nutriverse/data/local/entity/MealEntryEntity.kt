package com.monteirogames.nutriverse.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "meal_entry")
data class MealEntryEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val userId: String,
    val foodItemId: String,
    val date: Long, // timestamp for the day or specific time
    val mealType: String, // Breakfast, Lunch, Dinner, Snack
    val quantity: Float,
    val loggedAt: Long,
    val updatedAt: Long = 0L
)
