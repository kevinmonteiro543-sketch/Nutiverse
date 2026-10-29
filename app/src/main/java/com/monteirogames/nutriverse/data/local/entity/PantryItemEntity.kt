package com.monteirogames.nutriverse.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "pantry_item")
data class PantryItemEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val userId: String,
    val foodItemId: String,
    val quantity: Float,
    val unit: String,
    val expiryDate: Long,
    val dateAdded: Long,
    val updatedAt: Long = 0L
)
