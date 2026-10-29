package com.monteirogames.nutriverse.domain.repository

import com.monteirogames.nutriverse.domain.model.WeeklyReview
import kotlinx.coroutines.flow.Flow

interface WeeklyReviewRepository {
    suspend fun saveReview(review: WeeklyReview)
    suspend fun getReviewForPeriod(userId: String, startDate: String, endDate: String): WeeklyReview?
    fun getLatestReview(userId: String): Flow<WeeklyReview?>
    fun getAllReviews(userId: String): Flow<List<WeeklyReview>>
    fun getReviewById(id: String): Flow<WeeklyReview?>
    suspend fun deleteReviewById(id: String)
}
