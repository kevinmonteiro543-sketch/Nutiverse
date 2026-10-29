package com.monteirogames.nutriverse.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "grocery_item",
    indices = [
        Index(value = ["userId"]),
        Index(value = ["userId", "isPurchased"])
    ]
)
data class GroceryItemEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val userId: String,
    val name: String,
    val quantity: Float,
    val unit: String,
    val category: String,
    val isPurchased: Boolean = false,
    val source: String = "MANUAL",
    val sourceRecipeId: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val purchasedAt: Long? = null,
    val updatedAt: Long = 0L
)
