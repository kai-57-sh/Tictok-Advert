package com.example.ui

import android.widget.VideoView
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.draw.drawBehind
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.VolumeMute
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import com.example.data.AdEntity
import com.example.ui.theme.*
import java.io.File

@OptIn(ExperimentalAnimationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: AdvertViewModel,
    onNavigateToDetail: (String) -> Unit,
    onNavigateToSearch: () -> Unit,
    onNavigateToStats: () -> Unit
) {
    // Scroll states memory per channel for "返回位置保持" (retaining return scroll position)
    val listStates = remember {
        mapOf(
            "精选" to LazyListState(),
            "电商" to LazyListState(),
            "本地" to LazyListState()
        )
    }
    val activeState = listStates[viewModel.selectedChannel] ?: rememberLazyListState()

    // Controlled active playing ad ID in the feed
    var playingAdId by remember { mutableStateOf<String?>(null) }

    // Dynamic data flow collection for real-time reactivity
    val allAdsState by viewModel.allAds.collectAsState()
    val allAdsList = allAdsState

    // 1. Dynamic active channels based on existing ad quantities (Requirement 2)
    // If a channel has 0 advertisement count, delete (hide) that channel label.
    val activeChannels = remember(allAdsList) {
        listOf("精选", "电商", "本地").filter { channel ->
            allAdsList.any { it.channel == channel }
        }
    }

    // Auto-fallback/cleanup: if selected channel ceases to exist, select first active channel
    LaunchedEffect(activeChannels) {
        if (activeChannels.isNotEmpty() && !activeChannels.contains(viewModel.selectedChannel)) {
            viewModel.selectChannel(activeChannels.first())
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(AccentNeonBlue),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Text(
                            text = "Tictok Advert",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp,
                                color = SlateTextPrimary
                            )
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = onNavigateToSearch,
                        modifier = Modifier
                            .testTag("search_icon_button")
                            .background(SlateCharcoal, CircleShape)
                            .size(38.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "对话式搜索",
                            tint = SlateTextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = onNavigateToStats,
                        modifier = Modifier
                            .testTag("stats_icon_button")
                            .background(SlateCharcoal, CircleShape)
                            .size(38.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.BarChart,
                            contentDescription = "统计看板",
                            tint = SlateTextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = SlateDark
                )
            )
        },
        containerColor = SlateDark
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Channel Tabs Header Indicator Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .drawBehind {
                        val strokeWidth = 1.dp.toPx()
                        val y = this.size.height - strokeWidth / 2
                        drawLine(
                            color = PolishBorder,
                            start = androidx.compose.ui.geometry.Offset(0f, y),
                            end = androidx.compose.ui.geometry.Offset(this.size.width, y),
                            strokeWidth = strokeWidth
                        )
                    }
            ) {
                activeChannels.forEach { channelName ->
                    val isSelected = viewModel.selectedChannel == channelName
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                viewModel.selectChannel(channelName)
                                playingAdId = null // Pause any active video
                            }
                            .drawBehind {
                                if (isSelected) {
                                    val strokeWidth = 2.dp.toPx()
                                    val y = this.size.height - strokeWidth / 2
                                    drawLine(
                                        color = AccentNeonBlue,
                                        start = androidx.compose.ui.geometry.Offset(0f, y),
                                        end = androidx.compose.ui.geometry.Offset(this.size.width, y),
                                        strokeWidth = strokeWidth
                                    )
                                }
                            }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = channelName,
                            style = TextStyle(
                                color = if (isSelected) AccentNeonBlue else SlateTextSecondary,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 14.sp
                            )
                        )
                    }
                }
            }

            // 2. Dynamic tag filter generation based on active channel's ads (Requirement 1 & 2)
            // - Any tag visible must have at least 3 advertisements in the current channel (Requirement 1)
            // - Any tag with 0 matching ads under the current channel is deleted (Requirement 2)
            val activeTagFilter = viewModel.activeFilters[viewModel.selectedChannel]
            val channelTags = remember(allAdsList, viewModel.selectedChannel) {
                val currentChannelAds = allAdsList.filter { it.channel == viewModel.selectedChannel }
                val tagCounts = currentChannelAds.flatMap { it.getTagList() }
                    .groupBy { it }
                    .mapValues { it.value.size }
                
                // "at least guarantee each tag has 3 advertisements" => filter for counts >= 3
                // Requirement 2: filter out "小图", "视频" keywords from UI display
                val forbiddenKeywords = listOf("小图", "视频")
                val keptOtherTags = tagCounts.filter { it.value >= 3 }
                    .keys
                    .filter { it !in forbiddenKeywords }
                    .toList()
                listOf("全部") + keptOtherTags
            }

            // Fallback clear if chosen tag filter is suddenly filtered out or removed
            LaunchedEffect(channelTags) {
                if (activeTagFilter != null && !channelTags.contains(activeTagFilter)) {
                    viewModel.clearTagFilter(viewModel.selectedChannel)
                }
            }

            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(channelTags) { tag ->
                    val isAll = tag == "全部"
                    val isSelected = if (isAll) {
                        activeTagFilter == null
                    } else {
                        activeTagFilter == tag
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                if (isAll) {
                                    viewModel.clearTagFilter(viewModel.selectedChannel)
                                } else {
                                    viewModel.toggleTagFilter(viewModel.selectedChannel, tag)
                                }
                            }
                            .background(
                                if (isSelected) PolishPillActive else SlateCharcoal
                            )
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            if (isSelected && !isAll) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = PolishTextOnActive,
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                            Text(
                                text = if (isAll) tag else "#$tag",
                                style = TextStyle(
                                    color = if (isSelected) PolishTextOnActive else SlateTextSecondary,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium
                                )
                            )
                        }
                    }
                }
            }

            // Simulated Pull Down Loading Banner
            AnimatedVisibility(
                visible = viewModel.isRefreshing,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                        .background(SlateCharcoal)
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CircularProgressIndicator(
                            color = AccentNeonBlue,
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                        Text(
                            text = "AI 正在重新加载匹配推荐...",
                            color = SlateTextSecondary,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            // Ads Flow Column
            val rawAds by viewModel.currentChannelAds.collectAsState()
            val filteredAds = remember(rawAds, activeTagFilter) {
                if (activeTagFilter == null) {
                    rawAds
                } else {
                    rawAds.filter { it.getTagList().contains(activeTagFilter) }
                }
            }

            if (filteredAds.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Info,
                            contentDescription = null,
                            tint = SlateTextSecondary,
                            modifier = Modifier.size(48.dp)
                        )
                        Text(
                            text = "该过滤标签暂未发现广告内容",
                            color = SlateTextSecondary,
                            fontSize = 14.sp
                        )
                        Button(
                            onClick = { viewModel.clearTagFilter(viewModel.selectedChannel) },
                            colors = ButtonDefaults.buttonColors(containerColor = AccentNeonBlue)
                        ) {
                            Text("返回显示全部", color = SlateDark, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f)
                        .testTag("ads_lazy_column"),
                    state = activeState,
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        // Quick manual refresh trigger banner
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    Brush.linearGradient(listOf(SlateCard, SlateCharcoal))
                                )
                                .clickable { viewModel.refreshContent() }
                                .padding(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = "刷新",
                                        tint = AccentNeonBlue
                                    )
                                    Column {
                                        Text(
                                            text = "换一批推荐？一键智能刷新",
                                            color = Color.White,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "根据曝光反馈，AI 自动调整展现权重",
                                            color = SlateTextSecondary,
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                    contentDescription = null,
                                    tint = SlateTextSecondary,
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }
                    }

                    items(filteredAds, key = { it.id }) { ad ->
                        // Automatically register Exposure when rendering card on viewport!
                        LaunchedEffect(ad.id) {
                            viewModel.onAdExposed(ad.id)
                        }

                        // Render distinctive card based on style
                        when (ad.cardType) {
                            "video" -> VideoAdCard(
                                ad = ad,
                                isPlayingInFeed = playingAdId == ad.id,
                                onPlayClick = {
                                    playingAdId = if (playingAdId == ad.id) null else ad.id
                                },
                                onCardClick = {
                                    viewModel.onAdClicked(ad.id)
                                    onNavigateToDetail(ad.id)
                                },
                                onLikeClick = { viewModel.toggleLike(ad.id) },
                                onFavClick = { viewModel.toggleFavorite(ad.id) },
                                onShareClick = { viewModel.shareAd(ad.id) }
                            )
                            "small_image" -> SmallImageAdCard(
                                ad = ad,
                                onCardClick = {
                                    viewModel.onAdClicked(ad.id)
                                    onNavigateToDetail(ad.id)
                                },
                                onLikeClick = { viewModel.toggleLike(ad.id) },
                                onFavClick = { viewModel.toggleFavorite(ad.id) },
                                onShareClick = { viewModel.shareAd(ad.id) }
                            )
                            else -> BigImageAdCard(
                                ad = ad,
                                onCardClick = {
                                    viewModel.onAdClicked(ad.id)
                                    onNavigateToDetail(ad.id)
                                },
                                onLikeClick = { viewModel.toggleLike(ad.id) },
                                onFavClick = { viewModel.toggleFavorite(ad.id) },
                                onShareClick = { viewModel.shareAd(ad.id) }
                            )
                        }
                    }

                    item {
                        // Spacing at the bottom of the feed list
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "— 已经到底部啦，点击顶部刷新获得新广告 —",
                                color = SlateTextSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

// ==================== Ad Cards Sub-Composables ====================

@Composable
fun BigImageAdCard(
    ad: AdEntity,
    onCardClick: () -> Unit,
    onLikeClick: () -> Unit,
    onFavClick: () -> Unit,
    onShareClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("ad_card_big_image_${ad.id}")
            .clickable { onCardClick() },
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, PolishBorder),
        colors = CardDefaults.cardColors(containerColor = SlateCard),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header Info
            CardHeader(advertiserName = ad.advertiserName, channel = ad.channel)

            // Main Cover Image
            AsyncImage(
                model = ad.localCoverPath?.let(::File) ?: ad.coverUrl,
                contentDescription = ad.title,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop
            )

            // Title, description and AI Summary
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = ad.title,
                    color = SlateTextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = ad.description,
                    color = SlateTextSecondary,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(8.dp))

                // AI Summary Box (with dynamic prompt feedback indicator)
                AISummaryBox(summary = ad.summary)

                Spacer(modifier = Modifier.height(10.dp))

                // Horizontal tag row
                TagsListRow(tags = ad.getTagList())

                Spacer(modifier = Modifier.height(12.dp))

                // Divider and Action bar
                HorizontalDivider(color = PolishBorder)
                Spacer(modifier = Modifier.height(8.dp))
                ActionRow(
                    ad = ad,
                    onLikeClick = onLikeClick,
                    onFavClick = onFavClick,
                    onShareClick = onShareClick
                )
            }
        }
    }
}

