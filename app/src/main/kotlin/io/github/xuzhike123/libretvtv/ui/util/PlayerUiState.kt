package io.github.xuzhike123.libretvtv.ui.util

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * 播放页与根布局之间共享的少量 UI 状态。
 *
 * 目前只有一个：播放页是否处于「全屏」画面比例。
 * 全屏时根布局会把左侧导航栏收起来，让视频真正占满整个屏幕；
 * 否则导航栏会一直占掉约 20% 的宽度，全屏也就名不副实。
 */
object PlayerUiState {

    /** true = 播放页当前为全屏（裁剪填满 / 拉伸全屏） */
    var fullscreen by mutableStateOf(false)
        private set

    fun updateFullscreen(value: Boolean) {
        fullscreen = value
    }
}
