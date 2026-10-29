package com.monteirogames.nutriverse.domain.usecase

import com.monteirogames.nutriverse.domain.model.MealEntry
import com.monteirogames.nutriverse.domain.repository.MealRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class AddMealUseCase(private val repository: MealRepository) {
    suspend operator fun invoke(mealEntry: MealEntry) {
        repository.saveFoodItem(mealEntry.foodItem)
        repository.saveMealEntry(mealEntry)
    }
}

class UpdateMealUseCase(private val repository: MealRepository) {
    suspend operator fun invoke(mealEntry: MealEntry) {
        repository.saveFoodItem(mealEntry.foodItem)
        repository.updateMealEntry(mealEntry)
    }
}

class DeleteMealUseCase(private val repository: MealRepository) {
    suspend operator fun invoke(mealEntry: MealEntry) {
        repository.deleteMealEntry(mealEntry)
    }
}

class GetMealEntryUseCase(private val repository: MealRepository) {
    suspend operator fun invoke(id: String, userId: String): MealEntry? {
        return repository.getMealEntryById(id, userId)
    }
}

class ObserveTodayMealsUseCase(private val repository: MealRepository) {
    operator fun invoke(userId: String, timestamp: Long): Flow<List<MealEntry>> {
        return repository.getMealsForDate(userId, timestamp)
    }
}

data class DailyNutrition(
    val calories: Float,
    val protein: Float,
    val carbs: Float,
    val fat: Float
)

class CalculateDailyNutritionUseCase(private val repository: MealRepository) {
    operator fun invoke(userId: String, timestamp: Long): Flow<DailyNutrition> {
        return repository.getMealsForDate(userId, timestamp).map { meals ->
            meals.fold(DailyNutrition(0f, 0f, 0f, 0f)) { acc, meal ->
                DailyNutrition(
                    calories = acc.calories + meal.calculatedCalories,
                    protein = acc.protein + meal.calculatedProtein,
                    carbs = acc.carbs + meal.calculatedCarbs,
                    fat = acc.fat + meal.calculatedFat
                )
            }
        }
    }
}

data class MealTrackerUseCases(
    val addMeal: AddMealUseCase,
    val updateMeal: UpdateMealUseCase,
    val deleteMeal: DeleteMealUseCase,
    val getMeal: GetMealEntryUseCase,
    val observeTodayMeals: ObserveTodayMealsUseCase,
    val calculateDailyNutrition: CalculateDailyNutritionUseCase
)
