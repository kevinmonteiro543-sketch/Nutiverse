package com.monteirogames.nutriverse.data.repository

import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import com.monteirogames.nutriverse.domain.model.WeeklyReviewAiResponse
import com.monteirogames.nutriverse.domain.model.WeeklyReviewAnalysisParser
import com.monteirogames.nutriverse.domain.model.WeeklyReviewMetrics
import com.monteirogames.nutriverse.domain.repository.WeeklyReviewAnalysisRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

class WeeklyReviewAnalysisRepositoryImpl : WeeklyReviewAnalysisRepository {

    override suspend fun generateAiReview(
        metrics: WeeklyReviewMetrics
    ): Result<WeeklyReviewAiResponse> = withContext(Dispatchers.IO) {
        try {
            val model = Firebase.ai.generativeModel(
                modelName = "gemini-3.7-flash",
                generationConfig = generationConfig {
                    responseMimeType = "application/json"
                }
            )

            // Strictly anonymized payload with NO user identity or private credentials
            val prompt = """
                You are an expert, encouraging clinical nutrition coach. Analyze the user's completed weekly nutrition metrics and generate an actionable, motivating, and realistic weekly nutrition review.

                REVIEW PERIOD & TRACKING CONSISTENCY:
                - Calendar Period: ${metrics.displayRange} (${metrics.startDate} to ${metrics.endDate})
                - Tracked Days: ${metrics.trackedDays} of ${metrics.totalDays} calendar days (${metrics.trackingConsistency.roundToInt()}% consistency)
                - Total Logged Meals: ${metrics.totalMeals}

                NUTRITION INTAKE (Calculated across tracked days only):
                - Daily Average Calories: ${metrics.averageCalories.roundToInt()} kcal (Configured Target: ${metrics.targetCalories.roundToInt()} kcal, Adherence: ${metrics.calorieAdherence.roundToInt()}%)
                - Daily Average Protein: ${metrics.averageProtein.roundToInt()}g (Configured Target: ${metrics.targetProtein.roundToInt()}g, Adherence: ${metrics.proteinAdherence.roundToInt()}%)
                - Daily Average Carbs: ${metrics.averageCarbs.roundToInt()}g (Configured Target: ${metrics.targetCarbs.roundToInt()}g)
                - Daily Average Fat: ${metrics.averageFat.roundToInt()}g (Configured Target: ${metrics.targetFat.roundToInt()}g)
                ${if (metrics.highestCalorieDay != null) "- Highest Intake Day: ${metrics.highestCalorieDay}" else ""}
                ${if (metrics.lowestCalorieDay != null) "- Lowest Intake Day: ${metrics.lowestCalorieDay}" else ""}

                CRITICAL COACHING & SAFETY GUIDELINES:
                1. STRICTLY NON-DIAGNOSTIC: You must NOT diagnose any disease, illness, or medical condition. You must NOT prescribe medication or medical therapies.
                2. NO HARMFUL ADVICE: Do NOT recommend starvation, extreme caloric restriction, or cutting entire food groups.
                3. HONEST DATA ACKNOWLEDGMENT:
                   - Untracked days are simply unlogged days. Do NOT assume the user starved or binged on days with no logs.
                   - Celebrate actual tracking consistency (${metrics.trackedDays}/${metrics.totalDays} days).
                4. TONE: Constructive, positive, empathetic, and evidence-based.
                5. LENGTH: Concise and punchy. Avoid walls of text.

                Output ONLY valid raw JSON with this exact schema:
                {
                  "summary": "1 to 3 sentences synthesizing the week's overall nutritional story and progress.",
                  "strengths": [
                    "2 to 4 specific positive nutritional achievements or consistency highlights from the week."
                  ],
                  "improvements": [
                    "2 to 4 gentle, realistic areas where nutrition or logging can improve."
                  ],
                  "nutritionInsights": [
                    "2 to 4 educational insights on their calorie balance, protein pacing, or macro distribution."
                  ],
                  "recommendations": [
                    "2 to 4 practical, actionable food, meal-prep, or hydration recommendations for the upcoming week."
                  ],
                  "nextWeekFocus": [
                    "1 to 3 single-sentence key focus targets for the next 7 days."
                  ]
                }
            """.trimIndent()

            val response = model.generateContent(
                content {
                    text(prompt)
                }
            )

            val text = response.text
            if (text.isNullOrBlank()) {
                Result.failure(Exception("AI coach returned an empty response. Please try again."))
            } else {
                WeeklyReviewAnalysisParser.parse(text)
            }
        } catch (e: Exception) {
            val friendlyMsg = when {
                e.message?.contains("429", ignoreCase = true) == true || e.message?.contains("quota", ignoreCase = true) == true ->
                    "AI service limit reached. Please try again in a few moments."
                e.message?.contains("network", ignoreCase = true) == true || e.message?.contains("connect", ignoreCase = true) == true ->
                    "Network error. Please check your internet connection and try again."
                else ->
                    "Unable to generate weekly AI review: ${e.localizedMessage ?: "Unexpected error"}. Please try again."
            }
            Result.failure(Exception(friendlyMsg, e))
        }
    }
}
