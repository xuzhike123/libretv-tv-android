package io.github.xuzhike123.libretvtv.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.collectAsState
import io.github.xuzhike123.libretvtv.data.LiveChannel
import io.github.xuzhike123.libretvtv.data.M3uParser
import io.github.xuzhike123.libretvtv.data.SourceRepository
import io.github.xuzhike123.libretvtv.ui.components.FocusableSurface
import io.github.xuzhike123.libretvtv.ui.components.SectionHeader

/**
 * 直播页：拉取已启用直播源的 M3U，按分组浏览并播放。
 */
@Composable
fun LiveScreen(
    modifier: Modifier = Modifier,
    onChannelClick: (LiveChannel) -> Unit
) {
    val subs by SourceRepository.subscriptions.collectAsState()
    val customLive by SourceRepository.customLive.collectAsState()
    val liveSources = remember(subs, customLive) { SourceRepository.enabledLiveSources() }

    var channels by remember { mutableStateOf<List<LiveChannel>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf("") }
    var selectedGroup by remember { mutableStateOf("") }

    LaunchedEffect(liveSources) {
        loading = true
        error = ""
        channels = emptyList()
        if (liveSources.isEmpty()) {
            error = "尚未启用任何直播源，请先到「片源」页添加并启用订阅。"
            loading = false
            return@LaunchedEffect
        }
        try {
            val all = mutableListOf<LiveChannel>()
            // 优先加载用户单独导入的自定义直播源，保证「导入即在直播页生效」；再补齐订阅内置源
            val ordered = customLive + (liveSources - customLive)
            ordered.take(6).forEach { src ->
                val r = runCatching { M3uParser.fetchAndParse(src) }.getOrDefault(emptyList())
                r.forEach { ch -> if (all.none { it.url == ch.url }) all.add(ch) }
            }
            channels = all.take(300)
            selectedGroup = all.firstOrNull()?.group ?: ""
            if (all.isEmpty()) error = "直播列表为空，源地址可能失效或网络不可达。"
        } finally {
            loading = false
        }
    }

    val groups = remember(channels) { channels.map { it.group }.distinct() }
    val current = if (selectedGroup.isEmpty()) channels else channels.filter { it.group == selectedGroup }

    Row(
        modifier = modifier
            .fillMaxSize()
            .padding(28.dp),
        horizontalArrangement = Arrangement.spacedBy(28.dp)
    ) {
        Column(
            modifier = Modifier
                .width(300.dp)
                .padding(top = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SectionHeader(title = "分组")
            if (loading) Text("拉取直播列表中…", fontSize = 22.sp, color = MaterialTheme.colorScheme.primary)
            groups.forEach { group ->
                FocusableSurface(
                    onClick = { selectedGroup = group },
                    shape = RoundedCornerShape(14.dp),
                    containerColor = if (group == selectedGroup) {
                        MaterialTheme.colorScheme.surfaceVariant
                    } else {
                        Color.Transparent
                    },
                    modifier = Modifier.height(72.dp).fillMaxWidth()
                ) {
                    Text(
                        text = group, fontSize = 26.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 20.dp)
                    )
                }
            }
        }

        LazyColumn(modifier = Modifier.weight(1f)) {
            item {
                if (error.isNotEmpty()) {
                    Text(error, fontSize = 24.sp, color = MaterialTheme.colorScheme.error)
                } else {
                    SectionHeader(title = selectedGroup.ifEmpty { "全部频道" }, subtitle = "共 ${current.size} 个")
                }
            }
            current.chunked(4).forEach { rowChannels ->
                item {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(24.dp),
                        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp)
                    ) {
                        items(rowChannels) { channel ->
                            FocusableSurface(
                                onClick = { onChannelClick(channel) },
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.width(320.dp)
                            ) {
                                Row(modifier = Modifier.padding(22.dp)) {
                                    Column(modifier = Modifier.padding(start = 16.dp)) {
                                        Text(
                                            text = channel.name, fontSize = 26.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = channel.group, fontSize = 22.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
