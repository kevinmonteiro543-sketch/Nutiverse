package com.monteirogames.nutriverse.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "food_expense",
    indices = [
        Index(value = ["userId"]),
        Index(value = ["userId", "date"]),
        Index(value = ["groceryItemId"])
    ]
)
data class FoodExpenseEntity(
    @PrimaryKey
    val id: String,
    val userId: String,
    val name: String,
    val amount: Double,
    val category: String,
    val date: Long,
    val notes: String?,
    val groceryItemId: String?,
    val createdAt: Long,
    val updatedAt: Long = 0L
)
