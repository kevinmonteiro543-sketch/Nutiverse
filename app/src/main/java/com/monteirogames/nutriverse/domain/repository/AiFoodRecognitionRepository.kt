package com.monteirogames.nutriverse.domain.repository

import android.graphics.Bitmap
import com.monteirogames.nutriverse.domain.model.StructuredFoodEstimate

interface AiFoodRecognitionRepository {
    suspend fun analyzeFoodImage(bitmap: Bitmap): Result<StructuredFoodEstimate>
}
