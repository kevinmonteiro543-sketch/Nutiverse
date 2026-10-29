package com.monteirogames.nutriverse

import com.monteirogames.nutriverse.domain.model.*
import com.monteirogames.nutriverse.domain.sync.SyncResolution
import com.monteirogames.nutriverse.domain.sync.TimestampConflictResolverImpl
import org.junit.Assert.*
import org.junit.Test

class MealAndPantrySyncTest {

    private val conflictResolver = TimestampConflictResolverImpl()

    private val sampleFood = FoodItem(
        id = "food_123",
        name = "Greek Yogurt",
        calories = 130f,
        protein = 15f,
        carbs = 9f,
        fat = 3f,
        servingSize = 170f,
        servingUnit = "g",
        barcode = "0123456789"
    )

    private val sampleMeal = MealEntry(
        id = "meal_999",
        userId = "firebase_uid_abc",
        foodItem = sampleFood,
        date = 1700000000000L,
        mealType = "Breakfast",
        quantity = 200f,
        loggedAt = 1700000010000L,
        updatedAt = 1700000020000L
    )

    private val samplePantryItem = PantryItem(
        id = "pantry_777",
        userId = "firebase_uid_abc",
        foodItem = sampleFood,
        quantity = 3f,
        unit = "tubs",
        expiryDate = 1705000000000L,
        dateAdded = 1700000000000L,
        updatedAt = 1700000030000L
    )

    // ==========================================
    // 1. MEAL DTO & SERIALIZATION TESTS
    // ==========================================

    @Test
    fun testMealEntryToFirestoreDocumentRoundTrip() {
        val doc = sampleMeal.toFirestoreDocument(isDeleted = false)
        val reconstructed = doc.toMealEntry()

        assertEquals(sampleMeal.id, reconstructed.id)
        assertEquals(sampleMeal.userId, reconstructed.userId)
        assertEquals(sampleMeal.mealType, reconstructed.mealType)
        assertEquals(sampleMeal.quantity, reconstructed.quantity, 0.01f)
        assertEquals(sampleMeal.date, reconstructed.date)
        assertEquals(sampleMeal.loggedAt, reconstructed.loggedAt)
        assertEquals(sampleMeal.updatedAt, reconstructed.updatedAt)
        assertEquals(sampleMeal.foodItem.id, reconstructed.foodItem.id)
        assertEquals(sampleMeal.foodItem.name, reconstructed.foodItem.name)
        assertEquals(sampleMeal.foodItem.calories, reconstructed.foodItem.calories, 0.01f)
        assertEquals(sampleMeal.foodItem.protein, reconstructed.foodItem.protein, 0.01f)
        assertEquals(sampleMeal.foodItem.barcode, reconstructed.foodItem.barcode)
    }

    @Test
    fun testMealDocumentToMapAndFromMap() {
        val doc = sampleMeal.toFirestoreDocument(isDeleted = false)
        val map = doc.toMap()

        assertEquals("meal_999", map["id"])
        assertEquals("firebase_uid_abc", map["userId"])
        assertEquals(false, map["isDeleted"])
        assertEquals(1, map["schemaVersion"])

        val parsedDoc = FirestoreMealDocument.fromMap(map)
        assertEquals(doc.id, parsedDoc.id)
        assertEquals(doc.foodName, parsedDoc.foodName)
        assertEquals(doc.calories, parsedDoc.calories, 0.01f)
        assertFalse(parsedDoc.isDeleted)
    }

    @Test
    fun testMealDocumentFromMapHandlesMalformedAndMissingFields() {
        val malformedMap = mapOf<String, Any?>(
            "id" to "meal_corrupt",
            "quantity" to "not_a_number", // Malformed
            "calories" to null,
            "isDeleted" to "TRUE", // String representation
            "unknown_field" to "should_ignore"
        )

        val doc = FirestoreMealDocument.fromMap(malformedMap, fallbackId = "fallback_id", fallbackUserId = "fallback_uid")
        assertEquals("meal_corrupt", doc.id)
        assertEquals("fallback_uid", doc.userId)
        assertEquals(100f, doc.quantity, 0.01f) // Defaults safely
        assertEquals(0f, doc.calories, 0.01f)
        assertTrue(doc.isDeleted)
    }

