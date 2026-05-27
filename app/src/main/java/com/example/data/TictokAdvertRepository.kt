package com.example.data

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream

class TictokAdvertRepository(private val adDao: AdDao) {
    companion object {
        private const val TAG = "TictokRepository"
    }

    // Flows for general use
    val allAdsFlow: Flow<List<AdEntity>> = adDao.getAllAdsFlow()

    fun getAdsByChannelFlow(channel: String): Flow<List<AdEntity>> = adDao.getAdsByChannelFlow(channel)

    fun getAdByIdFlow(id: String): Flow<AdEntity?> = adDao.getAdByIdFlow(id)

    // Stats aggregates
    val allEventsFlow: Flow<List<AnalyticsEventEntity>> = adDao.getAllAnalyticsEventsFlow()
    val exposureCountFlow: Flow<Int> = adDao.getExposureCountFlow()
    val clickCountFlow: Flow<Int> = adDao.getClickCountFlow()
    val interactionCountFlow: Flow<Int> = adDao.getInteractionCountFlow()

    /**
     * Toggles the like state of an ad and registers an interaction event if liked.
     */
    suspend fun toggleLike(adId: String) {
        val ad = adDao.getAdById(adId) ?: return
        val newLiked = !ad.isLiked
        val isLikeIncrement = if (newLiked) 1 else -1
        val updatedAd = ad.copy(
            isLiked = newLiked,
            likeCount = (ad.likeCount + isLikeIncrement).coerceAtLeast(0)
        )
        adDao.updateAd(updatedAd)

        if (newLiked) {
            adDao.insertAnalyticsEvent(
                AnalyticsEventEntity(
                    adId = adId,
                    adTitle = ad.title,
                    eventType = "like",
                    channel = ad.channel,
                    cardType = ad.cardType
                )
            )
        }
    }

    /**
     * Toggles the favorite state of an ad.
     */
    suspend fun toggleFavorite(adId: String) {
        val ad = adDao.getAdById(adId) ?: return
        val newFav = !ad.isFavorited
        val favIncr = if (newFav) 1 else -1
        val updatedAd = ad.copy(
            isFavorited = newFav,
            favoriteCount = (ad.favoriteCount + favIncr).coerceAtLeast(0)
        )
        adDao.updateAd(updatedAd)

        if (newFav) {
            adDao.insertAnalyticsEvent(
                AnalyticsEventEntity(
                    adId = adId,
                    adTitle = ad.title,
                    eventType = "favorite",
                    channel = ad.channel,
                    cardType = ad.cardType
                )
            )
        }
    }

    /**
     * Records a share event.
     */
    suspend fun recordShare(adId: String) {
        val ad = adDao.getAdById(adId) ?: return
        val updatedAd = ad.copy(
            shareCount = ad.shareCount + 1
        )
        adDao.updateAd(updatedAd)

        adDao.insertAnalyticsEvent(
            AnalyticsEventEntity(
                adId = adId,
                adTitle = ad.title,
                eventType = "share",
                channel = ad.channel,
                cardType = ad.cardType
            )
        )
    }

    /**
     * Standard ad exposure/view logging and count update.
     */
    suspend fun recordExposure(adId: String) {
        val ad = adDao.getAdById(adId) ?: return
        val updatedAd = ad.copy(
            impressions = ad.impressions + 1
        )
        adDao.updateAd(updatedAd)

        adDao.insertAnalyticsEvent(
            AnalyticsEventEntity(
                adId = adId,
                adTitle = ad.title,
                eventType = "view",
                channel = ad.channel,
                cardType = ad.cardType
            )
        )
        Log.d(TAG, "Recorded impression for ad: $adId, total=" + updatedAd.impressions)
    }

    /**
     * Standard ad click logging and count update.
     */
    suspend fun recordClick(adId: String) {
        val ad = adDao.getAdById(adId) ?: return
        val updatedAd = ad.copy(
            clicks = ad.clicks + 1
        )
        adDao.updateAd(updatedAd)

        adDao.insertAnalyticsEvent(
            AnalyticsEventEntity(
                adId = adId,
                adTitle = ad.title,
                eventType = "click",
                channel = ad.channel,
                cardType = ad.cardType
            )
        )
        Log.d(TAG, "Recorded click for ad: $adId, total=" + updatedAd.clicks)
    }

    /**
     * Clear statistics database logs and reset impressions/clicks on items to 0.
     */
    suspend fun resetStats() {
        adDao.clearAllAnalytics()
        val allAds = adDao.getAllAds()
        val resetList = allAds.map {
            it.copy(
                impressions = 0,
                clicks = 0,
                isLiked = false,
                isFavorited = false,
                isShared = false
            )
        }
        adDao.insertAds(resetList)
        Log.d(TAG, "All stats reset successfully.")
    }

