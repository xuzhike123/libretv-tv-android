package io.github.xuzhike123.libretvtv.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.foundation.lazy.list.TvLazyColumn
import androidx.tv.foundation.lazy.list.TvLazyRow
import androidx.tv.foundation.lazy.list.items
import io.github.xuzhike123.libretvtv.data.sourceItems
import io.github.xuzhike123.libretvtv.ui.components.FocusableSurface
import io.github.xuzhike123.libretvtv.ui.components.SectionHeader

/**
 * 片源管理：健康度、延迟、失败源的阶梯停用状态。
 * 数据为占位演示，未接入真实远程订阅。
 */
@Composable
fun SourcesScreen(modifier: Modifier = Modifier) {
    val sources = sourceItems()
    TvLazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(28.dp)
    ) {
        item {
            SectionHeader(title = "已配置片源", subtitle = "健康度 · 连续失败按阶梯时长停用")
        }
        item {
            TvLazyRow(
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                items(sources) { source ->
                    FocusableSurface(
                        onClick = { },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.width(300.dp)
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Text(
                                text = source.name,
                                fontSize = 26.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = source.address,
                                fontSize = 22.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            val statusText = if (source.healthy) {
                                source.latencyMs.toString() + " ms · 健康"
                            } else {
                                "已停用 · 30 分钟后重试"
                            }
                            Text(
                                text = statusText,
                                fontSize = 22.sp,
                                color = if (source.healthy) Color(0xFF7EE081) else Color(0xFFFFB74D)
                            )
                        }
                    }
                }
            }
        }
        item {
            SectionHeader(title = "远程订阅", subtitle = "LibreTV-SourceList JSON")
        }
        item {
            TvLazyRow(
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                items(listOf("添加订阅地址", "导出并分享")) { label ->
                    FocusableSurface(
                        onClick = { },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.width(300.dp)
                    ) {
                        Text(
                            text = label,
                            fontSize = 26.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 24.dp)
                        )
                    }
                }
            }
        }
    }
}
