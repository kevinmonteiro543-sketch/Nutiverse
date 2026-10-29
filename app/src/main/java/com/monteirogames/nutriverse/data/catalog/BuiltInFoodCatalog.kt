package com.monteirogames.nutriverse.data.catalog

import com.monteirogames.nutriverse.domain.model.CatalogFoodItem
import com.monteirogames.nutriverse.domain.model.CatalogServingPreset

object BuiltInFoodCatalog {

    const val CATEGORY_GRAINS = "Grains & Breads"
    const val CATEGORY_LENTILS = "Lentils & Pulses"
    const val CATEGORY_DAIRY = "Dairy"
    const val CATEGORY_EGGS_MEAT = "Eggs & Meat"
    const val CATEGORY_VEGETABLES = "Vegetables"
    const val CATEGORY_FRUITS = "Fruits"
    const val CATEGORY_NUTS_SEEDS = "Nuts & Seeds"
    const val CATEGORY_OILS_FATS = "Oils & Fats"
    const val CATEGORY_SNACKS_DISHES = "Snacks & Dishes"
    const val CATEGORY_BEVERAGES = "Beverages"

    val categories = listOf(
        CATEGORY_GRAINS,
        CATEGORY_LENTILS,
        CATEGORY_DAIRY,
        CATEGORY_EGGS_MEAT,
        CATEGORY_VEGETABLES,
        CATEGORY_FRUITS,
        CATEGORY_NUTS_SEEDS,
        CATEGORY_OILS_FATS,
        CATEGORY_SNACKS_DISHES,
        CATEGORY_BEVERAGES
    )

