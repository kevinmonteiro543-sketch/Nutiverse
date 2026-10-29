package com.monteirogames.nutriverse

import com.monteirogames.nutriverse.domain.model.*
import com.monteirogames.nutriverse.domain.repository.AuthRepository
import com.monteirogames.nutriverse.domain.repository.AuthResult
import com.monteirogames.nutriverse.domain.repository.FirestoreResult
import com.monteirogames.nutriverse.domain.repository.FirestoreUserRepository
import com.monteirogames.nutriverse.domain.usecase.*
import com.monteirogames.nutriverse.data.repository.SyncRepositoryImpl
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FirestoreFoundationTest {

    class FakeAuthRepository : AuthRepository {
        var authenticatedUid: String? = null
        var authenticatedEmail: String? = null
        private val _userFlow = MutableStateFlow<String?>(null)
        override val currentUserFlow: Flow<String?> = _userFlow.asStateFlow()

        override fun getCurrentUserId(): String? = authenticatedUid

        override fun getCurrentUser(): AuthenticatedUser? =
            authenticatedUid?.let { AuthenticatedUser(uid = it, email = authenticatedEmail) }

        override fun observeAuthState(): Flow<AuthenticatedUser?> =
            flowOf(getCurrentUser())

        override suspend fun signIn(email: String, password: String): AuthResult<String> {
            authenticatedUid = "auth_uid_123"
            authenticatedEmail = email
            _userFlow.value = authenticatedUid
            return AuthResult.Success("auth_uid_123")
        }

        override suspend fun signUp(email: String, password: String): AuthResult<String> {
            authenticatedUid = "auth_uid_new"
            authenticatedEmail = email
            _userFlow.value = authenticatedUid
            return AuthResult.Success("auth_uid_new")
        }

        override suspend fun sendPasswordResetEmail(email: String): AuthResult<Unit> =
            AuthResult.Success(Unit)

        override suspend fun signOut() {
            authenticatedUid = null
            authenticatedEmail = null
            _userFlow.value = null
        }
    }

    class FakeFirestoreUserRepository(
        private val authRepository: AuthRepository
    ) : FirestoreUserRepository {

        private val userDocuments = mutableMapOf<String, FirestoreUserDocument>()

        override suspend fun getCurrentUserDocument(): FirestoreResult<FirestoreUserDocument?> {
            val uid = authRepository.getCurrentUserId()
                ?: return FirestoreResult.Error("No authenticated user found. Please sign in.")
            return getUserDocument(uid)
        }

        override suspend fun createOrUpdateUserDocument(document: FirestoreUserDocument): FirestoreResult<Unit> {
            val uid = authRepository.getCurrentUserId()
                ?: return FirestoreResult.Error("No authenticated user found. Please sign in.")

            if (document.uid.isBlank()) {
                return FirestoreResult.Error("Invalid document: UID cannot be empty.")
            }

            if (document.uid != uid) {
                return FirestoreResult.Error("Security violation: Cannot write document for another user identity.")
            }

            userDocuments[document.uid] = document
            return FirestoreResult.Success(Unit)
        }

        override suspend fun getUserDocument(uid: String): FirestoreResult<FirestoreUserDocument?> {
            val currentUid = authRepository.getCurrentUserId()
                ?: return FirestoreResult.Error("No authenticated user found. Please sign in.")

            if (uid.isBlank()) {
                return FirestoreResult.Error("User ID cannot be blank.")
            }

            if (uid != currentUid) {
                return FirestoreResult.Error("Security violation: Cannot access document of another user.")
            }

            return FirestoreResult.Success(userDocuments[uid])
        }
    }

    private lateinit var fakeAuthRepo: FakeAuthRepository
    private lateinit var fakeFirestoreUserRepo: FakeFirestoreUserRepository
    private lateinit var syncRepository: SyncRepositoryImpl
    private lateinit var syncUseCases: SyncUseCases

    @Before
    fun setup() {
        fakeAuthRepo = FakeAuthRepository()
        fakeFirestoreUserRepo = FakeFirestoreUserRepository(fakeAuthRepo)
        syncRepository = SyncRepositoryImpl(fakeAuthRepo, fakeFirestoreUserRepo)
        syncUseCases = SyncUseCases(
            getSyncState = GetSyncStateUseCase(syncRepository),
            observeSyncState = ObserveSyncStateUseCase(syncRepository),
            getCurrentUserDocument = GetCurrentUserDocumentUseCase(fakeFirestoreUserRepo),
            createOrUpdateUserDocument = CreateOrUpdateUserDocumentUseCase(fakeFirestoreUserRepo),
            syncAll = SyncAllUseCase(syncRepository)
        )
    }

    @Test
    fun testFirestoreCollections_enforcesUserScopedPaths() {
        val uid = "test_user_789"
        assertEquals("users/test_user_789", FirestoreCollections.userDocPath(uid))
        assertEquals("users/test_user_789/meals", FirestoreCollections.userSubcollectionPath(uid, FirestoreCollections.MEALS))
        assertEquals("users/test_user_789/pantry", FirestoreCollections.userSubcollectionPath(uid, FirestoreCollections.PANTRY))
        assertEquals("users/test_user_789/recipes", FirestoreCollections.userSubcollectionPath(uid, FirestoreCollections.RECIPES))
        assertEquals("users/test_user_789/grocery", FirestoreCollections.userSubcollectionPath(uid, FirestoreCollections.GROCERY))
        assertEquals("users/test_user_789/expenses", FirestoreCollections.userSubcollectionPath(uid, FirestoreCollections.EXPENSES))
        assertEquals("users/test_user_789/dietPlans", FirestoreCollections.userSubcollectionPath(uid, FirestoreCollections.DIET_PLANS))
        assertEquals("users/test_user_789/healthReports", FirestoreCollections.userSubcollectionPath(uid, FirestoreCollections.HEALTH_REPORTS))
    }

    @Test
    fun testFirestoreUserDocument_serialization() {
        val now = 1700000000000L
        val doc = FirestoreUserDocument(
            uid = "firebase_uid_456",
            email = "user@test.com",
            displayName = "NutriUser",
            createdAt = now,
            updatedAt = now,
            schemaVersion = 1
        )

        val map = doc.toMap()
        assertEquals("firebase_uid_456", map["uid"])
        assertEquals("user@test.com", map["email"])
        assertEquals("NutriUser", map["displayName"])
        assertEquals(now, map["createdAt"])
        assertEquals(now, map["updatedAt"])
        assertEquals(1, map["schemaVersion"])

        val deserialized = FirestoreUserDocument.fromMap(map)
        assertEquals(doc.uid, deserialized.uid)
        assertEquals(doc.email, deserialized.email)
        assertEquals(doc.displayName, deserialized.displayName)
        assertEquals(doc.createdAt, deserialized.createdAt)
        assertEquals(doc.schemaVersion, deserialized.schemaVersion)
    }

    @Test
    fun testSyncRepository_initialStateIsIdle() {
        val state = syncRepository.getSyncState()
        assertEquals(SyncStatus.IDLE, state.status)
        assertNull(state.lastSyncedAt)
        assertNull(state.errorMessage)

        val observed = syncUseCases.getSyncState()
        assertEquals(SyncStatus.IDLE, observed.status)
    }

    @Test
    fun testSyncRepository_placeholdersReturnSuccessWithoutMutations() = runTest {
        assertTrue(syncRepository.syncProfile().isSuccess)
        assertTrue(syncRepository.syncMeals().isSuccess)
        assertTrue(syncRepository.syncPantry().isSuccess)
        assertTrue(syncRepository.syncRecipes().isSuccess)
        assertTrue(syncRepository.syncGroceries().isSuccess)
        assertTrue(syncRepository.syncBudget().isSuccess)
        assertTrue(syncRepository.syncDietPlans().isSuccess)
        assertTrue(syncRepository.syncHealthReports().isSuccess)
        assertEquals(SyncStatus.IDLE, syncRepository.getSyncState().status)
    }

    @Test
    fun testUnauthenticatedAccess_producesControlledError() = runTest {
        fakeAuthRepo.authenticatedUid = null

        val resultGet = syncUseCases.getCurrentUserDocument()
        assertTrue(resultGet is FirestoreResult.Error)
        assertEquals("No authenticated user found. Please sign in.", (resultGet as FirestoreResult.Error).message)

        val doc = FirestoreUserDocument(uid = "some_uid", email = "test@example.com")
        val resultCreate = syncUseCases.createOrUpdateUserDocument(doc)
        assertTrue(resultCreate is FirestoreResult.Error)
        assertEquals("No authenticated user found. Please sign in.", (resultCreate as FirestoreResult.Error).message)
    }

    @Test
    fun testAuthenticatedUser_usesFirebaseUidAsCloudIdentity() = runTest {
        fakeAuthRepo.authenticatedUid = "firebase_uid_authenticated"
        fakeAuthRepo.authenticatedEmail = "myemail@nutriverse.com"

        val doc = FirestoreUserDocument(
            uid = "firebase_uid_authenticated",
            email = "myemail@nutriverse.com",
            displayName = "Chef Nutrition"
        )

        val writeResult = syncUseCases.createOrUpdateUserDocument(doc)
        assertTrue(writeResult is FirestoreResult.Success)

        val readResult = syncUseCases.getCurrentUserDocument()
        assertTrue(readResult is FirestoreResult.Success)
        val readDoc = (readResult as FirestoreResult.Success).data
        assertEquals("firebase_uid_authenticated", readDoc?.uid)
        assertEquals("myemail@nutriverse.com", readDoc?.email)
    }

    @Test
    fun testSecurityIsolation_cannotUseEmailOrOtherUidAsCloudIdentity() = runTest {
        fakeAuthRepo.authenticatedUid = "uid_owner_1"
        fakeAuthRepo.authenticatedEmail = "owner@test.com"

        // Attempting to query or create document using email as identity must fail
        val emailAsIdResult = fakeFirestoreUserRepo.getUserDocument("owner@test.com")
        assertTrue(emailAsIdResult is FirestoreResult.Error)
        assertEquals(
            "Security violation: Cannot access document of another user.",
            (emailAsIdResult as FirestoreResult.Error).message
        )

        // Attempting to access another user's UID must fail
        val otherUserResult = fakeFirestoreUserRepo.getUserDocument("uid_user_2")
        assertTrue(otherUserResult is FirestoreResult.Error)
        assertEquals(
            "Security violation: Cannot access document of another user.",
            (otherUserResult as FirestoreResult.Error).message
        )

        // Attempting to create document for another user's UID must fail
        val foreignDoc = FirestoreUserDocument(uid = "uid_user_2", email = "other@test.com")
        val foreignCreateResult = fakeFirestoreUserRepo.createOrUpdateUserDocument(foreignDoc)
        assertTrue(foreignCreateResult is FirestoreResult.Error)
        assertEquals(
            "Security violation: Cannot write document for another user identity.",
            (foreignCreateResult as FirestoreResult.Error).message
        )
    }
}
