package com.monteirogames.nutriverse

import com.monteirogames.nutriverse.domain.model.*
import com.monteirogames.nutriverse.domain.sync.SyncResolution
import com.monteirogames.nutriverse.domain.sync.TimestampConflictResolverImpl
import org.junit.Assert.*
import org.junit.Test

class RecipesGroceryBudgetSyncTest {

    private val conflictResolver = TimestampConflictResolverImpl()

    private val sampleRecipe = Recipe(
        id = "custom_recipe_101",
        name = "High Protein Oats",
        description = "Quick and nutritious breakfast",
        imageUrl = "https://example.com/oats.jpg",
        prepTimeMinutes = 5,
        cookTimeMinutes = 10,
        servings = 2,
        calories = 350f,
        protein = 28f,
        carbs = 45f,
        fat = 6f,
        difficulty = "EASY",
        dietaryTags = listOf("Vegetarian", "High-Protein"),
        allergyTags = listOf("Dairy"),
        ingredients = listOf(
            RecipeIngredient(
                id = "ing_1",
                recipeId = "custom_recipe_101",
                name = "Rolled Oats",
                quantity = 80f,
                unit = "g"
            ),
            RecipeIngredient(
                id = "ing_2",
                recipeId = "custom_recipe_101",
                name = "Whey Protein",
                quantity = 30f,
                unit = "g"
            )
        ),
        instructions = listOf(
            RecipeInstruction(
                id = "inst_1",
                recipeId = "custom_recipe_101",
                stepNumber = 1,
                instructionText = "Boil oats with water or milk."
            ),
            RecipeInstruction(
                id = "inst_2",
                recipeId = "custom_recipe_101",
                stepNumber = 2,
                instructionText = "Stir in whey protein powder until smooth."
            )
        ),
        isFavorite = true,
        createdAt = 1700000000000L,
        updatedAt = 1700000050000L
    )

    private val sampleGroceryItem = GroceryItem(
        id = "groc_555",
        userId = "firebase_uid_test",
        name = "Almond Milk",
        category = "Dairy",
        quantity = 2.0f,
        unit = "L",
        isPurchased = true,
        purchasedAt = 1700000020000L,
        source = "RECIPE",
        sourceRecipeId = "custom_recipe_101",
        createdAt = 1700000000000L,
        updatedAt = 1700000030000L
    )

    private val sampleBudget = Budget(
        id = "budget_777",
        userId = "firebase_uid_test",
        amount = 500.0,
        period = "MONTHLY",
        startDate = 1700000000000L,
        endDate = 1702592000000L,
        createdAt = 1700000000000L,
        isActive = true,
        updatedAt = 1700000040000L
    )

    private val sampleExpense = FoodExpense(
        id = "exp_888",
        userId = "firebase_uid_test",
        name = "Market Groceries",
        amount = 45.50,
        category = "Groceries",
        date = 1700000010000L,
        notes = "Weekly market trip",
        groceryItemId = "groc_555",
        createdAt = 1700000010000L,
        updatedAt = 1700000025000L
    )

    // ==========================================
    // 1. RECIPE DTO & SERIALIZATION TESTS
    // ==========================================

    @Test
    fun testRecipeToFirestoreDocumentRoundTrip() {
        val doc = sampleRecipe.toFirestoreDocument(userId = "firebase_uid_test", isDeleted = false)
        val reconstructed = doc.toRecipe()

        assertEquals(sampleRecipe.id, reconstructed.id)
        assertEquals(sampleRecipe.name, reconstructed.name)
        assertEquals(sampleRecipe.description, reconstructed.description)
        assertEquals(sampleRecipe.imageUrl, reconstructed.imageUrl)
        assertEquals(sampleRecipe.prepTimeMinutes, reconstructed.prepTimeMinutes)
        assertEquals(sampleRecipe.cookTimeMinutes, reconstructed.cookTimeMinutes)
        assertEquals(sampleRecipe.servings, reconstructed.servings)
        assertEquals(sampleRecipe.calories, reconstructed.calories, 0.01f)
        assertEquals(sampleRecipe.protein, reconstructed.protein, 0.01f)
        assertEquals(sampleRecipe.carbs, reconstructed.carbs, 0.01f)
        assertEquals(sampleRecipe.fat, reconstructed.fat, 0.01f)
        assertEquals(sampleRecipe.difficulty, reconstructed.difficulty)
        assertEquals(sampleRecipe.dietaryTags, reconstructed.dietaryTags)
        assertEquals(sampleRecipe.allergyTags, reconstructed.allergyTags)
        assertEquals(sampleRecipe.isFavorite, reconstructed.isFavorite)
        assertEquals(sampleRecipe.createdAt, reconstructed.createdAt)
        assertEquals(sampleRecipe.updatedAt, reconstructed.updatedAt)

        assertEquals(2, reconstructed.ingredients.size)
        assertEquals("Rolled Oats", reconstructed.ingredients[0].name)
        assertEquals(80f, reconstructed.ingredients[0].quantity, 0.01f)
        assertEquals("g", reconstructed.ingredients[0].unit)

        assertEquals(2, reconstructed.instructions.size)
        assertEquals(1, reconstructed.instructions[0].stepNumber)
        assertEquals("Boil oats with water or milk.", reconstructed.instructions[0].instructionText)
    }

