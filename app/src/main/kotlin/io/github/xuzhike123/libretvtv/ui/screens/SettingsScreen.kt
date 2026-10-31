package io.github.xuzhike123.libretvtv.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.foundation.lazy.list.TvLazyColumn
import io.github.xuzhike123.libretvtv.data.AccentTheme
import io.github.xuzhike123.libretvtv.data.BufferLevel
import io.github.xuzhike123.libretvtv.data.CardStyle
import io.github.xuzhike123.libretvtv.data.MAX_UI_SCALE
import io.github.xuzhike123.libretvtv.data.MIN_UI_SCALE
import io.github.xuzhike123.libretvtv.data.PLAYBACK_SPEEDS
import io.github.xuzhike123.libretvtv.data.UiSettings
import io.github.xuzhike123.libretvtv.ui.components.FocusableSurface
import io.github.xuzhike123.libretvtv.ui.components.SectionHeader

/**
 * 设置页：外观 / 播放 / 数据源与隐私。
 * 说明：偏好项当前只保存在内存中，进程重启后恢复默认值。
 */
@Composable
fun SettingsScreen(
    settings: UiSettings,
    modifier: Modifier = Modifier,
    onSettingsChange: (UiSettings) -> Unit
) {
    Row(
        modifier = modifier
            .fillMaxSize()
            .padding(28.dp),
        horizontalArrangement = Arrangement.spacedBy(40.dp)
    ) {
        TvLazyColumn(modifier = Modifier.width(900.dp)) {
            item {
                SectionHeader(title = "外观", subtitle = "参照 SmartTube 的 UI 选项")
            }
            item {
                SettingsRow(
                    label = "主题配色",
                    valueText = accentName(settings.accent)
                ) {
                    onSettingsChange(settings.copy(accent = nextAccent(settings.accent)))
                }
            }
            item {
                SettingsRow(
                    label = "界面缩放",
                    valueText = String.format("%.2fx", settings.uiScale)
                ) {
                    val next = settings.uiScale + 0.05f
                    val clamped = if (next > MAX_UI_SCALE) MIN_UI_SCALE else next
                    onSettingsChange(settings.copy(uiScale = clamped))
                }
            }
            item {
                SettingsRow(
                    label = "卡片标题",
                    valueText = cardStyleName(settings.cardStyle)
                ) {
                    onSettingsChange(
                        settings.copy(
                            cardStyle = when (settings.cardStyle) {
                                CardStyle.SINGLE_LINE_SCROLL -> CardStyle.TWO_LINE
                                CardStyle.TWO_LINE -> CardStyle.SINGLE_LINE_SCROLL
                            }
                        )
                    )
                }
            }
            item {
                SettingsToggleRow(
                    label = "焦点动画预览",
                    checked = settings.animatedPreview
                ) {
                    onSettingsChange(settings.copy(animatedPreview = !settings.animatedPreview))
                }
            }
            item {
                SettingsToggleRow(
                    label = "自动隐藏界面",
                    checked = settings.autoHideUi
                ) {
                    onSettingsChange(settings.copy(autoHideUi = !settings.autoHideUi))
                }
            }
            item {
                SectionHeader(title = "播放")
            }
            item {
                SettingsRow(
                    label = "默认倍速",
                    valueText = String.format("%.2fx", settings.playbackSpeed)
                ) {
                    val index = PLAYBACK_SPEEDS.indexOf(settings.playbackSpeed)
                    val next = PLAYBACK_SPEEDS[(index + 1) % PLAYBACK_SPEEDS.size]
                    onSettingsChange(settings.copy(playbackSpeed = next))
                }
            }
            item {
                SettingsRow(
                    label = "视频缓冲",
                    valueText = bufferName(settings.bufferLevel)
                ) {
                    val next = when (settings.bufferLevel) {
                        BufferLevel.NONE -> BufferLevel.LOW
                        BufferLevel.LOW -> BufferLevel.MEDIUM
                        BufferLevel.MEDIUM -> BufferLevel.HIGH
                        BufferLevel.HIGH -> BufferLevel.NONE
                    }
                    onSettingsChange(settings.copy(bufferLevel = next))
                }
            }
            item {
                SettingsToggleRow(
                    label = "自动帧率 AFR",
                    checked = settings.afrEnabled
                ) {
                    onSettingsChange(settings.copy(afrEnabled = !settings.afrEnabled))
                }
            }
            item {
                SettingsToggleRow(
                    label = "自动连播下一集",
                    checked = settings.autoPlayNext
                ) {
                    onSettingsChange(settings.copy(autoPlayNext = !settings.autoPlayNext))
                }
            }
            item {
                SectionHeader(title = "数据源与隐私")
            }
            item {
                SettingsToggleRow(
                    label = "源头健康度自动停用",
                    checked = settings.healthBasedDisable
                ) {
                    onSettingsChange(settings.copy(healthBasedDisable = !settings.healthBasedDisable))
                }
            }
            item {
                SettingsToggleRow(
                    label = "记录观看历史",
                    checked = settings.keepHistory
                ) {
                    onSettingsChange(settings.copy(keepHistory = !settings.keepHistory))
                }
            }
        }

        Column(modifier = Modifier.weight(1f)) {
            SectionHeader(title = "说明")
            Text(
                text = "界面结构与播放选项参考 SmartTube(SmartTubeNext) 的 UI 设置，" +
                    "业务能力对应 LibreTV 的聚合搜索、换源测速、本机进度。" +
                    "所有开关当前只保存在内存中。",
                fontSize = 26.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SettingsRow(
    label: String,
    valueText: String,
    onClick: () -> Unit
) {
    FocusableSurface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(84.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                fontSize = 26.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = valueText,
                fontSize = 22.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 24.dp)
            )
        }
    }
}

@Composable
private fun SettingsToggleRow(
    label: String,
    checked: Boolean,
    onToggle: () -> Unit
) {
    FocusableSurface(
        onClick = onToggle,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(84.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                fontSize = 26.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            Switch(
                checked = checked,
                onCheckedChange = null,
                modifier = Modifier.padding(start = 24.dp)
            )
        }
    }
}

private fun accentName(accent: AccentTheme): String = when (accent) {
    AccentTheme.TEAL -> "青色（默认）"
    AccentTheme.DARK_GREY -> "暗灰"
    AccentTheme.RED -> "红色"
}

private fun nextAccent(accent: AccentTheme): AccentTheme = when (accent) {
    AccentTheme.TEAL -> AccentTheme.DARK_GREY
    AccentTheme.DARK_GREY -> AccentTheme.RED
    AccentTheme.RED -> AccentTheme.TEAL
}

private fun cardStyleName(cardStyle: CardStyle): String = when (cardStyle) {
    CardStyle.SINGLE_LINE_SCROLL -> "单行滚动"
    CardStyle.TWO_LINE -> "双行截断"
}

private fun bufferName(level: BufferLevel): String = when (level) {
    BufferLevel.NONE -> "无"
    BufferLevel.LOW -> "低"
    BufferLevel.MEDIUM -> "中"
    BufferLevel.HIGH -> "高"
}
