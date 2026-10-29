package com.monteirogames.nutriverse.domain.model

import org.json.JSONArray
import org.json.JSONObject

object WeeklyReviewAnalysisParser {

    fun parse(jsonString: String): Result<WeeklyReviewAiResponse> {
        return try {
            val cleanJson = sanitizeJson(jsonString)
            val jsonObject = JSONObject(cleanJson)

            val summary = jsonObject.optString("summary").trim().ifEmpty {
                "Here is your weekly nutrition coaching summary based on your tracked days."
            }

            val parseStringList = { key: String, defaultItems: List<String> ->
                val list = mutableListOf<String>()
                val array = jsonObject.optJSONArray(key)
                if (array != null) {
                    for (i in 0 until array.length()) {
                        val item = array.optString(i).trim()
                        if (item.isNotEmpty()) {
                            list.add(item)
                        }
                    }
                }
                if (list.isEmpty()) defaultItems else list
            }

            val strengths = parseStringList(
                "strengths",
                listOf("Maintained consistent tracking during active days.", "Logged balanced meals.")
            )

            val improvements = parseStringList(
                "improvements",
                listOf("Focus on logging meals across more days of the week.", "Aim for closer alignment with daily targets.")
            )

            val nutritionInsights = parseStringList(
                "nutritionInsights",
                listOf("Calorie and macronutrient intake reflected steady tracking.", "Prioritizing protein helps sustain daily energy.")
            )

            val recommendations = parseStringList(
                "recommendations",
                listOf("Plan meals ahead to maintain tracking momentum.", "Keep hydration and wholesome snacks accessible.")
            )

            val nextWeekFocus = parseStringList(
                "nextWeekFocus",
                listOf("Build consistency across more days next week.")
            )

            Result.success(
                WeeklyReviewAiResponse(
                    summary = summary,
                    strengths = strengths,
                    improvements = improvements,
                    nutritionInsights = nutritionInsights,
                    recommendations = recommendations,
                    nextWeekFocus = nextWeekFocus
                )
            )
        } catch (e: Exception) {
            Result.failure(Exception("Failed to parse AI review response: ${e.message}", e))
        }
    }

    fun sanitizeJson(raw: String): String {
        var trimmed = raw.trim()
        if (trimmed.startsWith("```json")) {
            trimmed = trimmed.removePrefix("```json")
        } else if (trimmed.startsWith("```")) {
            trimmed = trimmed.removePrefix("```")
        }
        if (trimmed.endsWith("```")) {
            trimmed = trimmed.removeSuffix("```")
        }
        return trimmed.trim()
    }
}
