package com.monteirogames.nutriverse.data.repository

import android.graphics.Bitmap
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.generationConfig
import com.monteirogames.nutriverse.domain.model.StructuredFoodEstimate
import com.monteirogames.nutriverse.domain.repository.AiFoodRecognitionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

class AiFoodRecognitionRepositoryImpl : AiFoodRecognitionRepository {

    override suspend fun analyzeFoodImage(bitmap: Bitmap): Result<StructuredFoodEstimate> = withContext(Dispatchers.IO) {
        try {
            val model = Firebase.ai.generativeModel(
                modelName = "gemini-3.7-flash",
                generationConfig = generationConfig {
                    responseMimeType = "application/json"
                    // Temporarily simplifying schema to basic text mode until schema API resolves properly
                }
            )

            val prompt = """
                Analyze this food image and return a JSON object with this exact structure:
                {
                    "name": "Name of the food",
                    "servingDescription": "Optional description like '1 bowl'",
                    "servingQuantity": 1.0,
                    "calories": 250.0,
                    "protein": 10.0,
                    "carbs": 30.0,
                    "fat": 15.0,
                    "confidenceScore": 0.95,
                    "notes": "Optional notes"
                }
            """.trimIndent()
            
            val response = model.generateContent(
                content {
                    image(bitmap)
                    text(prompt)
                }
            )

            val responseText = response.text
            if (responseText != null) {
                val json = JSONObject(responseText)
                val estimate = StructuredFoodEstimate(
                    name = json.optString("name", "Unknown Food"),
                    servingDescription = if (json.has("servingDescription")) json.optString("servingDescription") else null,
                    servingQuantity = if (json.has("servingQuantity")) json.optDouble("servingQuantity").toFloat() else null,
                    calories = if (json.has("calories")) json.optDouble("calories").toFloat() else null,
                    protein = if (json.has("protein")) json.optDouble("protein").toFloat() else null,
                    carbs = if (json.has("carbs")) json.optDouble("carbs").toFloat() else null,
                    fat = if (json.has("fat")) json.optDouble("fat").toFloat() else null,
                    confidenceScore = if (json.has("confidenceScore")) json.optDouble("confidenceScore").toFloat() else null,
                    notes = if (json.has("notes")) json.optString("notes") else null
                )
                Result.success(estimate)
            } else {
                Result.failure(Exception("AI returned empty response"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
