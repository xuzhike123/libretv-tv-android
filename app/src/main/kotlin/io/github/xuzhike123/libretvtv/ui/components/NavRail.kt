package io.github.xuzhike123.libretvtv.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.xuzhike123.libretvtv.R
import io.github.xuzhike123.libretvtv.ui.Screen

private data class NavEntry(
    val screen: Screen,
    val iconRes: Int,
    val labelRes: Int
)

/**
 * 竖直导航栏：SmartTube 的核心入口结构，图标 + 文字，选中项用主题色高亮。
 */
@Composable
fun LibreTvNavRail(
    selectedRoute: String,
    modifier: Modifier = Modifier,
    onSelect: (Screen) -> Unit
) {
    val entries = listOf(
        NavEntry(Screen.Home, R.drawable.ic_home, R.string.nav_home),
        NavEntry(Screen.Search, R.drawable.ic_search, R.string.nav_search),
        NavEntry(Screen.Favorites, R.drawable.ic_favorite, R.string.nav_favorites),
        NavEntry(Screen.History, R.drawable.ic_history, R.string.nav_history),
        NavEntry(Screen.Live, R.drawable.ic_live, R.string.nav_live),
        NavEntry(Screen.Sources, R.drawable.ic_sources, R.string.nav_sources),
        NavEntry(Screen.Settings, R.drawable.ic_settings, R.string.nav_settings)
    )

    Column(
        modifier = modifier
            .width(240.dp)
            .padding(vertical = 24.dp, horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        entries.forEachIndexed { index, entry ->
            val selected = entry.screen.route == selectedRoute
            FocusableSurface(
                onClick = { onSelect(entry.screen) },
                shape = RoundedCornerShape(16.dp),
                containerColor = if (selected) {
                    MaterialTheme.colorScheme.surfaceVariant
                } else {
                    Color.Transparent
                },
                requestFocus = index == 0
            ) {
                Row(
                    modifier = Modifier
                        .height(76.dp)
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    Icon(
                        painter = painterResource(entry.iconRes),
                        contentDescription = null,
                        tint = if (selected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.size(32.dp)
                    )
                    Text(
                        text = stringResource(entry.labelRes),
                        fontSize = 26.sp,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (selected) {
                            Color.White
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            }
        }
    }
}
