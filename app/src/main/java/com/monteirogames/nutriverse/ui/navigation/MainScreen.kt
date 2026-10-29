package com.monteirogames.nutriverse.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.RestaurantMenu
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Kitchen
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.monteirogames.nutriverse.ui.screens.dashboard.*
import com.monteirogames.nutriverse.ui.screens.mealtracker.*
import com.monteirogames.nutriverse.ui.screens.pantry.*
import com.monteirogames.nutriverse.ui.screens.profile.*
import com.monteirogames.nutriverse.ui.screens.profile.analytics.*
import com.monteirogames.nutriverse.ui.screens.profile.health.*
import com.monteirogames.nutriverse.ui.screens.recipes.*
import com.monteirogames.nutriverse.ui.screens.budget.*
import kotlin.reflect.KClass

data class TopLevelRoute<T : Any>(val name: String, val route: T, val icon: androidx.compose.ui.graphics.vector.ImageVector)

val topLevelRoutes = listOf(
    TopLevelRoute("Dashboard", DashboardRoute, Icons.Filled.Home),
    TopLevelRoute("Meals", MealTrackerRoute, Icons.Filled.Fastfood),
    TopLevelRoute("Pantry", PantryRoute, Icons.Filled.Kitchen),
    TopLevelRoute("Recipes", RecipesRoute, Icons.Filled.RestaurantMenu),
    TopLevelRoute("Profile", ProfileRoute, Icons.Filled.Person)
)

