package com.monteirogames.nutriverse.data.local.entity

import androidx.room.Entity
import androidx.room.Index

/**
 * Tracks locally deleted items so they are safely tombstoned and not resurrected
 * when syncing with cloud Firestore collections.
 */
@Entity(
    tableName = "sync_tombstone",
    primaryKeys = ["entityType", "id"],
    indices = [Index(value = ["userId", "entityType"])]
)
data class SyncTombstoneEntity(
    val id: String,
    val entityType: String, // "meal", "pantry", etc.
    val userId: String,
    val deletedAt: Long
)
