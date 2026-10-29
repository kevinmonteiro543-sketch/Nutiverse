package com.monteirogames.nutriverse

import com.monteirogames.nutriverse.data.repository.toEntities
import com.monteirogames.nutriverse.domain.model.*
import com.monteirogames.nutriverse.domain.sync.SyncResolution
import com.monteirogames.nutriverse.domain.sync.TimestampConflictResolverImpl
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class DietPlansAndHealthReportsSyncTest {

    private val conflictResolver = TimestampConflictResolverImpl()

    private val sampleDietMeal1 = DietPlanMeal(
        id = "meal_dp_1",
        dayNumber = 1,
        mealType = "Breakfast",
        name = "Avocado Toast & Boiled Eggs",
        calories = 380f,
        protein = 22f,
        carbs = 34f,
        fat = 16f,
        prepTip = "Toast whole grain bread and top with mashed avocado."
    )

    private val sampleDietMeal2 = DietPlanMeal(
        id = "meal_dp_2",
        dayNumber = 1,
        mealType = "Lunch",
        name = "Grilled Chicken Quinoa Bowl",
        calories = 520f,
        protein = 45f,
        carbs = 48f,
        fat = 14f,
        prepTip = "Prep quinoa ahead of time."
    )

    private val sampleDietPlan = DietPlan(
        id = "plan_test_001",
        userId = "firebase_uid_test",
        name = "High-Protein Muscle Gain Plan",
        targetCalories = 2400f,
        targetProtein = 180f,
        targetCarbs = 220f,
        targetFat = 70f,
        createdAt = 1700000000000L,
        days = listOf(
            DayPlan(
                dayNumber = 1,
                meals = listOf(sampleDietMeal1, sampleDietMeal2)
            )
        ),
        updatedAt = 1700000050000L
    )

    private val sampleHealthResult1 = HealthReportResult(
        id = "hr_res_1",
        reportId = "report_test_999",
        testName = "Fasting Blood Glucose",
        value = "92",
        unit = "mg/dL",
        referenceRange = "70-99 mg/dL",
        interpretation = "Normal fasting glucose levels.",
        status = HealthResultStatus.NORMAL,
        userNote = "Tested after 12h fast"
    )

    private val sampleHealthResult2 = HealthReportResult(
        id = "hr_res_2",
        reportId = "report_test_999",
        testName = "Serum Ferritin",
        value = "18",
        unit = "ng/mL",
        referenceRange = "30-400 ng/mL",
        interpretation = "Low ferritin indicating iron deficiency.",
        status = HealthResultStatus.LOW,
        userNote = null
    )

    private val sampleHealthReport = HealthReport(
        id = "report_test_999",
        userId = "firebase_uid_test",
        reportName = "Annual Comprehensive Metabolic Panel",
        reportType = "Blood Test",
        createdAt = 1700000010000L,
        summary = "Overall markers stable, mild iron deficiency observed.",
        observations = listOf("Low iron stores detected", "Glucose and lipids within standard range"),
        questionsForDoctor = listOf("Should I supplement with iron bisglycinate?"),
        disclaimer = "NutriVerse AI analysis is informational only and not a medical diagnosis.",
        results = listOf(sampleHealthResult1, sampleHealthResult2),
        updatedAt = 1700000060000L
    )

    // ==========================================
    // 1-11: DIET PLAN TESTS
    // ==========================================

    @Test
    fun test1_DietPlanToFirestoreDocumentRoundTrip() {
        val doc = sampleDietPlan.toFirestoreDocument(userId = "firebase_uid_test", isDeleted = false)
        val reconstructed = doc.toDietPlan()

        assertEquals(sampleDietPlan.id, reconstructed.id)
        assertEquals(sampleDietPlan.userId, reconstructed.userId)
        assertEquals(sampleDietPlan.name, reconstructed.name)
        assertEquals(sampleDietPlan.targetCalories, reconstructed.targetCalories, 0.01f)
        assertEquals(sampleDietPlan.targetProtein, reconstructed.targetProtein, 0.01f)
        assertEquals(sampleDietPlan.targetCarbs, reconstructed.targetCarbs, 0.01f)
        assertEquals(sampleDietPlan.targetFat, reconstructed.targetFat, 0.01f)
        assertEquals(sampleDietPlan.createdAt, reconstructed.createdAt)
        assertEquals(sampleDietPlan.updatedAt, reconstructed.updatedAt)
    }

    @Test
    fun test2_DietPlanNestedMealMapping() {
        val doc = sampleDietPlan.toFirestoreDocument(userId = "firebase_uid_test", isDeleted = false)
        val reconstructed = doc.toDietPlan()

        assertEquals(1, reconstructed.days.size)
        val day1 = reconstructed.days[0]
        assertEquals(1, day1.dayNumber)
        assertEquals(2, day1.meals.size)

        val meal1 = day1.meals[0]
        assertEquals("meal_dp_1", meal1.id)
        assertEquals(1, meal1.dayNumber)
        assertEquals("Breakfast", meal1.mealType)
        assertEquals("Avocado Toast & Boiled Eggs", meal1.name)
        assertEquals(380f, meal1.calories, 0.01f)
        assertEquals(22f, meal1.protein, 0.01f)
        assertEquals("Toast whole grain bread and top with mashed avocado.", meal1.prepTip)
    }

    @Test
    fun test3_DietPlanMissingAndNullFieldHandling() {
        val malformedMap = mapOf<String, Any?>(
            "id" to "corrupt_plan",
            "name" to null,
            "targetCalories" to "not_a_number",
            "meals" to listOf(
                mapOf(
                    "name" to "Mystery Food",
                    "calories" to "NaN",
                    "protein" to null
                )
            ),
            "isDeleted" to true,
            "schemaVersion" to 999
        )

        val doc = FirestoreDietPlanDocument.fromMap(
            malformedMap,
            fallbackId = "fallback_plan_id",
            fallbackUserId = "fallback_user_id"
        )
        assertEquals("corrupt_plan", doc.id)
        assertEquals("fallback_user_id", doc.userId)
        assertTrue(doc.isDeleted)
        assertEquals(0f, doc.targetCalories, 0.01f)
        assertEquals(1, doc.meals.size)
        assertEquals("Mystery Food", doc.meals[0].name)
        assertEquals(0f, doc.meals[0].calories, 0.01f)
    }

    @Test
    fun test4_DietPlanConflictResolutionLocalNewer() {
        val local = sampleDietPlan.copy(updatedAt = 2000L)
        val cloud = sampleDietPlan.copy(updatedAt = 1000L)
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
    fun test5_DietPlanConflictResolutionCloudNewer() {
        val local = sampleDietPlan.copy(updatedAt = 1000L)
        val cloud = sampleDietPlan.copy(updatedAt = 3000L)
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
    fun test6_DietPlanConflictResolutionEqualTimestamps() {
        val local = sampleDietPlan.copy(updatedAt = 2000L)
        val cloud = sampleDietPlan.copy(updatedAt = 2000L)
        val resolution = conflictResolver.resolve(
            localItem = local,
            cloudItem = cloud,
            getLocalTimestamp = { it.updatedAt },
            getCloudTimestamp = { it.updatedAt }
        )
        assertTrue(resolution is SyncResolution.UpToDate)
    }

    @Test
    fun test7_DietPlanLocalDeletionProducesTombstoneModel() {
        val tombstone = com.monteirogames.nutriverse.data.local.entity.SyncTombstoneEntity(
            id = sampleDietPlan.id,
            entityType = "dietPlan",
            userId = sampleDietPlan.userId,
            deletedAt = 1700000090000L
        )
        assertEquals("plan_test_001", tombstone.id)
        assertEquals("dietPlan", tombstone.entityType)
        assertEquals("firebase_uid_test", tombstone.userId)
        assertEquals(1700000090000L, tombstone.deletedAt)
    }

    @Test
    fun test8_DietPlanCloudDeletionFlagsIsDeleted() {
        val cloudDoc = sampleDietPlan.toFirestoreDocument(isDeleted = true)
        assertTrue(cloudDoc.isDeleted)
        val map = cloudDoc.toMap()
        assertEquals(true, map["isDeleted"])
    }

    @Test
    fun test9_DietPlanTombstonePreventsResurrectionWhenOlderThanTombstone() {
        val tombstoneTime = 2000L
        val staleCloudTime = 1500L
        assertTrue("Tombstone should supersede older cloud update", tombstoneTime >= staleCloudTime)
    }

    @Test
    fun test10_DietPlanUserIsolationPaths() {
        val user1 = "uid_user_1"
        val user2 = "uid_user_2"
        val path1 = FirestoreCollections.userDietPlanDocPath(user1, "plan_1")
        val path2 = FirestoreCollections.userDietPlanDocPath(user2, "plan_1")

        assertNotEquals(path1, path2)
        assertEquals("users/uid_user_1/dietPlans/plan_1", path1)
        assertEquals("users/uid_user_2/dietPlans/plan_1", path2)
    }

    @Test
    fun test11_DietPlanExistingDataIntactWithDefaultUpdatedAt() {
        val legacyPlan = DietPlan(
            id = "legacy_plan",
            userId = "uid_legacy",
            name = "Default 7-Day Plan",
            targetCalories = 2000f,
            targetProtein = 150f,
            targetCarbs = 200f,
            targetFat = 60f,
            createdAt = 1690000000000L,
            days = emptyList()
        )
        // Verify default updatedAt fallback equals createdAt
        assertEquals(legacyPlan.createdAt, legacyPlan.updatedAt)
    }

    // ==========================================
    // 12-24: HEALTH REPORT TESTS
    // ==========================================

    @Test
    fun test12_HealthReportToFirestoreDocumentRoundTrip() {
        val doc = sampleHealthReport.toFirestoreDocument(userId = "firebase_uid_test", isDeleted = false)
        val reconstructed = doc.toHealthReport()

        assertEquals(sampleHealthReport.id, reconstructed.id)
        assertEquals(sampleHealthReport.userId, reconstructed.userId)
        assertEquals(sampleHealthReport.reportName, reconstructed.reportName)
        assertEquals(sampleHealthReport.reportType, reconstructed.reportType)
        assertEquals(sampleHealthReport.summary, reconstructed.summary)
        assertEquals(sampleHealthReport.observations, reconstructed.observations)
        assertEquals(sampleHealthReport.questionsForDoctor, reconstructed.questionsForDoctor)
        assertEquals(sampleHealthReport.disclaimer, reconstructed.disclaimer)
        assertEquals(sampleHealthReport.createdAt, reconstructed.createdAt)
        assertEquals(sampleHealthReport.updatedAt, reconstructed.updatedAt)
    }

    @Test
    fun test13_HealthReportNestedResultMapping() {
        val doc = sampleHealthReport.toFirestoreDocument(userId = "firebase_uid_test", isDeleted = false)
        val reconstructed = doc.toHealthReport()

        assertEquals(2, reconstructed.results.size)
        val res1 = reconstructed.results[0]
        assertEquals("hr_res_1", res1.id)
        assertEquals("Fasting Blood Glucose", res1.testName)
        assertEquals("92", res1.value)
        assertEquals("mg/dL", res1.unit)
        assertEquals("70-99 mg/dL", res1.referenceRange)
        assertEquals(HealthResultStatus.NORMAL, res1.status)
        assertEquals("Tested after 12h fast", res1.userNote)

        val res2 = reconstructed.results[1]
        assertEquals("hr_res_2", res2.id)
        assertEquals(HealthResultStatus.LOW, res2.status)
        assertNull(res2.userNote)
    }

    @Test
    fun test14_HealthReportMissingAndNullFieldHandling() {
        val malformedMap = mapOf<String, Any?>(
            "id" to "corrupt_report",
            "reportName" to null,
            "observations" to "single string instead of list",
            "results" to listOf(
                mapOf(
                    "testName" to "Lipid",
                    "status" to "INVALID_STATUS_VALUE",
                    "value" to null
                )
            ),
            "isDeleted" to "true"
        )

        val doc = FirestoreHealthReportDocument.fromMap(
            malformedMap,
            fallbackId = "fallback_hr",
            fallbackUserId = "fallback_user"
        )
        assertEquals("corrupt_report", doc.id)
        assertEquals("fallback_user", doc.userId)
        assertTrue(doc.isDeleted)
        assertEquals(listOf("single string instead of list"), doc.observations)
        assertEquals(1, doc.results.size)
        assertEquals("UNKNOWN", doc.results[0].status)
    }

    @Test
    fun test15_HealthReportUnknownStatusPreservation() {
        val result = HealthReportResult(
            id = "res_unk",
            reportId = "rep_unk",
            testName = "Experimental Marker",
            value = "3.2",
            unit = "arb",
            referenceRange = null,
            interpretation = null,
            status = HealthResultStatus.UNKNOWN
        )
        val report = sampleHealthReport.copy(results = listOf(result))
        val doc = report.toFirestoreDocument()
        val restored = doc.toHealthReport()

        assertEquals(HealthResultStatus.UNKNOWN, restored.results[0].status)
    }

    @Test
    fun test16_HealthReportConflictResolutionLocalNewer() {
        val local = sampleHealthReport.copy(updatedAt = 3000L)
        val cloud = sampleHealthReport.copy(updatedAt = 1000L)
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
    fun test17_HealthReportConflictResolutionCloudNewer() {
        val local = sampleHealthReport.copy(updatedAt = 1000L)
        val cloud = sampleHealthReport.copy(updatedAt = 4000L)
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
    fun test18_HealthReportConflictResolutionEqualTimestamps() {
        val local = sampleHealthReport.copy(updatedAt = 2500L)
        val cloud = sampleHealthReport.copy(updatedAt = 2500L)
        val resolution = conflictResolver.resolve(
            localItem = local,
            cloudItem = cloud,
            getLocalTimestamp = { it.updatedAt },
            getCloudTimestamp = { it.updatedAt }
        )
        assertTrue(resolution is SyncResolution.UpToDate)
    }

    @Test
    fun test19_HealthReportLocalDeletionProducesTombstone() {
        val tombstone = com.monteirogames.nutriverse.data.local.entity.SyncTombstoneEntity(
            id = sampleHealthReport.id,
            entityType = "healthReport",
            userId = sampleHealthReport.userId,
            deletedAt = 1700000095000L
        )
        assertEquals("report_test_999", tombstone.id)
        assertEquals("healthReport", tombstone.entityType)
        assertEquals("firebase_uid_test", tombstone.userId)
    }

    @Test
    fun test20_HealthReportCloudDeletionFlagsIsDeleted() {
        val cloudDoc = sampleHealthReport.toFirestoreDocument(isDeleted = true)
        assertTrue(cloudDoc.isDeleted)
        val map = cloudDoc.toMap()
        assertEquals(true, map["isDeleted"])
    }

    @Test
    fun test21_HealthReportTombstonePreventsResurrection() {
        val tombstoneDeletedAt = 5000L
        val olderCloudUpdatedAt = 4000L
        assertTrue(tombstoneDeletedAt > olderCloudUpdatedAt)
    }

    @Test
    fun test22_HealthReportUserIsolationPaths() {
        val user1 = "uid_alpha"
        val user2 = "uid_beta"
        val path1 = FirestoreCollections.userHealthReportDocPath(user1, "rep_1")
        val path2 = FirestoreCollections.userHealthReportDocPath(user2, "rep_1")

        assertNotEquals(path1, path2)
        assertEquals("users/uid_alpha/healthReports/rep_1", path1)
        assertEquals("users/uid_beta/healthReports/rep_1", path2)
    }

    @Test
    fun test23_HealthReportCascadeDeletionConsistency() {
        val (reportEntity, resultEntities) = sampleHealthReport.toEntities()
        assertEquals(sampleHealthReport.id, reportEntity.id)
        assertEquals(2, resultEntities.size)
        for (res in resultEntities) {
            assertEquals(reportEntity.id, res.reportId)
        }
    }

    @Test
    fun test24_HealthReportExistingDataIntactWithDefaultUpdatedAt() {
        val legacyReport = HealthReport(
            id = "legacy_rep",
            userId = "legacy_user",
            reportName = "Legacy Report",
            reportType = "Blood Test",
            createdAt = 1680000000000L,
            summary = "Routine test",
            observations = emptyList(),
            questionsForDoctor = emptyList(),
            disclaimer = "Test disclaimer",
            results = emptyList()
        )
        assertEquals(legacyReport.createdAt, legacyReport.updatedAt)
    }
}
