package com.monteirogames.nutriverse.data.repository

import android.graphics.Bitmap
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import com.monteirogames.nutriverse.domain.model.HealthReport
import com.monteirogames.nutriverse.domain.model.HealthReportAnalysisParser
import com.monteirogames.nutriverse.domain.model.UserProfile
import com.monteirogames.nutriverse.domain.repository.HealthReportAnalysisRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.max

class HealthReportAnalysisRepositoryImpl : HealthReportAnalysisRepository {

    override suspend fun analyzeReportImage(
        bitmap: Bitmap,
        userId: String,
        userProfile: UserProfile?
    ): Result<HealthReport> = withContext(Dispatchers.IO) {
        try {
            val resizedBitmap = prepareImageForAnalysis(bitmap, maxDimension = 1600)

            val model = Firebase.ai.generativeModel(
                modelName = "gemini-3.7-flash",
                generationConfig = generationConfig {
                    responseMimeType = "application/json"
                }
            )

            val profileContext = if (userProfile != null) {
                """
                USER CONTEXT (Informational reference for standard age/sex biological variance only):
                - Age: ${userProfile.age}
                - Sex/Gender: ${userProfile.gender}
                - Height: ${userProfile.heightCm} cm
                - Weight: ${userProfile.weightKg} kg
                - Dietary Preference: ${userProfile.dietaryPreference.ifBlank { "None" }}
                - Activity Level: ${userProfile.activityLevel.ifBlank { "Standard" }}
                """.trimIndent()
            } else {
                "USER CONTEXT: None provided."
            }

            val prompt = """
                You are an objective laboratory and health report reader. Your task is to extract test names, measured values, units, and reference ranges explicitly visible in the provided image of a health/lab report.

                $profileContext

                CRITICAL NON-DIAGNOSTIC SAFETY RULES:
                1. INFORMATIONAL ONLY: You must NOT diagnose diseases. You must NOT predict disease. You must NOT prescribe or adjust any medication or treatment.
                2. EXPLICIT EXTRACTION ONLY:
                   - Extract only test names, values, units, and reference ranges that are visible in the image.
                   - If a value cannot be clearly read, set "value": null and "status": "UNKNOWN". Do NOT invent or estimate values.
                   - If a reference range is not visible on the report, set "referenceRange": null. Do NOT invent a standard range.
                   - If the image is blurry, cut off, or illegible, state this clearly in "summary" and recommend checking the original physical report.
                3. STATUS CLASSIFICATION:
                   - Compare visible values strictly against the reference range shown on the report.
                   - "status" must be strictly one of: "LOW", "NORMAL", "HIGH", "UNKNOWN".
                   - Use neutral language: e.g., "Listed as above the reference range shown on the report" rather than "You suffer from condition X".
                4. QUESTIONS FOR DOCTOR:
                   - Provide 2-4 respectful, constructive questions the user may bring to their qualified physician or healthcare provider.

                Output ONLY valid JSON matching this exact structure:
                {
                  "reportType": "Complete Blood Count / Metabolic Panel / Lipid Panel / etc.",
                  "summary": "Objective, neutral overview of extracted report items.",
                  "results": [
                    {
                      "testName": "e.g. Hemoglobin",
                      "value": "14.2",
                      "unit": "g/dL",
                      "referenceRange": "13.8 - 17.2",
                      "interpretation": "Within the reference range specified on the report.",
                      "status": "NORMAL"
                    }
                  ],
                  "observations": [
                    "Key visible observations or tests noted as outside the printed reference range."
                  ],
                  "questionsForDoctor": [
                    "Questions to discuss with a healthcare professional."
                  ],
                  "disclaimer": "Informational only. This analysis is not a medical diagnosis or medical advice. Discuss your report and any concerns with a qualified healthcare professional."
                }
            """.trimIndent()

            val response = kotlinx.coroutines.withTimeout(60_000L) {
                model.generateContent(
                    content {
                        image(resizedBitmap)
                        text(prompt)
                    }
                )
            }

            val responseText = response.text
            if (responseText.isNullOrBlank()) {
                Result.failure(Exception(com.monteirogames.nutriverse.util.AiErrorMapper.MSG_EMPTY_RESPONSE))
            } else {
                HealthReportAnalysisParser.parse(
                    jsonString = responseText,
                    userId = userId
                )
            }
        } catch (e: kotlinx.coroutines.CancellationException) {
            if (e is kotlinx.coroutines.TimeoutCancellationException) {
                Result.failure(Exception(com.monteirogames.nutriverse.util.AiErrorMapper.MSG_TIMEOUT, e))
            } else {
                throw e
            }
        } catch (e: Exception) {
            val userFriendlyMessage = com.monteirogames.nutriverse.util.AiErrorMapper.mapToUserMessage(e, isImageAnalysis = true)
            Result.failure(Exception(userFriendlyMessage, e))
        }
    }

    private fun prepareImageForAnalysis(original: Bitmap, maxDimension: Int): Bitmap {
        val width = original.width
        val height = original.height
        if (width <= maxDimension && height <= maxDimension) {
            return original
        }

        val ratio = width.toFloat() / height.toFloat()
        val newWidth: Int
        val newHeight: Int

        if (width > height) {
            newWidth = maxDimension
            newHeight = max(1, (maxDimension / ratio).toInt())
        } else {
            newHeight = maxDimension
            newWidth = max(1, (maxDimension * ratio).toInt())
        }

        return Bitmap.createScaledBitmap(original, newWidth, newHeight, true)
    }
}