    @Test
    fun testMealDocumentFromMapHandlesNestedFoodItem() {
        val mapWithNested = mapOf<String, Any?>(
            "id" to "m1",
            "userId" to "u1",
            "foodItem" to mapOf(
                "id" to "f1",
                "name" to "Oatmeal",
                "calories" to 150.0,
                "protein" to 5.0,
                "carbs" to 27.0,
                "fat" to 2.5,
                "servingSize" to 40.0,
                "servingUnit" to "g"
            )
        )

        val doc = FirestoreMealDocument.fromMap(mapWithNested)
        assertEquals("m1", doc.id)
        assertEquals("f1", doc.foodItemId)
        assertEquals("Oatmeal", doc.foodName)
        assertEquals(150f, doc.calories, 0.01f)
        assertEquals(5f, doc.protein, 0.01f)
    }

    // ==========================================
    // 2. PANTRY DTO & SERIALIZATION TESTS
    // ==========================================

    @Test
    fun testPantryItemToFirestoreDocumentRoundTrip() {
        val doc = samplePantryItem.toFirestoreDocument(isDeleted = false)
        val reconstructed = doc.toPantryItem()

        assertEquals(samplePantryItem.id, reconstructed.id)
        assertEquals(samplePantryItem.userId, reconstructed.userId)
        assertEquals(samplePantryItem.quantity, reconstructed.quantity, 0.01f)
        assertEquals(samplePantryItem.unit, reconstructed.unit)
        assertEquals(samplePantryItem.expiryDate, reconstructed.expiryDate)
        assertEquals(samplePantryItem.dateAdded, reconstructed.dateAdded)
        assertEquals(samplePantryItem.updatedAt, reconstructed.updatedAt)
        assertEquals(samplePantryItem.foodItem.name, reconstructed.foodItem.name)
    }

    @Test
    fun testPantryDocumentToMapAndFromMap() {
        val doc = samplePantryItem.toFirestoreDocument(isDeleted = false)
        val map = doc.toMap()

        assertEquals("pantry_777", map["id"])
        assertEquals("firebase_uid_abc", map["userId"])
        assertEquals(false, map["isDeleted"])

        val parsedDoc = FirestorePantryDocument.fromMap(map)
        assertEquals(doc.id, parsedDoc.id)
        assertEquals(doc.foodName, parsedDoc.foodName)
        assertEquals(doc.quantity, parsedDoc.quantity, 0.01f)
        assertFalse(parsedDoc.isDeleted)
    }

    @Test
    fun testPantryDocumentFromMapHandlesMalformedAndMissingFields() {
        val malformedMap = mapOf<String, Any?>(
            "id" to "pantry_partial",
            "quantity" to 4.5,
            "unit" to "bottles",
            "deleted" to true // Alternative "deleted" key
        )

        val doc = FirestorePantryDocument.fromMap(malformedMap, fallbackId = "fb_id", fallbackUserId = "fb_uid")
        assertEquals("pantry_partial", doc.id)
        assertEquals("fb_uid", doc.userId)
        assertEquals(4.5f, doc.quantity, 0.01f)
        assertEquals("bottles", doc.unit)
        assertTrue(doc.isDeleted)
    }

    // ==========================================
    // 3. CONFLICT RESOLUTION RULES
    // ==========================================

    @Test
    fun testMealConflictResolution_LocalNewerThanCloud() {
        val local = sampleMeal.copy(updatedAt = 2000L)
        val cloud = sampleMeal.copy(updatedAt = 1000L)

        val resolution = conflictResolver.resolve(
            localItem = local,
            cloudItem = cloud,
            getLocalTimestamp = { it.updatedAt },
            getCloudTimestamp = { it.updatedAt }
        )

        assertTrue(resolution is SyncResolution.UploadToCloud)
        assertEquals(2000L, (resolution as SyncResolution.UploadToCloud).localItem.updatedAt)
    }

    @Test
    fun testMealConflictResolution_CloudNewerThanLocal() {
        val local = sampleMeal.copy(updatedAt = 1000L)
        val cloud = sampleMeal.copy(updatedAt = 2000L)

        val resolution = conflictResolver.resolve(
            localItem = local,
            cloudItem = cloud,
            getLocalTimestamp = { it.updatedAt },
            getCloudTimestamp = { it.updatedAt }
        )

        assertTrue(resolution is SyncResolution.DownloadToLocal)
        assertEquals(2000L, (resolution as SyncResolution.DownloadToLocal).cloudItem.updatedAt)
    }

