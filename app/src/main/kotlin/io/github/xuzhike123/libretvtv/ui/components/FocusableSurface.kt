package io.github.xuzhike123.libretvtv.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp

/**
 * 大屏通用焦点容器：获得焦点时整体放大并描一圈白边，
 * 这是 SmartTube 卡片与侧栏共用的视觉特征。
 */
@Composable
fun FocusableSurface(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = RoundedCornerShape(12.dp),
    focusScale: Float = 1.04f,
    containerColor: Color = Color.Unspecified,
    requestFocus: Boolean = false,
    content: @Composable (focused: Boolean) -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
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
            }
            .then(if (isFocused) Modifier.border(4.dp, Color.White, shape) else Modifier)
            .focusRequester(focusRequester)
            .focusable()
            .onFocusChanged { isFocused = it.isFocused }
            .clickable { onClick() }
    ) {
        content(isFocused)
    }
}
