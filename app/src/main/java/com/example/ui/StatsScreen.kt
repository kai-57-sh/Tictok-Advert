package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AnalyticsEventEntity
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(
    viewModel: AdvertViewModel,
    onNavigateBack: () -> Unit
) {
    val events by viewModel.analyticsEvents.collectAsState()
    val totalExposures by viewModel.exposureCount.collectAsState()
    val totalClicks by viewModel.clickCount.collectAsState()
    val totalInteractions by viewModel.interactionCount.collectAsState()
    val allAdsState by viewModel.allAds.collectAsState()

    val activeChannels = remember(events, allAdsState) {
        val dbChannels = allAdsState.map { it.channel }.toSet()
        val eventChannels = events.map { it.channel }.toSet()
        listOf("精选", "电商", "本地").filter { channel ->
            dbChannels.contains(channel) || eventChannels.contains(channel)
        }
    }

    val averageCtr = if (totalExposures > 0) {
        (totalClicks.toFloat() / totalExposures * 100)
    } else {
        0f
    }

    // Dynamic metrics calculators over live events
    val clicksByChannel = remember(events) {
        val map = mutableMapOf("精选" to 0, "电商" to 0, "本地" to 0)
        events.filter { it.eventType == "click" }.forEach {
            val key = it.channel
            map[key] = (map[key] ?: 0) + 1
        }
        map
    }

    val exposuresByChannel = remember(events) {
        val map = mutableMapOf("精选" to 0, "电商" to 0, "本地" to 0)
        events.filter { it.eventType == "view" }.forEach {
            val key = it.channel
            map[key] = (map[key] ?: 0) + 1
        }
        map
    }

    val clicksByStyle = remember(events) {
        val map = mutableMapOf("big_image" to 0, "small_image" to 0, "video" to 0)
        events.filter { it.eventType == "click" }.forEach {
            val key = it.cardType
            map[key] = (map[key] ?: 0) + 1
        }
        map
    }

    val exposuresByStyle = remember(events) {
        val map = mutableMapOf("big_image" to 0, "small_image" to 0, "video" to 0)
        events.filter { it.eventType == "view" }.forEach {
            val key = it.cardType
            map[key] = (map[key] ?: 0) + 1
        }
        map
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "广告曝光效能看板",
                        color = SlateTextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("stats_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回",
                            tint = SlateTextPrimary
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.resetStats() },
                        modifier = Modifier
                            .testTag("reset_stats_button")
                            .background(SlateCharcoal, CircleShape)
                            .size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteSweep,
                            contentDescription = "重置模拟埋点数据",
                            tint = AccentSunsetPink,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SlateDark)
            )
        },
        containerColor = SlateDark
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Summary Analytics Headline Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    shape = RoundedCornerShape(24.dp),
                    border = BorderStroke(1.dp, PolishBorder),
                    colors = CardDefaults.cardColors(containerColor = SlateCard)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .background(AccentNeonBlue.copy(alpha = 0.1f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Insights,
                                    contentDescription = null,
                                    tint = AccentNeonBlue,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                            Text(
                                text = "商业数据核心效能指标 (模拟点击埋点)",
                                color = SlateTextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }

                        // Grid metrics display 2x2
                        Row(modifier = Modifier.fillMaxWidth()) {
                            MetricItemBlock(
                                title = "累计曝光量 (PV)",
                                value = totalExposures.toString(),
                                accentColor = SlateTextPrimary,
                                modifier = Modifier.weight(1f)
                            )
                            MetricItemBlock(
                                title = "累计点击数",
                                value = totalClicks.toString(),
                                accentColor = AccentNeonBlue,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Row(modifier = Modifier.fillMaxWidth()) {
                            MetricItemBlock(
                                title = "智能转化率 (CTR)",
                                value = String.format("%.2f%%", averageCtr),
                                accentColor = AccentTealGreen,
                                modifier = Modifier.weight(1f)
                            )
                            MetricItemBlock(
                                title = "累计互动计数 (赞/藏/分)",
                                value = totalInteractions.toString(),
                                accentColor = AccentNeonBlue,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // Group Comparison Analytics Rows
            item {
                Text(
                    text = "多维商业转化率交叉对比 (CTR计算)",
                    color = SlateTextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    border = BorderStroke(1.dp, PolishBorder),
                    colors = CardDefaults.cardColors(containerColor = SlateCard)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = "按推荐频道划分对比",
                            color = SlateTextSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )

                        activeChannels.forEach { name ->
                            val clicks = clicksByChannel[name] ?: 0
                            val views = exposuresByChannel[name] ?: 0
                            val ctr = if (views > 0) (clicks.toFloat() / views * 100) else 0f

                            ComparisonMetricRow(
                                title = name,
                                clicks = clicks,
                                exposures = views,
                                ctrString = String.format("%.1f%%", ctr),
                                fillPercent = if (views > 0) (clicks.toFloat() / views).coerceAtMost(1f) else 0f,
                                barColor = AccentNeonBlue
                            )
                        }
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    border = BorderStroke(1.dp, PolishBorder),
                    colors = CardDefaults.cardColors(containerColor = SlateCard)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = "按卡片展现形态对比",
                            color = SlateTextSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )

                        val listMap = listOf(
                            Triple("big_image", "高精大图卡", AccentTealGreen),
                            Triple("small_image", "极简小图卡", AccentNeonBlue),
                            Triple("video", "原生视频流", AccentSunsetPink)
                        )

                        listMap.forEach { (type, displayName, color) ->
                            val clicks = clicksByStyle[type] ?: 0
                            val views = exposuresByStyle[type] ?: 0
                            val ctr = if (views > 0) (clicks.toFloat() / views * 100) else 0f

                            ComparisonMetricRow(
                                title = displayName,
                                clicks = clicks,
                                exposures = views,
                                ctrString = String.format("%.1f%%", ctr),
                                fillPercent = if (views > 0) (clicks.toFloat() / views).coerceAtMost(1f) else 0f,
                                barColor = color
                            )
                        }
                    }
                }
            }

            // Real Time Simulated Event stream logs
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "客户端埋点事件流 (实时监控)",
                        color = SlateTextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${events.size} 个包就绪",
                        color = SlateTextSecondary,
                        fontSize = 11.sp
                    )
                }
            }

            if (events.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 30.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "客户端尚未捕获埋点数据。请在首页信息流滑动浏览卡片并点击点赞、收藏，即可生成实时埋点回执！",
                            color = SlateTextSecondary,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(horizontal = 24.dp)
                        )
                    }
                }
            } else {
                items(events) { log ->
                    EventLogItemRow(log = log)
                }
            }
        }
    }
}

