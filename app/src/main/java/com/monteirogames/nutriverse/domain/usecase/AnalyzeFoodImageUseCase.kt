package com.monteirogames.nutriverse.domain.usecase

import android.graphics.Bitmap
import com.monteirogames.nutriverse.domain.model.StructuredFoodEstimate
import com.monteirogames.nutriverse.domain.repository.AiFoodRecognitionRepository

class AnalyzeFoodImageUseCase(
    private val repository: AiFoodRecognitionRepository
) {
    suspend operator fun invoke(bitmap: Bitmap): Result<StructuredFoodEstimate> {
        return repository.analyzeFoodImage(bitmap)
    }
}
