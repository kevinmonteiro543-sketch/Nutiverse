package com.monteirogames.nutriverse.data.repository

import com.google.android.gms.tasks.Task
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.SetOptions
import com.monteirogames.nutriverse.data.local.dao.SyncTombstoneDao
import com.monteirogames.nutriverse.domain.model.FirestoreCollections
import com.monteirogames.nutriverse.domain.model.FirestoreHealthReportDocument
import com.monteirogames.nutriverse.domain.model.HealthReport
import com.monteirogames.nutriverse.domain.model.SyncState
import com.monteirogames.nutriverse.domain.model.SyncStatus
import com.monteirogames.nutriverse.domain.model.toFirestoreDocument
import com.monteirogames.nutriverse.domain.repository.AuthRepository
import com.monteirogames.nutriverse.domain.repository.HealthReportRepository
import com.monteirogames.nutriverse.domain.repository.HealthReportSyncRepository
import com.monteirogames.nutriverse.domain.sync.SyncResolution
import com.monteirogames.nutriverse.domain.sync.TimestampConflictResolver
import com.monteirogames.nutriverse.domain.sync.TimestampConflictResolverImpl
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class HealthReportSyncRepositoryImpl(
    private val authRepository: AuthRepository,
    private val healthReportRepository: HealthReportRepository,
    private val syncTombstoneDao: SyncTombstoneDao,
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val conflictResolver: TimestampConflictResolver = TimestampConflictResolverImpl()
) : HealthReportSyncRepository {

    private val _syncState = MutableStateFlow(SyncState(status = SyncStatus.IDLE))
    override val syncState: StateFlow<SyncState> = _syncState.asStateFlow()

    override fun getSyncState(): SyncState = _syncState.value

    override suspend fun syncHealthReports(): Result<Int> {
        val currentUid = authRepository.getCurrentUserId()
        if (currentUid.isNullOrBlank()) {
            _syncState.value = SyncState(
                status = SyncStatus.ERROR,
                errorMessage = "Cannot sync health reports: User is not authenticated."
            )
            return Result.failure(IllegalStateException("Cannot sync health reports: User is not authenticated."))
        }

        _syncState.value = SyncState(status = SyncStatus.SYNCING)

        return try {
            // 1. Gather local reports and tombstones
            val localReports = healthReportRepository.getAllReportsList(currentUid)
            val localMap = localReports.associateBy { it.id }
            val tombstones = syncTombstoneDao.getTombstonesForUser(currentUid, "healthReport").associateBy { it.id }

            // 2. Query Firestore users/{uid}/healthReports
            val snapshot = firestore.collection(FirestoreCollections.userHealthReportsPath(currentUid))
                .get()
                .awaitResult()

            val cloudMap = snapshot.documents.mapNotNull { doc ->
                val data = doc.data ?: return@mapNotNull null
                FirestoreHealthReportDocument.fromMap(data, fallbackId = doc.id, fallbackUserId = currentUid)
            }.associateBy { it.id }

            // 3. Process all distinct health report IDs
            val allIds = (localMap.keys + cloudMap.keys + tombstones.keys).toSet()
            var syncedCount = 0

            for (id in allIds) {
                val localItem = localMap[id]
                val cloudDoc = cloudMap[id]
                val tombstone = tombstones[id]

                when {
                    // Scenario 1: Local record was deleted (tombstone exists)
                    tombstone != null -> {
                        if (cloudDoc == null) {
                            firestore.document(FirestoreCollections.userHealthReportDocPath(currentUid, id))
                                .set(
                                    mapOf(
                                        "id" to id,
                                        "userId" to currentUid,
                                        "isDeleted" to true,
                                        "updatedAt" to tombstone.deletedAt,
                                        "schemaVersion" to FirestoreHealthReportDocument.CURRENT_SCHEMA_VERSION
                                    ),
                                    SetOptions.merge()
                                )
                                .awaitResult()
                            syncedCount++
                        } else if (cloudDoc.isDeleted) {
                            if (cloudDoc.updatedAt >= tombstone.deletedAt) {
                                syncTombstoneDao.deleteTombstone("healthReport", id)
                            }
                        } else {
                            if (tombstone.deletedAt >= cloudDoc.updatedAt) {
                                firestore.document(FirestoreCollections.userHealthReportDocPath(currentUid, id))
                                    .set(
                                        mapOf(
                                            "id" to id,
                                            "userId" to currentUid,
                                            "isDeleted" to true,
                                            "updatedAt" to tombstone.deletedAt,
                                            "schemaVersion" to FirestoreHealthReportDocument.CURRENT_SCHEMA_VERSION
                                        ),
                                        SetOptions.merge()
                                    )
                                    .awaitResult()
                                syncedCount++
                            } else {
                                // Cloud updated after local deletion -> resurrect locally
                                healthReportRepository.saveReport(cloudDoc.toHealthReport())
                                syncTombstoneDao.deleteTombstone("healthReport", id)
                                syncedCount++
                            }
                        }
                    }

                    // Scenario 2: Cloud doc exists and is marked as deleted
                    cloudDoc != null && cloudDoc.isDeleted -> {
                        if (localItem != null) {
                            if (cloudDoc.updatedAt >= localItem.updatedAt) {
                                healthReportRepository.deleteReport(localItem.id, currentUid)
                                syncedCount++
                            } else {
                                firestore.document(FirestoreCollections.userHealthReportDocPath(currentUid, id))
                                    .set(localItem.toFirestoreDocument(userId = currentUid, isDeleted = false).toMap())
                                    .awaitResult()
                                syncedCount++
                            }
                        }
                    }

                    // Scenario 3: Standard sync resolution for active items
                    else -> {
                        val cloudItem = cloudDoc?.toHealthReport()
                        val resolution = conflictResolver.resolve(
                            localItem = localItem,
                            cloudItem = cloudItem,
                            getLocalTimestamp = { it.updatedAt },
                            getCloudTimestamp = { it.updatedAt }
                        )

                        when (resolution) {
                            is SyncResolution.UploadToCloud -> {
                                val toUpload = if (resolution.localItem.updatedAt == 0L) {
                                    val updated = resolution.localItem.copy(updatedAt = System.currentTimeMillis())
                                    healthReportRepository.saveReport(updated)
                                    updated
                                } else {
                                    resolution.localItem
                                }

                                firestore.document(FirestoreCollections.userHealthReportDocPath(currentUid, id))
                                    .set(toUpload.toFirestoreDocument(userId = currentUid, isDeleted = false).toMap())
                                    .awaitResult()
                                syncedCount++
                            }

                            is SyncResolution.DownloadToLocal -> {
                                healthReportRepository.saveReport(resolution.cloudItem)
                                syncedCount++
                            }

                            is SyncResolution.UpToDate -> {
                                // Already synchronized
                            }
                        }
                    }
                }
            }

            _syncState.value = SyncState(
                status = SyncStatus.SUCCESS,
                lastSyncedAt = System.currentTimeMillis()
            )
            Result.success(syncedCount)
        } catch (e: Exception) {
            val isOffline = e is FirebaseNetworkException ||
                    (e is FirebaseFirestoreException && e.code == FirebaseFirestoreException.Code.UNAVAILABLE)

            val errorMsg = if (isOffline) {
                "Offline: Health report sync deferred."
            } else {
                "Health report sync failed: ${e.message ?: "Unknown error"}"
            }

            _syncState.value = SyncState(
                status = SyncStatus.ERROR,
                errorMessage = errorMsg
            )
            Result.failure(e)
        }
    }

    override suspend fun uploadHealthReport(report: HealthReport): Result<Unit> {
        val currentUid = authRepository.getCurrentUserId()
        if (currentUid.isNullOrBlank()) {
            return Result.failure(IllegalStateException("Cannot upload: User is not authenticated."))
        }

        return try {
            val doc = report.toFirestoreDocument(userId = currentUid, isDeleted = false)
            firestore.document(FirestoreCollections.userHealthReportDocPath(currentUid, report.id))
                .set(doc.toMap())
                .awaitResult()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteHealthReportInCloud(reportId: String): Result<Unit> {
        val currentUid = authRepository.getCurrentUserId()
        if (currentUid.isNullOrBlank()) {
            return Result.failure(IllegalStateException("Cannot delete: User is not authenticated."))
        }

        return try {
            firestore.document(FirestoreCollections.userHealthReportDocPath(currentUid, reportId))
                .set(
                    mapOf(
                        "id" to reportId,
                        "userId" to currentUid,
                        "isDeleted" to true,
                        "updatedAt" to System.currentTimeMillis(),
                        "schemaVersion" to FirestoreHealthReportDocument.CURRENT_SCHEMA_VERSION
                    ),
                    SetOptions.merge()
                )
                .awaitResult()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun <T> Task<T>.awaitResult(): T = suspendCancellableCoroutine { continuation ->
        addOnSuccessListener { result ->
            continuation.resume(result)
        }
        addOnFailureListener { exception ->
            continuation.resumeWithException(exception)
        }
    }
}
