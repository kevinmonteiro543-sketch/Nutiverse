package com.monteirogames.nutriverse.domain.repository

import com.monteirogames.nutriverse.domain.model.WeeklyReviewAiResponse
import com.monteirogames.nutriverse.domain.model.WeeklyReviewMetrics

interface WeeklyReviewAnalysisRepository {
    suspend fun generateAiReview(metrics: WeeklyReviewMetrics): Result<WeeklyReviewAiResponse>
}
