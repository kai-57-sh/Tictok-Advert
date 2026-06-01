package com.example.ui

import android.widget.VideoView
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeMute
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import com.example.data.AdEntity
import com.example.ui.theme.*
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlin.random.Random

@OptIn(ExperimentalAnimationApi::class, ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: AdvertViewModel,
    onNavigateToDetail: (String) -> Unit,
    onNavigateToSearch: () -> Unit,
    onNavigateToStats: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val listStates = remember {
        mapOf(
            "精选" to LazyListState(),
            "电商" to LazyListState(),
            "本地" to LazyListState()
        )
    }
    var playingAdId by remember { mutableStateOf<String?>(null) }
    val allAdsState by viewModel.allAds.collectAsState()
    val allAdsList = allAdsState
    val activeChannels = remember(allAdsList) {
        listOf("精选", "电商", "本地").filter { channel ->
            allAdsList.any { it.channel == channel }
        }
    }
    val pagerChannels = remember(activeChannels) { buildPagerChannels(activeChannels) }
    val pagerState = rememberPagerState(pageCount = { pagerChannels.size })
    val selectedChannel = resolvePagerChannel(
        pagerChannels = pagerChannels,
        page = pagerState.settledPage,
        fallbackChannel = viewModel.selectedChannel
    )
    val activeTagFilter = viewModel.activeFilters[selectedChannel]
    val selectedChannelAds = remember(allAdsList, selectedChannel) {
        allAdsList.filter { it.channel == selectedChannel }
    }
    val tagUiState = remember(selectedChannelAds, activeTagFilter) {
        buildChannelTagUiState(
            channelAds = selectedChannelAds,
            activeTagFilter = activeTagFilter
        )
    }

    LaunchedEffect(activeChannels) {
        if (activeChannels.isNotEmpty() && !activeChannels.contains(viewModel.selectedChannel)) {
            viewModel.selectChannel(activeChannels.first())
        }
    }

    LaunchedEffect(activeChannels) {
        if (pagerChannels.isNotEmpty() && pagerState.currentPage > pagerChannels.lastIndex) {
            pagerState.scrollToPage(pagerChannels.lastIndex)
        }
    }

    LaunchedEffect(viewModel.selectedChannel, pagerChannels) {
        val targetPage = findChannelPage(pagerChannels, viewModel.selectedChannel)
        if (targetPage >= 0 && targetPage != pagerState.currentPage) {
            pagerState.animateScrollToPage(targetPage)
        }
    }

    LaunchedEffect(pagerState, pagerChannels) {
        snapshotFlow { pagerState.settledPage }.collectLatest { page ->
            if (page in pagerChannels.indices) {
                val channel = resolvePagerChannel(
                    pagerChannels = pagerChannels,
                    page = page,
                    fallbackChannel = viewModel.selectedChannel
                )
                if (viewModel.selectedChannel != channel) {
                    viewModel.selectChannel(channel)
                }
                playingAdId = null
            }
        }
    }

    LaunchedEffect(tagUiState.allSelectableTags, activeTagFilter, selectedChannel) {
        if (activeTagFilter != null && activeTagFilter !in tagUiState.allSelectableTags) {
            viewModel.clearTagFilter(selectedChannel)
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
                    val isSelected = selectedChannel == channelName
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                val targetPage = findChannelPage(pagerChannels, channelName)
                                if (targetPage >= 0) {
                                    coroutineScope.launch {
                                        playingAdId = null
                                        pagerState.animateScrollToPage(targetPage)
                                    }
                                }
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
                            color = if (isSelected) AccentNeonBlue else SlateTextSecondary,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 14.sp
                        )
                    }
                }
            }

            HomeTagFilterBar(
                tagUiState = tagUiState,
                activeTagFilter = activeTagFilter,
                onSelectAll = {
                    viewModel.clearTagFilter(selectedChannel)
                },
                onSelectTag = { tag ->
                    viewModel.toggleTagFilter(selectedChannel, tag)
                }
            )

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

            if (activeChannels.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "当前没有可展示的频道内容",
                        color = SlateTextSecondary,
                        fontSize = 14.sp
                    )
                }
            } else {
                PullToRefreshBox(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    isRefreshing = viewModel.isRefreshing,
                    onRefresh = {
                        playingAdId = null
                        viewModel.refreshContent(selectedChannel)
                    }
                ) {
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("channel_pager")
                    ) { page ->
                        val channel = pagerChannels[page]
                        val channelAds = remember(allAdsList, channel) {
                            allAdsList.filter { it.channel == channel }
                        }
                        val pageTagFilter = viewModel.activeFilters[channel]
                        val refreshToken = viewModel.refreshTokens[channel] ?: 0
                        val displayAds = remember(channelAds, pageTagFilter, refreshToken) {
                            buildDisplayAds(
                                channelAds = channelAds,
                                activeTagFilter = pageTagFilter,
                                refreshToken = refreshToken
                            )
                        }

                        if (displayAds.isEmpty()) {
                            EmptyChannelFilterState(
                                onClearFilter = { viewModel.clearTagFilter(channel) }
                            )
                        } else {
                            ChannelAdsPagerPage(
                                ads = displayAds,
                                listState = listStates[channel] ?: LazyListState(),
                                refreshToken = refreshToken,
                                playingAdId = playingAdId,
                                onPlayClick = { adId ->
                                    playingAdId = if (playingAdId == adId) null else adId
                                },
                                onCardClick = { adId ->
                                    viewModel.onAdClicked(adId)
                                    onNavigateToDetail(adId)
                                },
                                onLikeClick = { adId -> viewModel.toggleLike(adId) },
                                onFavClick = { adId -> viewModel.toggleFavorite(adId) },
                                onShareClick = { adId -> viewModel.shareAd(adId) },
                                onExpose = { adId -> viewModel.onAdExposed(adId) }
                            )
                        }
                    }
                }
            }
        }
    }
}

