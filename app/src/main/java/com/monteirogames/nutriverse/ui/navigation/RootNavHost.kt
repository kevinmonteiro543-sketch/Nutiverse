package com.monteirogames.nutriverse.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.monteirogames.nutriverse.ui.screens.auth.LoginScreen
import com.monteirogames.nutriverse.ui.screens.auth.ProfileSetupScreen
import com.monteirogames.nutriverse.ui.screens.auth.SplashScreen

@Composable
fun RootNavHost() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = SplashRoute
    ) {
        composable<SplashRoute> {
            SplashScreen(
                onNavigateToLogin = {
                    navController.navigate(LoginRoute) {
                        popUpTo(0)
                    }
                },
                onNavigateToProfileSetup = {
                    navController.navigate(ProfileSetupRoute) {
                        popUpTo(0)
                    }
                },
                onNavigateToMain = {
                    navController.navigate(MainRoute) {
                        popUpTo(0)
                    }
                }
            )
        }
        
        composable<LoginRoute> {
            LoginScreen(
                onNavigateToProfileSetup = {
                    navController.navigate(ProfileSetupRoute) {
                        popUpTo(0)
                    }
                },
                onNavigateToMain = {
                    navController.navigate(MainRoute) {
                        popUpTo(0)
                    }
                }
            )
        }
        
        composable<ProfileSetupRoute> {
            ProfileSetupScreen(
                onNavigateToMain = {
                    navController.navigate(MainRoute) {
                        popUpTo(0)
                    }
                }
            )
        }
        
        composable<MainRoute> {
            MainScreen(
                onLogout = {
                    navController.navigate(LoginRoute) {
                        popUpTo(0)
                    }
                }
            )
        }
    }
}
