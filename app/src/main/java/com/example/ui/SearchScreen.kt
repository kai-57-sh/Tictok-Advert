package com.example.ui

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.ui.draw.drawBehind
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AdEntity
import com.example.ui.theme.*

@OptIn(ExperimentalAnimationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    viewModel: AdvertViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToDetail: (String) -> Unit
) {
    var searchInput by remember { mutableStateOf("") }

    // Sample suggested queries
    val recommendedQuestions = listOf(
        "推荐适合考公的户外降噪耳机",
        "想看适合学生党的平价跑鞋广告",
        "同城好吃好玩的霸王餐火锅推荐",
        "高性价比的客厅记忆棉皮革沙发"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "AI 搜广告助手",
                        color = SlateTextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            viewModel.clearSearch()
                            onNavigateBack()
                        },
                        modifier = Modifier.testTag("search_back_button")
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
        containerColor = SlateDark
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            // Conversational Input Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, PolishBorder),
                colors = CardDefaults.cardColors(containerColor = SlateCard),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "用自然语言描述您的兴趣，AI 智慧搜索引擎将把文字精准映射至推荐广告标签与频道...",
                        color = SlateTextSecondary,
                        fontSize = 11.sp,
                        lineHeight = 16.sp
                    )

                    OutlinedTextField(
                        value = searchInput,
                        onValueChange = { searchInput = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("search_text_input"),
                        placeholder = {
                            Text(
                                "如: “打工人的高性价比通勤跑鞋”",
                                color = SlateTextSecondary,
                                fontSize = 13.sp
                            )
                        },
                        trailingIcon = {
                            if (searchInput.isNotEmpty()) {
                                IconButton(onClick = { searchInput = "" }) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "清除",
                                        tint = SlateTextSecondary
                                    )
                                }
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AccentNeonBlue,
                            unfocusedBorderColor = PolishBorder,
                            focusedContainerColor = SlateDark,
                            unfocusedContainerColor = SlateDark,
                            focusedTextColor = SlateTextPrimary,
                            unfocusedTextColor = SlateTextPrimary
                        ),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )

                    Button(
                        onClick = {
                            viewModel.performSearch(searchInput)
                        },
                        enabled = searchInput.isNotBlank() && !viewModel.isSearching,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .testTag("search_submit_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AccentNeonBlue,
                            disabledContainerColor = SlateCharcoal
                        ),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = if (searchInput.isNotBlank()) Color.White else SlateTextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "小钛智能解析意图并匹配",
                                color = if (searchInput.isNotBlank()) Color.White else SlateTextSecondary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }

            // Quick preset chips horizontal row
            Text(
                text = "点击推荐问题示例直接体验：",
                color = SlateTextSecondary,
                fontSize = 11.sp,
                modifier = Modifier.padding(vertical = 4.dp)
            )

            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(recommendedQuestions) { q ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                searchInput = q
                                viewModel.performSearch(q)
                            }
                            .background(SlateCharcoal)
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = q,
                            color = AccentNeonBlue,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // AI Searching Futuristic Loader
            AnimatedVisibility(
                visible = viewModel.isSearching,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(SlateCharcoal)
                        .border(1.dp, PolishBorder, RoundedCornerShape(12.dp))
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CircularProgressIndicator(color = AccentNeonBlue, modifier = Modifier.size(24.dp))
                        Text(
                            text = "小钛 AI 正在进行大语言模型条件投射解析中...",
                            color = AccentNeonBlue,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "提取语义实体 -> 映射频道 -> 对齐核心受控词标签对",
                            color = SlateTextSecondary,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            // AI Parsed Intent Explanation Box
            val aiIntent = viewModel.parsedSearchIntent
            if (!viewModel.isSearching && aiIntent != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
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
                                text = "小钛 AI 意图理解结果",
                                color = AccentNeonBlue,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Structured parsed targets details
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Column {
                                Text("意图投射频道", color = SlateTextSecondary, fontSize = 11.sp)
                                Text(
                                    text = aiIntent.channel ?: "精选 (全部)",
                                    color = SlateTextPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Column {
                                Text("智能识别核心受控词", color = SlateTextSecondary, fontSize = 11.sp)
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    val forbiddenKeywords = listOf("小图", "视频")
                                    val filteredTags = aiIntent.tags.filter { it !in forbiddenKeywords }
                                    if (filteredTags.isEmpty()) {
                                        Text("无具体受控词", color = SlateTextPrimary, fontSize = 13.sp)
                                    } else {
                                        filteredTags.forEach { tag ->
                                            Text(
                                                text = "#$tag",
                                                color = AccentNeonBlue,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        HorizontalDivider(color = PolishBorder)

                        // Paragraph explanation
                        Text(
                            text = aiIntent.explanation,
                            color = SlateTextSecondary,
                            fontSize = 12.sp,
                            lineHeight = 17.sp
                        )
                    }
                }
            }

            // Matching result counter
            if (!viewModel.isSearching && aiIntent != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "根据 AI 偏好提取，已筛选出 ${viewModel.searchResultsList.size} 条关联商业信息",
                        color = SlateTextSecondary,
                        fontSize = 11.sp
                    )
                    Text(
                        text = "重置过滤器",
                        color = AccentSunsetPink,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable {
                            viewModel.clearSearch()
                            searchInput = ""
                        }
                    )
                }
            }

            // Results List Lazy Column
            if (!viewModel.isSearching) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .testTag("search_results_lazy_column"),
                    contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (viewModel.searchResultsList.isEmpty() && aiIntent != null) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 40.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.SentimentDissatisfied,
                                        contentDescription = null,
                                        tint = SlateTextSecondary,
                                        modifier = Modifier.size(36.dp)
                                    )
                                    Text(
                                        text = "对不起，未查找到完全匹配该条件的广告推荐",
                                        color = SlateTextSecondary,
                                        fontSize = 13.sp,
                                        textAlign = TextAlign.Center
                                    )
                                    Text(
                                        text = "请尝试更换查询词，或点击示例问题快速体验！",
                                        color = SlateTextSecondary,
                                        fontSize = 11.sp,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    } else {
                        items(viewModel.searchResultsList, key = { "search_${it.id}" }) { ad ->
                            // Recycles identical rendering layouts seamlessly
                            when (ad.cardType) {
                                "video" -> VideoAdCard(
                                    ad = ad,
                                    isPlayingInFeed = false, // No streaming autoplay in query screen to preserve resource overhead
                                    onPlayClick = {
                                        viewModel.onAdClicked(ad.id)
                                        onNavigateToDetail(ad.id)
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
                    }
                }
            }
        }
    }
}