private data class ChannelTagUiState(
    val allSelectableTags: Set<String>,
    val primaryTags: List<String>,
    val overflowTags: List<String>
)

internal fun buildPagerChannels(activeChannels: List<String>): List<String> {
    return activeChannels
}

internal fun findChannelPage(pagerChannels: List<String>, channel: String): Int {
    return pagerChannels.indexOf(channel)
}

internal fun resolvePagerChannel(
    pagerChannels: List<String>,
    page: Int,
    fallbackChannel: String
): String {
    return pagerChannels.getOrNull(page) ?: fallbackChannel
}

private fun buildChannelTagUiState(
    channelAds: List<AdEntity>,
    activeTagFilter: String?
): ChannelTagUiState {
    val forbiddenKeywords = setOf("小图", "视频")
    val sortedTags = channelAds
        .flatMap { it.getTagList() }
        .groupingBy { it }
        .eachCount()
        .filter { (tag, count) -> count >= 3 && tag !in forbiddenKeywords }
        .toList()
        .sortedWith(compareByDescending<Pair<String, Int>> { it.second }.thenBy { it.first })
        .map { it.first }

    val primaryTags = sortedTags.take(3).toMutableList()
    if (activeTagFilter != null && activeTagFilter in sortedTags && activeTagFilter !in primaryTags) {
        if (primaryTags.size >= 3) {
            primaryTags[primaryTags.lastIndex] = activeTagFilter
        } else {
            primaryTags += activeTagFilter
        }
    }

    val distinctPrimaryTags = primaryTags.distinct()
    val overflowTags = sortedTags.filterNot { it in distinctPrimaryTags }
    return ChannelTagUiState(
        allSelectableTags = sortedTags.toSet(),
        primaryTags = distinctPrimaryTags,
        overflowTags = overflowTags
    )
}

private fun buildDisplayAds(
    channelAds: List<AdEntity>,
    activeTagFilter: String?,
    refreshToken: Int
): List<AdEntity> {
    val filteredAds = if (activeTagFilter == null) {
        channelAds
    } else {
        channelAds.filter { activeTagFilter in it.getTagList() }
    }

    return if (refreshToken > 0 && filteredAds.size > 1) {
        filteredAds.shuffled(Random(refreshToken))
    } else {
        filteredAds
    }
}

