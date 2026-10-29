package com.monteirogames.nutriverse.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "recipe_instruction",
    foreignKeys = [
        ForeignKey(
            entity = RecipeEntity::class,
            parentColumns = ["id"],
            childColumns = ["recipeId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["recipeId"])]
)
data class RecipeInstructionEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val recipeId: String,
    val stepNumber: Int,
    val instructionText: String
)
