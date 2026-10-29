package com.monteirogames.nutriverse.data.repository

import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import com.monteirogames.nutriverse.data.local.dao.DietPlanDao
import com.monteirogames.nutriverse.data.local.entity.DietPlanEntity
import com.monteirogames.nutriverse.data.local.entity.DietPlanMealEntity
import com.monteirogames.nutriverse.domain.model.DayPlan
import com.monteirogames.nutriverse.domain.model.DietPlan
import com.monteirogames.nutriverse.domain.model.DietPlanMeal
import com.monteirogames.nutriverse.domain.model.UserProfile
import com.monteirogames.nutriverse.domain.repository.DietPlannerRepository
import com.monteirogames.nutriverse.domain.usecase.NutritionTargets
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.util.UUID
import kotlin.math.roundToInt

class DietPlannerRepositoryImpl(
    private val dietPlanDao: DietPlanDao,
    private val syncTombstoneDao: com.monteirogames.nutriverse.data.local.dao.SyncTombstoneDao? = null
) : DietPlannerRepository {

    override suspend fun generate7DayPlan(
        userProfile: UserProfile,
        nutritionTargets: NutritionTargets
    ): Result<DietPlan> = withContext(Dispatchers.IO) {
        try {
            val model = Firebase.ai.generativeModel(
                modelName = "gemini-3.7-flash",
                generationConfig = generationConfig {
                    responseMimeType = "application/json"
                }
            )

            val prompt = """
                You are an expert clinical dietitian crafting a realistic, delicious 7-day meal plan tailored to the user's specific health goals and dietary restrictions.

                USER PROFILE:
                - Age: ${userProfile.age}
                - Gender: ${userProfile.gender}
                - Height: ${userProfile.heightCm} cm
                - Weight: ${userProfile.weightKg} kg
                - Goal: ${userProfile.goal}
                - Dietary Preference: ${if (userProfile.dietaryPreference.isNotBlank()) userProfile.dietaryPreference else "None"}
                - Allergies / Intolerances: ${if (userProfile.allergies.isNotBlank()) userProfile.allergies else "None"}
                - Activity Level: ${userProfile.activityLevel}
                - Workout Frequency: ${userProfile.workoutFrequency}
                - Workout Intensity: ${userProfile.workoutIntensity}

                CALCULATED DAILY NUTRITION TARGETS (Source of Truth):
                - Calories: ${nutritionTargets.calories.roundToInt()} kcal/day
                - Protein: ${nutritionTargets.protein.roundToInt()} g/day
                - Carbohydrates: ${nutritionTargets.carbs.roundToInt()} g/day
                - Fat: ${nutritionTargets.fat.roundToInt()} g/day

                STRICT DIETARY SAFETY RULES:
                1. STRICTLY AVOID all ingredients, cross-contaminants, and derivatives related to the user's allergies: "${userProfile.allergies}".
                2. STRICTLY RESPECT the user's dietary preference: "${userProfile.dietaryPreference}".
                3. The daily total of calories and macronutrients for EACH of the 7 days should reasonably approximate the daily targets within reasonable practical tolerance.
                4. For each day (Days 1 to 7), provide Breakfast, Lunch, Dinner, and an optional Snack.
                5. Keep meal descriptions concise and practical. Each meal must only include:
                   - mealType: "Breakfast", "Lunch", "Dinner", or "Snack"
                   - name: Concise descriptive meal name
                   - calories: numeric estimate
                   - protein: numeric estimate in grams
                   - carbs: numeric estimate in grams
                   - fat: numeric estimate in grams
                   - prepTip: 1-2 sentence preparation or storage tip
                6. Do NOT include medical claims, long stories, or extraneous text.

                Output ONLY valid raw JSON adhering to this exact schema:
                {
                  "days": [
                    {
                      "day": 1,
                      "meals": [
                        {
                          "mealType": "Breakfast",
                          "name": "Oatmeal with blueberries and chia seeds",
                          "calories": 420.0,
                          "protein": 18.0,
                          "carbs": 62.0,
                          "fat": 11.0,
                          "prepTip": "Cook rolled oats with water, fold in seeds and top with fresh blueberries."
                        }
                      ]
                    }
                  ]
                }
            """.trimIndent()

            val response = kotlinx.coroutines.withTimeout(60_000L) {
                model.generateContent(
                    content {
                        text(prompt)
                    }
                )
            }

            val responseText = response.text
            if (responseText.isNullOrBlank()) {
                return@withContext Result.failure(Exception(com.monteirogames.nutriverse.util.AiErrorMapper.MSG_EMPTY_RESPONSE))
            }

            val parsedPlan = parseDietPlanJson(
                jsonText = responseText,
                userId = userProfile.userId,
                targets = nutritionTargets
            )
            Result.success(parsedPlan)
        } catch (e: kotlinx.coroutines.CancellationException) {
            if (e is kotlinx.coroutines.TimeoutCancellationException) {
                Result.failure(Exception(com.monteirogames.nutriverse.util.AiErrorMapper.MSG_TIMEOUT, e))
            } else {
                throw e
            }
        } catch (e: Exception) {
            val userFriendlyError = com.monteirogames.nutriverse.util.AiErrorMapper.mapToUserMessage(e, isImageAnalysis = false)
            Result.failure(Exception(userFriendlyError, e))
        }
    }

    private fun parseDietPlanJson(
        jsonText: String,
        userId: String,
        targets: NutritionTargets
    ): DietPlan {
        val trimmed = jsonText.trim()
        val cleanJson = if (trimmed.startsWith("```")) {
            trimmed.substringAfter("\n").substringBeforeLast("```").trim()
        } else {
            trimmed
        }
        val root = JSONObject(cleanJson)
        val daysArray = root.optJSONArray("days")
            ?: throw IllegalArgumentException("Response missing 'days' list.")

        val dayPlans = mutableListOf<DayPlan>()
        val planId = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()

        for (i in 0 until daysArray.length()) {
            val dayObj = daysArray.optJSONObject(i) ?: continue
            val dayNumber = dayObj.optInt("day", i + 1)
            val mealsArray = dayObj.optJSONArray("meals") ?: continue

            val meals = mutableListOf<DietPlanMeal>()
            for (j in 0 until mealsArray.length()) {
                val mealObj = mealsArray.optJSONObject(j) ?: continue
                val mealType = mealObj.optString("mealType", "Meal").trim()
                val name = mealObj.optString("name", "Nutritious Meal").trim()
                val calories = mealObj.optDouble("calories", 0.0).toFloat().coerceAtLeast(0f)
                val protein = mealObj.optDouble("protein", 0.0).toFloat().coerceAtLeast(0f)
                val carbs = mealObj.optDouble("carbs", 0.0).toFloat().coerceAtLeast(0f)
                val fat = mealObj.optDouble("fat", 0.0).toFloat().coerceAtLeast(0f)
                val prepTip = mealObj.optString("prepTip", "").trim()

                meals.add(
                    DietPlanMeal(
                        id = UUID.randomUUID().toString(),
                        dayNumber = dayNumber,
                        mealType = mealType,
                        name = name,
                        calories = calories,
                        protein = protein,
                        carbs = carbs,
                        fat = fat,
                        prepTip = prepTip
                    )
                )
            }

            if (meals.isNotEmpty()) {
                dayPlans.add(DayPlan(dayNumber = dayNumber, meals = meals))
            }
        }

        if (dayPlans.isEmpty()) {
            throw IllegalArgumentException("AI returned no valid meal days in plan.")
        }

        dayPlans.sortBy { it.dayNumber }

        return DietPlan(
            id = planId,
            userId = userId,
            name = "7-Day AI Meal Plan",
            targetCalories = targets.calories,
            targetProtein = targets.protein,
            targetCarbs = targets.carbs,
            targetFat = targets.fat,
            createdAt = now,
            days = dayPlans,
            updatedAt = now
        )
    }

    override suspend fun saveDietPlan(plan: DietPlan) {
        withContext(Dispatchers.IO) {
            val timestamp = if (plan.updatedAt > 0L) plan.updatedAt else System.currentTimeMillis()
            val planEntity = DietPlanEntity(
                id = plan.id,
                userId = plan.userId,
                name = plan.name,
                targetCalories = plan.targetCalories,
                targetProtein = plan.targetProtein,
                targetCarbs = plan.targetCarbs,
                targetFat = plan.targetFat,
                createdAt = plan.createdAt,
                isActive = true,
                updatedAt = timestamp
            )

            val mealEntities = plan.days.flatMap { day ->
                day.meals.map { meal ->
                    DietPlanMealEntity(
                        id = meal.id,
                        planId = plan.id,
                        dayNumber = day.dayNumber,
                        mealType = meal.mealType,
                        name = meal.name,
                        calories = meal.calories,
                        protein = meal.protein,
                        carbs = meal.carbs,
                        fat = meal.fat,
                        prepTip = meal.prepTip
                    )
                }
            }

            dietPlanDao.saveActivePlan(planEntity, mealEntities)
            syncTombstoneDao?.deleteTombstone("dietPlan", plan.id)
        }
    }

    override fun getActiveDietPlan(userId: String): Flow<DietPlan?> {
        return dietPlanDao.getActivePlanWithMeals(userId).map { relation ->
            relation?.toDomain()
        }
    }

    override suspend fun getActiveDietPlanOnce(userId: String): DietPlan? = withContext(Dispatchers.IO) {
        dietPlanDao.getActivePlanWithMealsOnce(userId)?.toDomain()
    }

    override fun getAllDietPlans(userId: String): Flow<List<DietPlan>> {
        return dietPlanDao.getAllPlansWithMeals(userId).map { list ->
            list.map { it.toDomain() }
        }
    }

    override suspend fun getAllDietPlansList(userId: String): List<DietPlan> = withContext(Dispatchers.IO) {
        dietPlanDao.getAllPlansWithMealsList(userId).map { it.toDomain() }
    }

    override suspend fun getDietPlanById(planId: String, userId: String): DietPlan? = withContext(Dispatchers.IO) {
        val rel = dietPlanDao.getPlanWithMealsById(planId)
        if (rel != null && rel.plan.userId == userId) rel.toDomain() else null
    }

    override suspend fun deleteDietPlan(planId: String, userId: String) {
        withContext(Dispatchers.IO) {
            dietPlanDao.deletePlanById(planId, userId)
            if (userId.isNotBlank()) {
                syncTombstoneDao?.insertTombstone(
                    com.monteirogames.nutriverse.data.local.entity.SyncTombstoneEntity(
                        id = planId,
                        entityType = "dietPlan",
                        userId = userId,
                        deletedAt = System.currentTimeMillis()
                    )
                )
            }
        }
    }

    override suspend fun clearDietPlan(userId: String) {
        withContext(Dispatchers.IO) {
            val existing = dietPlanDao.getAllPlansWithMealsList(userId)
            val now = System.currentTimeMillis()
            for (p in existing) {
                syncTombstoneDao?.insertTombstone(
                    com.monteirogames.nutriverse.data.local.entity.SyncTombstoneEntity(
                        id = p.plan.id,
                        entityType = "dietPlan",
                        userId = userId,
                        deletedAt = now
                    )
                )
            }
            dietPlanDao.clearPlansForUser(userId)
        }
    }

    private fun com.monteirogames.nutriverse.data.local.dao.DietPlanWithMeals.toDomain(): DietPlan {
        val planEntity = plan
        val mealsByDay = meals
            .groupBy { it.dayNumber }
            .toSortedMap()
            .map { (dayNumber, mealEntities) ->
                DayPlan(
                    dayNumber = dayNumber,
                    meals = mealEntities.map { me ->
                        DietPlanMeal(
                            id = me.id,
                            dayNumber = me.dayNumber,
                            mealType = me.mealType,
                            name = me.name,
                            calories = me.calories,
                            protein = me.protein,
                            carbs = me.carbs,
                            fat = me.fat,
                            prepTip = me.prepTip
                        )
                    }
                )
            }

        return DietPlan(
            id = planEntity.id,
            userId = planEntity.userId,
            name = planEntity.name,
            targetCalories = planEntity.targetCalories,
            targetProtein = planEntity.targetProtein,
            targetCarbs = planEntity.targetCarbs,
            targetFat = planEntity.targetFat,
            createdAt = planEntity.createdAt,
            days = mealsByDay,
            updatedAt = if (planEntity.updatedAt > 0L) planEntity.updatedAt else planEntity.createdAt
        )
    }
}
