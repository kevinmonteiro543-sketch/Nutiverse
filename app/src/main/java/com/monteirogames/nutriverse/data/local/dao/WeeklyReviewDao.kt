package com.monteirogames.nutriverse.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.monteirogames.nutriverse.data.local.entity.WeeklyReviewEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WeeklyReviewDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReview(review: WeeklyReviewEntity)

    @Query("SELECT * FROM weekly_review WHERE userId = :userId AND startDate = :startDate AND endDate = :endDate LIMIT 1")
    suspend fun getReviewForPeriod(userId: String, startDate: String, endDate: String): WeeklyReviewEntity?

    @Query("SELECT * FROM weekly_review WHERE userId = :userId ORDER BY generatedAt DESC LIMIT 1")
    fun getLatestReview(userId: String): Flow<WeeklyReviewEntity?>

    @Query("SELECT * FROM weekly_review WHERE userId = :userId ORDER BY generatedAt DESC")
    fun getAllReviews(userId: String): Flow<List<WeeklyReviewEntity>>

    @Query("SELECT * FROM weekly_review WHERE id = :id LIMIT 1")
    fun getReviewById(id: String): Flow<WeeklyReviewEntity?>

    @Query("DELETE FROM weekly_review WHERE id = :id")
    suspend fun deleteReviewById(id: String)

    @Query("DELETE FROM weekly_review WHERE userId = :userId")
    suspend fun deleteReviewsForUser(userId: String)
}