@Composable
private fun HomeTagFilterBar(
    tagUiState: ChannelTagUiState,
    activeTagFilter: String?,
    onSelectAll: () -> Unit,
    onSelectTag: (String) -> Unit
) {
    var moreMenuExpanded by remember(tagUiState.overflowTags) { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilterPill(
                label = "全部",
                isSelected = activeTagFilter == null,
                onClick = onSelectAll,
                modifier = Modifier.weight(1f, fill = false)
            )

            tagUiState.primaryTags.forEach { tag ->
                val isAll = tag == "全部"
                FilterPill(
                    label = "#$tag",
                    isSelected = activeTagFilter == tag,
                    showCheck = activeTagFilter == tag,
                    onClick = { onSelectTag(tag) },
                    modifier = Modifier.weight(1f, fill = false)
                )
            }
        }

        if (tagUiState.overflowTags.isNotEmpty()) {
            Box {
                FilterPill(
                    label = "更多",
                    isSelected = false,
                    trailingIcon = Icons.Default.ArrowDropDown,
                    onClick = { moreMenuExpanded = true }
                )
                DropdownMenu(
                    expanded = moreMenuExpanded,
                    onDismissRequest = { moreMenuExpanded = false },
                    containerColor = SlateCard
                ) {
                    Column(
                        modifier = Modifier
                            .widthIn(min = 220.dp, max = 320.dp)
                            .padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        tagUiState.overflowTags.chunked(3).forEach { rowTags ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                rowTags.forEach { tag ->
                                    OverflowTagChip(
                                        label = "#$tag",
                                        isSelected = activeTagFilter == tag,
                                        modifier = Modifier.weight(1f),
                                        onClick = {
                                            moreMenuExpanded = false
                                            onSelectTag(tag)
                                        }
                                    )
                                }
                                repeat(3 - rowTags.size) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterPill(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    showCheck: Boolean = false,
    trailingIcon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .background(if (isSelected) PolishPillActive else SlateCharcoal)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (showCheck) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = PolishTextOnActive,
                    modifier = Modifier.size(12.dp)
                )
            }
            Text(
                text = label,
                color = if (isSelected) PolishTextOnActive else SlateTextSecondary,
                fontSize = 12.sp,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (trailingIcon != null) {
                Icon(
                    imageVector = trailingIcon,
                    contentDescription = null,
                    tint = if (isSelected) PolishTextOnActive else SlateTextSecondary,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

@Composable
private fun OverflowTagChip(
    label: String,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick),
        color = if (isSelected) PolishPillActive else SlateCharcoal,
        tonalElevation = 0.dp,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(
            width = 1.dp,
            color = if (isSelected) AccentNeonBlue.copy(alpha = 0.35f) else PolishBorder
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = PolishTextOnActive,
                    modifier = Modifier
                        .size(12.dp)
                        .padding(end = 2.dp)
                )
            }
            Text(
                text = label,
                color = if (isSelected) PolishTextOnActive else SlateTextPrimary,
                fontSize = 11.sp,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun EmptyChannelFilterState(
    onClearFilter: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
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
                onClick = onClearFilter,
                colors = ButtonDefaults.buttonColors(containerColor = AccentNeonBlue)
            ) {
                Text("返回显示全部", color = SlateDark, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun ChannelAdsPagerPage(
    ads: List<AdEntity>,
    listState: LazyListState,
    refreshToken: Int,
    playingAdId: String?,
    onPlayClick: (String) -> Unit,
    onCardClick: (String) -> Unit,
    onLikeClick: (String) -> Unit,
    onFavClick: (String) -> Unit,
    onShareClick: (String) -> Unit,
    onExpose: (String) -> Unit
) {
    LaunchedEffect(refreshToken) {
        if (refreshToken > 0) {
            listState.scrollToItem(0)
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("ads_lazy_column"),
        state = listState,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(ads, key = { it.id }) { ad ->
            LaunchedEffect(ad.id) {
                onExpose(ad.id)
            }

            when (ad.cardType) {
                "video" -> VideoAdCard(
                    ad = ad,
                    isPlayingInFeed = playingAdId == ad.id,
                    onPlayClick = { onPlayClick(ad.id) },
                    onCardClick = { onCardClick(ad.id) },
                    onLikeClick = { onLikeClick(ad.id) },
                    onFavClick = { onFavClick(ad.id) },
                    onShareClick = { onShareClick(ad.id) }
                )

                "small_image" -> SmallImageAdCard(
                    ad = ad,
                    onCardClick = { onCardClick(ad.id) },
                    onLikeClick = { onLikeClick(ad.id) },
                    onFavClick = { onFavClick(ad.id) },
                    onShareClick = { onShareClick(ad.id) }
                )

                else -> BigImageAdCard(
                    ad = ad,
                    onCardClick = { onCardClick(ad.id) },
                    onLikeClick = { onLikeClick(ad.id) },
                    onFavClick = { onFavClick(ad.id) },
                    onShareClick = { onShareClick(ad.id) }
                )
            }
        }

        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "— 已经到底部啦，下拉继续换一批 —",
                    color = SlateTextSecondary,
                    fontSize = 11.sp
                )
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
                model = ad.coverModel(),
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
                    model = ad.coverModel(),
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
                                model = ad.coverModel(),
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
                                    setVideoPath(ad.videoSourcePath())
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
                        model = ad.coverModel(),
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

    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        mainAxisSpacing = 6.dp,
        crossAxisSpacing = 6.dp
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
