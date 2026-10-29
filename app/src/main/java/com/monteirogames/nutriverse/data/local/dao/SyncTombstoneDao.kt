package com.monteirogames.nutriverse.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.monteirogames.nutriverse.data.local.entity.SyncTombstoneEntity

@Dao
interface SyncTombstoneDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTombstone(tombstone: SyncTombstoneEntity)

    @Query("SELECT * FROM sync_tombstone WHERE userId = :userId AND entityType = :entityType")
    suspend fun getTombstonesForUser(userId: String, entityType: String): List<SyncTombstoneEntity>

    @Query("SELECT * FROM sync_tombstone WHERE entityType = :entityType AND id = :id LIMIT 1")
    suspend fun getTombstone(entityType: String, id: String): SyncTombstoneEntity?

    @Query("DELETE FROM sync_tombstone WHERE entityType = :entityType AND id = :id")
    suspend fun deleteTombstone(entityType: String, id: String)

    @Query("DELETE FROM sync_tombstone WHERE entityType = :entityType AND id IN (:ids)")
    suspend fun deleteTombstones(entityType: String, ids: List<String>)

    @Query("DELETE FROM sync_tombstone WHERE userId = :userId AND entityType = :entityType")
    suspend fun clearTombstonesForUser(userId: String, entityType: String)
}
