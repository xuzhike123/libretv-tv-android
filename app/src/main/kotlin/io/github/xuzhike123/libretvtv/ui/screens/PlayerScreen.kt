package io.github.xuzhike123.libretvtv.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import io.github.xuzhike123.libretvtv.R
import io.github.xuzhike123.libretvtv.data.HistoryEntry
import io.github.xuzhike123.libretvtv.data.LocalStore
import io.github.xuzhike123.libretvtv.data.PendingPlay
import io.github.xuzhike123.libretvtv.ui.components.FocusableSurface
import io.github.xuzhike123.libretvtv.ui.util.onTvKey
import kotlinx.coroutines.delay

/**
 * 播放页：Media3(ExoPlayer) 渲染真实 m3u8 / 直链，控制条与右侧面板用 Compose 绘制。
 *
 * 点播与直播走不同的控制条：
 *  - 点播：可拖动/点击定位的进度条 + 上一集/下一集/倍速/循环等点播键；
 *  - 直播：显示「直播」角标与频道切换键，去掉所有点播专属按键（无进度条，直播不可拖动）。
 * 两者都提供「全屏」按键用于切换画面比例（原比例 / 裁剪填满 / 拉伸全屏）。
 */
@Composable
fun PlayerScreen(
    modifier: Modifier = Modifier,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val req = remember { PendingPlay.consume() }
    val isLive = req.isLive

    var curTitle by remember { mutableStateOf(req.title) }
    var curSource by remember { mutableStateOf(req.source) }
    var channelIndex by remember { mutableIntStateOf(req.liveIndex) }

    val player = remember {
        ExoPlayer.Builder(context).build().also { exoPlayer ->
            exoPlayer.playWhenReady = true
            if (req.url.isNotEmpty()) {
                exoPlayer.setMediaItem(MediaItem.fromUri(req.url))
                exoPlayer.prepare()
            }
        }
    }

    var playerError by remember { mutableStateOf("") }
    val titleForHistory = rememberUpdatedState(curTitle)
    val sourceForHistory = rememberUpdatedState(curSource)

    DisposableEffect(Unit) {
        val listener = object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) {
                playerError = error.localizedMessage ?: "播放出错"
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                // 播放就绪即清除旧错误（如切换频道后新流正常时，不应残留上一频道的报错）
                if (playbackState == Player.STATE_READY) playerError = ""
            }
        }
        player.addListener(listener)
        onDispose {
            val pos = player.currentPosition
            val dur = player.duration.coerceAtLeast(0L)
            // 仅点播记录历史：直播没有 videoId，也没有可续播的进度
            if (!isLive && PendingPlay.videoId.isNotEmpty() && dur > 0) {
                LocalStore.addHistory(
                    HistoryEntry(
                        videoId = PendingPlay.videoId,
                        title = titleForHistory.value,
                        source = sourceForHistory.value,
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
    var resizeIdx by remember { mutableIntStateOf(0) }

    val resizeModes = listOf(
        AspectRatioFrameLayout.RESIZE_MODE_FIT to "原比例",
        AspectRatioFrameLayout.RESIZE_MODE_ZOOM to "裁剪填满",
        AspectRatioFrameLayout.RESIZE_MODE_FILL to "拉伸全屏"
    )

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

    /** 按进度比例跳转（点击/拖动进度条） */
    fun seekToFraction(frac: Float) {
        val d = player.duration.coerceAtLeast(0L)
        if (d <= 0L) return
        val target = (frac * d).toLong().coerceIn(0L, d)
        player.seekTo(target)
        positionMs = target
    }

    /** 相对快退/快进（进度条聚焦时按左右键） */
    fun stepBy(deltaMs: Long) {
        val d = player.duration.coerceAtLeast(0L)
        if (d <= 0L) return
        val target = (player.currentPosition + deltaMs).coerceIn(0L, d)
        player.seekTo(target)
        positionMs = target
    }

    /** 直播：切换上/下一个频道 */
    fun switchChannel(delta: Int) {
        val list = req.liveChannels
        if (list.isEmpty()) return
        val size = list.size
        val next = ((channelIndex + delta) % size + size) % size
        channelIndex = next
        val ch = list[next]
        curTitle = ch.name
        curSource = ch.group
        player.setMediaItem(MediaItem.fromUri(ch.url))
        player.prepare()
        player.playWhenReady = true
        playing = true
        positionMs = 0L
        durationMs = 0L
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .onTvKey { key ->
                when (key) {
                    Key.Back, Key.Escape -> {
                        if (panelOpen) panelOpen = false else onBack()
                        true
                    }
                    Key.DirectionCenter, Key.Enter -> { togglePlay(); true }
                    else -> false
                }
            }
    ) {
        if (req.url.isEmpty()) {
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
                        view.resizeMode = resizeModes[resizeIdx].first
                    }
                },
                update = { view -> view.resizeMode = resizeModes[resizeIdx].first },
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

        // 标题区
        Column(modifier = Modifier.padding(28.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = curTitle, fontSize = 36.sp, fontWeight = FontWeight.Bold, color = Color.White)
                if (isLive) {
                    Spacer(modifier = Modifier.width(16.dp))
                    LiveBadge()
                }
            }
            val sub = if (isLive && req.liveChannels.isNotEmpty()) {
                "$curSource · 频道 ${channelIndex + 1}/${req.liveChannels.size}"
            } else curSource
            Text(text = sub, fontSize = 22.sp, color = Color.White.copy(alpha = 0.7f))
        }

        // 底部控制区
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(28.dp)
        ) {
            if (isLive) {
                Text(
                    text = "直播播放中，不支持拖动进度 · 左右键可在下方切换频道",
                    fontSize = 20.sp,
                    color = Color.White.copy(alpha = 0.6f)
                )
            } else {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(formatTime(positionMs), fontSize = 22.sp, color = Color.White)
                    Text(
                        if (durationMs > 0L) formatTime(durationMs) else "--:--",
                        fontSize = 22.sp, color = Color.White
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                val progress = if (durationMs > 0L) {
                    (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
                } else 0f
                SeekBar(
                    progress = progress,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(22.dp),
                    requestFocus = true,
                    onSeekFraction = { frac -> seekToFraction(frac) },
                    onStep = { delta -> stepBy(delta) }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 主控制行
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                if (isLive) {
                    PlayerControlButton("上一频道", R.drawable.ic_prev, onClick = { switchChannel(-1) })
                    PlayerControlButton(
                        label = if (playing) "暂停" else "播放",
                        iconRes = if (playing) R.drawable.ic_pause else R.drawable.ic_play,
                        onClick = { togglePlay() }
                    )
                    PlayerControlButton("下一频道", R.drawable.ic_next, onClick = { switchChannel(1) })
                } else {
                    PlayerControlButton("上一集", R.drawable.ic_prev, onClick = { })
                    PlayerControlButton(
                        label = if (playing) "暂停" else "播放",
                        iconRes = if (playing) R.drawable.ic_pause else R.drawable.ic_play,
                        onClick = { togglePlay() }
                    )
                    PlayerControlButton("下一集", R.drawable.ic_next, onClick = { })
                    PlayerControlButton("倍速", null, onClick = { })
                    PlayerControlButton("循环", null, onClick = { })
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 辅助控制行（含全屏/画面比例）
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                PlayerControlButton("画质", null, onClick = { panelOpen = true })
                PlayerControlButton("字幕", null, onClick = { })
                if (!isLive) PlayerControlButton("换源", null, onClick = { })
                PlayerControlButton("收藏", null, onClick = { })
                PlayerControlButton("后台", null, onClick = { })
                PlayerControlButton(
                    label = "全屏",
                    iconRes = null,
                    subLabel = resizeModes[resizeIdx].second,
                    onClick = { resizeIdx = (resizeIdx + 1) % resizeModes.size }
                )
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
                        Text(
                            text = entry, fontSize = 26.sp, color = Color.White,
                            modifier = Modifier.padding(horizontal = 18.dp, vertical = 20.dp)
                        )
                    }
                }
            }
        }
    }
}

/** 直播角标 */
@Composable
private fun LiveBadge() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFFFF5252))
            .padding(horizontal = 12.dp, vertical = 4.dp)
    ) {
        Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(Color.White))
        Spacer(modifier = Modifier.width(8.dp))
        Text("直播", fontSize = 20.sp, color = Color.White, fontWeight = FontWeight.Bold)
    }
}

