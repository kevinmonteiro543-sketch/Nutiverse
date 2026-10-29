package com.monteirogames.nutriverse.domain.model

/**
 * Centralized Firestore collection and path constants for NutriVerse.
 * All cloud user data is strictly scoped under `users/{uid}/...`.
 */
object FirestoreCollections {
    const val USERS = "users"
    const val MEALS = "meals"
    const val PANTRY = "pantry"
    const val RECIPES = "recipes"
    const val GROCERY = "grocery"
    const val EXPENSES = "expenses"
    const val BUDGET = "budget"
    const val DIET_PLANS = "dietPlans"
    const val HEALTH_REPORTS = "healthReports"

    fun userDocPath(uid: String): String = "$USERS/$uid"
    fun userSubcollectionPath(uid: String, subcollection: String): String = "$USERS/$uid/$subcollection"

    fun userMealsPath(uid: String): String = "$USERS/$uid/$MEALS"
    fun userMealDocPath(uid: String, mealId: String): String = "$USERS/$uid/$MEALS/$mealId"

    fun userPantryPath(uid: String): String = "$USERS/$uid/$PANTRY"
    fun userPantryDocPath(uid: String, pantryItemId: String): String = "$USERS/$uid/$PANTRY/$pantryItemId"

    fun userRecipesPath(uid: String): String = "$USERS/$uid/$RECIPES"
    fun userRecipeDocPath(uid: String, recipeId: String): String = "$USERS/$uid/$RECIPES/$recipeId"

    fun userGroceryPath(uid: String): String = "$USERS/$uid/$GROCERY"
    fun userGroceryDocPath(uid: String, groceryItemId: String): String = "$USERS/$uid/$GROCERY/$groceryItemId"

    fun userExpensesPath(uid: String): String = "$USERS/$uid/$EXPENSES"
    fun userExpenseDocPath(uid: String, expenseId: String): String = "$USERS/$uid/$EXPENSES/$expenseId"

    fun userBudgetPath(uid: String): String = "$USERS/$uid/$BUDGET"
    fun userBudgetDocPath(uid: String, budgetId: String): String = "$USERS/$uid/$BUDGET/$budgetId"

    fun userDietPlansPath(uid: String): String = "$USERS/$uid/$DIET_PLANS"
    fun userDietPlanDocPath(uid: String, dietPlanId: String): String = "$USERS/$uid/$DIET_PLANS/$dietPlanId"

    fun userHealthReportsPath(uid: String): String = "$USERS/$uid/$HEALTH_REPORTS"
    fun userHealthReportDocPath(uid: String, healthReportId: String): String = "$USERS/$uid/$HEALTH_REPORTS/$healthReportId"
}
