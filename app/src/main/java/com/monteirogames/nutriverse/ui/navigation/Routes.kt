package com.monteirogames.nutriverse.ui.navigation

import kotlinx.serialization.Serializable

@Serializable object SplashRoute
@Serializable object LoginRoute
@Serializable object ProfileSetupRoute
@Serializable object MainRoute

// Dashboard
@Serializable object DashboardRoute
@Serializable object WeeklyAiReviewRoute

// Meal Tracker
@Serializable object MealTrackerRoute
@Serializable object FoodCatalogRoute
@Serializable object AddMealRoute
@Serializable data class EditMealRoute(val mealId: String)
@Serializable object BarcodeScannerRoute
@Serializable data class ProductResultRoute(val barcode: String)
@Serializable object AiFoodRecognitionRoute

// Pantry
@Serializable object PantryRoute
@Serializable data class AddEditPantryItemRoute(val initialName: String? = null, val calories: Float? = null, val protein: Float? = null, val carbs: Float? = null, val fat: Float? = null, val barcode: String? = null)
@Serializable data class EditPantryItemRoute(val itemId: String)
@Serializable object GroceryPlannerRoute

// Recipes
@Serializable object RecipesRoute
@Serializable data class RecipeDetailRoute(val recipeId: String = "")

// Profile
@Serializable object ProfileRoute
@Serializable object DietPlannerRoute
@Serializable object NotificationSettingsRoute
@Serializable object BudgetRoute
@Serializable object ProgressAnalyticsRoute
@Serializable object HealthReportAnalysisRoute
