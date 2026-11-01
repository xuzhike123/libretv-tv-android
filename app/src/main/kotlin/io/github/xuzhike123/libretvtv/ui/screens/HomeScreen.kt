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

/** 「最近更新」的分栏顺序 */
private val LATEST_CATEGORIES = listOf("电影", "电视", "动漫", "综艺", "体育", "其他")

/**
 * 把采集站自带的分类名（type_name）归到上面六类。
 *
 * 判定顺序有讲究，实测过的真实取值：国产剧 / 韩国剧 / 日本剧 / 欧美剧 / 泰国剧 → 电视；
 * 日韩动漫 / 国产动漫 / 动画片 → 动漫；大陆综艺 / 日韩综艺 → 综艺；
 * 剧情片 / 恐怖片 / 喜剧片 → 电影；另见「动漫电影」（同时含『动漫』与『电影』，按动漫算）。
 * 所以要先判动漫，否则「动画片」「动漫电影」会被『片』抢走。
 */
private fun classifyType(typeName: String): String {
    val t = typeName.trim()
    return when {
        t.contains("动漫") || t.contains("动画") -> "动漫"
        t.contains("综艺") -> "综艺"
        t.contains("体育") || t.contains("足球") || t.contains("篮球") || t.contains("赛事") -> "体育"
        t.contains("剧") -> "电视"
        t.contains("片") || t.contains("电影") -> "电影"
        else -> "其他"
    }
}

/**
 * 首页：搜索入口 + 最近更新（按电影/电视/动漫/综艺/体育/其他分栏）+ 收藏 + 观看历史。
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
        val sources = SourceRepository.enabledVodSources()
        if (sources.isEmpty()) {
            latestError = "尚未启用任何点播源，请先到「片源」添加订阅。"
        } else {
            // 采集站域名经常失效，只盯第一个源会一直转圈；这里依次试前 3 个
            for (src in sources.take(3)) {
                list = runCatching { AppleCmsClient.latestMulti(src, pages = 3) }.getOrDefault(emptyList())
                if (list.isNotEmpty()) break
            }
            // 都不带「最新列表」就退回关键词检索
            if (list.isEmpty()) {
                for (src in sources.take(3)) {
                    list = runCatching { AppleCmsClient.search(src, "电影") }.getOrDefault(emptyList())
                    if (list.isNotEmpty()) break
                }
            }
            latest = list.map {
                VideoItem(id = it.id, title = it.name, source = it.sourceName, duration = it.remarks, secondaryLine = it.typeName, pic = it.pic)
            }
            if (list.isEmpty()) latestError = "已启用的源均未返回内容，可稍后重试或换源。"
        }
        loadingLatest = false
    }

    // 按 电影 / 电视 / 动漫 / 综艺 / 体育 / 其他 分栏；空分类不显示
    val latestRows = remember(latest) {
        LATEST_CATEGORIES.mapNotNull { cat ->
            val items = latest.filter { classifyType(it.secondaryLine) == cat }
            if (items.isEmpty()) null
            else ContentRow(title = cat, subtitle = "${items.size} 部", items = items)
        }
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
        if (!loadingLatest && latestRows.isNotEmpty()) {
            items(latestRows) { row ->
                ContentRowSection(
                    row = row,
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
