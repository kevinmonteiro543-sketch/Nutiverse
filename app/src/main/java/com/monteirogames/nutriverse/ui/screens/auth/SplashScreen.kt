package com.monteirogames.nutriverse.ui.screens.auth

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.monteirogames.nutriverse.ui.viewmodel.createViewModel

@Composable
fun SplashScreen(
    onNavigateToLogin: () -> Unit,
    onNavigateToProfileSetup: () -> Unit,
    onNavigateToMain: () -> Unit
) {
    val viewModel = createViewModel { app -> 
        SplashViewModel(app.authUseCases, app.userProfileUseCases, app.profileSyncUseCases) 
    }
    
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState) {
        when (uiState) {
            is SplashState.NavigateToLogin -> onNavigateToLogin()
            is SplashState.NavigateToProfileSetup -> onNavigateToProfileSetup()
            is SplashState.NavigateToMain -> onNavigateToMain()
            SplashState.Loading -> { }
        }
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("NutriVerse", style = MaterialTheme.typography.displayMedium)
            Spacer(modifier = Modifier.height(16.dp))
            CircularProgressIndicator()
        }
    }
}
