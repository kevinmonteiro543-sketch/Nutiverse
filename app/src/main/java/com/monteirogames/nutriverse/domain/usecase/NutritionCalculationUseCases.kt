package com.monteirogames.nutriverse.domain.usecase

import com.monteirogames.nutriverse.domain.model.UserProfile
import kotlin.math.max

data class NutritionTargets(
    val calories: Float,
    val protein: Float,
    val carbs: Float,
    val fat: Float
)

class CalculateNutritionTargetsUseCase {
    operator fun invoke(profile: UserProfile): NutritionTargets {
        val bmr = if (profile.gender.equals("Male", ignoreCase = true)) {
            10f * profile.weightKg + 6.25f * profile.heightCm - 5f * profile.age + 5f
        } else {
            10f * profile.weightKg + 6.25f * profile.heightCm - 5f * profile.age - 161f
        }

        val neatMultiplier = when (profile.activityLevel.lowercase()) {
            "sedentary" -> 1.15f
            "lightly active" -> 1.25f
            "moderately active" -> 1.40f
            "very active" -> 1.60f
            else -> 1.15f
        }

        val restingTdee = bmr * neatMultiplier

        val freqAvg = when (profile.workoutFrequency.lowercase()) {
            "0 days" -> 0f
            "1-3 days" -> 2f
            "4-5 days" -> 4.5f
            "6+ days" -> 6f
            else -> 0f
        }

        val netMet = when (profile.workoutIntensity.lowercase()) {
            "light" -> 3.0f
            "moderate" -> 4.0f
            "high" -> 6.0f
            "very high" -> 8.0f
            else -> 0f
        }

        val calsPerWorkout = profile.weightKg * netMet * 1f
        val dailyExerciseCals = (calsPerWorkout * freqAvg) / 7f

        val tdee = restingTdee + dailyExerciseCals

        val adjustedCals = when (profile.goal.lowercase()) {
            "lose weight" -> tdee * 0.8f
            "maintain weight" -> tdee
            "build muscle" -> tdee + 200f
            "gain weight" -> tdee + 300f
            else -> tdee
        }

        val floorCals = if (profile.gender.equals("Male", ignoreCase = true)) 1500f else 1200f
        val targetCalories = max(floorCals, adjustedCals)

        val proteinPerKg = when (profile.goal.lowercase()) {
            "lose weight" -> 2.2f
            "build muscle", "gain weight" -> 2.0f
            "maintain weight" -> 1.8f
            else -> 1.8f
        }
        val targetProtein = profile.weightKg * proteinPerKg

        val initialFatCals = targetCalories * 0.25f
        val initialFatGrams = initialFatCals / 9f
        val minFatGrams = profile.weightKg * 0.8f
        
        val targetFat = max(initialFatGrams, minFatGrams)
        val actualFatCals = targetFat * 9f

        val remainingCals = targetCalories - (targetProtein * 4f) - actualFatCals
        val targetCarbs = max(0f, remainingCals / 4f)

        return NutritionTargets(
            calories = targetCalories,
            protein = targetProtein,
            carbs = targetCarbs,
            fat = targetFat
        )
    }
}
