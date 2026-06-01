package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface AdDao {
    @Query("SELECT * FROM ads")
    fun getAllAdsFlow(): Flow<List<AdEntity>>

    @Query("SELECT * FROM ads")
    suspend fun getAllAds(): List<AdEntity>

    @Query("SELECT * FROM ads WHERE id = :id LIMIT 1")
    fun getAdByIdFlow(id: String): Flow<AdEntity?>

    @Query("SELECT * FROM ads WHERE id = :id LIMIT 1")
    suspend fun getAdById(id: String): AdEntity?

    @Query("SELECT * FROM ads WHERE channel = :channel")
    fun getAdsByChannelFlow(channel: String): Flow<List<AdEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAds(ads: List<AdEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAd(ad: AdEntity)

    @Update
    suspend fun updateAd(ad: AdEntity)

    // Analytics operations
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAnalyticsEvent(event: AnalyticsEventEntity)

    @Query("SELECT * FROM analytics_events ORDER BY timestamp DESC LIMIT 50")
    fun getAllAnalyticsEventsFlow(): Flow<List<AnalyticsEventEntity>>

    @Query("SELECT * FROM comments WHERE adId = :adId ORDER BY createdAt DESC, id DESC")
    fun getCommentsByAdIdFlow(adId: String): Flow<List<CommentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertComment(comment: CommentEntity)

    @Query("SELECT COUNT(*) FROM analytics_events WHERE eventType = 'view'")
    fun getExposureCountFlow(): Flow<Int>

    @Query("SELECT COUNT(*) FROM analytics_events WHERE eventType = 'click'")
    fun getClickCountFlow(): Flow<Int>

    @Query("SELECT COUNT(*) FROM analytics_events WHERE eventType IN ('like', 'favorite', 'share')")
    fun getInteractionCountFlow(): Flow<Int>

    @Query("DELETE FROM analytics_events")
    suspend fun clearAllAnalytics()

    @Query("DELETE FROM ads")
    suspend fun clearAllAds()

    @Query("DELETE FROM comments")
    suspend fun clearAllComments()
}
