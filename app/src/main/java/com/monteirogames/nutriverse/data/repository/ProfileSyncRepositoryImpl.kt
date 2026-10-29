package com.monteirogames.nutriverse.data.repository

import com.google.android.gms.tasks.Task
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.SetOptions
import com.monteirogames.nutriverse.domain.model.FirestoreCollections
import com.monteirogames.nutriverse.domain.model.FirestoreProfileDocument
import com.monteirogames.nutriverse.domain.model.SyncState
import com.monteirogames.nutriverse.domain.model.SyncStatus
import com.monteirogames.nutriverse.domain.model.UserProfile
import com.monteirogames.nutriverse.domain.model.toFirestoreDocument
import com.monteirogames.nutriverse.domain.model.toUserProfile
import com.monteirogames.nutriverse.domain.repository.AuthRepository
import com.monteirogames.nutriverse.domain.repository.ProfileSyncRepository
import com.monteirogames.nutriverse.domain.repository.UserProfileRepository
import com.monteirogames.nutriverse.domain.sync.SyncResolution
import com.monteirogames.nutriverse.domain.sync.TimestampConflictResolver
import com.monteirogames.nutriverse.domain.sync.TimestampConflictResolverImpl
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class ProfileSyncRepositoryImpl(
    private val authRepository: AuthRepository,
    private val userProfileRepository: UserProfileRepository,
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val conflictResolver: TimestampConflictResolver = TimestampConflictResolverImpl()
) : ProfileSyncRepository {

    private val _syncState = MutableStateFlow(SyncState(status = SyncStatus.IDLE))
    override val syncState: StateFlow<SyncState> = _syncState.asStateFlow()

    override fun getSyncState(): SyncState = _syncState.value

    override suspend fun syncProfile(): Result<UserProfile?> {
        val currentUid = authRepository.getCurrentUserId()
        if (currentUid.isNullOrBlank()) {
            _syncState.value = SyncState(
                status = SyncStatus.ERROR,
                errorMessage = "Cannot sync: User is not authenticated."
            )
            return Result.failure(IllegalStateException("Cannot sync: User is not authenticated."))
        }

        _syncState.value = SyncState(status = SyncStatus.SYNCING)

        return try {
            val localProfile = userProfileRepository.getUserProfile(currentUid).firstOrNull()

            val snapshot = firestore.collection(FirestoreCollections.USERS)
                .document(currentUid)
                .get()
                .awaitResult()

            val cloudDoc = if (snapshot.exists() && snapshot.contains("name")) {
                val data = snapshot.data ?: emptyMap()
                FirestoreProfileDocument.fromMap(data, fallbackUid = currentUid)
            } else {
                null
            }

            val cloudProfile = cloudDoc?.toUserProfile()

            val resolution = conflictResolver.resolve(
                localItem = localProfile,
                cloudItem = cloudProfile,
                getLocalTimestamp = { it.updatedAt },
                getCloudTimestamp = { it.updatedAt }
            )

            val resolvedProfile: UserProfile? = when (resolution) {
                is SyncResolution.UploadToCloud -> {
                    val toUpload = if (resolution.localItem.updatedAt == 0L) {
                        val updated = resolution.localItem.copy(updatedAt = System.currentTimeMillis())
                        userProfileRepository.saveProfile(updated)
                        updated
                    } else {
                        resolution.localItem
                    }

                    firestore.collection(FirestoreCollections.USERS)
                        .document(currentUid)
                        .set(toUpload.toFirestoreDocument().toMap(), SetOptions.merge())
                        .awaitResult()

                    toUpload
                }
                is SyncResolution.DownloadToLocal -> {
                    userProfileRepository.saveProfile(resolution.cloudItem)
                    resolution.cloudItem
                }
                is SyncResolution.UpToDate -> {
                    localProfile
                }
            }

            _syncState.value = SyncState(
                status = SyncStatus.SUCCESS,
                lastSyncedAt = System.currentTimeMillis()
            )

            Result.success(resolvedProfile)
        } catch (e: Exception) {
            val friendlyError = mapException(e)
            _syncState.value = SyncState(
                status = SyncStatus.ERROR,
                errorMessage = friendlyError
            )
            Result.failure(Exception(friendlyError, e))
        }
    }

    override suspend fun uploadProfile(profile: UserProfile): Result<Unit> {
        val currentUid = authRepository.getCurrentUserId()
        if (currentUid.isNullOrBlank()) {
            return Result.failure(IllegalStateException("User is not authenticated"))
        }

        if (profile.userId != currentUid) {
            return Result.failure(SecurityException("Cannot upload profile for a different user identity"))
        }

        return try {
            val validProfile = if (profile.updatedAt == 0L) {
                profile.copy(updatedAt = System.currentTimeMillis())
            } else {
                profile
            }

            firestore.collection(FirestoreCollections.USERS)
                .document(currentUid)
                .set(validProfile.toFirestoreDocument().toMap(), SetOptions.merge())
                .awaitResult()

            _syncState.value = SyncState(
                status = SyncStatus.SUCCESS,
                lastSyncedAt = System.currentTimeMillis()
            )

            Result.success(Unit)
        } catch (e: Exception) {
            val friendly = mapException(e)
            _syncState.value = SyncState(
                status = SyncStatus.ERROR,
                errorMessage = friendly
            )
            Result.failure(Exception(friendly, e))
        }
    }

    override suspend fun downloadProfile(): Result<UserProfile?> {
        val currentUid = authRepository.getCurrentUserId()
        if (currentUid.isNullOrBlank()) {
            return Result.failure(IllegalStateException("User is not authenticated"))
        }

        return try {
            val snapshot = firestore.collection(FirestoreCollections.USERS)
                .document(currentUid)
                .get()
                .awaitResult()

            if (snapshot.exists() && snapshot.contains("name")) {
                val data = snapshot.data ?: emptyMap()
                val profile = FirestoreProfileDocument.fromMap(data, fallbackUid = currentUid).toUserProfile()
                userProfileRepository.saveProfile(profile)
                Result.success(profile)
            } else {
                Result.success(null)
            }
        } catch (e: Exception) {
            Result.failure(Exception(mapException(e), e))
        }
    }

    private fun mapException(e: Exception): String {
        return when (e) {
            is FirebaseFirestoreException -> {
                when (e.code) {
                    FirebaseFirestoreException.Code.PERMISSION_DENIED ->
                        "Access denied. You do not have permission to sync this profile."
                    FirebaseFirestoreException.Code.UNAVAILABLE ->
                        "Cloud service is temporarily unavailable. Offline changes remain saved."
                    FirebaseFirestoreException.Code.UNAUTHENTICATED ->
                        "User is not authenticated. Please sign in to sync."
                    FirebaseFirestoreException.Code.DEADLINE_EXCEEDED ->
                        "Sync operation timed out. Will retry when connection improves."
                    else -> "Cloud synchronization failed: ${e.code.name}"
                }
            }
            is FirebaseNetworkException ->
                "Network unavailable. Profile changes remain saved locally."
            else -> {
                val msg = e.localizedMessage ?: e.message ?: "Cloud synchronization error"
                if (msg.contains("network", ignoreCase = true) || msg.contains("unable to resolve", ignoreCase = true)) {
                    "Network unavailable. Profile changes remain saved locally."
                } else {
                    msg
                }
            }
        }
    }

    companion object {
        suspend fun <T> Task<T>.awaitResult(): T = suspendCancellableCoroutine { continuation ->
            addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    continuation.resume(task.result)
                } else {
                    continuation.resumeWithException(
                        task.exception ?: RuntimeException("Firestore task failed without exception")
                    )
                }
            }
        }
    }
}
