package io.github.xuzhike123.libretvtv.ui.util

import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type

/**
 * 遥控器按键统一入口：只在按下时回调，避免 DOWN / UP 各触发一次。
 * 传出的 Key 可直接与 Key.Back、Key.DirectionCenter 等常量比较。
 */
fun Modifier.onTvKey(block: (key: Key) -> Boolean): Modifier =
    this.onKeyEvent { event ->
        if (event.type == KeyEventType.KeyDown) {
            block(event.key)
        } else {
            false
        }
    }
