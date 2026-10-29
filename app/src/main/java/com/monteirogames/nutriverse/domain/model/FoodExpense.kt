package com.monteirogames.nutriverse.domain.model

data class FoodExpense(
    val id: String,
    val userId: String,
    val name: String,
    val amount: Double,
    val category: String, // "Groceries", "Fruits & Vegetables", "Dairy", "Meat & Seafood", "Snacks", "Beverages", "Eating Out", "Other"
    val date: Long,
    val notes: String? = null,
    val groceryItemId: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = createdAt
)

object ExpenseCategoryHelper {
    val ALL_CATEGORIES = listOf(
        "Groceries",
        "Fruits & Vegetables",
        "Dairy",
        "Meat & Seafood",
        "Snacks",
        "Beverages",
        "Eating Out",
        "Other"
    )

    fun inferCategory(itemName: String): String {
        val lower = itemName.lowercase()
        return when {
            lower.contains("milk") || lower.contains("cheese") || lower.contains("yogurt") ||
                    lower.contains("butter") || lower.contains("paneer") || lower.contains("curd") ||
                    lower.contains("cream") -> "Dairy"

            lower.contains("chicken") || lower.contains("beef") || lower.contains("pork") ||
                    lower.contains("fish") || lower.contains("salmon") || lower.contains("shrimp") ||
                    lower.contains("mutton") || lower.contains("egg") -> "Meat & Seafood"

            lower.contains("apple") || lower.contains("banana") || lower.contains("spinach") ||
                    lower.contains("tomato") || lower.contains("onion") || lower.contains("potato") ||
                    lower.contains("carrot") || lower.contains("berry") || lower.contains("fruit") ||
                    lower.contains("vegetable") || lower.contains("salad") -> "Fruits & Vegetables"

            lower.contains("chip") || lower.contains("cookie") || lower.contains("biscuit") ||
                    lower.contains("chocolate") || lower.contains("nut") || lower.contains("popcorn") ||
                    lower.contains("snack") -> "Snacks"

            lower.contains("coffee") || lower.contains("tea") || lower.contains("juice") ||
                    lower.contains("soda") || lower.contains("water") || lower.contains("beer") ||
                    lower.contains("drink") || lower.contains("beverage") -> "Beverages"

            lower.contains("restaurant") || lower.contains("cafe") || lower.contains("takeout") ||
                    lower.contains("dining") || lower.contains("delivery") || lower.contains("swiggy") ||
                    lower.contains("zomato") || lower.contains("uber eats") || lower.contains("pizza") ||
                    lower.contains("burger") -> "Eating Out"

            else -> "Groceries"
        }
    }
}
