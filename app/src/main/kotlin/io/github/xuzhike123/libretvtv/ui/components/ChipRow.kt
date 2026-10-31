package io.github.xuzhike123.libretvtv.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items

/**
 * 通用横向条目行：详情页的选集、换源卡片都用它，
 * 保持与首页内容行一致的焦点样式。
 */
@Composable
fun ChipRow(
    labels: List<String>,
    modifier: Modifier = Modifier,
    itemWidth: androidx.compose.ui.unit.Dp = 190.dp,
    itemHeight: androidx.compose.ui.unit.Dp = 96.dp,
    onItemClick: (String) -> Unit = {}
) {
    ChipLazyRow(
        modifier = modifier,
        onItemClick = onItemClick,
        labels = labels,
        itemWidth = itemWidth,
        itemHeight = itemHeight
    )
}

@Composable
private fun ChipLazyRow(
    labels: List<String>,
    modifier: Modifier,
    itemWidth: androidx.compose.ui.unit.Dp,
    itemHeight: androidx.compose.ui.unit.Dp,
    onItemClick: (String) -> Unit
) {
    LazyRow(
        modifier = modifier.height(itemHeight + 32.dp),
        horizontalArrangement = Arrangement.spacedBy(24.dp),
        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 16.dp)
    ) {
        items(labels) { label ->
            FocusableSurface(
                onClick = { onItemClick(label) },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.width(itemWidth)
            ) {
                Column(
                    modifier = Modifier.height(itemHeight),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = label,
                        fontSize = 26.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            }
        }
    }
}
