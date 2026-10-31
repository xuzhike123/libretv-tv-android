package io.github.xuzhike123.libretvtv.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.foundation.lazy.list.TvLazyColumn
import androidx.tv.foundation.lazy.list.TvLazyRow
import androidx.tv.foundation.lazy.list.items
import io.github.xuzhike123.libretvtv.data.findVideo
import io.github.xuzhike123.libretvtv.data.sourceItems
import io.github.xuzhike123.libretvtv.ui.components.FocusableSurface
import io.github.xuzhike123.libretvtv.ui.components.SectionHeader
import androidx.compose.ui.input.key.Key
import io.github.xuzhike123.libretvtv.ui.util.onTvKey

/**
 * 详情页：海报 + 元信息 + 选集 + 换源测速。
 */
@Composable
fun DetailScreen(
    videoId: String,
    modifier: Modifier = Modifier,
    onPlay: (String) -> Unit,
    onBack: () -> Unit
) {
    val video = findVideo(videoId)
    val sources = sourceItems()

    Row(
        modifier = modifier
            .fillMaxSize()
            .padding(28.dp)
            .onTvKey { key ->
                if (key == Key.Back || key == Key.Escape) {
                    onBack()
                    true
                } else {
                    false
                }
            },
        horizontalArrangement = Arrangement.spacedBy(40.dp)
    ) {
        Box(
            modifier = Modifier
                .width(360.dp)
                .aspectRatio(2f / 3f)
                .clip(RoundedCornerShape(16.dp))
                .background(
                    Brush.linearGradient(
                        listOf(Color(0xFF1F4B5C), Color(0xFF0E2A33))
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = video.title.take(2),
                fontSize = 96.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White.copy(alpha = 0.22f)
            )
        }

        TvLazyColumn(modifier = Modifier.weight(1f)) {
            item {
                Text(
                    text = video.title,
                    fontSize = 44.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
            item {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.padding(top = 16.dp, bottom = 24.dp)
                ) {
                    listOf(video.source, video.duration, "示例类型 · 剧情").forEach { tag ->
                        Text(
                            text = tag,
                            fontSize = 22.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                    FocusableSurface(
                        onClick = { onPlay(video.id) },
                        shape = RoundedCornerShape(32.dp),
                        containerColor = MaterialTheme.colorScheme.primary
                    ) {
                        Text(
                            text = "立即播放",
                            fontSize = 26.sp,
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.padding(horizontal = 34.dp, vertical = 20.dp)
                        )
                    }
                    FocusableSurface(
                        onClick = { },
                        shape = RoundedCornerShape(32.dp)
                    ) {
                        Text(
                            text = "收藏",
                            fontSize = 26.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = 34.dp, vertical = 20.dp)
                        )
                    }
                    FocusableSurface(
                        onClick = { },
                        shape = RoundedCornerShape(32.dp)
                    ) {
                        Text(
                            text = "下载本集",
                            fontSize = 26.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = 34.dp, vertical = 20.dp)
                        )
                    }
                }
            }
            item {
                Text(
                    text = "这是一段用于排版验证的示例简介文字，真实数据来自各数据源返回的详情字段。LibreTV 本身不存储影片内容，只做检索与播放入口。",
                    fontSize = 26.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 24.dp, bottom = 16.dp)
                )
            }
            item {
                SectionHeader(title = "选集", subtitle = "共 24 集")
                TvLazyRow(
                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                    contentPadding = PaddingValues(vertical = 16.dp)
                ) {
                    items((1..24).toList()) { episode ->
                        FocusableSurface(
                            onClick = { onPlay(video.id) },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.width(180.dp)
                        ) {
                            Text(
                                text = "第 $episode 集",
                                fontSize = 26.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 28.dp)
                            )
                        }
                    }
                }
            }
            item {
                SectionHeader(title = "换源测速", subtitle = "切换后保留当前集数位置")
                TvLazyRow(
                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                    contentPadding = PaddingValues(vertical = 16.dp)
                ) {
                    items(sources) { source ->
                        FocusableSurface(
                            onClick = { },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.width(260.dp)
                        ) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                Text(
                                    text = source.name,
                                    fontSize = 26.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                val status = if (source.healthy) {
                                    source.latencyMs.toString() + " ms"
                                } else {
                                    "不可用"
                                }
                                Text(
                                    text = status,
                                    fontSize = 22.sp,
                                    color = if (source.healthy) {
                                        Color(0xFF7EE081)
                                    } else {
                                        Color(0xFFFFB74D)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