@Composable
fun MainScreen(onLogout: () -> Unit) {
    val navController = rememberNavController()

    Scaffold(
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination
                topLevelRoutes.forEach { topLevelRoute ->
                    val isSelected = currentDestination?.hierarchy?.any { 
                        it.hasRoute(topLevelRoute.route::class)
                    } == true
                    
                    NavigationBarItem(
                        icon = { Icon(topLevelRoute.icon, contentDescription = topLevelRoute.name) },
                        label = { Text(topLevelRoute.name) },
                        selected = isSelected,
                        onClick = {
                            navController.navigate(topLevelRoute.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = DashboardRoute,
            modifier = Modifier.padding(innerPadding)
        ) {
            // Dashboard
            composable<DashboardRoute> { 
                DashboardScreen(
                    onNavigateToWeeklyReview = { navController.navigate(WeeklyAiReviewRoute) },
                    onNavigateToBudget = { navController.navigate(BudgetRoute) }
                ) 
            }
            composable<WeeklyAiReviewRoute> { WeeklyAiReviewScreen(onNavigateBack = { navController.popBackStack() }) }

            // Meal Tracker
            composable<MealTrackerRoute> { 
                MealTrackerScreen(
                    onNavigateToAddMeal = { navController.navigate(AddMealRoute) },
                    onNavigateToEditMeal = { mealId -> navController.navigate(EditMealRoute(mealId)) },
                    onNavigateToBarcodeScanner = { navController.navigate(BarcodeScannerRoute) },
                    onNavigateToAiFoodRecognition = { navController.navigate(AiFoodRecognitionRoute) },
                    onNavigateToFoodCatalog = { navController.navigate(FoodCatalogRoute) }
                ) 
            }
            composable<FoodCatalogRoute> { 
                FoodCatalogScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onMealAdded = { navController.popBackStack() }
                ) 
            }
            composable<AddMealRoute> { AddMealScreen(onNavigateBack = { navController.popBackStack() }) }
            composable<EditMealRoute> { backStackEntry -> 
                val route = backStackEntry.toRoute<EditMealRoute>()
                AddMealScreen(mealId = route.mealId, onNavigateBack = { navController.popBackStack() }) 
            }
            composable<BarcodeScannerRoute> { 
                BarcodeScannerScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onBarcodeScanned = { barcode -> 
                        navController.popBackStack()
                        navController.navigate(ProductResultRoute(barcode))
                    }
                ) 
            }
            composable<ProductResultRoute> { backStackEntry ->
                val route = backStackEntry.toRoute<ProductResultRoute>()
                ProductResultScreen(
                    barcode = route.barcode,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToScanAgain = {
                        navController.popBackStack()
                        navController.navigate(BarcodeScannerRoute)
                    },
                    onAddToPantry = { product ->
                        navController.popBackStack()
                        navController.navigate(
                            AddEditPantryItemRoute(
                                initialName = product.name,
                                calories = product.energyKcal,
                                protein = product.protein,
                                carbs = product.carbs,
                                fat = product.fat,
                                barcode = product.barcode
                            )
                        )
                    }
                )
            }
            composable<AiFoodRecognitionRoute> { AiFoodRecognitionScreen(onNavigateBack = { navController.popBackStack() }) }

            // Pantry
            composable<PantryRoute> { 
                PantryScreen(
                    onNavigateToAddItem = { navController.navigate(AddEditPantryItemRoute()) },
                    onNavigateToEditItem = { itemId -> navController.navigate(EditPantryItemRoute(itemId)) },
                    onNavigateToGroceryPlanner = { navController.navigate(GroceryPlannerRoute) }
                ) 
            }
            composable<AddEditPantryItemRoute> { backStackEntry ->
                val route = backStackEntry.toRoute<AddEditPantryItemRoute>()
                AddEditPantryItemScreen(
                    initialName = route.initialName,
                    initialCalories = route.calories,
                    initialProtein = route.protein,
                    initialCarbs = route.carbs,
                    initialFat = route.fat,
                    initialBarcode = route.barcode,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable<EditPantryItemRoute> { backStackEntry ->
                val route = backStackEntry.toRoute<EditPantryItemRoute>()
                AddEditPantryItemScreen(itemId = route.itemId, onNavigateBack = { navController.popBackStack() })
            }
            composable<GroceryPlannerRoute> { 
                GroceryPlannerScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToBudget = { navController.navigate(BudgetRoute) }
                ) 
            }

            // Recipes
            composable<RecipesRoute> { 
                RecipesScreen(
                    onNavigateToRecipeDetail = { recipeId -> navController.navigate(RecipeDetailRoute(recipeId)) }
                ) 
            }
            composable<RecipeDetailRoute> { backStackEntry ->
                val route = backStackEntry.toRoute<RecipeDetailRoute>()
                RecipeDetailScreen(
                    recipeId = route.recipeId,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToMeals = { navController.navigate(MealTrackerRoute) },
                    onNavigateToGroceryPlanner = { navController.navigate(GroceryPlannerRoute) }
                )
            }

            // Profile
            composable<ProfileRoute> { 
                ProfileScreen(
                    onNavigateToDietPlanner = { navController.navigate(DietPlannerRoute) },
                    onNavigateToNotificationSettings = { navController.navigate(NotificationSettingsRoute) },
                    onNavigateToBudget = { navController.navigate(BudgetRoute) },
                    onNavigateToProgressAnalytics = { navController.navigate(ProgressAnalyticsRoute) },
                    onNavigateToHealthReportAnalysis = { navController.navigate(HealthReportAnalysisRoute) },
                    onNavigateToWeeklyReview = { navController.navigate(WeeklyAiReviewRoute) },
                    onLogout = onLogout
                ) 
            }
            composable<ProgressAnalyticsRoute> { ProgressAnalyticsScreen(onNavigateBack = { navController.popBackStack() }) }
            composable<HealthReportAnalysisRoute> { HealthReportAnalysisScreen(onNavigateBack = { navController.popBackStack() }) }
            composable<DietPlannerRoute> { DietPlannerScreen(onNavigateBack = { navController.popBackStack() }) }
            composable<NotificationSettingsRoute> { NotificationSettingsScreen(onNavigateBack = { navController.popBackStack() }) }

            // Food Budget
            composable<BudgetRoute> { BudgetScreen(onNavigateBack = { navController.popBackStack() }) }
        }
    }
}
