package com.monteirogames.nutriverse.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "recipe")
data class RecipeEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val description: String,
    val imageUrl: String = "",
    val prepTimeMinutes: Int,
    val cookTimeMinutes: Int,
    val servings: Int,
    val calories: Float,
    val protein: Float,
    val carbs: Float,
    val fat: Float,
    val difficulty: String,
    val dietaryTags: String, // Comma separated: "High Protein,Vegetarian,Lunch"
    val allergyTags: String, // Comma separated: "Dairy-Free,Nut-Free"
    val isFavorite: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = 0L
)
