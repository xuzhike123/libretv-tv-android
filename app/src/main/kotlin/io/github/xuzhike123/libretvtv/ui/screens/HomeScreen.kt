package io.github.xuzhike123.libretvtv.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.collectAsState
import io.github.xuzhike123.libretvtv.data.AppleCmsClient
import io.github.xuzhike123.libretvtv.data.ContentRow
import io.github.xuzhike123.libretvtv.data.LocalStore
import io.github.xuzhike123.libretvtv.data.SourceRepository
import io.github.xuzhike123.libretvtv.data.UiSettings
import io.github.xuzhike123.libretvtv.data.VideoItem
import io.github.xuzhike123.libretvtv.ui.components.ContentRowSection
import io.github.xuzhike123.libretvtv.ui.components.FocusableSurface
import io.github.xuzhike123.libretvtv.ui.components.SectionHeader

/**
 * 首页：搜索入口 + 最近更新（首个启用点播源）+ 收藏 + 观看历史。
 */
@Composable
fun HomeScreen(
    settings: UiSettings,
    modifier: Modifier = Modifier,
    onSearch: () -> Unit,
    onVideoClick: (VideoItem) -> Unit
) {
    val subs by SourceRepository.subscriptions.collectAsState()
    var latest by remember { mutableStateOf<List<VideoItem>>(emptyList()) }
    var loadingLatest by remember { mutableStateOf(true) }
    var latestError by remember { mutableStateOf("") }

    LaunchedEffect(subs) {
        loadingLatest = true
        latestError = ""
        latest = emptyList()
        var list = emptyList<io.github.xuzhike123.libretvtv.data.CmsVideo>()
        val src = SourceRepository.firstEnabledVodSource()
        if (src != null) {
            // 先取最新更新；若为空（部分源不带关键词不返回列表），退回热门词检索
            for (attempt in 1..2) {
                list = runCatching { AppleCmsClient.latest(src) }.getOrDefault(emptyList())
                if (list.isNotEmpty()) break
                kotlinx.coroutines.delay(800)
            }
            if (list.isEmpty()) {
                list = runCatching { AppleCmsClient.search(src, "电影") }.getOrDefault(emptyList())
            }
            latest = list.map {
                VideoItem(id = it.id, title = it.name, source = it.sourceName, duration = it.remarks, secondaryLine = it.typeName, pic = it.pic)
            }
            if (list.isEmpty()) latestError = "源未返回内容，可稍后重试或换源。"
        } else {
            latestError = "尚未启用任何点播源，请先到「片源」添加订阅。"
        }
        loadingLatest = false
    }

    val favorites = remember { LocalStore.getFavorites() }.map { (id, title) ->
        VideoItem(id = id, title = title, source = "收藏", duration = "")
    }
    val history = LocalStore.getHistory().map { h ->
        VideoItem(
            id = h.videoId,
            title = h.title,
            source = h.source,
            duration = if (h.durationMs > 0) "看到 ${(h.positionMs * 100 / h.durationMs)}%" else "",
            progressPercent = if (h.durationMs > 0) (h.positionMs * 100 / h.durationMs).toInt() else null
        )
    }

    LazyColumn(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 28.dp, vertical = 16.dp)
    ) {
        item {
            FocusableSurface(
                onClick = onSearch,
                modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp)
            ) {
                Text(
                    text = "🔍  搜索影片 / 剧集（聚焦按确认进入）",
                    fontSize = 34.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 28.dp)
                )
            }
        }

        item {
            SectionHeader(title = "最近更新", subtitle = if (loadingLatest) "加载中…" else latestError.ifEmpty { "来自已启用的点播源" })
        }
        if (!loadingLatest && latest.isNotEmpty()) {
            item {
                ContentRowSection(
                    row = ContentRow(title = "", items = latest),
                    settings = settings,
                    onVideoClick = onVideoClick
                )
            }
        }

        if (favorites.isNotEmpty()) {
            item {
                ContentRowSection(
                    row = ContentRow(title = "我的收藏", subtitle = "仅本机保存", items = favorites),
                    settings = settings,
                    onVideoClick = onVideoClick
                )
            }
        }

        if (history.isNotEmpty()) {
            item {
                ContentRowSection(
                    row = ContentRow(title = "观看历史", subtitle = "近 50 条", items = history),
                    settings = settings,
                    onVideoClick = onVideoClick
                )
            }
        }
    }
}
