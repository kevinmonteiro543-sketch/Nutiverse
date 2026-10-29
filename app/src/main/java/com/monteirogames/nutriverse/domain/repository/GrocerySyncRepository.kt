package com.monteirogames.nutriverse.domain.repository

import com.monteirogames.nutriverse.domain.model.GroceryItem
import com.monteirogames.nutriverse.domain.model.SyncState
import kotlinx.coroutines.flow.StateFlow

/**
 * Dedicated repository abstraction for Grocery cloud synchronization.
 * Synchronizes grocery items between local Room and Firestore `users/{FirebaseUID}/grocery/{groceryItemId}`.
 */
interface GrocerySyncRepository {
    val syncState: StateFlow<SyncState>
    fun getSyncState(): SyncState

    suspend fun syncGrocery(): Result<Int>
    suspend fun uploadGroceryItem(item: GroceryItem): Result<Unit>
    suspend fun deleteGroceryItemInCloud(itemId: String): Result<Unit>
}
