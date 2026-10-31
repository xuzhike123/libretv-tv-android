package io.github.xuzhike123.libretvtv.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import androidx.compose.ui.unit.sp
import io.github.xuzhike123.libretvtv.data.CardStyle
import io.github.xuzhike123.libretvtv.data.UiSettings
import io.github.xuzhike123.libretvtv.data.VideoItem

private val THUMB_PALETTES = listOf(
    listOf(Color(0xFF1F4B5C), Color(0xFF0E2A33)),
    listOf(Color(0xFF3C4E6B), Color(0xFF161F2E)),
    listOf(Color(0xFF5A4A6B), Color(0xFF211A2B)),
    listOf(Color(0xFF2E5A4A), Color(0xFF12261F)),
    listOf(Color(0xFF5C4438), Color(0xFF241A15)),
    listOf(Color(0xFF4B4A2E), Color(0xFF1F1E12)),
    listOf(Color(0xFF2E4B5C), Color(0xFF122026)),
    listOf(Color(0xFF4B2E3C), Color(0xFF1F111A))
)

private fun thumbBrush(id: String): Brush {
    val idx = Math.floorMod(id.hashCode(), THUMB_PALETTES.size)
    val palette = THUMB_PALETTES[idx]
    return Brush.linearGradient(palette)
}

/**
 * 影片卡片：16:9 缩略图 + 来源角标 + 时长角标 + 续播进度条 + 标题信息行。
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun VideoCard(
    video: VideoItem,
    settings: UiSettings,
    modifier: Modifier = Modifier,
    requestFocus: Boolean = false,
    onClick: () -> Unit
) {
    val cardWidth = 300.dp * settings.uiScale
    val titleSize = (26 * settings.uiScale).sp
    val metaSize = (22 * settings.uiScale).sp
    val initialSize = (54 * settings.uiScale).sp
    val singleLineScroll = settings.cardStyle == CardStyle.SINGLE_LINE_SCROLL

    FocusableSurface(
        onClick = onClick,
        modifier = modifier.width(cardWidth),
        requestFocus = requestFocus,
        focusScale = 1.12f
    ) { focused ->
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(remember(video.id) { thumbBrush(video.id) })
            ) {
                if (video.pic.isNotEmpty()) {
                    AsyncImage(
                        model = video.pic,
                        contentDescription = video.title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Text(
                        text = video.title.take(2),
                        fontSize = initialSize,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White.copy(alpha = 0.22f),
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                Text(
                    text = video.source,
                    fontSize = metaSize,
                    color = Color.White,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(10.dp)
                        .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                )
                Text(
                    text = video.duration,
                    fontSize = metaSize,
                    color = Color.White,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(10.dp)
                        .background(Color.Black.copy(alpha = 0.78f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                )
                video.progressPercent?.let { percent ->
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .height(6.dp)
                            .fillMaxWidth(percent.coerceIn(0, 100) / 100f)
                            .background(MaterialTheme.colorScheme.primary)
                    )
                }
                if (focused && settings.animatedPreview) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(10.dp)
                            .size(44.dp)
                            .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                    )
                }
            }
            Column(modifier = Modifier.padding(horizontal = 4.dp, vertical = 12.dp)) {
                Text(
                    text = video.title,
                    fontSize = titleSize,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = if (singleLineScroll) 1 else 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = if (singleLineScroll) Modifier.basicMarquee() else Modifier
                )
                val secondary = if (video.secondaryLine.isNotEmpty()) {
                    video.source + " · " + video.secondaryLine
                } else {
                    video.source
                }
                Text(
                    text = secondary,
                    fontSize = metaSize,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