@Composable
fun SmallImageAdCard(
    ad: AdEntity,
    onCardClick: () -> Unit,
    onLikeClick: () -> Unit,
    onFavClick: () -> Unit,
    onShareClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("ad_card_small_image_${ad.id}")
            .clickable { onCardClick() },
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, PolishBorder),
        colors = CardDefaults.cardColors(containerColor = SlateCard)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Row Layout for card cover beside text
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Cover
                AsyncImage(
                    model = ad.localCoverPath?.let(::File) ?: ad.coverUrl,
                    contentDescription = ad.title,
                    modifier = Modifier
                        .size(90.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .border(1.dp, PolishBorder, RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Crop
                )

                // Description
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = ad.advertiserName,
                            color = AccentNeonBlue,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = ad.title,
                        color = SlateTextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = ad.description,
                        color = SlateTextSecondary,
                        fontSize = 12.sp,
                        maxLines = 2,
                        lineHeight = 16.sp,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            AISummaryBox(summary = ad.summary)
            Spacer(modifier = Modifier.height(8.dp))
            TagsListRow(tags = ad.getTagList())
            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = PolishBorder)
            Spacer(modifier = Modifier.height(8.dp))
            ActionRow(
                ad = ad,
                onLikeClick = onLikeClick,
                onFavClick = onFavClick,
                onShareClick = onShareClick
            )
        }
    }
}

