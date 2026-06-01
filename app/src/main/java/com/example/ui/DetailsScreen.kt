package com.example.ui

import android.widget.VideoView
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.ui.draw.drawBehind
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Launch
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import com.example.data.AdEntity
import com.example.data.CommentEntity
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailsScreen(
    adId: String,
    viewModel: AdvertViewModel,
    onNavigateBack: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    val allAds by viewModel.allAds.collectAsState()
    val ad = remember(allAds, adId) { allAds.firstOrNull { it.id == adId } }
    val commentsFlow = remember(adId) { viewModel.getComments(adId) }
    val comments by commentsFlow.collectAsState(initial = emptyList())
    var commentInput by rememberSaveable(adId) { mutableStateOf("") }
    val normalizedComment = commentInput.trim()
    val canSubmitComment = normalizedComment.isNotEmpty()

    if (ad == null) {
        Box(
            modifier = Modifier.fillMaxSize().background(SlateDark),
            contentAlignment = Alignment.Center
        ) {
            Text("未找到该广告详情信息", color = Color.White)
        }
        return
    }

    var likeScale by remember { mutableStateOf(1f) }
    var favoriteScale by remember { mutableStateOf(1f) }
    var videoViewRef by remember { mutableStateOf<VideoView?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            videoViewRef?.let { view ->
                if (view.isPlaying) {
                    viewModel.saveVideoProgress(adId, view.currentPosition)
                }
                view.stopPlayback()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "广告内容详情",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = SlateTextPrimary
                        )
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            videoViewRef?.let { view ->
                                viewModel.saveVideoProgress(adId, view.currentPosition)
                            }
                            onNavigateBack()
                        },
                        modifier = Modifier.testTag("back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回",
                            tint = SlateTextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SlateDark)
            )
        },
        containerColor = SlateDark,
        bottomBar = {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .drawBehind {
                        val strokeWidth = 1.dp.toPx()
                        drawLine(
                            color = PolishBorder,
                            start = androidx.compose.ui.geometry.Offset(0f, 0f),
                            end = androidx.compose.ui.geometry.Offset(this.size.width, 0f),
                            strokeWidth = strokeWidth
                        )
                    },
                color = SlateCharcoal
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val infiniteTransition = rememberInfiniteTransition(label = "pulsing")
                    val pulseScale by infiniteTransition.animateFloat(
                        initialValue = 1f,
                        targetValue = 1.05f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(1000, easing = LinearEasing),
                            repeatMode = RepeatMode.Reverse
                        ), label = "pulse"
                    )

                    Button(
                        onClick = {
                            viewModel.onAdClicked(ad.id)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .scale(pulseScale)
                            .height(48.dp)
                            .testTag("details_cta_button"),
                        shape = RoundedCornerShape(24.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AccentNeonBlue
                        )
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Launch,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "免费咨询 / 立即申请",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp)
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                if (ad.hasPlayableVideo()) {
                    var isVideoPlaying by remember { mutableStateOf(true) }
                    var isPreparing by remember { mutableStateOf(true) }
                    Box(modifier = Modifier.fillMaxSize()) {
                        if (isPreparing) {
                            AsyncImage(
                                model = ad.coverModel(),
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                            CircularProgressIndicator(
                                color = AccentNeonBlue,
                                modifier = Modifier.size(48.dp).align(Alignment.Center)
                            )
                        }

                        AndroidView(
                            factory = { context ->
                                VideoView(context).apply {
                                    setVideoPath(ad.videoSourcePath())
                                    setOnPreparedListener { mp ->
                                        mp.isLooping = true
                                        mp.setVolume(1f, 1f)
                                        val savedPos = viewModel.getVideoProgress(adId)
                                        if (savedPos > 0) {
                                            seekTo(savedPos)
                                        }
                                        isPreparing = false
                                        start()
                                        isVideoPlaying = true
                                    }
                                    videoViewRef = this
                                }
                            },
                            modifier = Modifier.fillMaxSize(),
                            onRelease = { videoView ->
                                videoView.stopPlayback()
                            }
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clickable {
                                    videoViewRef?.let { view ->
                                        if (view.isPlaying) {
                                            view.pause()
                                            isVideoPlaying = false
                                        } else {
                                            view.start()
                                            isVideoPlaying = true
                                        }
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (!isVideoPlaying) {
                                Box(
                                    modifier = Modifier
                                        .size(64.dp)
                                        .background(Color.Black.copy(alpha = 0.5f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = "播放",
                                        tint = Color.White,
                                        modifier = Modifier.size(36.dp)
                                    )
                                }
                            }
                        }
                    }
                } else {
                    // Portrait styled AsyncImage
                    AsyncImage(
                        model = ad.coverModel(),
                        contentDescription = ad.title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
            }

                // Core Ad Information Column
                Column(
                    modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Brand Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(SlateCharcoal, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Storefront,
                                contentDescription = null,
                                tint = AccentNeonBlue,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Column {
                            Text(
                                text = ad.advertiserName,
                                color = SlateTextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "官方认证服务商",
                                color = SlateTextSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .background(AccentNeonBlue.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                            .border(0.5.dp, AccentNeonBlue.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = ad.channel,
                            color = AccentNeonBlue,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = ad.title,
                        color = SlateTextPrimary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = ad.description,
                        color = SlateTextSecondary,
                        fontSize = 14.sp,
                        lineHeight = 22.sp
                    )
                }

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
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = AccentNeonBlue,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "AI 核心卖点摘要",
                                color = AccentNeonBlue,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Text(
                            text = ad.summary,
                            color = SlateTextSecondary,
                            fontSize = 13.sp,
                            lineHeight = 18.sp
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            mainAxisSpacing = 8.dp,
                            crossAxisSpacing = 6.dp
                        ) {
                            val forbiddenKeywords = listOf("小图", "视频")
                            ad.getTagList().filter { it !in forbiddenKeywords }.forEach { tag ->
                                Box(
                                    modifier = Modifier
                                        .background(SlateDark, RoundedCornerShape(8.dp))
                                        .border(0.5.dp, PolishBorder, RoundedCornerShape(8.dp))
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "# $tag",
                                        color = SlateTextSecondary,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    }
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    border = BorderStroke(1.dp, PolishBorder),
                    colors = CardDefaults.cardColors(containerColor = SlateCard)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "本条广告曝光效能指标 (模拟)",
                            color = SlateTextSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            MetricIndicator(name = "展现量", value = ad.impressions.toString(), tint = SlateTextPrimary)
                            MetricIndicator(name = "点击量", value = ad.clicks.toString(), tint = AccentNeonBlue)
                            MetricIndicator(
                                name = "转化率",
                                value = if (ad.impressions > 0) String.format("%.1f%%", (ad.clicks.toFloat() / ad.impressions * 100)) else "0.0%",
                                tint = AccentTealGreen
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(SlateCard, RoundedCornerShape(16.dp))
                        .border(1.dp, PolishBorder, RoundedCornerShape(16.dp))
                        .padding(vertical = 12.dp, horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .scale(likeScale)
                            .testTag("like_action")
                            .clickable {
                                coroutineScope.launch {
                                    likeScale = 1.3f
                                    viewModel.toggleLike(ad.id)
                                    delay(150)
                                    likeScale = 1f
                                }
                            }
                    ) {
                        Icon(
                            imageVector = if (ad.isLiked) Icons.Default.Favorite else Icons.Outlined.FavoriteBorder,
                            contentDescription = "点赞",
                            tint = if (ad.isLiked) AccentSunsetPink else SlateTextSecondary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "赞 (${ad.likeCount})",
                            color = if (ad.isLiked) AccentSunsetPink else SlateTextSecondary,
                            fontSize = 11.sp
                        )
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .scale(favoriteScale)
                            .testTag("favorite_action")
                            .clickable {
                                coroutineScope.launch {
                                    favoriteScale = 1.3f
                                    viewModel.toggleFavorite(ad.id)
                                    delay(150)
                                    favoriteScale = 1f
                                }
                            }
                    ) {
                        Icon(
                            imageVector = if (ad.isFavorited) Icons.Default.Star else Icons.Outlined.StarBorder,
                            contentDescription = "收藏",
                            tint = if (ad.isFavorited) OrangeStar else SlateTextSecondary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "收藏 (${ad.favoriteCount})",
                            color = if (ad.isFavorited) OrangeStar else SlateTextSecondary,
                            fontSize = 11.sp
                        )
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.clickable {
                            viewModel.shareAd(ad.id)
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "分享",
                            tint = SlateTextSecondary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "分享 (${ad.shareCount})",
                            color = SlateTextSecondary,
                            fontSize = 11.sp
                        )
                    }
                }

                CommentsSection(
                    comments = comments,
                    commentInput = commentInput,
                    onCommentInputChange = { updated ->
                        commentInput = updated.take(AdvertViewModel.COMMENT_MAX_LENGTH)
                    },
                    onSubmitComment = {
                        viewModel.addComment(ad.id, commentInput)
                        commentInput = ""
                    },
                    canSubmitComment = canSubmitComment,
                    maxLength = AdvertViewModel.COMMENT_MAX_LENGTH
                )
            }
        }
    }
}

@Composable
fun MetricIndicator(name: String, value: String, tint: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = name, color = SlateTextSecondary, fontSize = 11.sp)
        Spacer(modifier = Modifier.height(2.dp))
        Text(text = value, color = tint, fontSize = 18.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun CommentsSection(
    comments: List<CommentEntity>,
    commentInput: String,
    onCommentInputChange: (String) -> Unit,
    onSubmitComment: () -> Unit,
    canSubmitComment: Boolean,
    maxLength: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, PolishBorder),
        colors = CardDefaults.cardColors(containerColor = SlateCard)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "评论 (${comments.size})",
                    color = SlateTextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${commentInput.length}/$maxLength",
                    color = SlateTextSecondary,
                    fontSize = 11.sp
                )
            }

            OutlinedTextField(
                value = commentInput,
                onValueChange = onCommentInputChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("comment_input"),
                minLines = 3,
                maxLines = 5,
                placeholder = {
                    Text("写下你对这条广告的看法...", color = SlateTextSecondary)
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = SlateCharcoal,
                    unfocusedContainerColor = SlateCharcoal,
                    focusedBorderColor = AccentNeonBlue,
                    unfocusedBorderColor = PolishBorder,
                    focusedTextColor = SlateTextPrimary,
                    unfocusedTextColor = SlateTextPrimary,
                    cursorColor = AccentNeonBlue
                )
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Button(
                    onClick = onSubmitComment,
                    enabled = canSubmitComment,
                    modifier = Modifier.testTag("comment_submit_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AccentNeonBlue,
                        disabledContainerColor = SlateCharcoal
                    )
                ) {
                    Text(
                        text = "发布评论",
                        color = if (canSubmitComment) SlateDark else SlateTextSecondary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (comments.isEmpty()) {
                Text(
                    text = "还没有评论，来发布第一条观点吧。",
                    color = SlateTextSecondary,
                    fontSize = 13.sp
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    comments.forEach { comment ->
                        CommentItem(comment = comment)
                    }
                }
            }
        }
    }
}

@Composable
private fun CommentItem(comment: CommentEntity) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(SlateCharcoal, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = comment.authorName.take(1),
                color = AccentNeonBlue,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = comment.authorName,
                    color = SlateTextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = formatCommentTime(comment.createdAt),
                    color = SlateTextSecondary,
                    fontSize = 11.sp
                )
            }
            Text(
                text = comment.content,
                color = SlateTextSecondary,
                fontSize = 13.sp,
                lineHeight = 19.sp
            )
        }
    }
}

private fun formatCommentTime(createdAt: Long): String {
    return SimpleDateFormat("MM-dd HH:mm", Locale.CHINA).format(Date(createdAt))
}

@Composable
fun FlowRow(
    modifier: Modifier = Modifier,
    mainAxisSpacing: androidx.compose.ui.unit.Dp = 0.dp,
    crossAxisSpacing: androidx.compose.ui.unit.Dp = 0.dp,
    content: @Composable () -> Unit
) {
    androidx.compose.ui.layout.Layout(
        modifier = modifier,
        content = content
    ) { measurables, constraints ->
        val childConstraints = constraints.copy(minWidth = 0, minHeight = 0)
        val placeables = measurables.map { it.measure(childConstraints) }

        var currentX = 0
        var currentY = 0
        var maxRowHeight = 0
        var totalWidth = 0

        val mainSpacingPx = mainAxisSpacing.roundToPx()
        val crossSpacingPx = crossAxisSpacing.roundToPx()

        val rows = mutableListOf<List<androidx.compose.ui.layout.Placeable>>()
        var currentRow = mutableListOf<androidx.compose.ui.layout.Placeable>()

        placeables.forEach { placeable ->
            val spacing = if (currentRow.isEmpty()) 0 else mainSpacingPx
            if (currentX + spacing + placeable.width > constraints.maxWidth && currentRow.isNotEmpty()) {
                rows.add(currentRow)
                currentRow = mutableListOf()
                totalWidth = maxOf(totalWidth, currentX)
                currentX = 0
                currentY += maxRowHeight + crossSpacingPx
                maxRowHeight = 0
            }
            currentRow.add(placeable)
            currentX += (if (currentRow.size == 1) 0 else mainSpacingPx) + placeable.width
            maxRowHeight = maxOf(maxRowHeight, placeable.height)
        }
        if (currentRow.isNotEmpty()) {
            rows.add(currentRow)
            totalWidth = maxOf(totalWidth, currentX)
            currentY += maxRowHeight
        }

        val width = if (constraints.hasBoundedWidth) {
            constraints.maxWidth
        } else {
            totalWidth.coerceIn(constraints.minWidth, constraints.maxWidth)
        }
        val height = currentY.coerceIn(constraints.minHeight, constraints.maxHeight)

        layout(width, height) {
            var x = 0
            var y = 0
            var rowH = 0
            placeables.forEach { placeable ->
                if (x + placeable.width > width && x > 0) {
                    x = 0
                    y += rowH + crossSpacingPx
                    rowH = 0
                }
                placeable.placeRelative(x = x, y = y)
                x += placeable.width + mainSpacingPx
                rowH = maxOf(rowH, placeable.height)
            }
        }
    }
}
