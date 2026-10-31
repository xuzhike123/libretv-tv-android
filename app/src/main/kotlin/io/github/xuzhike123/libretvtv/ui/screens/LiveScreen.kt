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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.foundation.lazy.list.TvLazyColumn
import androidx.tv.foundation.lazy.list.TvLazyRow
import androidx.tv.foundation.lazy.list.items
import io.github.xuzhike123.libretvtv.data.liveChannels
import io.github.xuzhike123.libretvtv.data.liveGroups
import io.github.xuzhike123.libretvtv.ui.components.FocusableSurface
import io.github.xuzhike123.libretvtv.ui.components.SectionHeader

/**
 * 直播页：左侧分组 + 右侧频道宫格（含当前/下一个节目）。
 * 注意：直播/IPTV 能力源自 LibreTV 社区分支，上游仓库文档未列出。
 */
@Composable
fun LiveScreen(modifier: Modifier = Modifier) {
    val groups = liveGroups()
    val channels = liveChannels()
    var selectedGroup by remember { mutableStateOf(groups.first()) }

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
            groups.forEach { group ->
                FocusableSurface(
                    onClick = { selectedGroup = group },
                    shape = RoundedCornerShape(14.dp),
                    containerColor = if (group == selectedGroup) {
                        MaterialTheme.colorScheme.surfaceVariant
                    } else {
                        androidx.compose.ui.graphics.Color.Transparent
                    },
                    modifier = Modifier
                        .height(72.dp)
                        .fillMaxWidth()
                ) {
                    Text(
                        text = group,
                        fontSize = 26.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 20.dp)
                    )
                }
            }
        }

        TvLazyColumn(modifier = Modifier.weight(1f)) {
            item {
                SectionHeader(title = selectedGroup, subtitle = "按 EPG 显示当前节目")
            }
            channels.chunked(4).forEach { rowChannels ->
                item {
                    TvLazyRow(
                        horizontalArrangement = Arrangement.spacedBy(24.dp),
                        contentPadding = PaddingValues(vertical = 16.dp)
                    ) {
                        items(rowChannels) { channel ->
                            FocusableSurface(
                                onClick = { },
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.width(300.dp)
                            ) {
                                Row(modifier = Modifier.padding(22.dp)) {
                                    Text(
                                        text = channel.number,
                                        fontSize = 26.sp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Column(modifier = Modifier.padding(start = 16.dp)) {
                                        Text(
                                            text = channel.name,
                                            fontSize = 26.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "正在播出：" + channel.nowPlaying,
                                            fontSize = 22.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = "接下来：" + channel.next,
                                            fontSize = 22.sp,
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
