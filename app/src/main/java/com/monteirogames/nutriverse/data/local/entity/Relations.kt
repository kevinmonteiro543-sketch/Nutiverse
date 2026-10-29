package com.monteirogames.nutriverse.data.local.entity

import androidx.room.Embedded
import androidx.room.Relation

data class MealEntryWithFoodItem(
    @Embedded val mealEntry: MealEntryEntity,
    @Relation(
        parentColumn = "foodItemId",
        entityColumn = "id"
    )
    val foodItem: FoodItemEntity
)

data class PantryItemWithFoodItem(
    @Embedded val pantryItem: PantryItemEntity,
    @Relation(
        parentColumn = "foodItemId",
        entityColumn = "id"
    )
    val foodItem: FoodItemEntity
)