    @Test
    fun testRecipeDocumentToMapAndFromMap() {
        val doc = sampleRecipe.toFirestoreDocument(userId = "firebase_uid_test", isDeleted = false)
        val map = doc.toMap()

        assertEquals("custom_recipe_101", map["id"])
        assertEquals("firebase_uid_test", map["userId"])
        assertEquals(false, map["isDeleted"])
        assertEquals(1, map["schemaVersion"])

        val parsedDoc = FirestoreRecipeDocument.fromMap(map)
        assertEquals(doc.id, parsedDoc.id)
        assertEquals(doc.name, parsedDoc.name)
        assertEquals(doc.calories, parsedDoc.calories, 0.01f)
        assertEquals(2, parsedDoc.ingredients.size)
        assertEquals(2, parsedDoc.instructions.size)
        assertFalse(parsedDoc.isDeleted)
    }

    @Test
    fun testRecipeDocumentFromMapHandlesMalformedAndMissingFields() {
        val malformedMap = mapOf<String, Any?>(
            "id" to "corrupt_recipe",
            "servings" to "NaN",
            "calories" to null,
            "ingredients" to listOf(
                mapOf("name" to "Flour", "quantity" to "not_a_number")
            ),
            "instructions" to "not_a_list",
            "isDeleted" to true,
            "extraUnrecognizedField" to 9999
        )

        val doc = FirestoreRecipeDocument.fromMap(malformedMap, fallbackId = "fallback_rec", fallbackUserId = "fallback_u")
        assertEquals("corrupt_recipe", doc.id)
        assertEquals("fallback_u", doc.userId)
        assertTrue(doc.isDeleted)
        assertEquals(1, doc.servings)
        assertEquals(0f, doc.calories, 0.01f)
        assertEquals(1, doc.ingredients.size)
        assertEquals(0, doc.instructions.size)
    }

    // ==========================================
    // 2. GROCERY DTO & SERIALIZATION TESTS
    // ==========================================

    @Test
    fun testGroceryItemToFirestoreDocumentRoundTrip() {
        val doc = sampleGroceryItem.toFirestoreDocument(isDeleted = false)
        val reconstructed = doc.toGroceryItem()

        assertEquals(sampleGroceryItem.id, reconstructed.id)
        assertEquals(sampleGroceryItem.userId, reconstructed.userId)
        assertEquals(sampleGroceryItem.name, reconstructed.name)
        assertEquals(sampleGroceryItem.category, reconstructed.category)
        assertEquals(sampleGroceryItem.quantity, reconstructed.quantity, 0.001f)
        assertEquals(sampleGroceryItem.unit, reconstructed.unit)
        assertEquals(sampleGroceryItem.isPurchased, reconstructed.isPurchased)
        assertEquals(sampleGroceryItem.purchasedAt, reconstructed.purchasedAt)
        assertEquals(sampleGroceryItem.source, reconstructed.source)
        assertEquals(sampleGroceryItem.sourceRecipeId, reconstructed.sourceRecipeId)
        assertEquals(sampleGroceryItem.createdAt, reconstructed.createdAt)
        assertEquals(sampleGroceryItem.updatedAt, reconstructed.updatedAt)
    }