// Summary Mini Card Item block
@Composable
fun MetricItemBlock(
    title: String,
    value: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(vertical = 4.dp, horizontal = 4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(text = title, color = SlateTextSecondary, fontSize = 11.sp)
        Text(text = value, color = accentColor, fontSize = 20.sp, fontWeight = FontWeight.Black)
    }
}

// Progressive bar UI line for metric groupings
@Composable
fun ComparisonMetricRow(
    title: String,
    clicks: Int,
    exposures: Int,
    ctrString: String,
    fillPercent: Float,
    barColor: Color
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = title, color = SlateTextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text(
                text = "$clicks 点击 / $exposures 曝光 (CTR: $ctrString)",
                color = SlateTextSecondary,
                fontSize = 11.sp
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        // Progress bar container
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(SlateCharcoal)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(fraction = fillPercent.coerceIn(0.005f, 1f))
                    .clip(RoundedCornerShape(3.dp))
                    .background(barColor)
            )
        }
    }
}

// Individual Log Event List item row
@Composable
fun EventLogItemRow(log: AnalyticsEventEntity) {
    val formatter = remember { SimpleDateFormat("HH:mm:ss.S", Locale.getDefault()) }
    val timeStr = formatter.format(Date(log.timestamp))

    val (badgeText, badgeColor) = when (log.eventType) {
        "view" -> "曝光(PV)" to SlateTextSecondary
        "click" -> "点击(CLK)" to AccentNeonBlue
        "like" -> "点赞(LIK)" to AccentSunsetPink
        "favorite" -> "收藏(FAV)" to OrangeStar
        else -> "分享(SHR)" to AccentNeonBlue
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SlateCard)
            .border(0.5.dp, PolishBorder, RoundedCornerShape(12.dp))
            .padding(10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .background(badgeColor.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                    .border(0.5.dp, badgeColor, RoundedCornerShape(4.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(text = badgeText, color = badgeColor, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            }

            Column {
                Text(text = log.adTitle, color = SlateTextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Text(text = "渠道: ${log.channel} | 结构: ${log.cardType}", color = SlateTextSecondary, fontSize = 10.sp)
            }
        }

        Text(text = timeStr, color = SlateTextSecondary, fontSize = 10.sp)
    }
}
