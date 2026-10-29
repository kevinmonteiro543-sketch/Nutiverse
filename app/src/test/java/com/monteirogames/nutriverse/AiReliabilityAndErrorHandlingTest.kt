package com.monteirogames.nutriverse

import com.monteirogames.nutriverse.domain.model.AuthenticatedUser
import com.monteirogames.nutriverse.domain.model.DietPlan
import com.monteirogames.nutriverse.domain.model.HealthReportAnalysisParser
import com.monteirogames.nutriverse.domain.model.UserProfile
import com.monteirogames.nutriverse.domain.repository.AuthRepository
import com.monteirogames.nutriverse.domain.repository.AuthResult
import com.monteirogames.nutriverse.domain.repository.DietPlannerRepository
import com.monteirogames.nutriverse.domain.repository.UserProfileRepository
import com.monteirogames.nutriverse.domain.usecase.AuthUseCases
import com.monteirogames.nutriverse.domain.usecase.CalculateNutritionTargetsUseCase
import com.monteirogames.nutriverse.domain.usecase.ClearDietPlanUseCase
import com.monteirogames.nutriverse.domain.usecase.ClearUserProfileUseCase
import com.monteirogames.nutriverse.domain.usecase.DietPlannerUseCases
import com.monteirogames.nutriverse.domain.usecase.GenerateDietPlanUseCase
import com.monteirogames.nutriverse.domain.usecase.GetActiveDietPlanUseCase
import com.monteirogames.nutriverse.domain.usecase.GetCurrentUserFlowUseCase
import com.monteirogames.nutriverse.domain.usecase.GetCurrentUserIdUseCase
import com.monteirogames.nutriverse.domain.usecase.GetUserProfileUseCase
import com.monteirogames.nutriverse.domain.usecase.NutritionTargets
import com.monteirogames.nutriverse.domain.usecase.SaveDietPlanUseCase
import com.monteirogames.nutriverse.domain.usecase.SaveUserProfileUseCase
import com.monteirogames.nutriverse.domain.usecase.SignInUseCase
import com.monteirogames.nutriverse.domain.usecase.SignOutUseCase
import com.monteirogames.nutriverse.domain.usecase.SignUpUseCase
import com.monteirogames.nutriverse.domain.usecase.UserProfileUseCases
import com.monteirogames.nutriverse.ui.screens.profile.DietPlannerUiState
import com.monteirogames.nutriverse.ui.screens.profile.DietPlannerViewModel
import com.monteirogames.nutriverse.util.AiErrorMapper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.IOException
import java.net.ConnectException
import java.net.UnknownHostException

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AiReliabilityAndErrorHandlingTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // 1. Friendly mapping for temporary overload errors
    @Test
    fun testTemporaryOverloadErrorMapping() {
        val overloadEx = Exception("INTERNAL: Overloaded: too many retries per request. DECODE_PREEMPTED — This model is currently experiencing high demand.")
        val mapped = AiErrorMapper.mapToUserMessage(overloadEx)
        assertEquals(AiErrorMapper.MSG_SERVICE_BUSY, mapped)
    }

    // 2. UNAVAILABLE and DECODE_PREEMPTED handling
    @Test
    fun testUnavailableAndDecodePreemptedHandling() {
        val unavailableEx = Exception("UNAVAILABLE: 503 Service Unavailable backend temporarily down")
        assertEquals(AiErrorMapper.MSG_SERVICE_BUSY, AiErrorMapper.mapToUserMessage(unavailableEx))

        val decodeEx = Exception("DECODE_PREEMPTED")
        assertEquals(AiErrorMapper.MSG_SERVICE_BUSY, AiErrorMapper.mapToUserMessage(decodeEx))
    }

    // 3. Genuine quota errors distinguished from transient overload
    @Test
    fun testQuotaErrorsDistinguishedFromTransientOverload() {
        val quotaEx = Exception("HTTP 429 RESOURCE_EXHAUSTED: You have exceeded your current quota.")
        val mappedQuota = AiErrorMapper.mapToUserMessage(quotaEx)
        assertEquals(AiErrorMapper.MSG_QUOTA_EXHAUSTED, mappedQuota)

        val transientRateLimit = Exception("HTTP 429 Too Many Requests: Rate limit exceeded, try again shortly")
        val mappedTransient = AiErrorMapper.mapToUserMessage(transientRateLimit)
        assertEquals(AiErrorMapper.MSG_SERVICE_BUSY, mappedTransient)
    }

    // 4. Network errors
    @Test
    fun testNetworkErrors() {
        val unknownHost = UnknownHostException("Unable to resolve host 'generativelanguage.googleapis.com': No address associated with hostname")
        assertEquals(AiErrorMapper.MSG_NETWORK_ERROR, AiErrorMapper.mapToUserMessage(unknownHost))

        val connectEx = ConnectException("Failed to connect to /142.250.190.42:443")
        assertEquals(AiErrorMapper.MSG_NETWORK_ERROR, AiErrorMapper.mapToUserMessage(connectEx))

        val ioNetwork = IOException("network error occurred during streaming")
        assertEquals(AiErrorMapper.MSG_NETWORK_ERROR, AiErrorMapper.mapToUserMessage(ioNetwork))
    }

    // 5. Configuration and App Check errors
    @Test
    fun testConfigurationAndAppCheckErrors() {
        val appCheckEx = Exception("AppCheck attestation failed: Error fetching App Check token with status 403 Forbidden")
        assertEquals(AiErrorMapper.MSG_SECURITY_CONFIG, AiErrorMapper.mapToUserMessage(appCheckEx))

        val permissionDenied = Exception("PERMISSION_DENIED: Firebase App Check token is invalid")
        assertEquals(AiErrorMapper.MSG_SECURITY_CONFIG, AiErrorMapper.mapToUserMessage(permissionDenied))
    }

    // 6. Empty responses
    @Test
    fun testEmptyResponses() {
        val emptyEx = Exception("AI returned an empty response.")
        assertEquals(AiErrorMapper.MSG_EMPTY_RESPONSE, AiErrorMapper.mapToUserMessage(emptyEx))

        val emptyParse = HealthReportAnalysisParser.parse("", "user_1")
        assertTrue(emptyParse.isFailure)
        assertEquals(AiErrorMapper.MSG_EMPTY_RESPONSE, AiErrorMapper.mapToUserMessage(emptyParse.exceptionOrNull()!!))
    }

    // 7. Malformed JSON
    @Test
    fun testMalformedJsonHandling() {
        val badJsonEx = org.json.JSONException("Value < at 0 of type java.lang.String cannot be converted to JSONObject")
        val mappedDiet = AiErrorMapper.mapToUserMessage(badJsonEx, isImageAnalysis = false)
        assertEquals(AiErrorMapper.MSG_UNPARSEABLE_RESPONSE, mappedDiet)

        val mappedHealth = AiErrorMapper.mapToUserMessage(badJsonEx, isImageAnalysis = true)
        assertTrue(mappedHealth.contains("Could not extract lab data"))
    }

    // 8. Invalid or incomplete Diet Plan responses
    @Test
    fun testInvalidOrIncompleteDietPlanResponse() {
        val missingDaysEx = IllegalArgumentException("Response missing 'days' list.")
        val mapped = AiErrorMapper.mapToUserMessage(missingDaysEx, isImageAnalysis = false)
        assertEquals(AiErrorMapper.MSG_UNPARSEABLE_RESPONSE, mapped)
    }

    // 9. Invalid or incomplete Health Report responses
    @Test
    fun testInvalidOrIncompleteHealthReportResponse() {
        val incompleteJson = "{ \"reportType\": \"CBC\" }"
        val result = HealthReportAnalysisParser.parse(incompleteJson, "user_1")
        assertTrue(result.isSuccess)
        val report = result.getOrThrow()
        assertEquals(0, report.results.size)
        assertNotNull(report.disclaimer)
    }

    // 10. Duplicate-request prevention in DietPlannerViewModel
    @Test
    fun testDuplicateRequestPreventionDietPlanner() = runBlocking {
        var generateCallCount = 0
        val fakeRepo = object : DietPlannerRepository {
            override suspend fun generate7DayPlan(userProfile: UserProfile, nutritionTargets: NutritionTargets): Result<DietPlan> {
                generateCallCount++
                return Result.success(
                    DietPlan(
                        id = "plan_1",
                        userId = userProfile.userId,
                        name = "Test Plan",
                        targetCalories = 2000f,
                        targetProtein = 120f,
                        targetCarbs = 200f,
                        targetFat = 65f,
                        createdAt = 1000L,
                        days = emptyList(),
                        updatedAt = 1000L
                    )
                )
            }
            override suspend fun saveDietPlan(plan: DietPlan) {}
            override fun getActiveDietPlan(userId: String): Flow<DietPlan?> = flowOf(null)
            override suspend fun getActiveDietPlanOnce(userId: String): DietPlan? = null
            override fun getAllDietPlans(userId: String): Flow<List<DietPlan>> = flowOf(emptyList())
            override suspend fun getAllDietPlansList(userId: String): List<DietPlan> = emptyList()
            override suspend fun getDietPlanById(planId: String, userId: String): DietPlan? = null
            override suspend fun deleteDietPlan(planId: String, userId: String) {}
            override suspend fun clearDietPlan(userId: String) {}
        }

        val testProfile = UserProfile(
            userId = "test_user",
            name = "Test",
            age = 25,
            gender = "Male",
            heightCm = 175f,
            weightKg = 70f,
            goal = "Maintain Weight",
            dietaryPreference = "Vegetarian",
            allergies = "None",
            activityLevel = "Moderate"
        )

        val fakeUserRepo = object : UserProfileRepository {
            override fun getUserProfile(userId: String): Flow<UserProfile?> = flowOf(testProfile)
            override suspend fun saveProfile(profile: UserProfile) {}
            override suspend fun clearProfile() {}
        }

        val fakeAuth = object : AuthRepository {
            override val currentUserFlow: Flow<String?> get() = flowOf("test_user")
            override fun getCurrentUserId(): String? = "test_user"
            override fun getCurrentUser(): AuthenticatedUser? = null
            override fun observeAuthState(): Flow<AuthenticatedUser?> = emptyFlow()
            override suspend fun signIn(email: String, password: String): AuthResult<String> = AuthResult.Success("test_user")
            override suspend fun signUp(email: String, password: String): AuthResult<String> = AuthResult.Success("test_user")
            override suspend fun sendPasswordResetEmail(email: String): AuthResult<Unit> = AuthResult.Success(Unit)
            override suspend fun signOut() {}
        }

        val viewModel = DietPlannerViewModel(
            userProfileUseCases = UserProfileUseCases(
                getUserProfile = GetUserProfileUseCase(fakeUserRepo),
                saveUserProfile = SaveUserProfileUseCase(fakeUserRepo),
                clearUserProfile = ClearUserProfileUseCase(fakeUserRepo),
                calculateNutritionTargets = CalculateNutritionTargetsUseCase()
            ),
            dietPlannerUseCases = DietPlannerUseCases(
                generateDietPlan = GenerateDietPlanUseCase(fakeRepo),
                saveDietPlan = SaveDietPlanUseCase(fakeRepo),
                getActiveDietPlan = GetActiveDietPlanUseCase(fakeRepo),
                clearDietPlan = ClearDietPlanUseCase(fakeRepo)
            ),
            authUseCases = AuthUseCases(
                signIn = SignInUseCase(fakeAuth),
                signUp = SignUpUseCase(fakeAuth),
                signOut = SignOutUseCase(fakeAuth),
                getCurrentUserId = GetCurrentUserIdUseCase(fakeAuth),
                getCurrentUserFlow = GetCurrentUserFlowUseCase(fakeAuth)
            )
        )

        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.uiState.value is DietPlannerUiState.Ready)

        // Trigger generation
        viewModel.generate7DayPlan()
        assertTrue(viewModel.uiState.value is DietPlannerUiState.Generating)

        // Trigger duplicate generation while Generating
        viewModel.generate7DayPlan()

        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(1, generateCallCount)
        assertTrue(viewModel.uiState.value is DietPlannerUiState.Reviewing)
    }

    // 11. Loading state reset after success and failure
    @Test
    fun testLoadingStateResetAfterFailure() = runBlocking {
        val failingRepo = object : DietPlannerRepository {
            override suspend fun generate7DayPlan(userProfile: UserProfile, nutritionTargets: NutritionTargets): Result<DietPlan> {
                return Result.failure(Exception(AiErrorMapper.MSG_SERVICE_BUSY))
            }
            override suspend fun saveDietPlan(plan: DietPlan) {}
            override fun getActiveDietPlan(userId: String): Flow<DietPlan?> = flowOf(null)
            override suspend fun getActiveDietPlanOnce(userId: String): DietPlan? = null
            override fun getAllDietPlans(userId: String): Flow<List<DietPlan>> = flowOf(emptyList())
            override suspend fun getAllDietPlansList(userId: String): List<DietPlan> = emptyList()
            override suspend fun getDietPlanById(planId: String, userId: String): DietPlan? = null
            override suspend fun deleteDietPlan(planId: String, userId: String) {}
            override suspend fun clearDietPlan(userId: String) {}
        }

        val testProfile = UserProfile(
            userId = "test_user",
            name = "Test",
            age = 25,
            gender = "Male",
            heightCm = 175f,
            weightKg = 70f,
            goal = "Maintain Weight",
            dietaryPreference = "Vegetarian",
            allergies = "None",
            activityLevel = "Moderate"
        )

        val fakeUserRepo = object : UserProfileRepository {
            override fun getUserProfile(userId: String): Flow<UserProfile?> = flowOf(testProfile)
            override suspend fun saveProfile(profile: UserProfile) {}
            override suspend fun clearProfile() {}
        }

        val fakeAuth = object : AuthRepository {
            override val currentUserFlow: Flow<String?> get() = flowOf("test_user")
            override fun getCurrentUserId(): String? = "test_user"
            override fun getCurrentUser(): AuthenticatedUser? = null
            override fun observeAuthState(): Flow<AuthenticatedUser?> = emptyFlow()
            override suspend fun signIn(email: String, password: String): AuthResult<String> = AuthResult.Success("test_user")
            override suspend fun signUp(email: String, password: String): AuthResult<String> = AuthResult.Success("test_user")
            override suspend fun sendPasswordResetEmail(email: String): AuthResult<Unit> = AuthResult.Success(Unit)
            override suspend fun signOut() {}
        }

        val viewModel = DietPlannerViewModel(
            userProfileUseCases = UserProfileUseCases(
                getUserProfile = GetUserProfileUseCase(fakeUserRepo),
                saveUserProfile = SaveUserProfileUseCase(fakeUserRepo),
                clearUserProfile = ClearUserProfileUseCase(fakeUserRepo),
                calculateNutritionTargets = CalculateNutritionTargetsUseCase()
            ),
            dietPlannerUseCases = DietPlannerUseCases(
                generateDietPlan = GenerateDietPlanUseCase(failingRepo),
                saveDietPlan = SaveDietPlanUseCase(failingRepo),
                getActiveDietPlan = GetActiveDietPlanUseCase(failingRepo),
                clearDietPlan = ClearDietPlanUseCase(failingRepo)
            ),
            authUseCases = AuthUseCases(
                signIn = SignInUseCase(fakeAuth),
                signUp = SignUpUseCase(fakeAuth),
                signOut = SignOutUseCase(fakeAuth),
                getCurrentUserId = GetCurrentUserIdUseCase(fakeAuth),
                getCurrentUserFlow = GetCurrentUserFlowUseCase(fakeAuth)
            )
        )

        testDispatcher.scheduler.advanceUntilIdle()
        assertTrue(viewModel.uiState.value is DietPlannerUiState.Ready)

        viewModel.generate7DayPlan()
        assertTrue(viewModel.uiState.value is DietPlannerUiState.Generating)

        testDispatcher.scheduler.advanceUntilIdle()
        val errorState = viewModel.uiState.value as DietPlannerUiState.Error
        assertEquals(AiErrorMapper.MSG_SERVICE_BUSY, errorState.message)
    }

    // 12. Existing saved data remains untouched after failed generation
    @Test
    fun testExistingSavedDataRemainsUntouchedAfterFailedGeneration() = runBlocking {
        var existingPlan: DietPlan? = DietPlan(
            id = "saved_plan_1",
            userId = "test_user",
            name = "Original Plan",
            targetCalories = 2000f,
            targetProtein = 120f,
            targetCarbs = 200f,
            targetFat = 65f,
            createdAt = 500L,
            days = emptyList(),
            updatedAt = 500L
        )

        val failingRepo = object : DietPlannerRepository {
            override suspend fun generate7DayPlan(userProfile: UserProfile, nutritionTargets: NutritionTargets): Result<DietPlan> {
                return Result.failure(Exception(AiErrorMapper.MSG_SERVICE_BUSY))
            }
            override suspend fun saveDietPlan(plan: DietPlan) {
                existingPlan = plan
            }
            override fun getActiveDietPlan(userId: String): Flow<DietPlan?> = flowOf(existingPlan)
            override suspend fun getActiveDietPlanOnce(userId: String): DietPlan? = existingPlan
            override fun getAllDietPlans(userId: String): Flow<List<DietPlan>> = flowOf(listOfNotNull(existingPlan))
            override suspend fun getAllDietPlansList(userId: String): List<DietPlan> = listOfNotNull(existingPlan)
            override suspend fun getDietPlanById(planId: String, userId: String): DietPlan? = existingPlan
            override suspend fun deleteDietPlan(planId: String, userId: String) {}
            override suspend fun clearDietPlan(userId: String) {}
        }

        val testProfile = UserProfile(
            userId = "test_user",
            name = "Test",
            age = 25,
            gender = "Male",
            heightCm = 175f,
            weightKg = 70f,
            goal = "Maintain Weight",
            dietaryPreference = "Vegetarian",
            allergies = "None",
            activityLevel = "Moderate"
        )

        val fakeUserRepo = object : UserProfileRepository {
            override fun getUserProfile(userId: String): Flow<UserProfile?> = flowOf(testProfile)
            override suspend fun saveProfile(profile: UserProfile) {}
            override suspend fun clearProfile() {}
        }

        val fakeAuth = object : AuthRepository {
            override val currentUserFlow: Flow<String?> get() = flowOf("test_user")
            override fun getCurrentUserId(): String? = "test_user"
            override fun getCurrentUser(): AuthenticatedUser? = null
            override fun observeAuthState(): Flow<AuthenticatedUser?> = emptyFlow()
            override suspend fun signIn(email: String, password: String): AuthResult<String> = AuthResult.Success("test_user")
            override suspend fun signUp(email: String, password: String): AuthResult<String> = AuthResult.Success("test_user")
            override suspend fun sendPasswordResetEmail(email: String): AuthResult<Unit> = AuthResult.Success(Unit)
            override suspend fun signOut() {}
        }

        val viewModel = DietPlannerViewModel(
            userProfileUseCases = UserProfileUseCases(
                getUserProfile = GetUserProfileUseCase(fakeUserRepo),
                saveUserProfile = SaveUserProfileUseCase(fakeUserRepo),
                clearUserProfile = ClearUserProfileUseCase(fakeUserRepo),
                calculateNutritionTargets = CalculateNutritionTargetsUseCase()
            ),
            dietPlannerUseCases = DietPlannerUseCases(
                generateDietPlan = GenerateDietPlanUseCase(failingRepo),
                saveDietPlan = SaveDietPlanUseCase(failingRepo),
                getActiveDietPlan = GetActiveDietPlanUseCase(failingRepo),
                clearDietPlan = ClearDietPlanUseCase(failingRepo)
            ),
            authUseCases = AuthUseCases(
                signIn = SignInUseCase(fakeAuth),
                signUp = SignUpUseCase(fakeAuth),
                signOut = SignOutUseCase(fakeAuth),
                getCurrentUserId = GetCurrentUserIdUseCase(fakeAuth),
                getCurrentUserFlow = GetCurrentUserFlowUseCase(fakeAuth)
            )
        )

        testDispatcher.scheduler.advanceUntilIdle()

        // Generate and fail
        viewModel.generate7DayPlan()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value as DietPlannerUiState.Error
        assertEquals("saved_plan_1", state.activePlan?.id)
        assertEquals("saved_plan_1", existingPlan?.id)
    }
}
