package io.github.xuzhike123.libretvtv.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import io.github.xuzhike123.libretvtv.data.ContentRow
import io.github.xuzhike123.libretvtv.data.UiSettings
import io.github.xuzhike123.libretvtv.data.VideoItem

/**
 * 一条横向内容行。LazyRow 会在焦点移动时自动把新焦点滚进可视区，
 * 这是 Compose for TV 相比普通 LazyRow 的核心差异。
 */
@Composable
fun ContentRowSection(
    row: ContentRow,
    settings: UiSettings,
    modifier: Modifier = Modifier,
    requestFirstFocus: Boolean = false,
    onVideoClick: (VideoItem) -> Unit
) {
    val firstId = row.items.firstOrNull()?.id
    Column(modifier = modifier.padding(vertical = 10.dp)) {
        SectionHeader(title = row.title, subtitle = row.subtitle)
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(26.dp),
            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 16.dp)
        ) {
            items(row.items) { item ->
                VideoCard(
                    video = item,
                    settings = settings,
                    requestFocus = requestFirstFocus && item.id == firstId,
                    onClick = { onVideoClick(item) }
                )
            }
        }
    }
}
