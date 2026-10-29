package com.monteirogames.nutriverse.ui.screens.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.monteirogames.nutriverse.domain.model.SyncState
import com.monteirogames.nutriverse.domain.usecase.AuthUseCases
import com.monteirogames.nutriverse.domain.usecase.ProfileSyncUseCases
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ProfileViewModel(
    private val authUseCases: AuthUseCases,
    private val profileSyncUseCases: ProfileSyncUseCases? = null
) : ViewModel() {

    val syncState: StateFlow<SyncState>? = profileSyncUseCases?.observeProfileSyncState?.invoke()

    fun syncNow() {
        viewModelScope.launch {
            profileSyncUseCases?.syncProfile?.invoke()
        }
    }

    fun logout() {
        viewModelScope.launch {
            authUseCases.signOut()
        }
    }
}
