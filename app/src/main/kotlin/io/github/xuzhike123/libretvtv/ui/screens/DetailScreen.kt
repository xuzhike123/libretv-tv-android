package io.github.xuzhike123.libretvtv.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.input.key.Key
import io.github.xuzhike123.libretvtv.data.AppleCmsClient
import io.github.xuzhike123.libretvtv.data.CmsDetail
import io.github.xuzhike123.libretvtv.data.LocalStore
import io.github.xuzhike123.libretvtv.data.PendingPlay
import io.github.xuzhike123.libretvtv.data.SourceRepository
import io.github.xuzhike123.libretvtv.data.VodSource
import io.github.xuzhike123.libretvtv.ui.components.FocusableSurface
import io.github.xuzhike123.libretvtv.ui.components.SectionHeader
import io.github.xuzhike123.libretvtv.ui.util.onTvKey
import kotlinx.coroutines.launch
import java.net.URLDecoder

/**
 * 详情页：根据 videoId(=vodId@sourceUrl) 拉取真实详情，展示多播放源与选集。
 */
@Composable
fun DetailScreen(
    videoId: String,
    modifier: Modifier = Modifier,
    onPlay: (url: String, title: String, source: String, videoId: String) -> Unit,
    onBack: () -> Unit
) {
    val decoded = remember(videoId) { runCatching { URLDecoder.decode(videoId, "UTF-8") }.getOrDefault(videoId) }
    val cut = decoded.split("@", limit = 2)
    val vodId = cut.getOrNull(0) ?: decoded
    val sourceUrl = cut.getOrNull(1) ?: ""

    var detail by remember { mutableStateOf<CmsDetail?>(null) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf("") }
    var selectedSource by remember { mutableStateOf(0) }
    val scope = rememberCoroutineScope()

    var isFav by remember { mutableStateOf(LocalStore.isFavorite(decoded)) }

    LaunchedEffect(decoded) {
        loading = true
        error = ""
        detail = null
        val source = SourceRepository.subscriptions.value
            .flatMap { it.vodSources }
            .firstOrNull { it.url == sourceUrl }
            ?: VodSource(name = sourceUrl, url = sourceUrl)
        val result = AppleCmsClient.detail(source, vodId)
        if (result == null) {
            error = "详情拉取失败，源可能不可用或需要代理。"
        } else if (result.episodes.isEmpty()) {
            error = "该影片暂无可用播放地址。"
        } else {
            detail = result
        }
        loading = false
    }

    val d = detail
    Row(
        modifier = modifier
            .fillMaxSize()
            .padding(28.dp)
            .onTvKey { key ->
                if (key == Key.Back || key == Key.Escape) { onBack(); true } else false
            },
        horizontalArrangement = Arrangement.spacedBy(40.dp)
    ) {
        Box(
            modifier = Modifier
                .width(360.dp)
                .aspectRatio(2f / 3f)
                .clip(RoundedCornerShape(16.dp))
                .background(
                    Brush.linearGradient(listOf(Color(0xFF1F4B5C), Color(0xFF0E2A33)))
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = (d?.name ?: "影").take(2),
                fontSize = 96.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White.copy(alpha = 0.22f)
            )
        }

        LazyColumn(modifier = Modifier.weight(1f)) {
            item {
                Text(
                    text = d?.name ?: "加载中…",
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
                    listOfNotNull(
                        d?.typeName?.takeIf { it.isNotEmpty() },
                        d?.year?.takeIf { it.isNotEmpty() },
                        d?.remarks?.takeIf { it.isNotEmpty() },
                        d?.sourceName
                    ).forEach { tag ->
                        Text(text = tag, fontSize = 22.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            if (loading) {
                item { Text("正在拉取详情…", fontSize = 24.sp, color = MaterialTheme.colorScheme.primary) }
            }
            if (error.isNotEmpty()) {
                item { Text(error, fontSize = 24.sp, color = MaterialTheme.colorScheme.error) }
            }
            if (d != null) {
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                        FocusableSurface(
                            onClick = {
                                val ep = d.episodes.first()
                                onPlay(ep.url, d.name + " " + ep.name, d.sourceName, decoded)
                            },
                            shape = RoundedCornerShape(32.dp),
                            containerColor = MaterialTheme.colorScheme.primary
                        ) {
                            Text(
                                text = "立即播放", fontSize = 26.sp,
                                color = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.padding(horizontal = 34.dp, vertical = 20.dp)
                            )
                        }
                        FocusableSurface(
                            onClick = {
                                LocalStore.toggleFavorite(decoded, d.name)
                                isFav = !isFav
                            },
                            shape = RoundedCornerShape(32.dp)
                        ) {
                            Text(
                                text = if (isFav) "已收藏" else "收藏", fontSize = 26.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(horizontal = 34.dp, vertical = 20.dp)
                            )
                        }
                    }
                }
                item {
                    if (d.desc.isNotEmpty()) {
                        Text(
                            text = d.desc, fontSize = 24.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 24.dp, bottom = 16.dp)
                        )
                    }
                }
                if (d.playSources.size > 1) {
                    item {
                        SectionHeader(title = "播放源", subtitle = "切换后保留当前选集位置")
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            d.playSources.forEachIndexed { idx, ps ->
                                FocusableSurface(
                                    onClick = { selectedSource = idx },
                                    shape = RoundedCornerShape(12.dp),
                                    containerColor = if (idx == selectedSource)
                                        MaterialTheme.colorScheme.surfaceVariant else Color.Transparent,
                                    modifier = Modifier.width(220.dp)
                                ) {
                                    Text(
                                        text = ps.name, fontSize = 24.sp,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
                item {
                    val episodes = d.playSources.getOrNull(selectedSource)?.episodes ?: emptyList()
                    SectionHeader(
                        title = "选集",
                        subtitle = "共 ${episodes.size} 集"
                    )
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(20.dp),
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp)
                    ) {
                        items(episodes) { ep ->
                            FocusableSurface(
                                onClick = { onPlay(ep.url, d.name + " " + ep.name, d.sourceName, decoded) },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.width(180.dp)
                            ) {
                                Text(
                                    text = ep.name.ifEmpty { "播放" }, fontSize = 24.sp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 26.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
