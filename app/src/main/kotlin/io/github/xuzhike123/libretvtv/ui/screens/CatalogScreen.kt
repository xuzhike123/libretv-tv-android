package io.github.xuzhike123.libretvtv.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import io.github.xuzhike123.libretvtv.data.ContentRow
import io.github.xuzhike123.libretvtv.data.UiSettings
import io.github.xuzhike123.libretvtv.data.VideoItem
import io.github.xuzhike123.libretvtv.ui.components.ContentRowSection

/**
 * 首页 / 收藏 / 历史共用的内容墙：一列横向内容行的纵向列表。
 */
@Composable
fun CatalogScreen(
    rows: List<ContentRow>,
    settings: UiSettings,
    modifier: Modifier = Modifier,
    onVideoClick: (VideoItem) -> Unit
) {
    val firstRow = rows.firstOrNull()
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 28.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
        contentPadding = PaddingValues(bottom = 48.dp)
    ) {
        items(rows) { row ->
            ContentRowSection(
                row = row,
                settings = settings,
                requestFirstFocus = row === firstRow,
                onVideoClick = onVideoClick
            )
        }
    }
}