    @Test
    fun testGroceryDocumentToMapAndFromMap() {
        val doc = sampleGroceryItem.toFirestoreDocument(isDeleted = false)
        val map = doc.toMap()

        assertEquals("groc_555", map["id"])
        assertEquals("firebase_uid_test", map["userId"])
        assertEquals(true, map["isPurchased"])
        assertEquals("RECIPE", map["source"])

        val parsed = FirestoreGroceryDocument.fromMap(map)
        assertEquals(doc.id, parsed.id)
        assertEquals(doc.name, parsed.name)
        assertTrue(parsed.isPurchased)
        assertFalse(parsed.isDeleted)
    }

    @Test
    fun testGroceryDocumentFromMapHandlesMalformedData() {
        val malformedMap = mapOf<String, Any?>(
            "id" to "groc_bad",
            "quantity" to "not_a_number",
            "isPurchased" to "YES",
            "source" to "UNKNOWN_SOURCE"
        )

        val doc = FirestoreGroceryDocument.fromMap(malformedMap, fallbackId = "fb_id", fallbackUserId = "fb_uid")
        assertEquals("groc_bad", doc.id)
        assertEquals(1.0f, doc.quantity, 0.001f)
        val item = doc.toGroceryItem()
        assertEquals("UNKNOWN_SOURCE", item.source)
    }

    // ==========================================
    // 3. BUDGET DTO & SERIALIZATION TESTS
    // ==========================================

    @Test
    fun testBudgetToFirestoreDocumentRoundTrip() {
        val doc = sampleBudget.toFirestoreDocument(isDeleted = false)
        val reconstructed = doc.toBudget()

        assertEquals(sampleBudget.id, reconstructed.id)
        assertEquals(sampleBudget.userId, reconstructed.userId)
        assertEquals(sampleBudget.amount, reconstructed.amount, 0.001)
        assertEquals(sampleBudget.period, reconstructed.period)
        assertEquals(sampleBudget.startDate, reconstructed.startDate)
        assertEquals(sampleBudget.endDate, reconstructed.endDate)
        assertEquals(sampleBudget.createdAt, reconstructed.createdAt)
        assertEquals(sampleBudget.isActive, reconstructed.isActive)
        assertEquals(sampleBudget.updatedAt, reconstructed.updatedAt)
    }

    @Test
    fun testBudgetDocumentToMapAndFromMap() {
        val doc = sampleBudget.toFirestoreDocument(isDeleted = false)
        val map = doc.toMap()

        assertEquals("budget_777", map["id"])
        assertEquals(500.0, (map["amount"] as Number).toDouble(), 0.001)
        assertEquals("MONTHLY", map["period"])
        assertEquals(true, map["isActive"])

        val parsed = FirestoreBudgetDocument.fromMap(map)
        assertEquals(doc.id, parsed.id)
        assertEquals(doc.amount, parsed.amount, 0.001)
        assertTrue(parsed.isActive)
    }

    @Test
    fun testBudgetDocumentFromMapHandlesMalformedData() {
        val malformedMap = mapOf<String, Any?>(
            "id" to "budget_bad",
            "amount" to "invalid_amount",
            "period" to "WEEKLY",
            "isActive" to "TRUE"
        )

        val doc = FirestoreBudgetDocument.fromMap(malformedMap, fallbackId = "b_fb", fallbackUserId = "u_fb")
        val budget = doc.toBudget()
        assertEquals(0.0, budget.amount, 0.001)
        assertEquals("WEEKLY", budget.period)
        assertTrue(budget.isActive)
    }

    // ==========================================
    // 4. EXPENSE DTO & SERIALIZATION TESTS
    // ==========================================

    @Test
    fun testExpenseToFirestoreDocumentRoundTrip() {
        val doc = sampleExpense.toFirestoreDocument(isDeleted = false)
        val reconstructed = doc.toFoodExpense()

        assertEquals(sampleExpense.id, reconstructed.id)
        assertEquals(sampleExpense.userId, reconstructed.userId)
        assertEquals(sampleExpense.name, reconstructed.name)
        assertEquals(sampleExpense.amount, reconstructed.amount, 0.001)
        assertEquals(sampleExpense.category, reconstructed.category)
        assertEquals(sampleExpense.date, reconstructed.date)
        assertEquals(sampleExpense.notes, reconstructed.notes)
        assertEquals(sampleExpense.groceryItemId, reconstructed.groceryItemId)
        assertEquals(sampleExpense.createdAt, reconstructed.createdAt)
        assertEquals(sampleExpense.updatedAt, reconstructed.updatedAt)
    }

