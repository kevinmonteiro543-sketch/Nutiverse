package com.monteirogames.nutriverse

import com.monteirogames.nutriverse.domain.model.AuthenticatedUser
import com.monteirogames.nutriverse.domain.model.UserProfile
import com.monteirogames.nutriverse.domain.repository.AuthRepository
import com.monteirogames.nutriverse.domain.repository.AuthResult
import com.monteirogames.nutriverse.domain.repository.UserProfileRepository
import com.monteirogames.nutriverse.domain.usecase.*
import com.monteirogames.nutriverse.ui.screens.auth.AuthMode
import com.monteirogames.nutriverse.ui.screens.auth.AuthUiState
import com.monteirogames.nutriverse.ui.screens.auth.AuthViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FirebaseAuthUnitTest {

    private val testDispatcher = StandardTestDispatcher()

    class FakeAuthRepository : AuthRepository {
        var storedUserId: String? = null
        var storedUser: AuthenticatedUser? = null
        private val _userFlow = MutableStateFlow<String?>(null)
        override val currentUserFlow: Flow<String?> = _userFlow.asStateFlow()

        var signInResponse: AuthResult<String> = AuthResult.Success("test_uid_123")
        var signUpResponse: AuthResult<String> = AuthResult.Success("test_uid_new")
        var resetResponse: AuthResult<Unit> = AuthResult.Success(Unit)

        override fun getCurrentUserId(): String? = storedUserId
        override fun getCurrentUser(): AuthenticatedUser? = storedUser

        override fun observeAuthState(): Flow<AuthenticatedUser?> {
            return flowOf(storedUser)
        }

        override suspend fun signIn(email: String, password: String): AuthResult<String> {
            if (signInResponse is AuthResult.Success) {
                storedUserId = (signInResponse as AuthResult.Success<String>).data
                storedUser = AuthenticatedUser(uid = storedUserId!!, email = email)
                _userFlow.value = storedUserId
            }
            return signInResponse
        }

        override suspend fun signUp(email: String, password: String): AuthResult<String> {
            if (signUpResponse is AuthResult.Success) {
                storedUserId = (signUpResponse as AuthResult.Success<String>).data
                storedUser = AuthenticatedUser(uid = storedUserId!!, email = email)
                _userFlow.value = storedUserId
            }
            return signUpResponse
        }

        override suspend fun sendPasswordResetEmail(email: String): AuthResult<Unit> {
            return resetResponse
        }

        override suspend fun signOut() {
            storedUserId = null
            storedUser = null
            _userFlow.value = null
        }
    }

    class FakeUserProfileRepository : UserProfileRepository {
        private val profiles = mutableMapOf<String, UserProfile>()

        override fun getUserProfile(userId: String): Flow<UserProfile?> {
            return flowOf(profiles[userId])
        }

        override suspend fun saveProfile(profile: UserProfile) {
            profiles[profile.userId] = profile
        }

        override suspend fun clearProfile() {
            profiles.clear()
        }
    }

    private lateinit var fakeAuthRepo: FakeAuthRepository
    private lateinit var fakeProfileRepo: FakeUserProfileRepository
    private lateinit var authUseCases: AuthUseCases
    private lateinit var userProfileUseCases: UserProfileUseCases

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        fakeAuthRepo = FakeAuthRepository()
        fakeProfileRepo = FakeUserProfileRepository()

        authUseCases = AuthUseCases(
            signIn = SignInUseCase(fakeAuthRepo),
            signUp = SignUpUseCase(fakeAuthRepo),
            signOut = SignOutUseCase(fakeAuthRepo),
            getCurrentUserId = GetCurrentUserIdUseCase(fakeAuthRepo),
            getCurrentUserFlow = GetCurrentUserFlowUseCase(fakeAuthRepo),
            sendPasswordResetEmail = SendPasswordResetEmailUseCase(fakeAuthRepo),
            getCurrentUser = GetCurrentUserUseCase(fakeAuthRepo),
            observeAuthState = ObserveAuthStateUseCase(fakeAuthRepo)
        )

        userProfileUseCases = UserProfileUseCases(
            getUserProfile = GetUserProfileUseCase(fakeProfileRepo),
            saveUserProfile = SaveUserProfileUseCase(fakeProfileRepo),
            clearUserProfile = ClearUserProfileUseCase(fakeProfileRepo),
            calculateNutritionTargets = CalculateNutritionTargetsUseCase()
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testSignInUseCase_success() = runTest {
        val result = authUseCases.signIn("user@example.com", "validPassword123")
        assertTrue(result is AuthResult.Success)
        assertEquals("test_uid_123", (result as AuthResult.Success).data)
        assertEquals("test_uid_123", authUseCases.getCurrentUserId())
        assertEquals("user@example.com", authUseCases.getCurrentUser()?.email)
    }

    @Test
    fun testSignUpUseCase_success() = runTest {
        val result = authUseCases.signUp("newuser@example.com", "securePassword123")
        assertTrue(result is AuthResult.Success)
        assertEquals("test_uid_new", (result as AuthResult.Success).data)
        assertEquals("test_uid_new", authUseCases.getCurrentUserId())
    }

    @Test
    fun testSignOutUseCase_clearsUser() = runTest {
        authUseCases.signIn("user@example.com", "pass")
        assertEquals("test_uid_123", authUseCases.getCurrentUserId())
        
        authUseCases.signOut()
        assertEquals(null, authUseCases.getCurrentUserId())
        assertEquals(null, authUseCases.getCurrentUser())
    }

    @Test
    fun testSendPasswordResetUseCase_success() = runTest {
        val result = authUseCases.sendPasswordResetEmail("reset@example.com")
        assertTrue(result is AuthResult.Success)
    }

    @Test
    fun testAuthViewModel_validation_emptyCredentials() = runTest {
        val viewModel = AuthViewModel(authUseCases, userProfileUseCases)
        viewModel.authenticate("", "")
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.uiState.value is AuthUiState.Error)
        assertEquals("Email and password cannot be empty", (viewModel.uiState.value as AuthUiState.Error).message)
    }

    @Test
    fun testAuthViewModel_validation_invalidEmail() = runTest {
        val viewModel = AuthViewModel(authUseCases, userProfileUseCases)
        viewModel.authenticate("invalid-email", "validpassword")
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.uiState.value is AuthUiState.Error)
        assertEquals("Please enter a valid email address", (viewModel.uiState.value as AuthUiState.Error).message)
    }

    @Test
    fun testAuthViewModel_registerValidation_shortPassword() = runTest {
        val viewModel = AuthViewModel(authUseCases, userProfileUseCases)
        viewModel.setMode(AuthMode.REGISTER)
        viewModel.authenticate("valid@example.com", "123", "123")
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.uiState.value is AuthUiState.Error)
        assertEquals("Password must be at least 6 characters long", (viewModel.uiState.value as AuthUiState.Error).message)
    }

    @Test
    fun testAuthViewModel_registerValidation_passwordMismatch() = runTest {
        val viewModel = AuthViewModel(authUseCases, userProfileUseCases)
        viewModel.setMode(AuthMode.REGISTER)
        viewModel.authenticate("valid@example.com", "password123", "password456")
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.uiState.value is AuthUiState.Error)
        assertEquals("Passwords do not match", (viewModel.uiState.value as AuthUiState.Error).message)
    }

    @Test
    fun testAuthViewModel_successfulLogin_profileNeeded() = runTest {
        val viewModel = AuthViewModel(authUseCases, userProfileUseCases)
        viewModel.authenticate("valid@example.com", "password123")
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.uiState.value is AuthUiState.SuccessProfileNeeded)
    }

    @Test
    fun testAuthViewModel_successfulLogin_profileExists() = runTest {
        fakeProfileRepo.saveProfile(
            UserProfile(
                userId = "test_uid_123",
                name = "Test User",
                age = 25,
                gender = "Other",
                heightCm = 175f,
                weightKg = 70f,
                goal = "Maintenance",
                dietaryPreference = "None",
                allergies = "None",
                activityLevel = "Sedentary"
            )
        )

        val viewModel = AuthViewModel(authUseCases, userProfileUseCases)
        viewModel.authenticate("valid@example.com", "password123")
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.uiState.value is AuthUiState.SuccessProfileExists)
    }

    @Test
    fun testAuthViewModel_sendPasswordReset_success() = runTest {
        val viewModel = AuthViewModel(authUseCases, userProfileUseCases)
        viewModel.sendPasswordReset("valid@example.com")
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.uiState.value is AuthUiState.PasswordResetSent)
        val resetState = viewModel.uiState.value as AuthUiState.PasswordResetSent
        assertEquals("valid@example.com", resetState.email)
    }

    @Test
    fun testAuthViewModel_modeToggle() {
        val viewModel = AuthViewModel(authUseCases, userProfileUseCases)
        assertEquals(AuthMode.LOGIN, viewModel.mode.value)
        viewModel.toggleMode()
        assertEquals(AuthMode.REGISTER, viewModel.mode.value)
        viewModel.toggleMode()
        assertEquals(AuthMode.LOGIN, viewModel.mode.value)
    }
}
