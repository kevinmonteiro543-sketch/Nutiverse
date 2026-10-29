package com.monteirogames.nutriverse.domain.sync

sealed class SyncResolution<out T> {
    data class UploadToCloud<T>(val localItem: T) : SyncResolution<T>()
    data class DownloadToLocal<T>(val cloudItem: T) : SyncResolution<T>()
    object UpToDate : SyncResolution<Nothing>()
}

/**
 * Isolated deterministic conflict resolution interface for entity synchronization.
 * Evaluates timestamps between local and cloud records to prevent silent data loss.
 */
interface TimestampConflictResolver {
    fun <T> resolve(
        localItem: T?,
        cloudItem: T?,
        getLocalTimestamp: (T) -> Long,
        getCloudTimestamp: (T) -> Long
    ): SyncResolution<T>
}

class TimestampConflictResolverImpl : TimestampConflictResolver {
    override fun <T> resolve(
        localItem: T?,
        cloudItem: T?,
        getLocalTimestamp: (T) -> Long,
        getCloudTimestamp: (T) -> Long
    ): SyncResolution<T> {
        return when {
            localItem == null && cloudItem == null -> SyncResolution.UpToDate
            localItem != null && cloudItem == null -> SyncResolution.UploadToCloud(localItem)
            localItem == null && cloudItem != null -> SyncResolution.DownloadToLocal(cloudItem)
            localItem != null && cloudItem != null -> {
                val localTime = getLocalTimestamp(localItem)
                val cloudTime = getCloudTimestamp(cloudItem)
                when {
                    localTime > cloudTime -> SyncResolution.UploadToCloud(localItem)
                    cloudTime > localTime -> SyncResolution.DownloadToLocal(cloudItem)
                    else -> SyncResolution.UpToDate // Equal timestamps -> prefer local, avoid unnecessary write
                }
            }
            else -> SyncResolution.UpToDate
        }
    }
}
