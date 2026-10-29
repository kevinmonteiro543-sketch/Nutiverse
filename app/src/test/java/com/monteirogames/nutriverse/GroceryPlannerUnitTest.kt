package com.monteirogames.nutriverse

import com.monteirogames.nutriverse.domain.model.FoodItem
import com.monteirogames.nutriverse.domain.model.GroceryCategoryHelper
import com.monteirogames.nutriverse.domain.model.GroceryItem
import com.monteirogames.nutriverse.domain.model.PantryItem
import com.monteirogames.nutriverse.domain.model.Recipe
import com.monteirogames.nutriverse.domain.model.RecipeIngredient
import com.monteirogames.nutriverse.domain.repository.GroceryRepository
import com.monteirogames.nutriverse.domain.repository.PantryRepository
import com.monteirogames.nutriverse.domain.usecase.AddMissingRecipeIngredientsUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GroceryPlannerUnitTest {

    @Test
    fun categoryInference_categorizesCorrectly() {
        assertEquals("Produce", GroceryCategoryHelper.inferCategory("Baby Spinach"))
        assertEquals("Produce", GroceryCategoryHelper.inferCategory("Avocado"))
        assertEquals("Dairy", GroceryCategoryHelper.inferCategory("Greek Yogurt"))
        assertEquals("Dairy", GroceryCategoryHelper.inferCategory("Paneer (Cottage Cheese)"))
        assertEquals("Meat & Seafood", GroceryCategoryHelper.inferCategory("Chicken Breast"))
        assertEquals("Meat & Seafood", GroceryCategoryHelper.inferCategory("Atlantic Salmon"))
        assertEquals("Grains", GroceryCategoryHelper.inferCategory("Rolled Oats"))
        assertEquals("Grains", GroceryCategoryHelper.inferCategory("Quinoa"))
        assertEquals("Pantry", GroceryCategoryHelper.inferCategory("Extra Virgin Olive Oil"))
        assertEquals("Pantry", GroceryCategoryHelper.inferCategory("Garam Masala"))
        assertEquals("Snacks", GroceryCategoryHelper.inferCategory("Almonds"))
        assertEquals("Beverages", GroceryCategoryHelper.inferCategory("Sparkling Water"))
        assertEquals("Other", GroceryCategoryHelper.inferCategory("Stainless Steel Fork"))
    }

    @Test
    fun nameNormalization_handlesDuplicatesCorrectly() {
        val norm1 = GroceryCategoryHelper.normalizeItemName("Fresh Milk")
        val norm2 = GroceryCategoryHelper.normalizeItemName("milk")
        assertTrue(norm1.contains(norm2) || norm2.contains(norm1))

        val spinach1 = GroceryCategoryHelper.normalizeItemName("Raw Spinach")
        val spinach2 = GroceryCategoryHelper.normalizeItemName("spinach")
        assertTrue(spinach1.contains(spinach2) || spinach2.contains(spinach1))
    }

    @Test
    fun addMissingRecipeIngredients_onlyAddsMissingItems() = runBlocking {
        val addedItems = mutableListOf<GroceryItem>()

        val fakeGroceryRepo = object : GroceryRepository {
            override fun getGroceryItems(userId: String): Flow<List<GroceryItem>> = flowOf(addedItems)
            override suspend fun getGroceryItemById(id: String, userId: String): GroceryItem? = addedItems.find { it.id == id }
            override suspend fun addGroceryItem(item: GroceryItem) { addedItems.add(item) }
            override suspend fun updateGroceryItem(item: GroceryItem) {}
            override suspend fun deleteGroceryItem(item: GroceryItem) {}
            override suspend fun deleteGroceryItemById(id: String, userId: String) {}
            override suspend fun togglePurchased(id: String, userId: String, isPurchased: Boolean) {}
            override suspend fun clearPurchasedItems(userId: String) {}
            override suspend fun addMissingRecipeIngredients(
                userId: String,
                recipe: Recipe,
                pantryItems: List<PantryItem>
            ): Int {
                val normalizedPantryNames = pantryItems.map {
                    GroceryCategoryHelper.normalizeItemName(it.foodItem.name)
                }

                var count = 0
                for (ing in recipe.ingredients) {
                    val norm = GroceryCategoryHelper.normalizeItemName(ing.name)
                    val inPantry = normalizedPantryNames.any { norm.contains(it) || it.contains(norm) }
                    if (!inPantry) {
                        addedItems.add(
                            GroceryItem(
                                id = "g_${ing.id}",
                                userId = userId,
                                name = ing.name,
                                quantity = ing.quantity,
                                unit = ing.unit,
                                category = GroceryCategoryHelper.inferCategory(ing.name),
                                isPurchased = false,
                                source = "RECIPE"
                            )
                        )
                        count++
                    }
                }
                return count
            }
        }

        val useCase = AddMissingRecipeIngredientsUseCase(fakeGroceryRepo)

        val recipe = Recipe(
            id = "r_bowl",
            name = "Chicken & Spinach Rice Bowl",
            description = "Healthy bowl",
            prepTimeMinutes = 10,
            cookTimeMinutes = 15,
            servings = 1,
            calories = 450f,
            protein = 40f,
            carbs = 40f,
            fat = 10f,
            difficulty = "Easy",
            ingredients = listOf(
                RecipeIngredient("i1", "r_bowl", "Chicken Breast", 200f, "g"),
                RecipeIngredient("i2", "r_bowl", "Jasmine Rice", 100f, "g"),
                RecipeIngredient("i3", "r_bowl", "Baby Spinach", 50f, "g"),
                RecipeIngredient("i4", "r_bowl", "Red Onion", 1f, "medium")
            )
        )

        // Pantry contains Chicken and Rice
        val pantry = listOf(
            PantryItem("p1", "u1", FoodItem("f1", "Chicken", 100f, 20f, 0f, 2f, 1f, "g"), 500f, "g", 0L, 0L),
            PantryItem("p2", "u1", FoodItem("f2", "Rice", 100f, 2f, 20f, 0f, 1f, "g"), 1000f, "g", 0L, 0L)
        )

        val addedCount = useCase("u1", recipe, pantry)

        assertEquals(2, addedCount)
        assertEquals(2, addedItems.size)
        // Verify only Spinach and Onion were added
        assertTrue(addedItems.any { it.name == "Baby Spinach" })
        assertTrue(addedItems.any { it.name == "Red Onion" })
        assertFalse(addedItems.any { it.name == "Chicken Breast" })
        assertFalse(addedItems.any { it.name == "Jasmine Rice" })
    }
}