    val foods: List<CatalogFoodItem> = listOf(
        // ==========================================
        // 1. Grains & Breads
        // ==========================================
        CatalogFoodItem(
            id = "cat_rice_cooked",
            name = "White Rice (Cooked)",
            category = CATEGORY_GRAINS,
            caloriesPer100g = 130f,
            proteinPer100g = 2.7f,
            carbsPer100g = 28.2f,
            fatPer100g = 0.3f,
            presets = listOf(
                CatalogServingPreset("100g (1/2 cup)", 100f),
                CatalogServingPreset("150g (3/4 cup)", 150f),
                CatalogServingPreset("200g (1 medium bowl)", 200f)
            ),
            isCooked = true,
            description = "Standard boiled white basmati / sonamasuri rice"
        ),
        CatalogFoodItem(
            id = "cat_rice_raw",
            name = "White Rice (Raw / Uncooked)",
            category = CATEGORY_GRAINS,
            caloriesPer100g = 365f,
            proteinPer100g = 7.1f,
            carbsPer100g = 80.0f,
            fatPer100g = 0.7f,
            presets = listOf(
                CatalogServingPreset("50g", 50f),
                CatalogServingPreset("100g", 100f)
            ),
            isCooked = false,
            description = "Uncooked dry white rice grains"
        ),
        CatalogFoodItem(
            id = "cat_brown_rice_cooked",
            name = "Brown Rice (Cooked)",
            category = CATEGORY_GRAINS,
            caloriesPer100g = 123f,
            proteinPer100g = 2.7f,
            carbsPer100g = 25.6f,
            fatPer100g = 1.0f,
            presets = listOf(
                CatalogServingPreset("100g", 100f),
                CatalogServingPreset("150g", 150f),
                CatalogServingPreset("200g", 200f)
            ),
            isCooked = true,
            description = "Steamed whole grain brown rice"
        ),
        CatalogFoodItem(
            id = "cat_roti_chapati",
            name = "Roti / Chapati (Whole Wheat, No Oil)",
            category = CATEGORY_GRAINS,
            caloriesPer100g = 297f,
            proteinPer100g = 9.0f,
            carbsPer100g = 55.0f,
            fatPer100g = 3.5f,
            defaultQuantityGrams = 40f,
            presets = listOf(
                CatalogServingPreset("1 roti (~40g)", 40f),
                CatalogServingPreset("2 rotis (~80g)", 80f),
                CatalogServingPreset("3 rotis (~120g)", 120f)
            ),
            isCooked = true,
            description = "Freshly rolled and puffed whole wheat flatbread on tawa"
        ),
        CatalogFoodItem(
            id = "cat_paratha_plain",
            name = "Paratha (Plain, Shallow Fried)",
            category = CATEGORY_GRAINS,
            caloriesPer100g = 320f,
            proteinPer100g = 6.5f,
            carbsPer100g = 45.0f,
            fatPer100g = 13.0f,
            defaultQuantityGrams = 60f,
            presets = listOf(
                CatalogServingPreset("1 paratha (~60g)", 60f),
                CatalogServingPreset("2 parathas (~120g)", 120f)
            ),
            isCooked = true,
            description = "Layered whole wheat flatbread prepared with oil or ghee"
        ),
        CatalogFoodItem(
            id = "cat_naan_plain",
            name = "Naan (Plain, Tandoor Baked)",
            category = CATEGORY_GRAINS,
            caloriesPer100g = 310f,
            proteinPer100g = 9.0f,
            carbsPer100g = 54.0f,
            fatPer100g = 6.0f,
            defaultQuantityGrams = 80f,
            presets = listOf(
                CatalogServingPreset("1 naan (~80g)", 80f),
                CatalogServingPreset("1/2 naan (~40g)", 40f)
            ),
            isCooked = true,
            description = "Leavened oven-baked flatbread"
        ),
        CatalogFoodItem(
            id = "cat_poha_cooked",
            name = "Poha (Cooked Flattened Rice with Onion & Spices)",
            category = CATEGORY_GRAINS,
            caloriesPer100g = 145f,
            proteinPer100g = 2.8f,
            carbsPer100g = 24.5f,
            fatPer100g = 4.0f,
            defaultQuantityGrams = 150f,
            presets = listOf(
                CatalogServingPreset("100g", 100f),
                CatalogServingPreset("150g (1 plate)", 150f),
                CatalogServingPreset("200g (hearty bowl)", 200f)
            ),
            isCooked = true,
            description = "Tempered flattened rice with turmeric, mustard seeds, and onion"
        ),
        CatalogFoodItem(
            id = "cat_upma_cooked",
            name = "Upma (Cooked Semolina / Rava)",
            category = CATEGORY_GRAINS,
            caloriesPer100g = 130f,
            proteinPer100g = 3.2f,
            carbsPer100g = 21.5f,
            fatPer100g = 3.5f,
            defaultQuantityGrams = 150f,
            presets = listOf(
                CatalogServingPreset("100g", 100f),
                CatalogServingPreset("150g (1 bowl)", 150f),
                CatalogServingPreset("200g", 200f)
            ),
            isCooked = true,
            description = "Savory semolina porridge with vegetables and tempering"
        ),
        CatalogFoodItem(
            id = "cat_idli_steamed",
            name = "Idli (Steamed Rice & Urad Dal Cake)",
            category = CATEGORY_GRAINS,
            caloriesPer100g = 132f,
            proteinPer100g = 3.8f,
            carbsPer100g = 27.0f,
            fatPer100g = 0.8f,
            defaultQuantityGrams = 80f,
            presets = listOf(
                CatalogServingPreset("1 idli (~40g)", 40f),
                CatalogServingPreset("2 idlis (~80g)", 80f),
                CatalogServingPreset("3 idlis (~120g)", 120f),
                CatalogServingPreset("4 idlis (~160g)", 160f)
            ),
            isCooked = true,
            description = "Traditional fermented and steamed South Indian rice cake"
        ),
        CatalogFoodItem(
            id = "cat_dosa_plain",
            name = "Dosa (Plain, Crispy Crepe)",
            category = CATEGORY_GRAINS,
            caloriesPer100g = 168f,
            proteinPer100g = 4.2f,
            carbsPer100g = 29.0f,
            fatPer100g = 3.7f,
            defaultQuantityGrams = 80f,
            presets = listOf(
                CatalogServingPreset("1 medium dosa (~80g)", 80f),
                CatalogServingPreset("1 large dosa (~120g)", 120f)
            ),
            isCooked = true,
            description = "Fermented rice and black gram savory crepe made on tawa"
        ),
        CatalogFoodItem(
            id = "cat_dosa_masala",
            name = "Masala Dosa (with Potato Filling)",
            category = CATEGORY_GRAINS,
            caloriesPer100g = 185f,
            proteinPer100g = 4.0f,
            carbsPer100g = 28.0f,
            fatPer100g = 6.5f,
            defaultQuantityGrams = 150f,
            presets = listOf(
                CatalogServingPreset("1 masala dosa (~150g)", 150f)
            ),
            isCooked = true,
            description = "Crisp dosa filled with seasoned spiced mashed potatoes"
        ),
        CatalogFoodItem(
            id = "cat_bread_white",
            name = "Bread (White, Sliced)",
            category = CATEGORY_GRAINS,
            caloriesPer100g = 265f,
            proteinPer100g = 9.0f,
            carbsPer100g = 49.0f,
            fatPer100g = 3.2f,
            defaultQuantityGrams = 56f,
            presets = listOf(
                CatalogServingPreset("1 slice (~28g)", 28f),
                CatalogServingPreset("2 slices (~56g)", 56f),
                CatalogServingPreset("3 slices (~84g)", 84f)
            ),
            isCooked = true,
            description = "Standard commercial white sandwich bread"
        ),
        CatalogFoodItem(
            id = "cat_bread_whole_wheat",
            name = "Bread (Whole Wheat / Brown)",
            category = CATEGORY_GRAINS,
            caloriesPer100g = 247f,
            proteinPer100g = 13.0f,
            carbsPer100g = 41.0f,
            fatPer100g = 3.4f,
            defaultQuantityGrams = 56f,
            presets = listOf(
                CatalogServingPreset("1 slice (~28g)", 28f),
                CatalogServingPreset("2 slices (~56g)", 56f),
                CatalogServingPreset("3 slices (~84g)", 84f)
            ),
            isCooked = true,
            description = "Whole wheat sandwich bread with dietary fiber"
        ),
        CatalogFoodItem(
            id = "cat_oats_rolled_dry",
            name = "Oats (Rolled / Quick, Dry)",
            category = CATEGORY_GRAINS,
            caloriesPer100g = 389f,
            proteinPer100g = 16.9f,
            carbsPer100g = 66.3f,
            fatPer100g = 6.9f,
            defaultQuantityGrams = 40f,
            presets = listOf(
                CatalogServingPreset("40g (1 serving)", 40f),
                CatalogServingPreset("50g", 50f),
                CatalogServingPreset("80g (large bowl)", 80f)
            ),
            isCooked = false,
            description = "Dry rolled whole oat flakes"
        ),
        CatalogFoodItem(
            id = "cat_oats_cooked_water",
            name = "Oats (Cooked with Water, Porridge)",
            category = CATEGORY_GRAINS,
            caloriesPer100g = 71f,
            proteinPer100g = 2.5f,
            carbsPer100g = 12.0f,
            fatPer100g = 1.5f,
            defaultQuantityGrams = 150f,
            presets = listOf(
                CatalogServingPreset("100g", 100f),
                CatalogServingPreset("150g (1 small bowl)", 150f),
                CatalogServingPreset("250g (1 large bowl)", 250f)
            ),
            isCooked = true,
            description = "Rolled oats simmered in plain water"
        ),

        // ==========================================
        // 2. Lentils & Pulses
        // ==========================================
        CatalogFoodItem(
            id = "cat_dal_yellow_cooked",
            name = "Yellow Dal Tadka (Cooked Toor / Moong)",
            category = CATEGORY_LENTILS,
            caloriesPer100g = 98f,
            proteinPer100g = 5.5f,
            carbsPer100g = 12.5f,
            fatPer100g = 3.0f,
            defaultQuantityGrams = 150f,
            presets = listOf(
                CatalogServingPreset("100g", 100f),
                CatalogServingPreset("150g (1 small bowl)", 150f),
                CatalogServingPreset("200g (1 medium katori)", 200f)
            ),
            isCooked = true,
            description = "Simmered yellow split pigeon peas with cumin, garlic, and ghee tadka"
        ),
        CatalogFoodItem(
            id = "cat_dal_makhani_cooked",
            name = "Dal Makhani (Cooked Black Lentils with Butter)",
            category = CATEGORY_LENTILS,
            caloriesPer100g = 145f,
            proteinPer100g = 5.0f,
            carbsPer100g = 14.0f,
            fatPer100g = 8.0f,
            defaultQuantityGrams = 150f,
            presets = listOf(
                CatalogServingPreset("100g", 100f),
                CatalogServingPreset("150g (1 bowl)", 150f),
                CatalogServingPreset("200g", 200f)
            ),
            isCooked = true,
            description = "Slow-cooked whole black urad and kidney beans with butter & cream"
        ),
        CatalogFoodItem(
            id = "cat_rajma_curry_cooked",
            name = "Rajma Curry (Cooked Kidney Beans in Gravy)",
            category = CATEGORY_LENTILS,
            caloriesPer100g = 128f,
            proteinPer100g = 6.2f,
            carbsPer100g = 18.5f,
            fatPer100g = 3.5f,
            defaultQuantityGrams = 150f,
            presets = listOf(
                CatalogServingPreset("100g", 100f),
                CatalogServingPreset("150g (1 bowl)", 150f),
                CatalogServingPreset("200g (standard portion)", 200f)
            ),
            isCooked = true,
            description = "Red kidney beans simmered in spiced tomato-onion masala"
        ),
        CatalogFoodItem(
            id = "cat_chana_masala_cooked",
            name = "Chana Masala (Cooked Chickpea Curry)",
            category = CATEGORY_LENTILS,
            caloriesPer100g = 142f,
            proteinPer100g = 6.8f,
            carbsPer100g = 20.0f,
            fatPer100g = 4.0f,
            defaultQuantityGrams = 150f,
            presets = listOf(
                CatalogServingPreset("100g", 100f),
                CatalogServingPreset("150g (1 bowl)", 150f),
                CatalogServingPreset("200g", 200f)
            ),
            isCooked = true,
            description = "White chickpeas cooked in aromatic onion, tomato, and ginger gravy"
        ),
        CatalogFoodItem(
            id = "cat_chickpeas_boiled",
            name = "Boiled Chickpeas / Kabuli Chana (Plain)",
            category = CATEGORY_LENTILS,
            caloriesPer100g = 164f,
            proteinPer100g = 8.9f,
            carbsPer100g = 27.4f,
            fatPer100g = 2.6f,
            presets = listOf(
                CatalogServingPreset("50g", 50f),
                CatalogServingPreset("100g (1/2 cup)", 100f),
                CatalogServingPreset("150g", 150f)
            ),
            isCooked = true,
            description = "Tender pressure-cooked garbanzo beans without added oil"
        ),
        CatalogFoodItem(
            id = "cat_chickpeas_raw",
            name = "Chickpeas / Chana (Raw / Dry)",
            category = CATEGORY_LENTILS,
            caloriesPer100g = 378f,
            proteinPer100g = 20.5f,
            carbsPer100g = 63.0f,
            fatPer100g = 6.0f,
            presets = listOf(
                CatalogServingPreset("50g", 50f),
                CatalogServingPreset("100g", 100f)
            ),
            isCooked = false,
            description = "Dry uncooked raw garbanzo beans"
        ),
        CatalogFoodItem(
            id = "cat_moong_sprouts_raw",
            name = "Moong Sprouts (Raw Green Gram)",
            category = CATEGORY_LENTILS,
            caloriesPer100g = 30f,
            proteinPer100g = 3.1f,
            carbsPer100g = 5.9f,
            fatPer100g = 0.2f,
            presets = listOf(
                CatalogServingPreset("50g (1 cup raw)", 50f),
                CatalogServingPreset("100g", 100f)
            ),
            isCooked = false,
            description = "Germinated fresh green mung bean sprouts"
        ),
        CatalogFoodItem(
            id = "cat_sambar_cooked",
            name = "Sambar (Lentil & Mixed Vegetable Stew)",
            category = CATEGORY_LENTILS,
            caloriesPer100g = 68f,
            proteinPer100g = 2.8f,
            carbsPer100g = 10.0f,
            fatPer100g = 2.0f,
            defaultQuantityGrams = 150f,
            presets = listOf(
                CatalogServingPreset("100g", 100f),
                CatalogServingPreset("150g (1 bowl)", 150f),
                CatalogServingPreset("200g", 200f)
            ),
            isCooked = true,
            description = "Traditional tamarind and toor dal soup with vegetables and curry leaves"
        ),
        CatalogFoodItem(
            id = "cat_besan_raw",
            name = "Besan / Gram Flour (Chickpea Flour, Raw)",
            category = CATEGORY_LENTILS,
            caloriesPer100g = 387f,
            proteinPer100g = 22.4f,
            carbsPer100g = 57.8f,
            fatPer100g = 6.7f,
            defaultQuantityGrams = 30f,
            presets = listOf(
                CatalogServingPreset("30g (2 tbsp)", 30f),
                CatalogServingPreset("50g", 50f),
                CatalogServingPreset("100g", 100f)
            ),
            isCooked = false,
            description = "Milled flour made from split brown chickpeas / chana dal"
        ),

        // ==========================================
        // 3. Dairy
        // ==========================================
        CatalogFoodItem(
            id = "cat_milk_whole_cow",
            name = "Cow Milk (Whole / Full Cream)",
            category = CATEGORY_DAIRY,
            caloriesPer100g = 62f,
            proteinPer100g = 3.2f,
            carbsPer100g = 4.7f,
            fatPer100g = 3.6f,
            defaultQuantityGrams = 200f,
            servingUnitName = "ml / g",
            presets = listOf(
                CatalogServingPreset("100g (~100ml)", 100f),
                CatalogServingPreset("200g (1 glass / ~200ml)", 200f),
                CatalogServingPreset("250g (1 mug)", 250f)
            ),
            isCooked = false,
            description = "Fresh unskimmed pasteurized cow milk"
        ),
        CatalogFoodItem(
            id = "cat_milk_toned",
            name = "Cow / Buffalo Milk (Toned, 3% Fat)",
            category = CATEGORY_DAIRY,
            caloriesPer100g = 58f,
            proteinPer100g = 3.1f,
            carbsPer100g = 4.8f,
            fatPer100g = 3.0f,
            defaultQuantityGrams = 200f,
            servingUnitName = "ml / g",
            presets = listOf(
                CatalogServingPreset("100g (~100ml)", 100f),
                CatalogServingPreset("200g (1 glass)", 200f),
                CatalogServingPreset("250g", 250f)
            ),
            isCooked = false,
            description = "Standard homogenized toned milk widely consumed in India"
        ),
        CatalogFoodItem(
            id = "cat_curd_dahi_plain",
            name = "Curd / Dahi (Plain Whole Milk Yogurt)",
            category = CATEGORY_DAIRY,
            caloriesPer100g = 60f,
            proteinPer100g = 3.5f,
            carbsPer100g = 4.5f,
            fatPer100g = 3.2f,
            defaultQuantityGrams = 100f,
            presets = listOf(
                CatalogServingPreset("50g", 50f),
                CatalogServingPreset("100g (1 small katori)", 100f),
                CatalogServingPreset("150g (1 cup)", 150f),
                CatalogServingPreset("200g", 200f)
            ),
            isCooked = false,
            description = "Natural unsweetened Indian dahi"
        ),
        CatalogFoodItem(
            id = "cat_greek_yogurt",
            name = "Greek Yogurt / Hung Curd (Plain)",
            category = CATEGORY_DAIRY,
            caloriesPer100g = 97f,
            proteinPer100g = 9.0f,
            carbsPer100g = 4.0f,
            fatPer100g = 5.0f,
            defaultQuantityGrams = 100f,
            presets = listOf(
                CatalogServingPreset("100g (1 serving)", 100f),
                CatalogServingPreset("150g", 150f)
            ),
            isCooked = false,
            description = "Strained high-protein cultured yogurt"
        ),
        CatalogFoodItem(
            id = "cat_paneer_raw",
            name = "Paneer / Indian Cottage Cheese (Fresh, Raw)",
            category = CATEGORY_DAIRY,
            caloriesPer100g = 265f,
            proteinPer100g = 18.3f,
            carbsPer100g = 3.4f,
            fatPer100g = 20.8f,
            defaultQuantityGrams = 100f,
            presets = listOf(
                CatalogServingPreset("50g (~4-5 cubes)", 50f),
                CatalogServingPreset("100g", 100f),
                CatalogServingPreset("150g", 150f)
            ),
            isCooked = false,
            description = "Fresh unaged non-melting curd cheese"
        ),
        CatalogFoodItem(
            id = "cat_paneer_bhurji_cooked",
            name = "Paneer Bhurji / Scrambled Paneer (Cooked)",
            category = CATEGORY_DAIRY,
            caloriesPer100g = 240f,
            proteinPer100g = 14.0f,
            carbsPer100g = 5.0f,
            fatPer100g = 18.0f,
            defaultQuantityGrams = 100f,
            presets = listOf(
                CatalogServingPreset("100g", 100f),
                CatalogServingPreset("150g", 150f)
            ),
            isCooked = true,
            description = "Crumpled paneer sauteed with onions, tomatoes, and cumin"
        ),
        CatalogFoodItem(
            id = "cat_ghee_pure",
            name = "Desi Ghee (Clarified Butter)",
            category = CATEGORY_DAIRY,
            caloriesPer100g = 890f,
            proteinPer100g = 0.1f,
            carbsPer100g = 0.0f,
            fatPer100g = 99.5f,
            defaultQuantityGrams = 5f,
            presets = listOf(
                CatalogServingPreset("5g (1 teaspoon)", 5f),
                CatalogServingPreset("15g (1 tablespoon)", 15f)
            ),
            isCooked = true,
            description = "Traditional clarified butter with nutty aroma"
        ),
        CatalogFoodItem(
            id = "cat_butter_salted",
            name = "Butter (Table / Salted)",
            category = CATEGORY_DAIRY,
            caloriesPer100g = 717f,
            proteinPer100g = 0.9f,
            carbsPer100g = 0.1f,
            fatPer100g = 81.1f,
            defaultQuantityGrams = 10f,
            presets = listOf(
                CatalogServingPreset("10g (1 pat / cube)", 10f),
                CatalogServingPreset("20g (2 pats)", 20f)
            ),
            isCooked = false,
            description = "Pasteurized cream dairy butter"
        ),
        CatalogFoodItem(
            id = "cat_cheese_cheddar",
            name = "Cheese (Cheddar / Processed Slice)",
            category = CATEGORY_DAIRY,
            caloriesPer100g = 390f,
            proteinPer100g = 24.0f,
            carbsPer100g = 2.0f,
            fatPer100g = 32.0f,
            defaultQuantityGrams = 20f,
            presets = listOf(
                CatalogServingPreset("20g (1 slice)", 20f),
                CatalogServingPreset("40g (2 slices)", 40f)
            ),
            isCooked = false,
            description = "Semi-hard natural or processed cheese slice"
        ),

        // ==========================================
        // 4. Eggs & Meat
        // ==========================================
        CatalogFoodItem(
            id = "cat_egg_whole_boiled",
            name = "Egg (Whole, Hard Boiled)",
            category = CATEGORY_EGGS_MEAT,
            caloriesPer100g = 155f,
            proteinPer100g = 12.6f,
            carbsPer100g = 1.1f,
            fatPer100g = 10.6f,
            defaultQuantityGrams = 50f,
            presets = listOf(
                CatalogServingPreset("1 large egg (~50g)", 50f),
                CatalogServingPreset("2 large eggs (~100g)", 100f),
                CatalogServingPreset("3 large eggs (~150g)", 150f)
            ),
            isCooked = true,
            description = "Hard-boiled chicken egg with yolk"
        ),
        CatalogFoodItem(
            id = "cat_egg_white_boiled",
            name = "Egg White (Boiled)",
            category = CATEGORY_EGGS_MEAT,
            caloriesPer100g = 52f,
            proteinPer100g = 11.0f,
            carbsPer100g = 0.7f,
            fatPer100g = 0.2f,
            defaultQuantityGrams = 33f,
            presets = listOf(
                CatalogServingPreset("1 white (~33g)", 33f),
                CatalogServingPreset("2 whites (~66g)", 66f),
                CatalogServingPreset("3 whites (~100g)", 100f)
            ),
            isCooked = true,
            description = "Pure boiled egg albumen, zero yolk"
        ),
        CatalogFoodItem(
            id = "cat_egg_omelette",
            name = "Egg Omelette (with Onion & Oil)",
            category = CATEGORY_EGGS_MEAT,
            caloriesPer100g = 180f,
            proteinPer100g = 11.5f,
            carbsPer100g = 2.5f,
            fatPer100g = 14.0f,
            defaultQuantityGrams = 65f,
            presets = listOf(
                CatalogServingPreset("1 egg omelette (~65g)", 65f),
                CatalogServingPreset("2 egg omelette (~130g)", 130f)
            ),
            isCooked = true,
            description = "Pan-cooked beaten egg with chopped onion and green chilli"
        ),
        CatalogFoodItem(
            id = "cat_chicken_breast_cooked",
            name = "Chicken Breast (Boneless, Grilled / Boiled)",
            category = CATEGORY_EGGS_MEAT,
            caloriesPer100g = 165f,
            proteinPer100g = 31.0f,
            carbsPer100g = 0.0f,
            fatPer100g = 3.6f,
            presets = listOf(
                CatalogServingPreset("100g (1 palm portion)", 100f),
                CatalogServingPreset("150g", 150f),
                CatalogServingPreset("200g (full breast)", 200f)
            ),
            isCooked = true,
            description = "Lean skinless cooked chicken breast fillet"
        ),
        CatalogFoodItem(
            id = "cat_chicken_curry_cooked",
            name = "Chicken Curry (Standard Home-style Gravy)",
            category = CATEGORY_EGGS_MEAT,
            caloriesPer100g = 160f,
            proteinPer100g = 14.5f,
            carbsPer100g = 4.5f,
            fatPer100g = 9.5f,
            defaultQuantityGrams = 150f,
            presets = listOf(
                CatalogServingPreset("100g", 100f),
                CatalogServingPreset("150g (1 piece + gravy)", 150f),
                CatalogServingPreset("200g (2 pieces + gravy)", 200f)
            ),
            isCooked = true,
            description = "Chicken pieces stewed in traditional onion, tomato, and spice curry"
        ),
        CatalogFoodItem(
            id = "cat_fish_curry_cooked",
            name = "Fish Curry (Home-style Indian Gravy)",
            category = CATEGORY_EGGS_MEAT,
            caloriesPer100g = 120f,
            proteinPer100g = 13.0f,
            carbsPer100g = 4.0f,
            fatPer100g = 5.5f,
            defaultQuantityGrams = 150f,
            presets = listOf(
                CatalogServingPreset("100g", 100f),
                CatalogServingPreset("150g (1 fish piece + gravy)", 150f),
                CatalogServingPreset("200g", 200f)
            ),
            isCooked = true,
            description = "Freshwater or sea fish simmered in tangy spiced curry"
        ),
        CatalogFoodItem(
            id = "cat_fish_cooked_grilled",
            name = "Fish Fillet (Rohu / Indian Carp, Pan-cooked)",
            category = CATEGORY_EGGS_MEAT,
            caloriesPer100g = 135f,
            proteinPer100g = 19.0f,
            carbsPer100g = 0.0f,
            fatPer100g = 6.5f,
            presets = listOf(
                CatalogServingPreset("100g (1 medium piece)", 100f),
                CatalogServingPreset("150g", 150f)
            ),
            isCooked = true,
            description = "Pan-seared Rohu fish fillet marinated with turmeric and salt"
        ),
        CatalogFoodItem(
            id = "cat_mutton_curry_cooked",
            name = "Mutton / Goat Curry (Home-style Gravy)",
            category = CATEGORY_EGGS_MEAT,
            caloriesPer100g = 215f,
            proteinPer100g = 16.0f,
            carbsPer100g = 3.5f,
            fatPer100g = 15.0f,
            defaultQuantityGrams = 150f,
            presets = listOf(
                CatalogServingPreset("100g", 100f),
                CatalogServingPreset("150g (1 small serving)", 150f),
                CatalogServingPreset("200g", 200f)
            ),
            isCooked = true,
            description = "Tender goat meat slow-simmered in rich aromatic gravy"
        ),

        // ==========================================
        // 5. Vegetables
        // ==========================================
        CatalogFoodItem(
            id = "cat_potato_boiled",
            name = "Potato (Boiled, Peeled, Plain)",
            category = CATEGORY_VEGETABLES,
            caloriesPer100g = 87f,
            proteinPer100g = 1.9f,
            carbsPer100g = 20.1f,
            fatPer100g = 0.1f,
            presets = listOf(
                CatalogServingPreset("100g (1 small potato)", 100f),
                CatalogServingPreset("150g (1 medium potato)", 150f),
                CatalogServingPreset("200g", 200f)
            ),
            isCooked = true,
            description = "Plain boiled starchy potato tuber"
        ),
        CatalogFoodItem(
            id = "cat_aloo_sabzi_cooked",
            name = "Aloo Sabzi (Cooked Potato Curry with Spices & Oil)",
            category = CATEGORY_VEGETABLES,
            caloriesPer100g = 120f,
            proteinPer100g = 2.0f,
            carbsPer100g = 18.0f,
            fatPer100g = 4.5f,
            defaultQuantityGrams = 150f,
            presets = listOf(
                CatalogServingPreset("100g", 100f),
                CatalogServingPreset("150g (1 small bowl)", 150f),
                CatalogServingPreset("200g", 200f)
            ),
            isCooked = true,
            description = "Sauteed potatoes seasoned with cumin, turmeric, and mustard oil"
        ),
        CatalogFoodItem(
            id = "cat_tomato_raw",
            name = "Tomato (Fresh, Raw)",
            category = CATEGORY_VEGETABLES,
            caloriesPer100g = 18f,
            proteinPer100g = 0.9f,
            carbsPer100g = 3.9f,
            fatPer100g = 0.2f,
            defaultQuantityGrams = 100f,
            presets = listOf(
                CatalogServingPreset("60g (1 small)", 60f),
                CatalogServingPreset("120g (1 medium tomato)", 120f)
            ),
            isCooked = false,
            description = "Fresh ripe red salad tomato"
        ),
        CatalogFoodItem(
            id = "cat_onion_raw",
            name = "Onion (Fresh, Raw)",
            category = CATEGORY_VEGETABLES,
            caloriesPer100g = 40f,
            proteinPer100g = 1.1f,
            carbsPer100g = 9.3f,
            fatPer100g = 0.1f,
            defaultQuantityGrams = 50f,
            presets = listOf(
                CatalogServingPreset("50g (1 small onion)", 50f),
                CatalogServingPreset("100g (1 medium onion)", 100f)
            ),
            isCooked = false,
            description = "Raw chopped or sliced red onion"
        ),
        CatalogFoodItem(
            id = "cat_cucumber_raw",
            name = "Cucumber (Fresh, Raw with Peel)",
            category = CATEGORY_VEGETABLES,
            caloriesPer100g = 15f,
            proteinPer100g = 0.7f,
            carbsPer100g = 3.6f,
            fatPer100g = 0.1f,
            presets = listOf(
                CatalogServingPreset("100g (~1/2 cucumber)", 100f),
                CatalogServingPreset("150g (1 medium cucumber)", 150f)
            ),
            isCooked = false,
            description = "Hydrating crisp salad cucumber"
        ),
        CatalogFoodItem(
            id = "cat_spinach_cooked",
            name = "Palak / Spinach (Cooked / Steamed)",
            category = CATEGORY_VEGETABLES,
            caloriesPer100g = 30f,
            proteinPer100g = 3.0f,
            carbsPer100g = 3.8f,
            fatPer100g = 0.5f,
            presets = listOf(
                CatalogServingPreset("100g (1/2 cup cooked)", 100f),
                CatalogServingPreset("150g", 150f)
            ),
            isCooked = true,
            description = "Tender cooked leafy green spinach"
        ),
        CatalogFoodItem(
            id = "cat_palak_paneer_cooked",
            name = "Palak Paneer (Cooked Spinach & Paneer Gravy)",
            category = CATEGORY_VEGETABLES,
            caloriesPer100g = 140f,
            proteinPer100g = 7.0f,
            carbsPer100g = 5.0f,
            fatPer100g = 10.5f,
            defaultQuantityGrams = 150f,
            presets = listOf(
                CatalogServingPreset("100g", 100f),
                CatalogServingPreset("150g (1 bowl)", 150f),
                CatalogServingPreset("200g", 200f)
            ),
            isCooked = true,
            description = "Blanched pureed spinach with fresh paneer cubes and spices"
        ),
        CatalogFoodItem(
            id = "cat_carrot_raw",
            name = "Carrot (Fresh, Raw)",
            category = CATEGORY_VEGETABLES,
            caloriesPer100g = 41f,
            proteinPer100g = 0.9f,
            carbsPer100g = 9.6f,
            fatPer100g = 0.2f,
            presets = listOf(
                CatalogServingPreset("60g (1 small carrot)", 60f),
                CatalogServingPreset("120g (1 medium carrot)", 120f)
            ),
            isCooked = false,
            description = "Fresh crunchy orange/red root carrot"
        ),
        CatalogFoodItem(
            id = "cat_cauliflower_sabzi",
            name = "Gobi Sabzi (Cooked Cauliflower with Spices)",
            category = CATEGORY_VEGETABLES,
            caloriesPer100g = 65f,
            proteinPer100g = 2.2f,
            carbsPer100g = 6.5f,
            fatPer100g = 3.5f,
            defaultQuantityGrams = 150f,
            presets = listOf(
                CatalogServingPreset("100g", 100f),
                CatalogServingPreset("150g (1 bowl)", 150f)
            ),
            isCooked = true,
            description = "Stir-fried spiced cauliflower florets"
        ),
        CatalogFoodItem(
            id = "cat_bhindi_sabzi",
            name = "Bhindi Sabzi (Cooked Okra with Spices & Oil)",
            category = CATEGORY_VEGETABLES,
            caloriesPer100g = 78f,
            proteinPer100g = 2.0f,
            carbsPer100g = 8.0f,
            fatPer100g = 4.5f,
            defaultQuantityGrams = 150f,
            presets = listOf(
                CatalogServingPreset("100g", 100f),
                CatalogServingPreset("150g (1 bowl)", 150f)
            ),
            isCooked = true,
            description = "Pan-tossed lady finger / okra with dry Indian spices"
        ),
        CatalogFoodItem(
            id = "cat_green_peas_boiled",
            name = "Green Peas / Matar (Boiled)",
            category = CATEGORY_VEGETABLES,
            caloriesPer100g = 81f,
            proteinPer100g = 5.4f,
            carbsPer100g = 14.5f,
            fatPer100g = 0.4f,
            presets = listOf(
                CatalogServingPreset("50g", 50f),
                CatalogServingPreset("100g (1/2 cup)", 100f)
            ),
            isCooked = true,
            description = "Sweet green garden peas boiled until tender"
        ),
        CatalogFoodItem(
            id = "cat_karela_sabzi",
            name = "Karela Sabzi (Cooked Bitter Gourd)",
            category = CATEGORY_VEGETABLES,
            caloriesPer100g = 70f,
            proteinPer100g = 1.8f,
            carbsPer100g = 7.0f,
            fatPer100g = 4.0f,
            presets = listOf(
                CatalogServingPreset("50g", 50f),
                CatalogServingPreset("100g", 100f)
            ),
            isCooked = true,
            description = "Shallow-fried bitter melon with onions and amchur"
        ),

        // ==========================================
        // 6. Fruits
        // ==========================================
        CatalogFoodItem(
            id = "cat_banana",
            name = "Banana (Fresh, Peeled)",
            category = CATEGORY_FRUITS,
            caloriesPer100g = 89f,
            proteinPer100g = 1.1f,
            carbsPer100g = 22.8f,
            fatPer100g = 0.3f,
            defaultQuantityGrams = 120f,
            presets = listOf(
                CatalogServingPreset("100g (1 small banana)", 100f),
                CatalogServingPreset("120g (1 medium banana)", 120f),
                CatalogServingPreset("150g (1 large banana)", 150f)
            ),
            isCooked = false,
            description = "Fresh ripe peeled yellow banana"
        ),
        CatalogFoodItem(
            id = "cat_apple",
            name = "Apple (Fresh with Skin)",
            category = CATEGORY_FRUITS,
            caloriesPer100g = 52f,
            proteinPer100g = 0.3f,
            carbsPer100g = 13.8f,
            fatPer100g = 0.2f,
            defaultQuantityGrams = 150f,
            presets = listOf(
                CatalogServingPreset("100g (1 small apple)", 100f),
                CatalogServingPreset("150g (1 medium apple)", 150f),
                CatalogServingPreset("200g (1 large apple)", 200f)
            ),
            isCooked = false,
            description = "Crisp sweet eating apple"
        ),
        CatalogFoodItem(
            id = "cat_orange",
            name = "Orange (Fresh, Peeled)",
            category = CATEGORY_FRUITS,
            caloriesPer100g = 47f,
            proteinPer100g = 0.9f,
            carbsPer100g = 11.8f,
            fatPer100g = 0.1f,
            defaultQuantityGrams = 130f,
            presets = listOf(
                CatalogServingPreset("100g", 100f),
                CatalogServingPreset("130g (1 medium orange)", 130f)
            ),
            isCooked = false,
            description = "Juicy citrus orange segments"
        ),
        CatalogFoodItem(
            id = "cat_mango",
            name = "Mango (Fresh, Peeled Flesh)",
            category = CATEGORY_FRUITS,
            caloriesPer100g = 60f,
            proteinPer100g = 0.8f,
            carbsPer100g = 15.0f,
            fatPer100g = 0.4f,
            defaultQuantityGrams = 150f,
            presets = listOf(
                CatalogServingPreset("100g", 100f),
                CatalogServingPreset("150g (1 small mango / 1 cup)", 150f),
                CatalogServingPreset("200g", 200f)
            ),
            isCooked = false,
            description = "Sweet tropical ripe mango slices"
        ),
        CatalogFoodItem(
            id = "cat_papaya",
            name = "Papaya (Fresh Cubes)",
            category = CATEGORY_FRUITS,
            caloriesPer100g = 43f,
            proteinPer100g = 0.5f,
            carbsPer100g = 10.8f,
            fatPer100g = 0.3f,
            defaultQuantityGrams = 140f,
            presets = listOf(
                CatalogServingPreset("100g", 100f),
                CatalogServingPreset("140g (1 cup diced)", 140f)
            ),
            isCooked = false,
            description = "Ripe orange papaya chunks"
        ),
        CatalogFoodItem(
            id = "cat_guava",
            name = "Guava (Fresh, Whole)",
            category = CATEGORY_FRUITS,
            caloriesPer100g = 68f,
            proteinPer100g = 2.6f,
            carbsPer100g = 14.3f,
            fatPer100g = 1.0f,
            presets = listOf(
                CatalogServingPreset("55g (1 small guava)", 55f),
                CatalogServingPreset("100g (1 medium guava)", 100f)
            ),
            isCooked = false,
            description = "High fiber fresh Indian amrood / guava"
        ),
        CatalogFoodItem(
            id = "cat_pomegranate",
            name = "Pomegranate (Fresh Arils)",
            category = CATEGORY_FRUITS,
            caloriesPer100g = 83f,
            proteinPer100g = 1.7f,
            carbsPer100g = 18.7f,
            fatPer100g = 1.2f,
            presets = listOf(
                CatalogServingPreset("80g (1/2 cup arils)", 80f),
                CatalogServingPreset("150g (1 cup)", 150f)
            ),
            isCooked = false,
            description = "Tart and sweet ruby red anar seeds"
        ),
        CatalogFoodItem(
            id = "cat_watermelon",
            name = "Watermelon (Fresh Diced)",
            category = CATEGORY_FRUITS,
            caloriesPer100g = 30f,
            proteinPer100g = 0.6f,
            carbsPer100g = 7.6f,
            fatPer100g = 0.2f,
            defaultQuantityGrams = 150f,
            presets = listOf(
                CatalogServingPreset("150g (1 cup cubes)", 150f),
                CatalogServingPreset("250g (1 large wedge)", 250f)
            ),
            isCooked = false,
            description = "Refreshing chilled watermelon cubes"
        ),

        // ==========================================
        // 7. Nuts & Seeds
        // ==========================================
        CatalogFoodItem(
            id = "cat_peanuts_roasted",
            name = "Peanuts (Roasted, Unsalted)",
            category = CATEGORY_NUTS_SEEDS,
            caloriesPer100g = 567f,
            proteinPer100g = 25.8f,
            carbsPer100g = 16.1f,
            fatPer100g = 49.2f,
            defaultQuantityGrams = 28f,
            presets = listOf(
                CatalogServingPreset("15g (small handful)", 15f),
                CatalogServingPreset("28g (1 oz / standard handful)", 28f),
                CatalogServingPreset("50g", 50f)
            ),
            isCooked = true,
            description = "Dry roasted crunchy groundnuts"
        ),
        CatalogFoodItem(
            id = "cat_almonds_raw",
            name = "Almonds (Raw, Whole)",
            category = CATEGORY_NUTS_SEEDS,
            caloriesPer100g = 579f,
            proteinPer100g = 21.2f,
            carbsPer100g = 21.6f,
            fatPer100g = 49.9f,
            defaultQuantityGrams = 28f,
            presets = listOf(
                CatalogServingPreset("15g (~12 almonds)", 15f),
                CatalogServingPreset("28g (~23 almonds)", 28f),
                CatalogServingPreset("50g", 50f)
            ),
            isCooked = false,
            description = "Raw unsalted badam / almonds"
        ),
        CatalogFoodItem(
            id = "cat_cashews_raw",
            name = "Cashews (Raw / Unsalted)",
            category = CATEGORY_NUTS_SEEDS,
            caloriesPer100g = 553f,
            proteinPer100g = 18.2f,
            carbsPer100g = 30.2f,
            fatPer100g = 43.8f,
            defaultQuantityGrams = 28f,
            presets = listOf(
                CatalogServingPreset("15g (~10 cashews)", 15f),
                CatalogServingPreset("28g (1 oz / ~18 cashews)", 28f),
                CatalogServingPreset("50g", 50f)
            ),
            isCooked = false,
            description = "Whole kaju / cashew kernels"
        ),
        CatalogFoodItem(
            id = "cat_walnuts_raw",
            name = "Walnuts (Raw Halves)",
            category = CATEGORY_NUTS_SEEDS,
            caloriesPer100g = 654f,
            proteinPer100g = 15.2f,
            carbsPer100g = 13.7f,
            fatPer100g = 65.2f,
            defaultQuantityGrams = 28f,
            presets = listOf(
                CatalogServingPreset("14g (~7 halves)", 14f),
                CatalogServingPreset("28g (1 oz)", 28f)
            ),
            isCooked = false,
            description = "Nutrient-dense raw akhrot / walnut kernels"
        ),
        CatalogFoodItem(
            id = "cat_chia_seeds",
            name = "Chia Seeds (Dry)",
            category = CATEGORY_NUTS_SEEDS,
            caloriesPer100g = 486f,
            proteinPer100g = 16.5f,
            carbsPer100g = 42.1f,
            fatPer100g = 30.7f,
            defaultQuantityGrams = 15f,
            presets = listOf(
                CatalogServingPreset("15g (1 tablespoon)", 15f),
                CatalogServingPreset("30g (2 tablespoons)", 30f)
            ),
            isCooked = false,
            description = "Whole raw edible chia seeds"
        ),
        CatalogFoodItem(
            id = "cat_flax_seeds",
            name = "Flax Seeds (Raw / Ground)",
            category = CATEGORY_NUTS_SEEDS,
            caloriesPer100g = 534f,
            proteinPer100g = 18.3f,
            carbsPer100g = 28.9f,
            fatPer100g = 42.2f,
            defaultQuantityGrams = 10f,
            presets = listOf(
                CatalogServingPreset("10g (1 tablespoon)", 10f),
                CatalogServingPreset("20g (2 tablespoons)", 20f)
            ),
            isCooked = false,
            description = "Brown alsi / flax seeds rich in omega-3"
        ),

        // ==========================================
        // 8. Oils & Fats
        // ==========================================
        CatalogFoodItem(
            id = "cat_mustard_oil",
            name = "Mustard Oil (Kachi Ghani)",
            category = CATEGORY_OILS_FATS,
            caloriesPer100g = 884f,
            proteinPer100g = 0.0f,
            carbsPer100g = 0.0f,
            fatPer100g = 100.0f,
            defaultQuantityGrams = 5f,
            presets = listOf(
                CatalogServingPreset("5g (1 tsp)", 5f),
                CatalogServingPreset("14g (1 tbsp)", 14f)
            ),
            isCooked = false,
            description = "Pungent cold-pressed mustard seed oil"
        ),
        CatalogFoodItem(
            id = "cat_sunflower_oil",
            name = "Sunflower Cooking Oil",
            category = CATEGORY_OILS_FATS,
            caloriesPer100g = 884f,
            proteinPer100g = 0.0f,
            carbsPer100g = 0.0f,
            fatPer100g = 100.0f,
            defaultQuantityGrams = 5f,
            presets = listOf(
                CatalogServingPreset("5g (1 tsp)", 5f),
                CatalogServingPreset("14g (1 tbsp)", 14f)
            ),
            isCooked = false,
            description = "Neutral refined sunflower seed cooking oil"
        ),
        CatalogFoodItem(
            id = "cat_olive_oil",
            name = "Olive Oil (Extra Virgin)",
            category = CATEGORY_OILS_FATS,
            caloriesPer100g = 884f,
            proteinPer100g = 0.0f,
            carbsPer100g = 0.0f,
            fatPer100g = 100.0f,
            defaultQuantityGrams = 5f,
            presets = listOf(
                CatalogServingPreset("5g (1 tsp)", 5f),
                CatalogServingPreset("14g (1 tbsp)", 14f)
            ),
            isCooked = false,
            description = "Cold pressed monounsaturated olive oil"
        ),
        CatalogFoodItem(
            id = "cat_coconut_oil",
            name = "Coconut Oil (Virgin / Cooking)",
            category = CATEGORY_OILS_FATS,
            caloriesPer100g = 862f,
            proteinPer100g = 0.0f,
            carbsPer100g = 0.0f,
            fatPer100g = 99.1f,
            defaultQuantityGrams = 5f,
            presets = listOf(
                CatalogServingPreset("5g (1 tsp)", 5f),
                CatalogServingPreset("14g (1 tbsp)", 14f)
            ),
            isCooked = false,
            description = "Pure coconut oil used in South Indian cooking"
        ),

        // ==========================================
        // 9. Snacks & Dishes
        // ==========================================
        CatalogFoodItem(
            id = "cat_khichdi_cooked",
            name = "Moong Dal Khichdi (Cooked Rice & Lentils)",
            category = CATEGORY_SNACKS_DISHES,
            caloriesPer100g = 115f,
            proteinPer100g = 4.2f,
            carbsPer100g = 19.5f,
            fatPer100g = 2.5f,
            defaultQuantityGrams = 200f,
            presets = listOf(
                CatalogServingPreset("150g (1 small bowl)", 150f),
                CatalogServingPreset("200g (1 medium bowl)", 200f),
                CatalogServingPreset("300g (large plate)", 300f)
            ),
            isCooked = true,
            description = "Comforting one-pot dish of rice, yellow moong dal, and light ghee"
        ),
        CatalogFoodItem(
            id = "cat_veg_biryani_cooked",
            name = "Vegetable Biryani (Home-style / Cooked)",
            category = CATEGORY_SNACKS_DISHES,
            caloriesPer100g = 155f,
            proteinPer100g = 3.5f,
            carbsPer100g = 26.0f,
            fatPer100g = 4.0f,
            defaultQuantityGrams = 200f,
            presets = listOf(
                CatalogServingPreset("150g", 150f),
                CatalogServingPreset("200g (1 plate)", 200f),
                CatalogServingPreset("300g", 300f)
            ),
            isCooked = true,
            description = "Spiced basmati rice layered with mixed vegetables and saffron"
        ),
        CatalogFoodItem(
            id = "cat_chicken_biryani_cooked",
            name = "Chicken Biryani (Cooked Rice with Chicken Pieces)",
            category = CATEGORY_SNACKS_DISHES,
            caloriesPer100g = 180f,
            proteinPer100g = 8.5f,
            carbsPer100g = 24.0f,
            fatPer100g = 5.5f,
            defaultQuantityGrams = 250f,
            presets = listOf(
                CatalogServingPreset("200g", 200f),
                CatalogServingPreset("250g (1 standard plate)", 250f),
                CatalogServingPreset("350g (full restaurant portion)", 350f)
            ),
            isCooked = true,
            description = "Dum-cooked basmati rice with marinated chicken pieces and spices"
        ),
        CatalogFoodItem(
            id = "cat_dhokla_steamed",
            name = "Khaman Dhokla (Steamed Gram Flour Cake)",
            category = CATEGORY_SNACKS_DISHES,
            caloriesPer100g = 160f,
            proteinPer100g = 6.0f,
            carbsPer100g = 24.0f,
            fatPer100g = 4.5f,
            defaultQuantityGrams = 100f,
            presets = listOf(
                CatalogServingPreset("50g (1 piece)", 50f),
                CatalogServingPreset("100g (2 pieces)", 100f),
                CatalogServingPreset("150g (3 pieces)", 150f)
            ),
            isCooked = true,
            description = "Fluffy Gujarati steamed besan cake tempered with mustard seeds"
        ),
        CatalogFoodItem(
            id = "cat_samosa_fried",
            name = "Samosa (Potato & Pea Stuffed, Deep Fried)",
            category = CATEGORY_SNACKS_DISHES,
            caloriesPer100g = 262f,
            proteinPer100g = 4.2f,
            carbsPer100g = 32.0f,
            fatPer100g = 13.0f,
            defaultQuantityGrams = 75f,
            presets = listOf(
                CatalogServingPreset("75g (1 medium samosa)", 75f),
                CatalogServingPreset("150g (2 samosas)", 150f)
            ),
            isCooked = true,
            description = "Crispy triangular pastry filled with spiced potatoes and peas"
        ),
        CatalogFoodItem(
            id = "cat_vegetable_pakora",
            name = "Vegetable Pakora / Fritters",
            category = CATEGORY_SNACKS_DISHES,
            caloriesPer100g = 270f,
            proteinPer100g = 5.5f,
            carbsPer100g = 28.0f,
            fatPer100g = 15.0f,
            defaultQuantityGrams = 60f,
            presets = listOf(
                CatalogServingPreset("60g (~3-4 pakoras)", 60f),
                CatalogServingPreset("100g", 100f)
            ),
            isCooked = true,
            description = "Deep-fried gram flour fritters with onion, potato, and spinach"
        ),

        // ==========================================
        // 10. Beverages
        // ==========================================
        CatalogFoodItem(
            id = "cat_chai_milk_sugar",
            name = "Chai / Tea (with Cow Milk & 1 tsp Sugar)",
            category = CATEGORY_BEVERAGES,
            caloriesPer100g = 65f,
            proteinPer100g = 2.0f,
            carbsPer100g = 8.5f,
            fatPer100g = 2.5f,
            defaultQuantityGrams = 150f,
            servingUnitName = "ml / g",
            presets = listOf(
                CatalogServingPreset("100g (~100ml cutting chai)", 100f),
                CatalogServingPreset("150g (~150ml standard teacup)", 150f),
                CatalogServingPreset("200g (~200ml mug)", 200f)
            ),
            isCooked = true,
            description = "Indian spiced milk tea brewed with cardamom, ginger, and sugar"
        ),
        CatalogFoodItem(
            id = "cat_coffee_milk_sugar",
            name = "Coffee (with Cow Milk & 1 tsp Sugar)",
            category = CATEGORY_BEVERAGES,
            caloriesPer100g = 70f,
            proteinPer100g = 2.2f,
            carbsPer100g = 9.0f,
            fatPer100g = 2.8f,
            defaultQuantityGrams = 150f,
            servingUnitName = "ml / g",
            presets = listOf(
                CatalogServingPreset("150g (~150ml cup)", 150f),
                CatalogServingPreset("200g (~200ml mug)", 200f)
            ),
            isCooked = true,
            description = "South Indian filter or instant coffee with frothy milk and sugar"
        ),
        CatalogFoodItem(
            id = "cat_black_coffee",
            name = "Black Coffee / Espresso (Unsweetened)",
            category = CATEGORY_BEVERAGES,
            caloriesPer100g = 2f,
            proteinPer100g = 0.3f,
            carbsPer100g = 0.2f,
            fatPer100g = 0.0f,
            defaultQuantityGrams = 150f,
            servingUnitName = "ml / g",
            presets = listOf(
                CatalogServingPreset("60g (double espresso)", 60f),
                CatalogServingPreset("150g (americano)", 150f),
                CatalogServingPreset("240g (large black coffee)", 240f)
            ),
            isCooked = true,
            description = "Brewed black coffee without milk or sugar"
        ),
        CatalogFoodItem(
            id = "cat_green_tea",
            name = "Green Tea (Unsweetened)",
            category = CATEGORY_BEVERAGES,
            caloriesPer100g = 1f,
            proteinPer100g = 0.2f,
            carbsPer100g = 0.1f,
            fatPer100g = 0.0f,
            defaultQuantityGrams = 200f,
            servingUnitName = "ml / g",
            presets = listOf(
                CatalogServingPreset("150g (1 cup)", 150f),
                CatalogServingPreset("200g (1 mug)", 200f)
            ),
            isCooked = true,
            description = "Steeped green tea leaves without sugar"
        )
    )
}
