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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.collectAsState
import io.github.xuzhike123.libretvtv.data.SourceRepository
import io.github.xuzhike123.libretvtv.data.Subscription
import io.github.xuzhike123.libretvtv.ui.components.FocusableSurface
import io.github.xuzhike123.libretvtv.ui.components.SectionHeader
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 片源管理：真实订阅的拉取 / 解析 / 持久化 / 启用 / 同步 / 删除。
 */
@Composable
fun SourcesScreen(modifier: Modifier = Modifier) {
    val subs by SourceRepository.subscriptions.collectAsState()
    val busy by SourceRepository.busy.collectAsState()
    val scope = rememberCoroutineScope()

    var showAddDialog by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    var newUrl by remember { mutableStateOf("") }

    val enabledVod = subs.filter { it.enabled }.sumOf { it.vodCount }
    val enabledLive = subs.filter { it.enabled }.sumOf { it.liveCount }

    if (showAddDialog) {
        AddSubscriptionDialog(
            value = newUrl,
            busy = busy,
            onValueChange = { newUrl = it },
            onConfirm = {
                val u = newUrl.trim()
                if (u.isNotEmpty()) scope.launch { SourceRepository.addSubscription(u) }
                newUrl = ""
                showAddDialog = false
            },
            onDismiss = { newUrl = ""; showAddDialog = false }
        )
    }

    if (showExportDialog) {
        val allVod = subs.filter { it.enabled }.flatMap { it.vodSources }
        val allLive = subs.filter { it.enabled }.flatMap { it.liveSources }
        AlertDialog(
            onDismissRequest = { showExportDialog = false },
            confirmButton = { TextButton(onClick = { showExportDialog = false }) { Text("知道了") } },
            title = { Text("导出并分享") },
            text = {
                Text(
                    "当前共 ${allVod.size} 个点播源、${allLive.size} 个直播源。\n\n" +
                        (if (allVod.isEmpty() && allLive.isEmpty())
                            "暂无可导出数据源，请先添加订阅并等待同步成功。"
                        else
                            "点播源示例：\n" + allVod.take(3).joinToString("\n") { "· ${it.name} (${it.url})" } +
                            "\n直播源示例：\n" + allLive.take(3).joinToString("\n") { "· ${it.name} (${it.url})" })
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
            SectionHeader(
                title = "数据源概览",
                subtitle = "已启用点播源 $enabledVod 个 · 直播源 $enabledLive 个"
            )
        }

        item {
            SectionHeader(title = "远程订阅", subtitle = "LibreTV-SourceList / TVBOX 配置")
        }
        item {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                items(subs) { sub ->
                    SubscriptionCard(
                        sub = sub,
                        onToggle = { scope.launch { SourceRepository.setEnabled(sub.url, !sub.enabled) } },
                        onResync = { scope.launch { SourceRepository.resync(sub) } },
                        onDelete = { SourceRepository.removeSubscription(sub.url) }
                    )
                }

                if (subs.isEmpty()) {
                    item {
                        Column(modifier = Modifier.width(420.dp).padding(vertical = 24.dp)) {
                            Text(
                                text = "还没有任何订阅。点击「添加订阅地址」粘贴 LibreTV 源列表 JSON 地址；或点「载入示例」一键填入官方公开源。",
                                fontSize = 22.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            FocusableSurface(
                                onClick = { scope.launch { SourceRepository.addSubscription(SourceRepository.EXAMPLE_SUBSCRIPTION_URL) } },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.width(300.dp).padding(top = 16.dp)
                            ) {
                                Text(
                                    text = "载入示例订阅",
                                    fontSize = 26.sp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 24.dp)
                                )
                            }
                        }
                    }
                }

                item {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        FocusableSurface(
                            onClick = { showAddDialog = true },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.width(300.dp)
                        ) {
                            Text(
                                text = "添加订阅地址",
                                fontSize = 26.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 24.dp)
                            )
                        }
                        FocusableSurface(
                            onClick = { showExportDialog = true },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.width(300.dp)
                        ) {
                            Text(
                                text = "导出并分享",
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
}

@Composable
private fun SubscriptionCard(
    sub: Subscription,
    onToggle: () -> Unit,
    onResync: () -> Unit,
    onDelete: () -> Unit
) {
    val fmt = remember { SimpleDateFormat("MM-dd HH:mm", Locale.CHINA) }
    FocusableSurface(
        onClick = onToggle,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.width(460.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = sub.name.ifEmpty { "未命名订阅" },
                fontSize = 26.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = sub.url,
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )
            Text(
                text = "点播 ${sub.vodCount} · 直播 ${sub.liveCount}",
                fontSize = 22.sp,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(top = 10.dp)
            )
            if (sub.lastError.isNotEmpty()) {
                Text(
                    text = "同步失败：${sub.lastError}",
                    fontSize = 18.sp,
                    color = Color(0xFFEF5350),
                    modifier = Modifier.padding(top = 6.dp)
                )
            } else if (sub.syncedAt > 0) {
                Text(
                    text = "已同步 ${fmt.format(Date(sub.syncedAt))}",
                    fontSize = 18.sp,
                    color = Color(0xFF7EE081),
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
            Text(
                text = if (sub.enabled) "已启用（聚焦按确认停用）" else "已停用（聚焦按确认启用）",
                fontSize = 18.sp,
                color = if (sub.enabled) MaterialTheme.colorScheme.primary else Color(0xFFFFB74D),
                modifier = Modifier.padding(top = 6.dp)
            )
            androidx.compose.foundation.layout.Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(top = 12.dp)
            ) {
                FocusableSurface(
                    onClick = onResync,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.width(140.dp)
                ) {
                    Text(
                        text = "重新同步",
                        fontSize = 20.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp)
                    )
                }
                FocusableSurface(
                    onClick = onDelete,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.width(140.dp)
                ) {
                    Text(
                        text = "删除",
                        fontSize = 20.sp,
                        color = Color(0xFFEF5350),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun AddSubscriptionDialog(
    value: String,
    busy: Boolean,
    onValueChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onConfirm, enabled = !busy) { Text("确定") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
        title = { Text("添加订阅地址") },
        text = {
            Column {
                OutlinedTextField(
                    value = value,
                    onValueChange = onValueChange,
                    label = { Text("LibreTV-SourceList JSON 地址") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (busy) {
                    Text(
                        text = "正在拉取并解析…",
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 12.dp)
                    )
                }
            }
        }
    )
}