    /**
     * Downloads a video from URL and saves it to internal storage for offline playback.
     */
    suspend fun downloadVideoLocally(context: Context, adId: String, url: String): String? = withContext(Dispatchers.IO) {
        if (url.isEmpty()) return@withContext null
        
        val fileName = "video_${adId}.mp4"
        val file = File(context.filesDir, fileName)
        
        if (file.exists() && file.length() > 1024) {
            return@withContext file.absolutePath
        }

        try {
            val client = OkHttpClient()
            val request = Request.Builder().url(url).build()
            val response = client.newCall(request).execute()
            
            if (response.isSuccessful) {
                response.body?.byteStream()?.use { input ->
                    FileOutputStream(file).use { output ->
                        input.copyTo(output)
                    }
                }
                return@withContext file.absolutePath
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to download video for $adId: ${e.message}")
        }
        null
    }

    suspend fun updateLocalVideoPath(adId: String, path: String) {
        val ad = adDao.getAdById(adId) ?: return
        adDao.updateAd(ad.copy(localVideoPath = path))
    }

    /**
     * Populate standard high-quality advertising data if empty in database.
     */
    suspend fun populateMockDataIfEmpty() {
        val allAds = adDao.getAllAds()
        val count = allAds.size
        // Trigger re-population if we don't have the full 100-item set yet
        if (count >= 100) {
            Log.d(TAG, "Database already populated with $count ads.")
            return
        }

        Log.d(TAG, "Populating database with 100 high-quality ads for offline storage...")
        
        adDao.clearAllAds()

        val channels = listOf("精选", "电商", "本地")
        val cardTypes = listOf("video", "big_image", "small_image")
        
        val tagsPool = listOf("运动", "学生党", "性价比", "本地生活", "通勤", "家居", "美味", "生活服务", "数码推荐", "高品质生活", "穿搭必备", "周末好去处", "心流推荐")
        
        val coverUrls = listOf(
            "https://images.unsplash.com/photo-1505740420928-5e560c06d30e?w=600&auto=format&fit=crop&q=80",
            "https://images.unsplash.com/photo-1542291026-7eec264c27ff?w=600&auto=format&fit=crop&q=80",
            "https://images.unsplash.com/photo-1511556532299-8f662fc26c06?w=600&auto=format&fit=crop&q=80",
            "https://images.unsplash.com/photo-1519735775438-e4b2f29b46e8?w=600&auto=format&fit=crop&q=80",
            "https://images.unsplash.com/photo-1523275335684-37898b6baf30?w=600&auto=format&fit=crop&q=80",
            "https://images.unsplash.com/photo-1555041469-a586c61ea9bc?w=600&auto=format&fit=crop&q=80",
            "https://images.unsplash.com/photo-1524758631624-e2822e304c36?w=600&auto=format&fit=crop&q=80",
            "https://images.unsplash.com/photo-1509042239860-f550ce710b93?w=600&auto=format&fit=crop&q=80",
            "https://images.unsplash.com/photo-1544025162-d76694265947?w=600&auto=format&fit=crop&q=80",
            "https://images.unsplash.com/photo-1511919884226-fd3cad34687c?w=600&auto=format&fit=crop&q=80"
        )

        val adItems = mutableListOf<AdEntity>()
        
        // Detailed templates to ensure precision matching and high-quality aesthetic
        val templates = listOf(
            Triple("🔥 幻影智控 极速碳板跑鞋", "专为竞速设计，毫秒级回弹，轻量透气。", listOf("运动", "性价比", "穿搭必备")),
            Triple("👟 飞跃巅峰 专业级专业篮球鞋", "缓震护踝，抓地力强，实战利器。", listOf("运动", "学生党", "性价比")),
            Triple("🏃 极光声学 运动蓝牙耳机", "狂甩不掉，IPX7防水，音质震撼。", listOf("运动", "数码推荐", "高品质生活")),
            Triple("🍲 蜀大侠 招牌红油火锅套餐", "正宗川味，同城速达，美味享不停。", listOf("美味", "本地生活", "周末好去处")),
            Triple("💻 未来代码 客制化机械键盘", "铝合金外壳，Gasket结构，打字如丝。", listOf("数码推荐", "通勤", "高品质生活")),
            Triple("🛋️ 云朵意式 极简云感沙发", "躺入云端的舒适，意式美学，持久承托。", listOf("家居", "高品质生活", "心流推荐")),
            Triple("🎒 匠心筑梦 复古手工皮包", "植鞣牛皮，老匠人缝制，经久耐用。", listOf("穿搭必备", "性价比", "家居")),
            Triple("☕ 槐树下咖啡 手冲精品豆", "产地直采，醇厚香气，治愈每一个午后。", listOf("美味", "本地生活", "心流推荐")),
            Triple("🚗 卡丁狂飙 极速竞技赛票", "释放压力，体验贴地飞行的快感。", listOf("本地生活", "周末好去处", "生活服务")),
            Triple("📱 乾坤数码 旗舰智能折叠屏", "超窄边框，灵动内外双屏，科技巅峰。", listOf("数码推荐", "高品质生活", "通勤")),
            Triple("🧤 户外探险 巅峰防风保暖手套", "触屏设计，纳米抓绒，极地御寒首选。", listOf("运动", "性价比", "生活服务")),
            Triple("⌚ 乾坤数码 全全能健康监控手表", "24h血氧监测，多运动模式，时尚商务感。", listOf("数码推荐", "高品质生活", "通勤")),
            Triple("🥩 暮光海岸 尊享澳洲谷饲熟成牛排", "排酸工艺，汁水饱满，法式浪漫晚餐。", listOf("美味", "本地生活", "高品质生活")),
            Triple("🧴 极简美学 天然萃取草本护手霜", "瞬间吸收，深度锁水，植物清香体验。", listOf("性价比", "学生党", "穿搭必备")),
            Triple("🎮 未来代码 旗舰电竞游戏鼠标", "轻量化设计，超高轮询率，毫秒响应延迟。", listOf("数码推荐", "高品质生活", "通勤")),
            Triple("🛶 自由行纪 秘境蓝湾双人皮划艇", "拥抱自然，绝美海景，专业安全教练指导。", listOf("周末好去处", "本地生活", "心流推荐")),
            Triple("🍶 古槐雅舍 匠心手工陶瓷茶具", "传统器形，高温釉下彩，慢生活茶道。", listOf("家居", "高品质生活", "心流推荐")),
            Triple("🎧 音浪声学 旗舰级头戴降噪耳机", "大师调音，40h超长续航，静谧视听。", listOf("数码推荐", "通勤", "高品质生活")),
            Triple("🍰 自由行纪 低卡法式红丝绒蛋糕", "无糖配方，天然乳脂，甜蜜无负负担。", listOf("美味", "本地生活", "周末好去处")),
            Triple("🧗 阿尔卑斯 专业级高山攀岩绳", "高强度耐磨，安全认证，挑战极限巅峰。", listOf("运动", "高品质生活", "生活服务"))
        )

        // Using high-speed robust video assets
        val videoLibrary = listOf(
            "https://www.w3schools.com/html/mov_bbb.mp4",
            "https://www.w3schools.com/html/movie.mp4"
        )

        for (i in 1..100) {
            val template = templates[(i - 1) % templates.size]
            val channel = when {
                template.third.contains("美味") || template.third.contains("本地生活") -> "本地"
                template.third.contains("运动") || template.third.contains("数码推荐") -> "电商"
                else -> channels[i % channels.size]
            }
            
            // Assign video based on content type for better matching
            val videoUrl = if (i % 3 == 0) {
                // Alternating video URLs for variety among the 100 ads
                videoLibrary[i % videoLibrary.size]
            } else ""

            val ad = AdEntity(
                id = "ad_${i.toString().padStart(3, '0')}",
                channel = channel,
                cardType = if (videoUrl.isNotEmpty()) "video" else cardTypes[i % cardTypes.size],
                title = template.first, // Removed the "第N款" suffix
                description = "${template.second} 这是针对${template.first}品牌推出的精品系列，融合了卓越的设计与极致的性能体验。",
                advertiserName = "品牌商 ${template.first.split(" ")[1]}",
                coverUrl = coverUrls[i % coverUrls.size],
                videoUrl = videoUrl,
                summary = "AI 推荐理由：这款产品具备 ${template.third.joinToString("、")} 等多个核心卖点，值得体验。",
                tags = template.third.joinToString(","),
                likeCount = (1000..5000).random(),
                favoriteCount = (500..2000).random(),
                shareCount = (100..500).random(),
                impressions = (10000..50000).random(),
                clicks = (1000..5000).random()
            )
            adItems.add(ad)
        }

        adDao.insertAds(adItems)
        Log.d(TAG, "100 Ads successfully preloaded to Room db.")
    }
}