/**
 * 可交互进度条：鼠标/触摸点击可直接跳到指定时间点；
 * 遥控器聚焦后按左右键快退/快进 10 秒。
 */
@Composable
private fun SeekBar(
    progress: Float,
    modifier: Modifier = Modifier,
    requestFocus: Boolean = false,
    onSeekFraction: (Float) -> Unit,
    onStep: (Long) -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
    val focusRequester = remember { FocusRequester() }
    var widthPx by remember { mutableFloatStateOf(0f) }
    val density = LocalDensity.current

    LaunchedEffect(Unit) {
        if (requestFocus) runCatching { focusRequester.requestFocus() }
    }

    val safeProgress = progress.coerceIn(0f, 1f)
    val handleOffsetDp = with(density) { (widthPx * safeProgress).toDp() }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (focused) Color.White.copy(alpha = 0.35f) else Color.White.copy(alpha = 0.22f))
            .then(if (focused) Modifier.border(4.dp, Color.White, RoundedCornerShape(8.dp)) else Modifier)
            .focusRequester(focusRequester)
            .focusable(interactionSource = interactionSource)
            .onTvKey { key ->
                when (key) {
                    Key.DirectionLeft -> { onStep(-10000L); true }
                    Key.DirectionRight -> { onStep(10000L); true }
                    else -> false
                }
            }
            .pointerInput(widthPx) {
                detectTapGestures { offset ->
                    if (widthPx > 0f) {
                        onSeekFraction((offset.x / widthPx).coerceIn(0f, 1f))
                    }
                }
            }
            .onSizeChanged { widthPx = it.width.toFloat() }
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(safeProgress)
                .background(MaterialTheme.colorScheme.primary)
        )
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .offset(x = handleOffsetDp - 10.dp)
                .size(20.dp)
                .clip(CircleShape)
                .background(Color.White)
        )
        if (focused) {
            Text(
                text = "← → 快退 / 快进 10 秒",
                fontSize = 16.sp,
                color = Color.White,
                modifier = Modifier.align(Alignment.Center)
            )
        }
    }
}

@Composable
private fun PlayerControlButton(
    label: String,
    iconRes: Int? = null,
    subLabel: String? = null,
    requestFocus: Boolean = false,
    onClick: () -> Unit
) {
    FocusableSurface(
        onClick = onClick,
        shape = CircleShape,
        containerColor = Color(0x33FFFFFF),
        requestFocus = requestFocus,
        modifier = Modifier.width(96.dp)
    ) {
        Column(
            modifier = Modifier.height(96.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = label, fontSize = 20.sp, color = Color.White, textAlign = TextAlign.Center)
            if (subLabel != null) {
                Text(
                    text = subLabel, fontSize = 16.sp,
                    color = Color.White.copy(alpha = 0.75f), textAlign = TextAlign.Center
                )
            } else if (iconRes != null) {
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

private fun formatTime(ms: Long): String {
    if (ms < 0) return "--:--"
    val totalSec = ms / 1000
    val h = totalSec / 3600
    val m = (totalSec % 3600) / 60
    val s = totalSec % 60
    return if (h > 0) String.format("%d:%02d:%02d", h, m, s) else String.format("%02d:%02d", m, s)
}
