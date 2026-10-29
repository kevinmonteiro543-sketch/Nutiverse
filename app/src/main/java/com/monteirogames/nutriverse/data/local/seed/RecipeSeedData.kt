package com.monteirogames.nutriverse.data.local.seed

import com.monteirogames.nutriverse.data.local.entity.RecipeEntity
import com.monteirogames.nutriverse.data.local.entity.RecipeIngredientEntity
import com.monteirogames.nutriverse.data.local.entity.RecipeInstructionEntity
import java.util.UUID

object RecipeSeedData {

    data class SeedRecipeBundle(
        val recipe: RecipeEntity,
        val ingredients: List<RecipeIngredientEntity>,
        val instructions: List<RecipeInstructionEntity>
    )

    fun getInitialRecipes(): List<SeedRecipeBundle> {
        val list = mutableListOf<SeedRecipeBundle>()

        // 1. High-Protein Grilled Chicken & Quinoa Bowl
        run {
            val id = "seed_recipe_chicken_quinoa"
            val recipe = RecipeEntity(
                id = id,
                name = "Grilled Chicken Quinoa Bowl",
                description = "Lean grilled chicken breast served over fluffy quinoa, roasted peppers, avocado, and lime vinaigrette.",
                imageUrl = "",
                prepTimeMinutes = 15,
                cookTimeMinutes = 20,
                servings = 1,
                calories = 520f,
                protein = 46f,
                carbs = 48f,
                fat = 16f,
                difficulty = "Easy",
                dietaryTags = "High Protein,Lunch,Dinner,Quick",
                allergyTags = "Gluten-Free,Dairy-Free",
                isFavorite = true
            )
            val ingredients = listOf(
                RecipeIngredientEntity(id = UUID.randomUUID().toString(), recipeId = id, name = "Chicken Breast", quantity = 180f, unit = "g"),
                RecipeIngredientEntity(id = UUID.randomUUID().toString(), recipeId = id, name = "Cooked Quinoa", quantity = 150f, unit = "g"),
                RecipeIngredientEntity(id = UUID.randomUUID().toString(), recipeId = id, name = "Avocado", quantity = 50f, unit = "g"),
                RecipeIngredientEntity(id = UUID.randomUUID().toString(), recipeId = id, name = "Bell Peppers", quantity = 80f, unit = "g"),
                RecipeIngredientEntity(id = UUID.randomUUID().toString(), recipeId = id, name = "Olive Oil", quantity = 1f, unit = "tbsp"),
                RecipeIngredientEntity(id = UUID.randomUUID().toString(), recipeId = id, name = "Lime Juice", quantity = 1f, unit = "tbsp")
            )
            val instructions = listOf(
                RecipeInstructionEntity(id = UUID.randomUUID().toString(), recipeId = id, stepNumber = 1, instructionText = "Season chicken breast with paprika, salt, pepper, and garlic powder."),
                RecipeInstructionEntity(id = UUID.randomUUID().toString(), recipeId = id, stepNumber = 2, instructionText = "Grill chicken on medium-high heat for 6-7 minutes per side until fully cooked."),
                RecipeInstructionEntity(id = UUID.randomUUID().toString(), recipeId = id, stepNumber = 3, instructionText = "Assemble warm quinoa in a bowl, top with sliced chicken, bell peppers, and avocado."),
                RecipeInstructionEntity(id = UUID.randomUUID().toString(), recipeId = id, stepNumber = 4, instructionText = "Drizzle with olive oil and fresh lime juice before serving.")
            )
            list.add(SeedRecipeBundle(recipe, ingredients, instructions))
        }

        // 2. Paneer Tikka Masala
        run {
            val id = "seed_recipe_paneer_tikka"
            val recipe = RecipeEntity(
                id = id,
                name = "Paneer Tikka Masala",
                description = "Marinated cottage cheese cubes simmered in a spiced tomato and onion gravy with aromatic spices.",
                imageUrl = "",
                prepTimeMinutes = 20,
                cookTimeMinutes = 25,
                servings = 2,
                calories = 440f,
                protein = 24f,
                carbs = 22f,
                fat = 28f,
                difficulty = "Medium",
                dietaryTags = "Vegetarian,Indian,High Protein,Dinner",
                allergyTags = "Gluten-Free,Nut-Free",
                isFavorite = false
            )
            val ingredients = listOf(
                RecipeIngredientEntity(id = UUID.randomUUID().toString(), recipeId = id, name = "Paneer (Cottage Cheese)", quantity = 250f, unit = "g"),
                RecipeIngredientEntity(id = UUID.randomUUID().toString(), recipeId = id, name = "Tomato Puree", quantity = 200f, unit = "g"),
                RecipeIngredientEntity(id = UUID.randomUUID().toString(), recipeId = id, name = "Onion", quantity = 1f, unit = "medium"),
                RecipeIngredientEntity(id = UUID.randomUUID().toString(), recipeId = id, name = "Ginger-Garlic Paste", quantity = 1f, unit = "tbsp"),
                RecipeIngredientEntity(id = UUID.randomUUID().toString(), recipeId = id, name = "Garam Masala", quantity = 1f, unit = "tsp"),
                RecipeIngredientEntity(id = UUID.randomUUID().toString(), recipeId = id, name = "Cumin & Coriander Powder", quantity = 1f, unit = "tsp"),
                RecipeIngredientEntity(id = UUID.randomUUID().toString(), recipeId = id, name = "Greek Yogurt", quantity = 2f, unit = "tbsp")
            )
            val instructions = listOf(
                RecipeInstructionEntity(id = UUID.randomUUID().toString(), recipeId = id, stepNumber = 1, instructionText = "Toss paneer cubes in spices and yogurt, pan-sear for 3-4 minutes until golden."),
                RecipeInstructionEntity(id = UUID.randomUUID().toString(), recipeId = id, stepNumber = 2, instructionText = "Sauté chopped onions and ginger-garlic paste until translucent."),
                RecipeInstructionEntity(id = UUID.randomUUID().toString(), recipeId = id, stepNumber = 3, instructionText = "Add tomato puree, ground spices, and simmer for 10 minutes until sauce thickens."),
                RecipeInstructionEntity(id = UUID.randomUUID().toString(), recipeId = id, stepNumber = 4, instructionText = "Gently fold in seared paneer cubes and garnish with fresh cilantro.")
            )
            list.add(SeedRecipeBundle(recipe, ingredients, instructions))
        }

        // 3. Masala Oats with Vegetables
        run {
            val id = "seed_recipe_masala_oats"
            val recipe = RecipeEntity(
                id = id,
                name = "Savory Masala Vegetable Oats",
                description = "Warm comforting rolled oats cooked with turmeric, mustard seeds, carrots, peas, and fresh coriander.",
                imageUrl = "",
                prepTimeMinutes = 10,
                cookTimeMinutes = 10,
                servings = 1,
                calories = 310f,
                protein = 12f,
                carbs = 48f,
                fat = 7f,
                difficulty = "Easy",
                dietaryTags = "Vegetarian,Indian,Breakfast,Quick",
                allergyTags = "Dairy-Free,Nut-Free",
                isFavorite = false
            )
            val ingredients = listOf(
                RecipeIngredientEntity(id = UUID.randomUUID().toString(), recipeId = id, name = "Rolled Oats", quantity = 50f, unit = "g"),
                RecipeIngredientEntity(id = UUID.randomUUID().toString(), recipeId = id, name = "Green Peas & Carrots", quantity = 75f, unit = "g"),
                RecipeIngredientEntity(id = UUID.randomUUID().toString(), recipeId = id, name = "Onion", quantity = 0.5f, unit = "small"),
                RecipeIngredientEntity(id = UUID.randomUUID().toString(), recipeId = id, name = "Green Chili", quantity = 1f, unit = "piece"),
                RecipeIngredientEntity(id = UUID.randomUUID().toString(), recipeId = id, name = "Turmeric & Mustard Seeds", quantity = 1f, unit = "tsp"),
                RecipeIngredientEntity(id = UUID.randomUUID().toString(), recipeId = id, name = "Water", quantity = 1.5f, unit = "cups")
            )
            val instructions = listOf(
                RecipeInstructionEntity(id = UUID.randomUUID().toString(), recipeId = id, stepNumber = 1, instructionText = "Heat oil in a saucepan, crackle mustard seeds, and sauté onion, green chili, and vegetables for 3 minutes."),
                RecipeInstructionEntity(id = UUID.randomUUID().toString(), recipeId = id, stepNumber = 2, instructionText = "Add turmeric, salt, and water. Bring to a rolling boil."),
                RecipeInstructionEntity(id = UUID.randomUUID().toString(), recipeId = id, stepNumber = 3, instructionText = "Stir in rolled oats, reduce heat, and simmer for 4-5 minutes until porridge consistency."),
                RecipeInstructionEntity(id = UUID.randomUUID().toString(), recipeId = id, stepNumber = 4, instructionText = "Finish with fresh lemon juice and chopped cilantro.")
            )
            list.add(SeedRecipeBundle(recipe, ingredients, instructions))
        }

        // 4. Greek Yogurt & Berry Parfait
        run {
            val id = "seed_recipe_greek_parfait"
            val recipe = RecipeEntity(
                id = id,
                name = "High-Protein Greek Yogurt Parfait",
                description = "Creamy non-fat Greek yogurt layered with antioxidant-rich mixed berries, chia seeds, and crunchy pumpkin seeds.",
                imageUrl = "",
                prepTimeMinutes = 5,
                cookTimeMinutes = 0,
                servings = 1,
                calories = 290f,
                protein = 26f,
                carbs = 32f,
                fat = 6f,
                difficulty = "Easy",
                dietaryTags = "High Protein,Vegetarian,Breakfast,Snacks,Quick",
                allergyTags = "Gluten-Free,Nut-Free",
                isFavorite = true
            )
            val ingredients = listOf(
                RecipeIngredientEntity(id = UUID.randomUUID().toString(), recipeId = id, name = "Plain Greek Yogurt (0% Fat)", quantity = 220f, unit = "g"),
                RecipeIngredientEntity(id = UUID.randomUUID().toString(), recipeId = id, name = "Mixed Berries (Blueberries, Raspberries)", quantity = 100f, unit = "g"),
                RecipeIngredientEntity(id = UUID.randomUUID().toString(), recipeId = id, name = "Chia Seeds", quantity = 1f, unit = "tbsp"),
                RecipeIngredientEntity(id = UUID.randomUUID().toString(), recipeId = id, name = "Pumpkin Seeds", quantity = 1f, unit = "tbsp"),
                RecipeIngredientEntity(id = UUID.randomUUID().toString(), recipeId = id, name = "Honey or Stevia", quantity = 1f, unit = "tsp")
            )
            val instructions = listOf(
                RecipeInstructionEntity(id = UUID.randomUUID().toString(), recipeId = id, stepNumber = 1, instructionText = "Spoon half of the Greek yogurt into a glass or bowl."),
                RecipeInstructionEntity(id = UUID.randomUUID().toString(), recipeId = id, stepNumber = 2, instructionText = "Layer with half of the fresh berries and chia seeds."),
                RecipeInstructionEntity(id = UUID.randomUUID().toString(), recipeId = id, stepNumber = 3, instructionText = "Top with remaining yogurt, berries, and sprinkle pumpkin seeds on top.")
            )
            list.add(SeedRecipeBundle(recipe, ingredients, instructions))
        }

        // 5. Classic Dal Tadka with Brown Rice
        run {
            val id = "seed_recipe_dal_tadka"
            val recipe = RecipeEntity(
                id = id,
                name = "Home-Style Dal Tadka with Brown Rice",
                description = "Nutritious yellow toor dal tempered with cumin, garlic, and ghee, served alongside steamed brown rice.",
                imageUrl = "",
                prepTimeMinutes = 15,
                cookTimeMinutes = 25,
                servings = 2,
                calories = 380f,
                protein = 16f,
                carbs = 62f,
                fat = 7f,
                difficulty = "Easy",
                dietaryTags = "Vegetarian,Indian,Lunch,Dinner",
                allergyTags = "Gluten-Free,Nut-Free",
                isFavorite = false
            )
            val ingredients = listOf(
                RecipeIngredientEntity(id = UUID.randomUUID().toString(), recipeId = id, name = "Yellow Lentils (Toor Dal)", quantity = 150f, unit = "g"),
                RecipeIngredientEntity(id = UUID.randomUUID().toString(), recipeId = id, name = "Cooked Brown Rice", quantity = 200f, unit = "g"),
                RecipeIngredientEntity(id = UUID.randomUUID().toString(), recipeId = id, name = "Tomato & Onion", quantity = 1f, unit = "cup"),
                RecipeIngredientEntity(id = UUID.randomUUID().toString(), recipeId = id, name = "Garlic Cloves (minced)", quantity = 4f, unit = "cloves"),
                RecipeIngredientEntity(id = UUID.randomUUID().toString(), recipeId = id, name = "Ghee", quantity = 1f, unit = "tbsp"),
                RecipeIngredientEntity(id = UUID.randomUUID().toString(), recipeId = id, name = "Cumin Seeds & Asafoetida", quantity = 1f, unit = "tsp")
            )
            val instructions = listOf(
                RecipeInstructionEntity(id = UUID.randomUUID().toString(), recipeId = id, stepNumber = 1, instructionText = "Pressure cook lentils with turmeric and salt until soft."),
                RecipeInstructionEntity(id = UUID.randomUUID().toString(), recipeId = id, stepNumber = 2, instructionText = "Heat ghee in a pan, add cumin seeds, minced garlic, and onions until lightly browned."),
                RecipeInstructionEntity(id = UUID.randomUUID().toString(), recipeId = id, stepNumber = 3, instructionText = "Add diced tomatoes, chili powder, and cook until oil separates."),
                RecipeInstructionEntity(id = UUID.randomUUID().toString(), recipeId = id, stepNumber = 4, instructionText = "Pour cooked dal into the tempering, simmer for 5 minutes, and serve warm with brown rice.")
            )
            list.add(SeedRecipeBundle(recipe, ingredients, instructions))
        }

        // 6. Baked Lemon Herb Salmon
        run {
            val id = "seed_recipe_baked_salmon"
            val recipe = RecipeEntity(
                id = id,
                name = "Lemon Herb Baked Salmon & Asparagus",
                description = "Omega-3 rich salmon fillet seasoned with dill, garlic, and lemon, roasted with tender asparagus spears.",
                imageUrl = "",
                prepTimeMinutes = 10,
                cookTimeMinutes = 15,
                servings = 1,
                calories = 460f,
                protein = 40f,
                carbs = 8f,
                fat = 28f,
                difficulty = "Easy",
                dietaryTags = "High Protein,Dinner,Quick",
                allergyTags = "Gluten-Free,Dairy-Free,Nut-Free",
                isFavorite = true
            )
            val ingredients = listOf(
                RecipeIngredientEntity(id = UUID.randomUUID().toString(), recipeId = id, name = "Salmon Fillet", quantity = 200f, unit = "g"),
                RecipeIngredientEntity(id = UUID.randomUUID().toString(), recipeId = id, name = "Fresh Asparagus", quantity = 150f, unit = "g"),
                RecipeIngredientEntity(id = UUID.randomUUID().toString(), recipeId = id, name = "Olive Oil", quantity = 1f, unit = "tbsp"),
                RecipeIngredientEntity(id = UUID.randomUUID().toString(), recipeId = id, name = "Lemon Slices & Juice", quantity = 1f, unit = "lemon"),
                RecipeIngredientEntity(id = UUID.randomUUID().toString(), recipeId = id, name = "Fresh Dill & Garlic Powder", quantity = 1f, unit = "tsp")
            )
            val instructions = listOf(
                RecipeInstructionEntity(id = UUID.randomUUID().toString(), recipeId = id, stepNumber = 1, instructionText = "Preheat oven to 200°C (400°F) and line a baking sheet with parchment paper."),
                RecipeInstructionEntity(id = UUID.randomUUID().toString(), recipeId = id, stepNumber = 2, instructionText = "Place salmon and trimmed asparagus on the sheet. Drizzle with olive oil, salt, pepper, and garlic powder."),
                RecipeInstructionEntity(id = UUID.randomUUID().toString(), recipeId = id, stepNumber = 3, instructionText = "Top salmon with lemon slices and chopped dill."),
                RecipeInstructionEntity(id = UUID.randomUUID().toString(), recipeId = id, stepNumber = 4, instructionText = "Bake for 12-15 minutes until salmon flakes easily with a fork.")
            )
            list.add(SeedRecipeBundle(recipe, ingredients, instructions))
        }

        // 7. Tofu & Broccoli Stir-Fry
        run {
            val id = "seed_recipe_tofu_stir_fry"
            val recipe = RecipeEntity(
                id = id,
                name = "Crispy Tofu & Broccoli Stir-Fry",
                description = "Crispy pressed tofu cubes sautéed with fresh broccoli florets in a savory ginger soy sauce.",
                imageUrl = "",
                prepTimeMinutes = 15,
                cookTimeMinutes = 15,
                servings = 1,
                calories = 360f,
                protein = 22f,
                carbs = 26f,
                fat = 18f,
                difficulty = "Easy",
                dietaryTags = "Vegetarian,High Protein,Lunch,Dinner,Quick",
                allergyTags = "Dairy-Free,Nut-Free",
                isFavorite = false
            )
            val ingredients = listOf(
                RecipeIngredientEntity(id = UUID.randomUUID().toString(), recipeId = id, name = "Extra Firm Tofu", quantity = 180f, unit = "g"),
                RecipeIngredientEntity(id = UUID.randomUUID().toString(), recipeId = id, name = "Broccoli Florets", quantity = 150f, unit = "g"),
                RecipeIngredientEntity(id = UUID.randomUUID().toString(), recipeId = id, name = "Low Sodium Soy Sauce", quantity = 2f, unit = "tbsp"),
                RecipeIngredientEntity(id = UUID.randomUUID().toString(), recipeId = id, name = "Sesame Oil", quantity = 1f, unit = "tsp"),
                RecipeIngredientEntity(id = UUID.randomUUID().toString(), recipeId = id, name = "Garlic & Fresh Ginger", quantity = 1f, unit = "tbsp"),
                RecipeIngredientEntity(id = UUID.randomUUID().toString(), recipeId = id, name = "Sesame Seeds", quantity = 1f, unit = "tsp")
            )
            val instructions = listOf(
                RecipeInstructionEntity(id = UUID.randomUUID().toString(), recipeId = id, stepNumber = 1, instructionText = "Press tofu dry and cut into 1-inch cubes."),
                RecipeInstructionEntity(id = UUID.randomUUID().toString(), recipeId = id, stepNumber = 2, instructionText = "Pan-sear tofu in sesame oil until all sides are crisp and golden (about 6 minutes)."),
                RecipeInstructionEntity(id = UUID.randomUUID().toString(), recipeId = id, stepNumber = 3, instructionText = "Add broccoli florets, minced garlic, ginger, and 2 tbsp water; steam-fry for 3-4 minutes."),
                RecipeInstructionEntity(id = UUID.randomUUID().toString(), recipeId = id, stepNumber = 4, instructionText = "Stir in soy sauce, coat thoroughly, and garnish with sesame seeds.")
            )
            list.add(SeedRecipeBundle(recipe, ingredients, instructions))
        }

        // 8. Scrambled Eggs with Avocado Toast
        run {
            val id = "seed_recipe_scrambled_eggs_toast"
            val recipe = RecipeEntity(
                id = id,
                name = "Fluffy Scrambled Eggs & Avocado Toast",
                description = "Soft scrambled eggs on toasted sourdough bread layered with mashed creamy avocado and chili flakes.",
                imageUrl = "",
                prepTimeMinutes = 5,
                cookTimeMinutes = 8,
                servings = 1,
                calories = 410f,
                protein = 23f,
                carbs = 34f,
                fat = 20f,
                difficulty = "Easy",
                dietaryTags = "High Protein,Vegetarian,Breakfast,Quick",
                allergyTags = "Dairy-Free,Nut-Free",
                isFavorite = false
            )
            val ingredients = listOf(
                RecipeIngredientEntity(id = UUID.randomUUID().toString(), recipeId = id, name = "Whole Eggs", quantity = 3f, unit = "large"),
                RecipeIngredientEntity(id = UUID.randomUUID().toString(), recipeId = id, name = "Whole Grain Sourdough Toast", quantity = 1f, unit = "slice"),
                RecipeIngredientEntity(id = UUID.randomUUID().toString(), recipeId = id, name = "Ripe Avocado", quantity = 50f, unit = "g"),
                RecipeIngredientEntity(id = UUID.randomUUID().toString(), recipeId = id, name = "Salt, Pepper, Chili Flakes", quantity = 1f, unit = "pinch"),
                RecipeIngredientEntity(id = UUID.randomUUID().toString(), recipeId = id, name = "Olive Oil Spray", quantity = 1f, unit = "spray")
            )
            val instructions = listOf(
                RecipeInstructionEntity(id = UUID.randomUUID().toString(), recipeId = id, stepNumber = 1, instructionText = "Whisk eggs with salt and black pepper in a small bowl."),
                RecipeInstructionEntity(id = UUID.randomUUID().toString(), recipeId = id, stepNumber = 2, instructionText = "Toast the sourdough bread slice until golden and crisp."),
                RecipeInstructionEntity(id = UUID.randomUUID().toString(), recipeId = id, stepNumber = 3, instructionText = "Mash avocado with lemon juice and spread generously on the toast."),
                RecipeInstructionEntity(id = UUID.randomUUID().toString(), recipeId = id, stepNumber = 4, instructionText = "Cook eggs over low heat gently pushing curds for 2-3 minutes. Place on top of toast.")
            )
            list.add(SeedRecipeBundle(recipe, ingredients, instructions))
        }

        // 9. Spiced Chickpea & Spinach Curry (Chana Palak)
        run {
            val id = "seed_recipe_chana_palak"
            val recipe = RecipeEntity(
                id = id,
                name = "Spiced Chickpea & Spinach Curry",
                description = "Nutrient-packed garbanzo beans stewed in pureed spinach, cumin, and garam masala gravy.",
                imageUrl = "",
                prepTimeMinutes = 15,
                cookTimeMinutes = 20,
                servings = 2,
                calories = 340f,
                protein = 17f,
                carbs = 48f,
                fat = 9f,
                difficulty = "Easy",
                dietaryTags = "Vegetarian,Indian,High Protein,Lunch,Dinner",
                allergyTags = "Gluten-Free,Dairy-Free,Nut-Free",
                isFavorite = false
            )
            val ingredients = listOf(
                RecipeIngredientEntity(id = UUID.randomUUID().toString(), recipeId = id, name = "Cooked Chickpeas", quantity = 300f, unit = "g"),
                RecipeIngredientEntity(id = UUID.randomUUID().toString(), recipeId = id, name = "Fresh Spinach (Palak)", quantity = 250f, unit = "g"),
                RecipeIngredientEntity(id = UUID.randomUUID().toString(), recipeId = id, name = "Onion & Tomato Paste", quantity = 1f, unit = "cup"),
                RecipeIngredientEntity(id = UUID.randomUUID().toString(), recipeId = id, name = "Olive Oil", quantity = 1f, unit = "tbsp"),
                RecipeIngredientEntity(id = UUID.randomUUID().toString(), recipeId = id, name = "Indian Spices (Cumin, Garam Masala)", quantity = 2f, unit = "tsp")
            )
            val instructions = listOf(
                RecipeInstructionEntity(id = UUID.randomUUID().toString(), recipeId = id, stepNumber = 1, instructionText = "Blanch spinach in hot water for 2 minutes, then blend into a smooth puree."),
                RecipeInstructionEntity(id = UUID.randomUUID().toString(), recipeId = id, stepNumber = 2, instructionText = "Sauté cumin seeds, onions, and tomato paste in olive oil until aromatic."),
                RecipeInstructionEntity(id = UUID.randomUUID().toString(), recipeId = id, stepNumber = 3, instructionText = "Add cooked chickpeas, spinach puree, and garam masala; simmer for 10 minutes."),
                RecipeInstructionEntity(id = UUID.randomUUID().toString(), recipeId = id, stepNumber = 4, instructionText = "Season to taste and serve with whole wheat flatbread or steamed rice.")
            )
            list.add(SeedRecipeBundle(recipe, ingredients, instructions))
        }

        // 10. Turkey & Quinoa Stuffed Bell Peppers
        run {
            val id = "seed_recipe_stuffed_peppers"
            val recipe = RecipeEntity(
                id = id,
                name = "Turkey & Quinoa Stuffed Bell Peppers",
                description = "Vibrant bell peppers stuffed with seasoned lean ground turkey, quinoa, black beans, and melted mozzarella.",
                imageUrl = "",
                prepTimeMinutes = 20,
                cookTimeMinutes = 30,
                servings = 2,
                calories = 430f,
                protein = 38f,
                carbs = 36f,
                fat = 14f,
                difficulty = "Medium",
                dietaryTags = "High Protein,Dinner",
                allergyTags = "Gluten-Free,Nut-Free",
                isFavorite = false
            )
            val ingredients = listOf(
                RecipeIngredientEntity(id = UUID.randomUUID().toString(), recipeId = id, name = "Lean Ground Turkey (93%)", quantity = 250f, unit = "g"),
                RecipeIngredientEntity(id = UUID.randomUUID().toString(), recipeId = id, name = "Large Bell Peppers", quantity = 2f, unit = "pieces"),
                RecipeIngredientEntity(id = UUID.randomUUID().toString(), recipeId = id, name = "Cooked Quinoa", quantity = 100f, unit = "g"),
                RecipeIngredientEntity(id = UUID.randomUUID().toString(), recipeId = id, name = "Black Beans", quantity = 80f, unit = "g"),
                RecipeIngredientEntity(id = UUID.randomUUID().toString(), recipeId = id, name = "Part-Skim Mozzarella", quantity = 40f, unit = "g")
            )
            val instructions = listOf(
                RecipeInstructionEntity(id = UUID.randomUUID().toString(), recipeId = id, stepNumber = 1, instructionText = "Cut bell peppers in half lengthwise and clean out seeds."),
                RecipeInstructionEntity(id = UUID.randomUUID().toString(), recipeId = id, stepNumber = 2, instructionText = "Brown ground turkey with taco seasoning; fold in cooked quinoa and black beans."),
                RecipeInstructionEntity(id = UUID.randomUUID().toString(), recipeId = id, stepNumber = 3, instructionText = "Spoon mixture into pepper halves and top with shredded mozzarella cheese."),
                RecipeInstructionEntity(id = UUID.randomUUID().toString(), recipeId = id, stepNumber = 4, instructionText = "Bake at 190°C (375°F) for 25-30 minutes until peppers are tender and cheese is bubbling.")
            )
            list.add(SeedRecipeBundle(recipe, ingredients, instructions))
        }

        // 11. Chia Seed Peanut Butter Pudding
        run {
            val id = "seed_recipe_chia_pudding"
            val recipe = RecipeEntity(
                id = id,
                name = "Peanut Butter Chia Seed Pudding",
                description = "Nutrient-dense overnight chia seed pudding blended with creamy peanut butter and unsweetened almond milk.",
                imageUrl = "",
                prepTimeMinutes = 5,
                cookTimeMinutes = 0,
                servings = 1,
                calories = 280f,
                protein = 11f,
                carbs = 24f,
                fat = 16f,
                difficulty = "Easy",
                dietaryTags = "Vegetarian,Breakfast,Snacks,Quick",
                allergyTags = "Gluten-Free,Dairy-Free",
                isFavorite = false
            )
            val ingredients = listOf(
                RecipeIngredientEntity(id = UUID.randomUUID().toString(), recipeId = id, name = "Chia Seeds", quantity = 3f, unit = "tbsp"),
                RecipeIngredientEntity(id = UUID.randomUUID().toString(), recipeId = id, name = "Unsweetened Almond Milk", quantity = 1f, unit = "cup"),
                RecipeIngredientEntity(id = UUID.randomUUID().toString(), recipeId = id, name = "Natural Peanut Butter", quantity = 1f, unit = "tbsp"),
                RecipeIngredientEntity(id = UUID.randomUUID().toString(), recipeId = id, name = "Pure Maple Syrup", quantity = 1f, unit = "tsp"),
                RecipeIngredientEntity(id = UUID.randomUUID().toString(), recipeId = id, name = "Cinnamon", quantity = 0.5f, unit = "tsp")
            )
            val instructions = listOf(
                RecipeInstructionEntity(id = UUID.randomUUID().toString(), recipeId = id, stepNumber = 1, instructionText = "Whisk almond milk, peanut butter, and maple syrup together in a jar until smooth."),
                RecipeInstructionEntity(id = UUID.randomUUID().toString(), recipeId = id, stepNumber = 2, instructionText = "Stir in chia seeds and cinnamon; let sit for 10 minutes then stir again to break up clumps."),
                RecipeInstructionEntity(id = UUID.randomUUID().toString(), recipeId = id, stepNumber = 3, instructionText = "Refrigerate for at least 2 hours (or overnight) until thickened to a pudding texture.")
            )
            list.add(SeedRecipeBundle(recipe, ingredients, instructions))
        }

        // 12. Mediterranean Tuna Salad Bowl
        run {
            val id = "seed_recipe_med_tuna"
            val recipe = RecipeEntity(
                id = id,
                name = "Mediterranean Tuna & Chickpea Salad",
                description = "Light and zesty salad with solid white tuna, chickpeas, crisp cucumbers, cherry tomatoes, and kalamata olives.",
                imageUrl = "",
                prepTimeMinutes = 10,
                cookTimeMinutes = 0,
                servings = 1,
                calories = 390f,
                protein = 36f,
                carbs = 28f,
                fat = 14f,
                difficulty = "Easy",
                dietaryTags = "High Protein,Lunch,Quick",
                allergyTags = "Gluten-Free,Dairy-Free,Nut-Free",
                isFavorite = false
            )
            val ingredients = listOf(
                RecipeIngredientEntity(id = UUID.randomUUID().toString(), recipeId = id, name = "Canned Tuna in Water (Drained)", quantity = 150f, unit = "g"),
                RecipeIngredientEntity(id = UUID.randomUUID().toString(), recipeId = id, name = "Rinsed Chickpeas", quantity = 80f, unit = "g"),
                RecipeIngredientEntity(id = UUID.randomUUID().toString(), recipeId = id, name = "Diced Cucumber & Cherry Tomatoes", quantity = 100f, unit = "g"),
                RecipeIngredientEntity(id = UUID.randomUUID().toString(), recipeId = id, name = "Kalamata Olives (sliced)", quantity = 25f, unit = "g"),
                RecipeIngredientEntity(id = UUID.randomUUID().toString(), recipeId = id, name = "Extra Virgin Olive Oil", quantity = 1f, unit = "tbsp"),
                RecipeIngredientEntity(id = UUID.randomUUID().toString(), recipeId = id, name = "Lemon Juice & Dried Oregano", quantity = 1f, unit = "tsp")
            )
            val instructions = listOf(
                RecipeInstructionEntity(id = UUID.randomUUID().toString(), recipeId = id, stepNumber = 1, instructionText = "Drain tuna and flake with a fork into a salad bowl."),
                RecipeInstructionEntity(id = UUID.randomUUID().toString(), recipeId = id, stepNumber = 2, instructionText = "Add chickpeas, cucumber, cherry tomatoes, and sliced olives."),
                RecipeInstructionEntity(id = UUID.randomUUID().toString(), recipeId = id, stepNumber = 3, instructionText = "Drizzle with olive oil, fresh lemon juice, oregano, salt, and pepper."),
                RecipeInstructionEntity(id = UUID.randomUUID().toString(), recipeId = id, stepNumber = 4, instructionText = "Toss thoroughly and serve immediately.")
            )
            list.add(SeedRecipeBundle(recipe, ingredients, instructions))
        }

        // 13. Egg White & Vegetable Frittata
        run {
            val id = "seed_recipe_egg_frittata"
            val recipe = RecipeEntity(
                id = id,
                name = "Egg White & Spinach Frittata",
                description = "Fluffy baked egg white skillet loaded with sautéed baby spinach, mushrooms, and crumbled feta cheese.",
                imageUrl = "",
                prepTimeMinutes = 10,
                cookTimeMinutes = 15,
                servings = 1,
                calories = 240f,
                protein = 28f,
                carbs = 8f,
                fat = 9f,
                difficulty = "Easy",
                dietaryTags = "High Protein,Vegetarian,Breakfast,Quick",
                allergyTags = "Gluten-Free,Nut-Free",
                isFavorite = false
            )
            val ingredients = listOf(
                RecipeIngredientEntity(id = UUID.randomUUID().toString(), recipeId = id, name = "Liquid Egg Whites", quantity = 200f, unit = "ml"),
                RecipeIngredientEntity(id = UUID.randomUUID().toString(), recipeId = id, name = "Baby Spinach", quantity = 60f, unit = "g"),
                RecipeIngredientEntity(id = UUID.randomUUID().toString(), recipeId = id, name = "Sliced Mushrooms", quantity = 50f, unit = "g"),
                RecipeIngredientEntity(id = UUID.randomUUID().toString(), recipeId = id, name = "Crumbled Feta Cheese", quantity = 25f, unit = "g"),
                RecipeIngredientEntity(id = UUID.randomUUID().toString(), recipeId = id, name = "Olive Oil", quantity = 1f, unit = "tsp")
            )
            val instructions = listOf(
                RecipeInstructionEntity(id = UUID.randomUUID().toString(), recipeId = id, stepNumber = 1, instructionText = "Heat olive oil in an oven-safe skillet and sauté mushrooms and spinach for 3 minutes."),
                RecipeInstructionEntity(id = UUID.randomUUID().toString(), recipeId = id, stepNumber = 2, instructionText = "Pour egg whites evenly over vegetables and cook undisturbed for 2 minutes."),
                RecipeInstructionEntity(id = UUID.randomUUID().toString(), recipeId = id, stepNumber = 3, instructionText = "Sprinkle crumbled feta on top and transfer skillet to an oven preheated to 180°C (350°F)."),
                RecipeInstructionEntity(id = UUID.randomUUID().toString(), recipeId = id, stepNumber = 4, instructionText = "Bake for 10-12 minutes until center is completely set.")
            )
            list.add(SeedRecipeBundle(recipe, ingredients, instructions))
        }

        // 14. Roasted Moong Dal & Peanut Snack
        run {
            val id = "seed_recipe_roasted_moong"
            val recipe = RecipeEntity(
                id = id,
                name = "Spiced Roasted Moong Dal & Peanuts",
                description = "Crunchy roasted split yellow lentils and peanuts tossed with chaat masala, rock salt, and lemon zest.",
                imageUrl = "",
                prepTimeMinutes = 5,
                cookTimeMinutes = 10,
                servings = 2,
                calories = 220f,
                protein = 10f,
                carbs = 24f,
                fat = 9f,
                difficulty = "Easy",
                dietaryTags = "Vegetarian,Indian,Snacks,Quick,High Protein",
                allergyTags = "Gluten-Free,Dairy-Free",
                isFavorite = false
            )
            val ingredients = listOf(
                RecipeIngredientEntity(id = UUID.randomUUID().toString(), recipeId = id, name = "Yellow Moong Dal (soaked & dried)", quantity = 80f, unit = "g"),
                RecipeIngredientEntity(id = UUID.randomUUID().toString(), recipeId = id, name = "Roasted Peanuts", quantity = 30f, unit = "g"),
                RecipeIngredientEntity(id = UUID.randomUUID().toString(), recipeId = id, name = "Chaat Masala", quantity = 1f, unit = "tsp"),
                RecipeIngredientEntity(id = UUID.randomUUID().toString(), recipeId = id, name = "Black Salt (Kala Namak)", quantity = 0.5f, unit = "tsp"),
                RecipeIngredientEntity(id = UUID.randomUUID().toString(), recipeId = id, name = "Olive Oil", quantity = 1f, unit = "tsp")
            )
            val instructions = listOf(
                RecipeInstructionEntity(id = UUID.randomUUID().toString(), recipeId = id, stepNumber = 1, instructionText = "Heat oil in a heavy-bottomed pan on medium heat."),
                RecipeInstructionEntity(id = UUID.randomUUID().toString(), recipeId = id, stepNumber = 2, instructionText = "Roast the dried yellow moong dal for 6-8 minutes until golden and crisp."),
                RecipeInstructionEntity(id = UUID.randomUUID().toString(), recipeId = id, stepNumber = 3, instructionText = "Toss in roasted peanuts and sprinkle chaat masala and black salt."),
                RecipeInstructionEntity(id = UUID.randomUUID().toString(), recipeId = id, stepNumber = 4, instructionText = "Cool down before serving as a high-protein crunchy snack.")
            )
            list.add(SeedRecipeBundle(recipe, ingredients, instructions))
        }

        // 15. Quinoa Bowl with Roasted Veggies & Hummus
        run {
            val id = "seed_recipe_quinoa_hummus"
            val recipe = RecipeEntity(
                id = id,
                name = "Quinoa Bowl with Roasted Veggies & Hummus",
                description = "Nutritious plant-powered bowl with tri-color quinoa, roasted zucchini, chickpeas, and a scoop of creamy hummus.",
                imageUrl = "",
                prepTimeMinutes = 15,
                cookTimeMinutes = 20,
                servings = 1,
                calories = 420f,
                protein = 15f,
                carbs = 58f,
                fat = 15f,
                difficulty = "Easy",
                dietaryTags = "Vegetarian,Lunch,Dinner",
                allergyTags = "Gluten-Free,Dairy-Free,Nut-Free",
                isFavorite = false
            )
            val ingredients = listOf(
                RecipeIngredientEntity(id = UUID.randomUUID().toString(), recipeId = id, name = "Cooked Quinoa", quantity = 160f, unit = "g"),
                RecipeIngredientEntity(id = UUID.randomUUID().toString(), recipeId = id, name = "Zucchini & Red Onion", quantity = 100f, unit = "g"),
                RecipeIngredientEntity(id = UUID.randomUUID().toString(), recipeId = id, name = "Cooked Chickpeas", quantity = 80f, unit = "g"),
                RecipeIngredientEntity(id = UUID.randomUUID().toString(), recipeId = id, name = "Creamy Hummus", quantity = 2f, unit = "tbsp"),
                RecipeIngredientEntity(id = UUID.randomUUID().toString(), recipeId = id, name = "Olive Oil & Paprika", quantity = 1f, unit = "tsp")
            )
            val instructions = listOf(
                RecipeInstructionEntity(id = UUID.randomUUID().toString(), recipeId = id, stepNumber = 1, instructionText = "Toss sliced zucchini, red onion, and chickpeas in olive oil, paprika, and salt."),
                RecipeInstructionEntity(id = UUID.randomUUID().toString(), recipeId = id, stepNumber = 2, instructionText = "Roast on a baking sheet at 200°C (400°F) for 18 minutes until caramelised."),
                RecipeInstructionEntity(id = UUID.randomUUID().toString(), recipeId = id, stepNumber = 3, instructionText = "Layer warm quinoa into a bowl, arrange roasted vegetables on top."),
                RecipeInstructionEntity(id = UUID.randomUUID().toString(), recipeId = id, stepNumber = 4, instructionText = "Add a generous scoop of hummus in the center and serve.")
            )
            list.add(SeedRecipeBundle(recipe, ingredients, instructions))
        }

        // 16. Grilled Chicken Tikka Skewers
        run {
            val id = "seed_recipe_chicken_tikka"
            val recipe = RecipeEntity(
                id = id,
                name = "Smoky Chicken Tikka Skewers",
                description = "Tender boneless chicken thighs marinated in yogurt, Kashmiri chili, garam masala, and roasted to smoky perfection.",
                imageUrl = "",
                prepTimeMinutes = 25,
                cookTimeMinutes = 15,
                servings = 2,
                calories = 360f,
                protein = 42f,
                carbs = 8f,
                fat = 16f,
                difficulty = "Medium",
                dietaryTags = "High Protein,Indian,Dinner,Snacks",
                allergyTags = "Gluten-Free,Nut-Free",
                isFavorite = true
            )
            val ingredients = listOf(
                RecipeIngredientEntity(id = UUID.randomUUID().toString(), recipeId = id, name = "Boneless Chicken Breast or Thighs", quantity = 350f, unit = "g"),
                RecipeIngredientEntity(id = UUID.randomUUID().toString(), recipeId = id, name = "Greek Yogurt", quantity = 3f, unit = "tbsp"),
                RecipeIngredientEntity(id = UUID.randomUUID().toString(), recipeId = id, name = "Ginger-Garlic Paste", quantity = 1f, unit = "tbsp"),
                RecipeIngredientEntity(id = UUID.randomUUID().toString(), recipeId = id, name = "Kashmiri Red Chili Powder", quantity = 1f, unit = "tsp"),
                RecipeIngredientEntity(id = UUID.randomUUID().toString(), recipeId = id, name = "Garam Masala & Kasuri Methi", quantity = 1f, unit = "tsp"),
                RecipeIngredientEntity(id = UUID.randomUUID().toString(), recipeId = id, name = "Mustard Oil or Olive Oil", quantity = 1f, unit = "tbsp")
            )
            val instructions = listOf(
                RecipeInstructionEntity(id = UUID.randomUUID().toString(), recipeId = id, stepNumber = 1, instructionText = "Cut chicken into bite-sized cubes and mix with yogurt, oil, and all spices."),
                RecipeInstructionEntity(id = UUID.randomUUID().toString(), recipeId = id, stepNumber = 2, instructionText = "Marinate for at least 20 minutes (or overnight in the fridge)."),
                RecipeInstructionEntity(id = UUID.randomUUID().toString(), recipeId = id, stepNumber = 3, instructionText = "Thread chicken onto skewers with onion and bell pepper chunks."),
                RecipeInstructionEntity(id = UUID.randomUUID().toString(), recipeId = id, stepNumber = 4, instructionText = "Grill or broil on high heat for 12-14 minutes, turning once, until lightly charred.")
            )
            list.add(SeedRecipeBundle(recipe, ingredients, instructions))
        }

        return list
    }
}
