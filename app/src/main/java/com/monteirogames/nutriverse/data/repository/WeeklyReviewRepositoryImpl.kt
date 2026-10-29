package com.monteirogames.nutriverse.data.repository

import com.monteirogames.nutriverse.data.local.dao.WeeklyReviewDao
import com.monteirogames.nutriverse.data.local.entity.toDomain
import com.monteirogames.nutriverse.data.local.entity.toEntity
import com.monteirogames.nutriverse.domain.model.WeeklyReview
import com.monteirogames.nutriverse.domain.repository.WeeklyReviewRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class WeeklyReviewRepositoryImpl(
    private val weeklyReviewDao: WeeklyReviewDao
) : WeeklyReviewRepository {

    override suspend fun saveReview(review: WeeklyReview) = withContext(Dispatchers.IO) {
        weeklyReviewDao.insertReview(review.toEntity())
    }

    override suspend fun getReviewForPeriod(
        userId: String,
        startDate: String,
        endDate: String
    ): WeeklyReview? = withContext(Dispatchers.IO) {
        weeklyReviewDao.getReviewForPeriod(userId, startDate, endDate)?.toDomain()
    }

    override fun getLatestReview(userId: String): Flow<WeeklyReview?> {
        return weeklyReviewDao.getLatestReview(userId).map { it?.toDomain() }
    }

    override fun getAllReviews(userId: String): Flow<List<WeeklyReview>> {
        return weeklyReviewDao.getAllReviews(userId).map { list -> list.map { it.toDomain() } }
    }

    override fun getReviewById(id: String): Flow<WeeklyReview?> {
        return weeklyReviewDao.getReviewById(id).map { it?.toDomain() }
    }

    override suspend fun deleteReviewById(id: String) = withContext(Dispatchers.IO) {
        weeklyReviewDao.deleteReviewById(id)
    }
}