@Composable
fun VideoAdCard(
    ad: AdEntity,
    isPlayingInFeed: Boolean,
    onPlayClick: () -> Unit,
    onCardClick: () -> Unit,
    onLikeClick: () -> Unit,
    onFavClick: () -> Unit,
    onShareClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("ad_card_video_${ad.id}"),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, PolishBorder),
        colors = CardDefaults.cardColors(containerColor = SlateCard)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            CardHeader(advertiserName = ad.advertiserName, channel = ad.channel, isVideo = true)

            // Video Player Area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp)
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                if (isPlayingInFeed) {
                    var isPreparing by remember { mutableStateOf(true) }
                    Box(modifier = Modifier.fillMaxSize()) {
                        // Keep cover image visible while preparing to avoid black screen
                        if (isPreparing) {
                            AsyncImage(
                                model = ad.localCoverPath?.let(::File) ?: ad.coverUrl,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                            CircularProgressIndicator(
                                color = AccentNeonBlue,
                                modifier = Modifier.size(32.dp).align(Alignment.Center)
                            )
                        }
                        
                        AndroidView(
                            factory = { context ->
                                VideoView(context).apply {
                                    val videoPath = ad.localVideoPath ?: ad.videoUrl
                                    setVideoPath(videoPath)
                                    setOnPreparedListener { mp ->
                                        mp.isLooping = true
                                        mp.setVolume(0f, 0f)
                                        isPreparing = false
                                        start()
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxSize(),
                            onRelease = { videoView ->
                                videoView.stopPlayback()
                            }
                        )
                        // Clickable Overlay Box to capture clicks in Compose and toggle play/pause
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clickable { onPlayClick() }
                        )
                    }
                    // Small floating speaker muted icon overlay
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(12.dp)
                            .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                            .padding(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.VolumeMute,
                            contentDescription = "静音中",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                } else {
                    // Static Cover Image
                    AsyncImage(
                        model = ad.localCoverPath?.let(::File) ?: ad.coverUrl,
                        contentDescription = ad.title,
                        modifier = Modifier
                            .fillMaxSize()
                            .clickable { onPlayClick() },
                        contentScale = ContentScale.Crop
                    )

                    // Big glowing play button overlay on polished light theme
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .background(Color.White.copy(alpha = 0.85f), CircleShape)
                            .border(1.5.dp, AccentNeonBlue, CircleShape)
                            .clickable { onPlayClick() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "播放",
                            tint = AccentNeonBlue,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }

            // Clickable details area
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onCardClick() }
                    .padding(14.dp)
            ) {
                Text(
                    text = ad.title,
                    color = SlateTextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                AISummaryBox(summary = ad.summary)
                Spacer(modifier = Modifier.height(8.dp))
                TagsListRow(tags = ad.getTagList())
                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = PolishBorder)
                Spacer(modifier = Modifier.height(8.dp))
                ActionRow(
                    ad = ad,
                    onLikeClick = onLikeClick,
                    onFavClick = onFavClick,
                    onShareClick = onShareClick
                )
            }
        }
    }
}

