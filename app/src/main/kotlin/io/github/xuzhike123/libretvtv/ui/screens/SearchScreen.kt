package io.github.xuzhike123.libretvtv.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import io.github.xuzhike123.libretvtv.data.AppleCmsClient
import io.github.xuzhike123.libretvtv.data.CmsVideo
import io.github.xuzhike123.libretvtv.data.LocalStore
import io.github.xuzhike123.libretvtv.data.SourceRepository
import io.github.xuzhike123.libretvtv.data.UiSettings
import io.github.xuzhike123.libretvtv.data.VideoItem
import io.github.xuzhike123.libretvtv.ui.components.FocusableSurface
import io.github.xuzhike123.libretvtv.ui.components.SectionHeader
import io.github.xuzhike123.libretvtv.ui.components.VideoCard
import kotlinx.coroutines.launch

/**
 * 搜索页：屏上键盘输入关键词，跨所有已启用点播源聚合检索。
 */
@Composable
fun SearchScreen(
    modifier: Modifier = Modifier,
    settings: UiSettings,
    onVideoClick: (VideoItem) -> Unit
) {
    var query by remember { mutableStateOf("") }
    val results = remember { mutableStateListOf<VideoItem>() }
    var searching by remember { mutableStateOf(false) }
    var hasSearched by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    suspend fun doSearch(keyword: String) {
        val kw = keyword.trim()
        if (kw.isEmpty()) return
        searching = true
        errorMessage = ""
        hasSearched = true
        results.clear()
        try {
            val sources = SourceRepository.enabledVodSources().take(8)
            if (sources.isEmpty()) {
                errorMessage = "尚未启用任何点播源，请先到「片源」页添加并启用订阅。"
            } else {
                val merged = mutableListOf<CmsVideo>()
                sources.forEach { src ->
                    val r = runCatching { AppleCmsClient.search(src, kw) }.getOrDefault(emptyList())
                    r.forEach {
                        if (merged.none { it.name == it.name && it.sourceUrl == it.sourceUrl }) merged.add(it)
                    }
                }
                results.addAll(merged.map { toItem(it) })
                if (merged.isEmpty()) errorMessage = "没有找到相关影片，可换个关键词或检查源是否可用。"
            }
        } finally {
            searching = false
        }
    }

    Row(
        modifier = modifier
            .fillMaxSize()
            .padding(28.dp),
        horizontalArrangement = Arrangement.spacedBy(40.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = if (query.isEmpty()) "输入片名后按“搜索”" else query,
                fontSize = 34.sp,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier
                    .height(96.dp)
                    .padding(bottom = 20.dp)
            )

            if (!hasSearched) {
                val letters = remember { ('A'..'Z').map { it.toString() } }
                val digits = remember { ('0'..'9').map { it.toString() } }
                val keys = remember { digits + letters }
                keys.chunked(9).forEach { rowKeys ->
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        rowKeys.forEach { key ->
                            KeyButton(text = key, onClick = { if (query.length < 24) query += key })
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    KeyButton(text = "空格", width = 2, onClick = { query += " " })
                    KeyButton(text = "删除", width = 2, onClick = { query = query.dropLast(1) })
                    KeyButton(text = "清空", width = 2, onClick = { query = "" })
                    KeyButton(text = "搜索", width = 2, onClick = { scope.launch { doSearch(query) } })
                }
            } else {
                if (searching) {
                    Text("正在检索 ${SourceRepository.enabledVodSources().size} 个点播源…", fontSize = 24.sp, color = MaterialTheme.colorScheme.primary)
                } else if (errorMessage.isNotEmpty()) {
                    Text(errorMessage, fontSize = 24.sp, color = MaterialTheme.colorScheme.error)
                } else {
                    Text("找到 ${results.size} 条结果", fontSize = 24.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(top = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    items(results.chunked(5)) { rowItems ->
                        LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(26.dp),
                        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp)
                    ) {
                            items(rowItems) { item ->
                                VideoCard(
                                    video = item,
                                    settings = settings,
                                    onClick = {
                                        LocalStore.addHistory(
                                            io.github.xuzhike123.libretvtv.data.HistoryEntry(
                                                videoId = item.id,
                                                title = item.title,
                                                source = item.source,
                                                positionMs = 0, durationMs = 0, updatedAt = System.currentTimeMillis()
                                            )
                                        )
                                        onVideoClick(item)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        Column(modifier = Modifier.width(420.dp)) {
            if (!hasSearched) {
                SectionHeader(title = "搜索提示")
                Text(
                    text = "在左侧键盘输入关键词，聚焦「搜索」按确认即可跨全部已启用点播源聚合检索。",
                    fontSize = 22.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp)
                )
            } else {
                SectionHeader(title = "操作")
                FocusableSurface(
                    onClick = { hasSearched = false; results.clear(); errorMessage = "" },
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Text("返回重新搜索", fontSize = 22.sp, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(horizontal = 22.dp, vertical = 12.dp))
                }
            }
        }
    }
}

private fun toItem(v: CmsVideo): VideoItem = VideoItem(
    id = v.id,
    title = v.name,
    source = v.sourceName,
    duration = v.remarks,
    secondaryLine = v.typeName,
    pic = v.pic
)

@Composable
private fun KeyButton(
    text: String,
    width: Int = 1,
    onClick: () -> Unit
) {
    val buttonWidth: Dp = 88.dp * width
    FocusableSurface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .height(70.dp)
            .width(buttonWidth)
    ) {
        Text(
            text = text,
            fontSize = 26.sp,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
