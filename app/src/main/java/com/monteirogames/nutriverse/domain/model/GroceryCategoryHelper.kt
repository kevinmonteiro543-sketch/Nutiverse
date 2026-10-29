package com.monteirogames.nutriverse.domain.model

object GroceryCategoryHelper {

    val CATEGORIES = listOf(
        "Produce",
        "Dairy",
        "Meat & Seafood",
        "Grains",
        "Snacks",
        "Beverages",
        "Pantry",
        "Other"
    )

    fun inferCategory(name: String): String {
        val lower = name.lowercase().trim()

        val produceKeywords = listOf(
            "apple", "banana", "orange", "lemon", "lime", "berry", "strawberr", "blueberr",
            "avocado", "spinach", "tomato", "onion", "garlic", "pepper", "broccoli", "carrot",
            "lettuce", "cucumber", "cilantro", "parsley", "herb", "potato", "mushroom",
            "ginger", "celery", "cabbage", "zucchini", "kale", "corn", "mint", "pea"
        )
        if (produceKeywords.any { lower.contains(it) }) return "Produce"

        val dairyKeywords = listOf(
            "milk", "yogurt", "cheese", "paneer", "butter", "cream", "curd", "ghee",
            "cottage cheese", "mozzarella", "cheddar", "parmesan", "sour cream"
        )
        if (dairyKeywords.any { lower.contains(it) }) return "Dairy"

        val meatKeywords = listOf(
            "chicken", "beef", "pork", "steak", "turkey", "salmon", "tuna", "fish",
            "shrimp", "prawn", "crab", "lobster", "egg", "eggs", "bacon", "sausage",
            "lamb", "tilapia", "cod", "meat"
        )
        if (meatKeywords.any { lower.contains(it) }) return "Meat & Seafood"

        val grainKeywords = listOf(
            "rice", "quinoa", "oat", "oats", "bread", "pasta", "flour", "wheat",
            "noodle", "barley", "tortilla", "cereal", "couscous", "spaghetti"
        )
        if (grainKeywords.any { lower.contains(it) }) return "Grains"

        val snackKeywords = listOf(
            "nut", "nuts", "peanut", "almond", "walnut", "cashew", "chip", "chips",
            "popcorn", "cookie", "cracker", "chocolate", "protein bar", "granola"
        )
        if (snackKeywords.any { lower.contains(it) }) return "Snacks"

        val beverageKeywords = listOf(
            "water", "coffee", "tea", "juice", "soda", "coke", "drink", "milkshake", "smoothie"
        )
        if (beverageKeywords.any { lower.contains(it) }) return "Beverages"

        val pantryKeywords = listOf(
            "oil", "olive oil", "vinegar", "salt", "spice", "sugar", "honey", "syrup",
            "mustard", "mayo", "sauce", "ketchup", "cumin", "turmeric", "masala",
            "cinnamon", "vanilla", "baking powder", "yeast", "soy sauce"
        )
        if (pantryKeywords.any { lower.contains(it) }) return "Pantry"

        return "Other"
    }

    fun normalizeItemName(name: String): String {
        return name.lowercase().trim()
            .replace(Regex("[^a-z0-9\\s]"), " ")
            .split("\\s+".toRegex())
            .filter { it.length > 2 && it !in listOf("fresh", "raw", "cooked", "diced", "chopped", "whole", "cup", "cups", "tbsp", "tsp", "gram", "grams") }
            .joinToString(" ")
    }
}
