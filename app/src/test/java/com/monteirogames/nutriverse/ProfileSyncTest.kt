package com.monteirogames.nutriverse

import com.monteirogames.nutriverse.domain.model.*
import com.monteirogames.nutriverse.domain.repository.AuthRepository
import com.monteirogames.nutriverse.domain.repository.AuthResult
import com.monteirogames.nutriverse.domain.repository.ProfileSyncRepository
import com.monteirogames.nutriverse.domain.repository.UserProfileRepository
import com.monteirogames.nutriverse.domain.sync.SyncResolution
import com.monteirogames.nutriverse.domain.sync.TimestampConflictResolverImpl
import com.monteirogames.nutriverse.domain.usecase.CalculateNutritionTargetsUseCase
import com.monteirogames.nutriverse.domain.usecase.GetUserProfileUseCase
import com.monteirogames.nutriverse.domain.usecase.SaveUserProfileUseCase
import com.monteirogames.nutriverse.domain.usecase.UserProfileUseCases
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ProfileSyncTest {

    class FakeAuthRepository : AuthRepository {
        var authenticatedUid: String? = "test_firebase_uid"
        override val currentUserFlow: Flow<String?> = flowOf(authenticatedUid)

        override fun getCurrentUserId(): String? = authenticatedUid
        override fun getCurrentUser(): AuthenticatedUser? =
            authenticatedUid?.let { AuthenticatedUser(uid = it, email = "user@test.com") }

        override fun observeAuthState(): Flow<AuthenticatedUser?> = flowOf(getCurrentUser())

        override suspend fun signIn(email: String, password: String): AuthResult<String> {
            authenticatedUid = "test_firebase_uid"
            return AuthResult.Success("test_firebase_uid")
        }

        override suspend fun signUp(email: String, password: String): AuthResult<String> {
            authenticatedUid = "test_firebase_uid"
            return AuthResult.Success("test_firebase_uid")
        }

        override suspend fun sendPasswordResetEmail(email: String): AuthResult<Unit> = AuthResult.Success(Unit)

        override suspend fun signOut() {
            authenticatedUid = null
        }
    }

    class FakeUserProfileRepository : UserProfileRepository {
        val profiles = mutableMapOf<String, UserProfile>()

        override fun getUserProfile(userId: String): Flow<UserProfile?> =
            flowOf(profiles[userId])

        override suspend fun saveProfile(profile: UserProfile) {
            profiles[profile.userId] = profile
        }

        override suspend fun clearProfile() {
            profiles.clear()
        }
    }

    class FakeCloudStorage {
        val cloudProfiles = mutableMapOf<String, FirestoreProfileDocument>()
        var isNetworkAvailable: Boolean = true

        fun getDoc(uid: String): FirestoreProfileDocument? {
            if (!isNetworkAvailable) throw RuntimeException("Network unavailable")
            return cloudProfiles[uid]
        }

        fun setDoc(uid: String, doc: FirestoreProfileDocument) {
            if (!isNetworkAvailable) throw RuntimeException("Network unavailable")
            cloudProfiles[uid] = doc
        }
    }

    class TestableProfileSyncRepository(
        private val authRepository: AuthRepository,
        private val userProfileRepository: UserProfileRepository,
        private val cloudStorage: FakeCloudStorage,
        private val conflictResolver: TimestampConflictResolverImpl = TimestampConflictResolverImpl()
    ) : ProfileSyncRepository {

        private val _syncState = MutableStateFlow(SyncState(status = SyncStatus.IDLE))
        override val syncState: StateFlow<SyncState> = _syncState.asStateFlow()

        override fun getSyncState(): SyncState = _syncState.value

        override suspend fun syncProfile(): Result<UserProfile?> {
            val uid = authRepository.getCurrentUserId()
            if (uid.isNullOrBlank()) {
                _syncState.value = SyncState(status = SyncStatus.ERROR, errorMessage = "User is not authenticated")
                return Result.failure(IllegalStateException("User is not authenticated"))
            }

            _syncState.value = SyncState(status = SyncStatus.SYNCING)

            return try {
                val local = userProfileRepository.getUserProfile(uid).firstOrNull()
                val cloudDoc = cloudStorage.getDoc(uid)
                val cloud = cloudDoc?.toUserProfile()

                val resolution = conflictResolver.resolve(
                    localItem = local,
                    cloudItem = cloud,
                    getLocalTimestamp = { it.updatedAt },
                    getCloudTimestamp = { it.updatedAt }
                )

                val resolved: UserProfile? = when (resolution) {
                    is SyncResolution.UploadToCloud -> {
                        val toUpload = if (resolution.localItem.updatedAt == 0L) {
                            val updated = resolution.localItem.copy(updatedAt = 1000L)
                            userProfileRepository.saveProfile(updated)
                            updated
                        } else resolution.localItem
                        cloudStorage.setDoc(uid, toUpload.toFirestoreDocument())
                        toUpload
                    }
                    is SyncResolution.DownloadToLocal -> {
                        userProfileRepository.saveProfile(resolution.cloudItem)
                        resolution.cloudItem
                    }
                    is SyncResolution.UpToDate -> {
                        local
                    }
                }

                _syncState.value = SyncState(status = SyncStatus.SUCCESS, lastSyncedAt = System.currentTimeMillis())
                Result.success(resolved)
            } catch (e: Exception) {
                _syncState.value = SyncState(status = SyncStatus.ERROR, errorMessage = e.message)
                Result.failure(e)
            }
        }

        override suspend fun uploadProfile(profile: UserProfile): Result<Unit> {
            val uid = authRepository.getCurrentUserId()
            if (uid.isNullOrBlank()) return Result.failure(IllegalStateException("User is not authenticated"))
            if (profile.userId != uid) return Result.failure(SecurityException("Cannot upload profile for a different user identity"))

            return try {
                cloudStorage.setDoc(uid, profile.toFirestoreDocument())
                _syncState.value = SyncState(status = SyncStatus.SUCCESS, lastSyncedAt = System.currentTimeMillis())
                Result.success(Unit)
            } catch (e: Exception) {
                _syncState.value = SyncState(status = SyncStatus.ERROR, errorMessage = e.message)
                Result.failure(e)
            }
        }

        override suspend fun downloadProfile(): Result<UserProfile?> {
            val uid = authRepository.getCurrentUserId()
            if (uid.isNullOrBlank()) return Result.failure(IllegalStateException("User is not authenticated"))

            return try {
                val doc = cloudStorage.getDoc(uid)
                val profile = doc?.toUserProfile()
                if (profile != null) userProfileRepository.saveProfile(profile)
                Result.success(profile)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    private lateinit var fakeAuthRepo: FakeAuthRepository
    private lateinit var fakeProfileRepo: FakeUserProfileRepository
    private lateinit var fakeCloudStorage: FakeCloudStorage
    private lateinit var syncRepo: TestableProfileSyncRepository
    private lateinit var conflictResolver: TimestampConflictResolverImpl

    @Before
    fun setup() {
        fakeAuthRepo = FakeAuthRepository()
        fakeProfileRepo = FakeUserProfileRepository()
        fakeCloudStorage = FakeCloudStorage()
        conflictResolver = TimestampConflictResolverImpl()
        syncRepo = TestableProfileSyncRepository(fakeAuthRepo, fakeProfileRepo, fakeCloudStorage, conflictResolver)
    }

    @Test
    fun testConflictResolver_missingCloud_uploadsLocal() {
        val local = UserProfile(
            userId = "uid_1", name = "Alice", age = 30, gender = "Female",
            heightCm = 165f, weightKg = 60f, goal = "Maintain Weight",
            dietaryPreference = "None", allergies = "None", activityLevel = "Moderate",
            updatedAt = 100L
        )

        val resolution = conflictResolver.resolve(
            localItem = local,
            cloudItem = null,
            getLocalTimestamp = { it.updatedAt },
            getCloudTimestamp = { it.updatedAt }
        )

        assertTrue(resolution is SyncResolution.UploadToCloud)
        assertEquals("Alice", (resolution as SyncResolution.UploadToCloud).localItem.name)
    }

    @Test
    fun testConflictResolver_missingLocal_downloadsCloud() {
        val cloud = UserProfile(
            userId = "uid_1", name = "Bob", age = 28, gender = "Male",
            heightCm = 180f, weightKg = 75f, goal = "Lose Weight",
            dietaryPreference = "None", allergies = "None", activityLevel = "Active",
            updatedAt = 200L
        )

        val resolution = conflictResolver.resolve(
            localItem = null,
            cloudItem = cloud,
            getLocalTimestamp = { it.updatedAt },
            getCloudTimestamp = { it.updatedAt }
        )

        assertTrue(resolution is SyncResolution.DownloadToLocal)
        assertEquals("Bob", (resolution as SyncResolution.DownloadToLocal).cloudItem.name)
    }

    @Test
    fun testConflictResolver_newerLocalWins() {
        val local = UserProfile(
            userId = "uid_1", name = "Alice Updated", age = 30, gender = "Female",
            heightCm = 165f, weightKg = 59f, goal = "Maintain Weight",
            dietaryPreference = "None", allergies = "None", activityLevel = "Moderate",
            updatedAt = 500L
        )
        val cloud = UserProfile(
            userId = "uid_1", name = "Alice Old", age = 30, gender = "Female",
            heightCm = 165f, weightKg = 60f, goal = "Maintain Weight",
            dietaryPreference = "None", allergies = "None", activityLevel = "Moderate",
            updatedAt = 400L
        )

        val resolution = conflictResolver.resolve(
            localItem = local,
            cloudItem = cloud,
            getLocalTimestamp = { it.updatedAt },
            getCloudTimestamp = { it.updatedAt }
        )

        assertTrue(resolution is SyncResolution.UploadToCloud)
        assertEquals("Alice Updated", (resolution as SyncResolution.UploadToCloud).localItem.name)
    }

    @Test
    fun testConflictResolver_newerCloudWins() {
        val local = UserProfile(
            userId = "uid_1", name = "Local Old", age = 25, gender = "Other",
            heightCm = 170f, weightKg = 70f, goal = "Maintain Weight",
            dietaryPreference = "None", allergies = "None", activityLevel = "Light",
            updatedAt = 100L
        )
        val cloud = UserProfile(
            userId = "uid_1", name = "Cloud Newer", age = 25, gender = "Other",
            heightCm = 170f, weightKg = 68f, goal = "Maintain Weight",
            dietaryPreference = "None", allergies = "None", activityLevel = "Light",
            updatedAt = 200L
        )

        val resolution = conflictResolver.resolve(
            localItem = local,
            cloudItem = cloud,
            getLocalTimestamp = { it.updatedAt },
            getCloudTimestamp = { it.updatedAt }
        )

        assertTrue(resolution is SyncResolution.DownloadToLocal)
        assertEquals("Cloud Newer", (resolution as SyncResolution.DownloadToLocal).cloudItem.name)
    }

    @Test
    fun testConflictResolver_equalTimestamps_noChange() {
        val local = UserProfile(
            userId = "uid_1", name = "Same Version", age = 25, gender = "Other",
            heightCm = 170f, weightKg = 70f, goal = "Maintain Weight",
            dietaryPreference = "None", allergies = "None", activityLevel = "Light",
            updatedAt = 300L
        )
        val cloud = local.copy()

        val resolution = conflictResolver.resolve(
            localItem = local,
            cloudItem = cloud,
            getLocalTimestamp = { it.updatedAt },
            getCloudTimestamp = { it.updatedAt }
        )

        assertTrue(resolution is SyncResolution.UpToDate)
    }

    @Test
    fun testSyncProfile_unauthenticated_rejected() = runTest {
        fakeAuthRepo.authenticatedUid = null

        val result = syncRepo.syncProfile()
        assertTrue(result.isFailure)
        assertEquals(SyncStatus.ERROR, syncRepo.getSyncState().status)
    }

    @Test
    fun testUploadProfile_mismatchedUid_rejectedWithSecurityException() = runTest {
        fakeAuthRepo.authenticatedUid = "legit_user_uid"

        val intruderProfile = UserProfile(
            userId = "victim_user_uid", name = "Hacker", age = 20, gender = "Other",
            heightCm = 170f, weightKg = 70f, goal = "Gain Weight",
            dietaryPreference = "None", allergies = "None", activityLevel = "Sedentary"
        )

        val result = syncRepo.uploadProfile(intruderProfile)
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is SecurityException)
    }

    @Test
    fun testSyncProfile_fullFlow_missingCloud_uploadsToCloud() = runTest {
        val uid = "test_firebase_uid"
        val local = UserProfile(
            userId = uid, name = "Local Nutritionist", age = 35, gender = "Female",
            heightCm = 168f, weightKg = 62f, goal = "Maintain Weight",
            dietaryPreference = "Vegetarian", allergies = "Nuts", activityLevel = "Active",
            updatedAt = 1000L
        )
        fakeProfileRepo.saveProfile(local)

        val result = syncRepo.syncProfile()
        assertTrue(result.isSuccess)
        assertEquals(SyncStatus.SUCCESS, syncRepo.getSyncState().status)

        // Cloud should now have the document
        val cloudDoc = fakeCloudStorage.getDoc(uid)
        assertNotNull(cloudDoc)
        assertEquals("Local Nutritionist", cloudDoc?.name)
        assertEquals("Vegetarian", cloudDoc?.dietaryPreference)
    }

    @Test
    fun testSyncProfile_fullFlow_missingLocal_downloadsToRoom() = runTest {
        val uid = "test_firebase_uid"
        val cloudDoc = FirestoreProfileDocument(
            uid = uid, name = "Cloud Restored User", age = 40, gender = "Male",
            heightCm = 175f, weightKg = 80f, goal = "Lose Weight",
            dietaryPreference = "Keto", allergies = "None", activityLevel = "Sedentary",
            updatedAt = 2000L
        )
        fakeCloudStorage.setDoc(uid, cloudDoc)

        val result = syncRepo.syncProfile()
        assertTrue(result.isSuccess)
        assertEquals(SyncStatus.SUCCESS, syncRepo.getSyncState().status)

        // Room should now have the restored profile
        val localProfile = fakeProfileRepo.getUserProfile(uid).firstOrNull()
        assertNotNull(localProfile)
        assertEquals("Cloud Restored User", localProfile?.name)
        assertEquals("Keto", localProfile?.dietaryPreference)
    }

    @Test
    fun testOfflineEditing_remainsFunctionalWhenCloudFails() = runTest {
        fakeCloudStorage.isNetworkAvailable = false

        val local = UserProfile(
            userId = "test_firebase_uid", name = "Offline User", age = 22, gender = "Female",
            heightCm = 160f, weightKg = 55f, goal = "Maintain Weight",
            dietaryPreference = "Vegan", allergies = "None", activityLevel = "Moderate",
            updatedAt = 3000L
        )
        // Local saving in Room must succeed unconditionally
        fakeProfileRepo.saveProfile(local)

        val savedLocal = fakeProfileRepo.getUserProfile("test_firebase_uid").firstOrNull()
        assertNotNull(savedLocal)
        assertEquals("Offline User", savedLocal?.name)

        // Sync fails gracefully without crashing the app
        val syncResult = syncRepo.syncProfile()
        assertTrue(syncResult.isFailure)
        assertEquals(SyncStatus.ERROR, syncRepo.getSyncState().status)

        // Local data remains completely intact
        val intactLocal = fakeProfileRepo.getUserProfile("test_firebase_uid").firstOrNull()
        assertEquals("Offline User", intactLocal?.name)
    }

    @Test
    fun testNutritionCalculation_operatesIdenticallyOnSyncedProfile() {
        val calcUseCase = CalculateNutritionTargetsUseCase()
        val profile = UserProfile(
            userId = "test_firebase_uid", name = "Fitness Pro", age = 25, gender = "Male",
            heightCm = 180f, weightKg = 75f, goal = "Maintain Weight",
            dietaryPreference = "High Protein", allergies = "None", activityLevel = "Active",
            workoutFrequency = "5 days", workoutIntensity = "Moderate"
        )

        val targets = calcUseCase(profile)
        assertTrue(targets.calories > 0)
        assertTrue(targets.protein > 0)
        assertTrue(targets.carbs > 0)
        assertTrue(targets.fat > 0)
    }

    @Test
    fun testFirestoreProfileDocument_fromMap_handlesMalformedAndNullFields() {
        val malformedMap = mapOf<String, Any?>(
            "uid" to "some_uid",
            "name" to null,
            "age" to "not_a_number", // Malformed
            "heightCm" to 172.5, // Double
            "unknownField" to "arbitrary data"
        )

        val doc = FirestoreProfileDocument.fromMap(malformedMap, fallbackUid = "some_uid")
        assertEquals("some_uid", doc.uid)
        assertEquals("", doc.name)
        assertEquals(0, doc.age)
        assertEquals(172.5f, doc.heightCm)
        assertEquals(1, doc.schemaVersion)
    }
}
