package io.github.xuzhike123.libretvtv.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import io.github.xuzhike123.libretvtv.R
import io.github.xuzhike123.libretvtv.data.LocalStore
import io.github.xuzhike123.libretvtv.data.PendingPlay
import io.github.xuzhike123.libretvtv.ui.components.FocusableSurface
import androidx.compose.ui.input.key.Key
import io.github.xuzhike123.libretvtv.ui.util.onTvKey
import kotlinx.coroutines.delay

/**
 * 播放页：Media3(ExoPlayer) 渲染真实 m3u8 / 直链，控制条与右侧面板用 Compose 绘制。
 */
@Composable
fun PlayerScreen(
    modifier: Modifier = Modifier,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val (url, title, source) = PendingPlay.consume()

    val player = remember {
        ExoPlayer.Builder(context).build().also { exoPlayer ->
            exoPlayer.playWhenReady = true
            if (url.isNotEmpty()) {
                exoPlayer.setMediaItem(MediaItem.fromUri(url))
                exoPlayer.prepare()
            }
        }
    }

    var playerError by remember { mutableStateOf("") }
    DisposableEffect(Unit) {
        val listener = object : Player.Listener {
            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                playerError = error.localizedMessage ?: "播放出错"
            }
        }
        player.addListener(listener)
        onDispose {
            val pos = player.currentPosition
            val dur = player.duration.coerceAtLeast(0L)
            if (PendingPlay.videoId.isNotEmpty() && dur > 0) {
                LocalStore.addHistory(
                    io.github.xuzhike123.libretvtv.data.HistoryEntry(
                        videoId = PendingPlay.videoId,
                        title = title,
                        source = source,
                        positionMs = pos,
                        durationMs = dur,
                        updatedAt = System.currentTimeMillis()
                    )
                )
            }
            player.removeListener(listener)
            player.release()
        }
    }

    var playing by remember { mutableStateOf(true) }
    var positionMs by remember { mutableStateOf(0L) }
    var durationMs by remember { mutableStateOf(0L) }
    var panelOpen by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        while (true) {
            positionMs = player.currentPosition
            durationMs = player.duration.coerceAtLeast(0L)
            delay(500L)
        }
    }

    fun togglePlay() {
        val next = !playing
        playing = next
        player.playWhenReady = next
    }

    fun seekBy(deltaMs: Long) {
        val limit = player.duration.coerceAtLeast(0L)
        val target = (player.currentPosition + deltaMs).coerceIn(0L, limit)
        player.seekTo(target)
        positionMs = target
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .onTvKey { key ->
                when (key) {
                    Key.Back, Key.Escape -> { onBack(); true }
                    Key.DirectionCenter, Key.Enter -> { togglePlay(); true }
                    Key.DirectionLeft -> { seekBy(-15000L); true }
                    Key.DirectionRight -> { seekBy(15000L); true }
                    else -> false
                }
            }
    ) {
        if (url.isEmpty()) {
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("没有可用的播放地址", fontSize = 32.sp, color = Color.White, fontWeight = FontWeight.Bold)
                Text("请返回详情页选择其它源或剧集", fontSize = 22.sp, color = Color.White.copy(alpha = 0.7f))
            }
        } else {
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).also { view ->
                        view.player = player
                        view.useController = false
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        if (playerError.isNotEmpty()) {
            Text(
                text = "播放失败：$playerError",
                fontSize = 22.sp, color = Color(0xFFFF8A80),
                modifier = Modifier.align(Alignment.Center)
            )
        }

        Column(modifier = Modifier.padding(28.dp)) {
            Text(text = title, fontSize = 36.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Text(text = source, fontSize = 22.sp, color = Color.White.copy(alpha = 0.7f))
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(28.dp)
        ) {
            val progress = if (durationMs > 0L) {
                (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
            } else 0f
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color.White.copy(alpha = 0.2f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(progress)
                        .background(MaterialTheme.colorScheme.primary)
                )
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.padding(top = 16.dp)
            ) {
                PlayerControlButton(label = "上一集", iconRes = R.drawable.ic_prev, onClick = { })
                PlayerControlButton(
                    label = if (playing) "暂停" else "播放",
                    iconRes = if (playing) R.drawable.ic_pause else R.drawable.ic_play,
                    onClick = { togglePlay() }
                )
                PlayerControlButton(label = "下一集", iconRes = R.drawable.ic_next, onClick = { })
                listOf("循环", "倍速", "画质", "字幕", "换源", "收藏", "后台").forEach { label ->
                    PlayerControlButton(label = label, iconRes = null, onClick = { if (label == "画质") panelOpen = true })
                }
            }
        }

        if (panelOpen) {
            Column(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .width(460.dp)
                    .background(Color(0xEE14171C))
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(text = "播放设置", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color.White)
                listOf("视频格式", "音频轨道", "后台播放", "视频缓冲", "自动帧率").forEach { entry ->
                    FocusableSurface(
                        onClick = { },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().height(72.dp)
                    ) {
                        Text(text = entry, fontSize = 26.sp, color = Color.White, modifier = Modifier.padding(horizontal = 18.dp, vertical = 20.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun PlayerControlButton(
    label: String,
    iconRes: Int?,
    onClick: () -> Unit
) {
    FocusableSurface(
        onClick = onClick,
        shape = CircleShape,
        containerColor = Color(0x33FFFFFF),
        modifier = Modifier.width(96.dp)
    ) {
        Column(
            modifier = Modifier.height(96.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = label, fontSize = 20.sp, color = Color.White, textAlign = TextAlign.Center)
            if (iconRes != null) {
                Icon(
                    painter = painterResource(iconRes),
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}
