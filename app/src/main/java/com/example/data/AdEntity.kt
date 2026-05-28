package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.File

@Entity(tableName = "ads")
data class AdEntity(
    @PrimaryKey val id: String,
    val channel: String, // "精选", "电商", "本地"
    val cardType: String, // "big_image", "small_image", "video"
    val title: String,
    val description: String,
    val advertiserName: String,
    val coverUrl: String,
    val videoUrl: String,
    val summary: String,
    val tags: String, // Comma-separated tags, e.g. "运动,学生党,性价比"
    val likeCount: Int,
    val favoriteCount: Int,
    val shareCount: Int,
    val isLiked: Boolean = false,
    val isFavorited: Boolean = false,
    val isShared: Boolean = false,
    val impressions: Int = 0,
    val clicks: Int = 0,
    val localVideoPath: String? = null,
    val localCoverPath: String? = null
) {
    fun getTagList(): List<String> {
        return tags.split(",").map { it.trim() }.filter { it.isNotEmpty() }
    }

    fun coverModel(): Any = localCoverPath?.let(::File) ?: coverUrl

    fun videoSourcePath(): String = localVideoPath ?: videoUrl

    fun hasPlayableVideo(): Boolean = cardType == "video" && videoSourcePath().isNotBlank()
}

@Entity(tableName = "analytics_events")
data class AnalyticsEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val adId: String,
    val adTitle: String,
    val eventType: String, // "view", "click", "like", "favorite", "share"
    val timestamp: Long = System.currentTimeMillis(),
    val channel: String,
    val cardType: String
)
