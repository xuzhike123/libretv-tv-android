package io.github.xuzhike123.libretvtv.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
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
import io.github.xuzhike123.libretvtv.data.sourceItems
import io.github.xuzhike123.libretvtv.ui.components.FocusableSurface
import io.github.xuzhike123.libretvtv.ui.components.SectionHeader

/**
 * 片源管理：健康度、延迟、失败源的阶梯停用状态。
 * 远程订阅支持在端内添加 / 删除（内存态，演示用，未持久化）。
 */
@Composable
fun SourcesScreen(modifier: Modifier = Modifier) {
    val sources = sourceItems()
    val subscriptions = remember { mutableStateListOf<String>() }
    var showAddDialog by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    var newUrl by remember { mutableStateOf("") }

    if (showAddDialog) {
        AddSubscriptionDialog(
            value = newUrl,
            onValueChange = { newUrl = it },
            onConfirm = {
                val u = newUrl.trim()
                if (u.isNotEmpty()) subscriptions.add(u)
                newUrl = ""
                showAddDialog = false
            },
            onDismiss = {
                newUrl = ""
                showAddDialog = false
            }
        )
    }

    if (showExportDialog) {
        AlertDialog(
            onDismissRequest = { showExportDialog = false },
            confirmButton = {
                TextButton(onClick = { showExportDialog = false }) {
                    Text("知道了")
                }
            },
            title = { Text("导出订阅") },
            text = {
                Text(
                    if (subscriptions.isEmpty()) {
                        "当前还没有任何远程订阅地址。"
                    } else {
                        "已导出 ${subscriptions.size} 个订阅地址：\n" +
                            subscriptions.joinToString("\n")
                    }
                )
            }
        )
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(28.dp)
    ) {
        item {
            SectionHeader(title = "已配置片源", subtitle = "健康度 · 连续失败按阶梯时长停用")
        }
        item {
            LazyRow(
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
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                if (subscriptions.isEmpty()) {
                    item {
                        Text(
                            text = "暂无订阅，点击右侧「添加订阅地址」",
                            fontSize = 22.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 24.dp)
                        )
                    }
                }
                items(subscriptions) { url ->
                    FocusableSurface(
                        onClick = { subscriptions.remove(url) },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.width(420.dp)
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Text(
                                text = "订阅源",
                                fontSize = 22.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = url,
                                fontSize = 24.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "聚焦后按确认键删除",
                                fontSize = 18.sp,
                                color = Color(0xFFFFB74D)
                            )
                        }
                    }
                }
                items(listOf("添加订阅地址", "导出并分享")) { label ->
                    FocusableSurface(
                        onClick = {
                            if (label == "添加订阅地址") showAddDialog = true
                            else showExportDialog = true
                        },
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

@Composable
private fun AddSubscriptionDialog(
    value: String,
    onValueChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onConfirm) { Text("确定") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        },
        title = { Text("添加订阅地址") },
        text = {
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                label = { Text("LibreTV-SourceList JSON 地址") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }
    )
}
