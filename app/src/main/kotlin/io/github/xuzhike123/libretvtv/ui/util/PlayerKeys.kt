package io.github.xuzhike123.libretvtv.ui.util

import android.view.KeyEvent
import androidx.compose.ui.input.key.Key

/**
 * 遥控器按键的统一分发口（仅播放页使用）。
 *
 * 为什么不直接在播放页用 Compose 的 onKeyEvent：
 * 可聚焦节点会随界面状态变化（比如底部操作栏收起后其子项被移出组合），
 * 一旦当前没有任何节点持有焦点，Compose 就不会把按键分发给任何
 * onKeyEvent / onPreviewKeyEvent，遥控器表现为“完全失灵”。
 *
 * 这里改由 Activity 的 dispatchKeyEvent 统一接收，先交给播放页处理；
 * 只有播放页不消费的按键才继续走系统/Compose 的常规分发（焦点导航等）。
 */
object PlayerKeys {

    private var handler: ((Key) -> Boolean)? = null

    /** 播放页进入组合时注册；onDispose 里必须注销 */
    fun register(block: (Key) -> Boolean) {
        handler = block
    }

    fun unregister() {
        handler = null
    }

    /** 由 MainActivity.dispatchKeyEvent 调用，返回 true 表示已消费 */
    fun dispatch(event: KeyEvent): Boolean {
        if (event.action != KeyEvent.ACTION_DOWN) return false
        val key = when (event.keyCode) {
            KeyEvent.KEYCODE_DPAD_UP -> Key.DirectionUp
            KeyEvent.KEYCODE_DPAD_DOWN -> Key.DirectionDown
            KeyEvent.KEYCODE_DPAD_LEFT -> Key.DirectionLeft
            KeyEvent.KEYCODE_DPAD_RIGHT -> Key.DirectionRight
            KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> Key.DirectionCenter
            KeyEvent.KEYCODE_MENU, KeyEvent.KEYCODE_INFO -> Key.Menu
            KeyEvent.KEYCODE_BACK, KeyEvent.KEYCODE_ESCAPE -> Key.Back
            else -> return false
        }
        return handler?.invoke(key) ?: false
    }
}