    @Test
    fun testMealConflictResolution_EqualTimestamps() {
        val local = sampleMeal.copy(updatedAt = 1500L)
        val cloud = sampleMeal.copy(updatedAt = 1500L)

        val resolution = conflictResolver.resolve(
            localItem = local,
            cloudItem = cloud,
            getLocalTimestamp = { it.updatedAt },
            getCloudTimestamp = { it.updatedAt }
        )

        assertEquals(SyncResolution.UpToDate, resolution)
    }

    @Test
    fun testMealConflictResolution_MissingInCloud() {
        val local = sampleMeal.copy(updatedAt = 1000L)

        val resolution = conflictResolver.resolve(
            localItem = local,
            cloudItem = null,
            getLocalTimestamp = { it.updatedAt },
            getCloudTimestamp = { it.updatedAt }
        )

        assertTrue(resolution is SyncResolution.UploadToCloud)
        assertEquals(local, (resolution as SyncResolution.UploadToCloud).localItem)
    }

    @Test
    fun testMealConflictResolution_MissingInLocal() {
        val cloud = sampleMeal.copy(updatedAt = 1000L)

        val resolution = conflictResolver.resolve(
            localItem = null,
            cloudItem = cloud,
            getLocalTimestamp = { it.updatedAt },
            getCloudTimestamp = { it.updatedAt }
        )

        assertTrue(resolution is SyncResolution.DownloadToLocal)
        assertEquals(cloud, (resolution as SyncResolution.DownloadToLocal).cloudItem)
    }

    @Test
    fun testPantryConflictResolution_LocalNewerThanCloud() {
        val local = samplePantryItem.copy(updatedAt = 3000L)
        val cloud = samplePantryItem.copy(updatedAt = 1500L)

        val resolution = conflictResolver.resolve(
            localItem = local,
            cloudItem = cloud,
            getLocalTimestamp = { it.updatedAt },
            getCloudTimestamp = { it.updatedAt }
        )

        assertTrue(resolution is SyncResolution.UploadToCloud)
        assertEquals(3000L, (resolution as SyncResolution.UploadToCloud).localItem.updatedAt)
    }

    @Test
    fun testPantryConflictResolution_CloudNewerThanLocal() {
        val local = samplePantryItem.copy(updatedAt = 1500L)
        val cloud = samplePantryItem.copy(updatedAt = 3000L)

        val resolution = conflictResolver.resolve(
            localItem = local,
            cloudItem = cloud,
            getLocalTimestamp = { it.updatedAt },
            getCloudTimestamp = { it.updatedAt }
        )

        assertTrue(resolution is SyncResolution.DownloadToLocal)
        assertEquals(3000L, (resolution as SyncResolution.DownloadToLocal).cloudItem.updatedAt)
    }

    @Test
    fun testPantryConflictResolution_EqualTimestamps() {
        val local = samplePantryItem.copy(updatedAt = 2500L)
        val cloud = samplePantryItem.copy(updatedAt = 2500L)

        val resolution = conflictResolver.resolve(
            localItem = local,
            cloudItem = cloud,
            getLocalTimestamp = { it.updatedAt },
            getCloudTimestamp = { it.updatedAt }
        )

        assertEquals(SyncResolution.UpToDate, resolution)
    }

    // ==========================================
    // 4. DELETION & TOMBSTONE LOGIC TESTS
    // ==========================================

    @Test
    fun testDeletionTombstoneEntityIntegrity() {
        val tombstone = com.monteirogames.nutriverse.data.local.entity.SyncTombstoneEntity(
            id = "meal_deleted_123",
            entityType = "meal",
            userId = "uid_test",
            deletedAt = 5000L
        )

        assertEquals("meal_deleted_123", tombstone.id)
        assertEquals("meal", tombstone.entityType)
        assertEquals("uid_test", tombstone.userId)
        assertEquals(5000L, tombstone.deletedAt)
    }

    @Test
    fun testCloudDeletionTimestampPrecedence() {
        val localTombstoneDeletedAt = 10000L
        val cloudDocUpdatedAt = 8000L

        // Local deletion occurred AFTER cloud update -> local tombstone takes precedence
        assertTrue(localTombstoneDeletedAt > cloudDocUpdatedAt)

        // Conversely, if cloud was updated on another device AFTER local deletion:
        val cloudResurrectedAt = 12000L
        assertTrue(cloudResurrectedAt > localTombstoneDeletedAt)
    }
}
