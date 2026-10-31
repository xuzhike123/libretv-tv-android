package io.github.xuzhike123.libretvtv.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 大屏通用焦点容器：获得焦点时整体放大、加粗亮色描边并投射阴影光晕，
 * 这是 SmartTube / 电视端统一的焦点语言，保证遥控器操作时“当前选中项”一目了然。
 *
 * 焦点状态统一从 clickable 的 InteractionSource 派生，避免“双 focusable 导致
 * onFocusChanged 收不到焦点”的隐患，同时保留触摸点击与遥控器确认键。
 */
@Composable
fun FocusableSurface(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = RoundedCornerShape(12.dp),
    focusScale: Float = 1.08f,
    borderWidth: Dp = 6.dp,
    containerColor: Color = Color.Unspecified,
    requestFocus: Boolean = false,
    content: @Composable (focused: Boolean) -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isFocused) focusScale else 1f,
        label = "focusScale"
    )
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        if (requestFocus) {
            runCatching { focusRequester.requestFocus() }
        }
    }

    val resolvedColor = if (containerColor != Color.Unspecified) {
        containerColor
    } else {
        MaterialTheme.colorScheme.background
    }

    Surface(
        color = resolvedColor,
        shape = shape,
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                shadowElevation = if (isFocused) 20.dp.toPx() else 0f
                this.shape = shape
                clip = false
            }
            .then(if (isFocused) Modifier.border(borderWidth, Color.White, shape) else Modifier)
            .focusRequester(focusRequester)
            .clickable(interactionSource = interactionSource, onClick = onClick)
    ) {
        content(isFocused)
    }
}
