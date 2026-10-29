package com.monteirogames.nutriverse.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.monteirogames.nutriverse.data.local.entity.NotificationRecordEntity
import com.monteirogames.nutriverse.data.local.entity.NotificationSettingsEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NotificationDao {
    @Query("SELECT * FROM notification_settings WHERE userId = :userId LIMIT 1")
    fun getNotificationSettings(userId: String): Flow<NotificationSettingsEntity?>

    @Query("SELECT * FROM notification_settings WHERE userId = :userId LIMIT 1")
    suspend fun getNotificationSettingsOnce(userId: String): NotificationSettingsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveNotificationSettings(settings: NotificationSettingsEntity)

    @Query("SELECT COUNT(*) > 0 FROM notification_record WHERE userId = :userId AND deduplicationKey = :deduplicationKey")
    suspend fun isNotificationSent(userId: String, deduplicationKey: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun recordNotificationSent(record: NotificationRecordEntity)

    @Query("DELETE FROM notification_record WHERE userId = :userId")
    suspend fun clearUserRecords(userId: String)

    @Query("DELETE FROM notification_record WHERE sentAt < :thresholdTime")
    suspend fun pruneOldRecords(thresholdTime: Long)
}