// Core Card Header Supporting Part
@Composable
fun CardHeader(advertiserName: String, channel: String, isVideo: Boolean = false) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .background(SlateCharcoal, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isVideo) Icons.Default.Videocam else Icons.Default.Storefront,
                    contentDescription = null,
                    tint = AccentNeonBlue,
                    modifier = Modifier.size(14.dp)
                )
            }
            Text(
                text = advertiserName,
                color = SlateTextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Box(
            modifier = Modifier
                .background(AccentNeonBlue.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                .border(0.5.dp, AccentNeonBlue.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text(
                text = "${channel}热度推荐",
                color = AccentNeonBlue,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

// AI summary card component designed specifically for Professional Polish theme
@Composable
fun AISummaryBox(summary: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SlateCharcoal)
            .drawBehind {
                val strokeWidth = 4.dp.toPx()
                drawLine(
                    color = AccentNeonBlue,
                    start = androidx.compose.ui.geometry.Offset(strokeWidth / 2, 0f),
                    end = androidx.compose.ui.geometry.Offset(strokeWidth / 2, this.size.height),
                    strokeWidth = strokeWidth
                )
            }
            .padding(start = 14.dp, top = 10.dp, bottom = 10.dp, end = 12.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = AccentNeonBlue,
                    modifier = Modifier.size(12.dp)
                )
                Text(
                    text = "AI 核心卖点摘要",
                    color = AccentNeonBlue,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
            }
            Text(
                text = summary,
                color = SlateTextSecondary,
                fontSize = 12.sp,
                lineHeight = 16.sp
            )
        }
    }
}

// Tags horizontal scroller item list
@Composable
fun TagsListRow(tags: List<String>) {
    val forbiddenKeywords = listOf("小图", "视频")
    val filteredTags = tags.filter { it !in forbiddenKeywords }
    
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        filteredTags.forEach { tag ->
            Box(
                modifier = Modifier
                    .background(SlateCharcoal, RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "# $tag",
                    color = SlateTextSecondary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

// Actions bottom block details
@Composable
fun ActionRow(
    ad: AdEntity,
    onLikeClick: () -> Unit,
    onFavClick: () -> Unit,
    onShareClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Stats on impressions and clicks
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Visibility,
                    contentDescription = null,
                    tint = SlateTextSecondary,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = ad.impressions.toString(),
                    color = SlateTextSecondary,
                    fontSize = 11.sp
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Mouse,
                    contentDescription = null,
                    tint = SlateTextSecondary,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = ad.clicks.toString(),
                    color = SlateTextSecondary,
                    fontSize = 11.sp
                )
            }
        }

        // Live interaction triggers
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            IconButton(
                onClick = onLikeClick,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = if (ad.isLiked) Icons.Default.Favorite else Icons.Outlined.FavoriteBorder,
                    contentDescription = "点赞",
                    tint = if (ad.isLiked) AccentSunsetPink else SlateTextSecondary,
                    modifier = Modifier.size(18.dp)
                )
            }
            IconButton(
                onClick = onFavClick,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = if (ad.isFavorited) Icons.Default.Star else Icons.Outlined.StarBorder,
                    contentDescription = "收藏",
                    tint = if (ad.isFavorited) OrangeStar else SlateTextSecondary,
                    modifier = Modifier.size(18.dp)
                )
            }
            IconButton(
                onClick = onShareClick,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Share,
                    contentDescription = "分享",
                    tint = SlateTextSecondary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
