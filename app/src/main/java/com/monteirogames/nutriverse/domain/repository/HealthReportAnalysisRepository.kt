package com.monteirogames.nutriverse.domain.repository

import android.graphics.Bitmap
import com.monteirogames.nutriverse.domain.model.HealthReport
import com.monteirogames.nutriverse.domain.model.UserProfile

interface HealthReportAnalysisRepository {
    suspend fun analyzeReportImage(
        bitmap: Bitmap,
        userId: String,
        userProfile: UserProfile?
    ): Result<HealthReport>
}