    @Test
    fun testExpenseDocumentToMapAndFromMap() {
        val doc = sampleExpense.toFirestoreDocument(isDeleted = false)
        val map = doc.toMap()

        assertEquals("exp_888", map["id"])
        assertEquals(45.50, (map["amount"] as Number).toDouble(), 0.001)
        assertEquals("Groceries", map["category"])

        val parsed = FirestoreExpenseDocument.fromMap(map)
        assertEquals(doc.id, parsed.id)
        assertEquals(doc.amount, parsed.amount, 0.001)
    }

    @Test
    fun testExpenseDocumentFromMapHandlesMalformedData() {
        val malformedMap = mapOf<String, Any?>(
            "id" to "exp_bad",
            "amount" to "not_a_double",
            "category" to "INVALID_CAT"
        )

        val doc = FirestoreExpenseDocument.fromMap(malformedMap, fallbackId = "e_fb", fallbackUserId = "u_fb")
        val expense = doc.toFoodExpense()
        assertEquals(0.0, expense.amount, 0.001)
        assertEquals("INVALID_CAT", expense.category)
    }

    // ==========================================
    // 5. CONFLICT RESOLUTION TESTS
    // ==========================================

    @Test
    fun testConflictResolutionLocalNewer() {
        val local = sampleRecipe.copy(updatedAt = 2000L)
        val cloud = sampleRecipe.copy(updatedAt = 1000L)
        val resolution = conflictResolver.resolve(
            localItem = local,
            cloudItem = cloud,
            getLocalTimestamp = { it.updatedAt },
            getCloudTimestamp = { it.updatedAt }
        )
        assertTrue(resolution is SyncResolution.UploadToCloud)
        assertEquals(local, (resolution as SyncResolution.UploadToCloud).localItem)
    }

    @Test
    fun testConflictResolutionCloudNewer() {
        val local = sampleRecipe.copy(updatedAt = 1000L)
        val cloud = sampleRecipe.copy(updatedAt = 3000L)
        val resolution = conflictResolver.resolve(
            localItem = local,
            cloudItem = cloud,
            getLocalTimestamp = { it.updatedAt },
            getCloudTimestamp = { it.updatedAt }
        )
        assertTrue(resolution is SyncResolution.DownloadToLocal)
        assertEquals(cloud, (resolution as SyncResolution.DownloadToLocal).cloudItem)
    }

    @Test
    fun testConflictResolutionEqual() {
        val local = sampleRecipe.copy(updatedAt = 2000L)
        val cloud = sampleRecipe.copy(updatedAt = 2000L)
        val resolution = conflictResolver.resolve(
            localItem = local,
            cloudItem = cloud,
            getLocalTimestamp = { it.updatedAt },
            getCloudTimestamp = { it.updatedAt }
        )
        assertTrue(resolution is SyncResolution.UpToDate)
    }

    @Test
    fun testConflictResolutionMissingCloud() {
        val local = sampleRecipe
        val resolution = conflictResolver.resolve(
            localItem = local,
            cloudItem = null,
            getLocalTimestamp = { it.updatedAt },
            getCloudTimestamp = { 0L }
        )
        assertTrue(resolution is SyncResolution.UploadToCloud)
    }

    @Test
    fun testConflictResolutionMissingLocal() {
        val cloud = sampleRecipe
        val resolution = conflictResolver.resolve(
            localItem = null,
            cloudItem = cloud,
            getLocalTimestamp = { 0L },
            getCloudTimestamp = { it.updatedAt }
        )
        assertTrue(resolution is SyncResolution.DownloadToLocal)
    }

    // ==========================================
    // 6. SEED RECIPES DISCRIMINATION
    // ==========================================

    @Test
    fun testSeedRecipeIdIdentification() {
        val seedRecipeId = "seed_recipe_oatmeal"
        val customRecipeId = "user_rec_uuid_123"

        assertTrue(seedRecipeId.startsWith("seed_recipe_"))
        assertFalse(customRecipeId.startsWith("seed_recipe_"))
    }
}
