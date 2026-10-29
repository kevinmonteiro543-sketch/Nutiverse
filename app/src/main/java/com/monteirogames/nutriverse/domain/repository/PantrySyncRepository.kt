package com.monteirogames.nutriverse.domain.repository

import com.monteirogames.nutriverse.domain.model.PantryItem
import com.monteirogames.nutriverse.domain.model.SyncState
import kotlinx.coroutines.flow.StateFlow

/**
 * Dedicated repository abstraction for Pantry cloud synchronization.
 * Synchronizes pantry items between local Room and Firestore users/{FirebaseUID}/pantry/{pantryItemId}.
 */
interface PantrySyncRepository {
    val syncState: StateFlow<SyncState>
    fun getSyncState(): SyncState

    suspend fun syncPantry(): Result<Int>
    suspend fun uploadPantryItem(item: PantryItem): Result<Unit>
    suspend fun deletePantryItemInCloud(itemId: String): Result<Unit>
}
