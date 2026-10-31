package io.github.xuzhike123.libretvtv.ui.screens

import androidx.activity.compose.BackHandler
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
import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
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

    var playing by remember { mutableStateOf(true) }
    var positionMs by remember { mutableStateOf(0L) }
    var durationMs by remember { mutableStateOf(0L) }
    var panelOpen by remember { mutableStateOf(false) }
    var resizeIdx by remember { mutableIntStateOf(0) }

    /** 「后台播放」：开启后回到电视桌面仍继续播放声音 */
    var backgroundPlay by remember { mutableStateOf(false) }
    /** 「自动帧率」：开启后让屏幕刷新率匹配视频帧率（切换时重建播放器并保留进度） */
    var autoFrameRate by remember { mutableStateOf(false) }

    /** 倍速档位，默认 1.0x */
    val speeds = listOf(0.75f, 1.0f, 1.25f, 1.5f, 2.0f)
    var speedIdx by remember { mutableIntStateOf(1) }

    /** 单集循环开关 */
    var loopOn by remember { mutableStateOf(false) }

    /** 字幕选择：0 = 关闭，1..N = 第 N 条字幕轨 */
    var subIdx by remember { mutableIntStateOf(0) }
    /** 当前媒体里可用的字幕轨名称（播放器就绪后自动刷新） */
    var textTrackLabels by remember { mutableStateOf(listOf<String>()) }
    /** 点播：当前播放源 / 集数下标，供「换源」「上一集 / 下一集」使用 */
    var srcIdx by remember { mutableIntStateOf(req.sourceIndex) }
    var epIdx by remember { mutableIntStateOf(req.episodeIndex) }

    /** 收藏状态（进入播放页时从本地收藏表读取） */
    var favOn by remember {
        mutableStateOf(req.videoId.isNotEmpty() && LocalStore.isFavorite(req.videoId))
    }

    /** 播放设置面板里的实时信息，每 500ms 刷新 */
    var videoInfoText by remember { mutableStateOf("加载中…") }
    var audioInfoText by remember { mutableStateOf("加载中…") }
    var bufferedPct by remember { mutableIntStateOf(0) }
    var bufferedText by remember { mutableStateOf("0%") }
    /** 「画质」按钮副标签：当前实际分辨率 */
    var qualityText by remember { mutableStateOf("自动") }
    /** 「字幕」按钮副标签：当前字幕轨名称 */
    var subtitleText by remember { mutableStateOf("关闭") }

    /** 画质按钮的焦点锚点：关闭面板后把焦点还回去 */
    val qualityFocusRequester = remember { FocusRequester() }

    /** 当前影片名（不含集数），切换集数时用它拼标题 */
    val baseTitle = req.baseTitle.ifEmpty { req.title }

    val player = remember(autoFrameRate) {
        ExoPlayer.Builder(context)
            .setVideoChangeFrameRateStrategy(
                // 开：在可无缝切换时让屏幕刷新率匹配视频帧率；关：始终不切换刷新率
                if (autoFrameRate) C.VIDEO_CHANGE_FRAME_RATE_STRATEGY_ONLY_IF_SEAMLESS
                else C.VIDEO_CHANGE_FRAME_RATE_STRATEGY_OFF
            )
            .build()
            .also { exoPlayer ->
                exoPlayer.playWhenReady = true
                exoPlayer.setHandleAudioBecomingNoisy(!backgroundPlay)
                exoPlayer.setForegroundMode(backgroundPlay)
                exoPlayer.playbackParameters = PlaybackParameters(speeds[speedIdx])
                exoPlayer.repeatMode =
                    if (loopOn) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
                if (req.url.isNotEmpty()) {
                    exoPlayer.setMediaItem(MediaItem.fromUri(req.url))
                    exoPlayer.prepare()
                    // 因切换「自动帧率」而重建播放器时，恢复原有播放进度
                    if (positionMs > 0L) exoPlayer.seekTo(positionMs)
                }
            }
    }

    var playerError by remember { mutableStateOf("") }
    val titleForHistory = rememberUpdatedState(curTitle)
    val sourceForHistory = rememberUpdatedState(curSource)

    DisposableEffect(player) {
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

    val resizeModes = listOf(
        AspectRatioFrameLayout.RESIZE_MODE_FIT to "原比例",
        AspectRatioFrameLayout.RESIZE_MODE_ZOOM to "裁剪填满",
        AspectRatioFrameLayout.RESIZE_MODE_FILL to "拉伸全屏"
    )

    // 每 500ms 刷新进度与播放设置面板中的实时信息
    LaunchedEffect(player) {
        while (true) {
            positionMs = player.currentPosition
            durationMs = player.duration.coerceAtLeast(0L)
            bufferedPct = player.bufferedPercentage
            bufferedText = "$bufferedPct% · 已缓冲 " + formatTime(
                player.bufferedPosition.coerceAtLeast(0L)
            )
            val vf = player.videoFormat
            videoInfoText = describeVideoFormat(vf)
            qualityText = if (vf != null && vf.height > 0) "${vf.height}p" else "自动"
            audioInfoText = describeAudioFormat(player.audioFormat)

            // 字幕轨名称（播放器就绪后才有内容）
            val labels = mutableListOf<String>()
            for (g in player.currentTracks.groups) {
                if (g.type != C.TRACK_TYPE_TEXT) continue
                for (i in 0 until g.length) {
                    val f = runCatching { g.getTrackFormat(i) }.getOrNull()
                    val name = f?.label?.takeIf { it.isNotBlank() }
                        ?: f?.language?.takeIf { it.isNotBlank() }
                        ?: "字幕 ${labels.size + 1}"
                    labels += name
                }
            }
            textTrackLabels = labels
            if (labels.isEmpty()) subtitleText = "无字幕轨"
            delay(500L)
        }
    }

    // 「后台播放」开关状态同步到播放器
    LaunchedEffect(backgroundPlay) {
        player.setForegroundMode(backgroundPlay)
        player.setHandleAudioBecomingNoisy(!backgroundPlay)
    }

    // 面板关闭后把焦点交还给「画质」按钮
    LaunchedEffect(panelOpen) {
        if (!panelOpen) runCatching { qualityFocusRequester.requestFocus() }
    }

    // 系统返回键：面板打开时优先关闭面板，否则退出播放页
    BackHandler(enabled = panelOpen) {
        panelOpen = false
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

    /** 循环切换倍速：0.75x → 1.0x → 1.25x → 1.5x → 2.0x */
    fun cycleSpeed() {
        val next = (speedIdx + 1) % speeds.size
        speedIdx = next
        player.playbackParameters = PlaybackParameters(speeds[next])
    }

    /** 单集循环开关 */
    fun toggleLoop() {
        loopOn = !loopOn
        player.repeatMode = if (loopOn) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
    }

    /** 收藏 / 取消收藏当前影片（写入本设备 localstore.json） */
    fun toggleFavorite() {
        val vid = req.videoId
        if (vid.isEmpty()) return
        LocalStore.toggleFavorite(vid, baseTitle)
        favOn = LocalStore.isFavorite(vid)
    }

    /** 播放指定「播放源 + 集数」：原地换流，不退出播放页 */
    fun playEpisode(sIdx: Int, eIdx: Int) {
        val ps = req.playSources.getOrNull(sIdx) ?: return
        val eps = ps.episodes
        if (eps.isEmpty()) return
        val safeE = eIdx.coerceIn(0, eps.size - 1)
        val ep = eps[safeE]
        srcIdx = sIdx
        epIdx = safeE
        curTitle = buildEpisodeTitle(baseTitle, ep.name, safeE)
        curSource = req.source
        player.setMediaItem(MediaItem.fromUri(ep.url))
        player.prepare()
        player.playWhenReady = true
        playing = true
        positionMs = 0L
        durationMs = 0L
    }

    /** 换源：切到下一个播放源，并尽量停在同一个集数 */
    fun switchSource() {
        val size = req.playSources.size
        if (size <= 1) return
        playEpisode((srcIdx + 1) % size, epIdx)
    }

    /** 应用当前字幕选择（subIdx = 0 即关闭字幕） */
    fun applySubtitle() {
        val builder = player.trackSelectionParameters.buildUpon()
        if (subIdx == 0) {
            builder.setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
            subtitleText = "关闭"
        } else {
            builder.setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
            var remaining = subIdx - 1
            var applied = false
            for (g in player.currentTracks.groups) {
                if (g.type != C.TRACK_TYPE_TEXT) continue
                for (i in 0 until g.length) {
                    if (!applied && remaining == 0) {
                        builder.setOverrideForType(TrackSelectionOverride(g.mediaTrackGroup, i))
                        subtitleText = textTrackLabels.getOrNull(subIdx - 1) ?: "字幕"
                        applied = true
                    }
                    remaining--
                }
            }
            if (!applied) {
                subIdx = 0
                subtitleText = "关闭"
            }
        }
        player.trackSelectionParameters = builder.build()
    }

    /** 循环切换字幕：关闭 → 第 1 条 → 第 2 条 … */
    fun cycleSubtitle() {
        if (textTrackLabels.isEmpty()) {
            subtitleText = "无字幕轨"
            return
        }
        subIdx = (subIdx + 1) % (textTrackLabels.size + 1)
        applySubtitle()
    }

    /** 当前播放源下的集数列表 */
    val currentEpisodes = req.playSources.getOrNull(srcIdx)?.episodes ?: emptyList()
    val epCount = currentEpisodes.size

    /** 第 i 集的显示名（源里没给名字时按序号生成） */
    fun episodeName(i: Int): String {
        val ep = currentEpisodes.getOrNull(i) ?: return ""
        return ep.name.ifEmpty { String.format("第%02d集", i + 1) }
    }

    /** 「换源」按钮副标签：当前播放源名 + 序号 */
    val sourceText = when {
        req.playSources.isEmpty() -> "默认源"
        req.playSources.size == 1 -> req.playSources[0].name.ifEmpty { "默认源" }
        else -> {
            val n = req.playSources.getOrNull(srcIdx)?.name.orEmpty().ifEmpty { "源" }
            "$n ${srcIdx + 1}/${req.playSources.size}"
        }
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
                    PlayerControlButton(
                        label = "上一集",
                        iconRes = R.drawable.ic_prev,
                        subLabel = if (epIdx > 0) episodeName(epIdx - 1) else "已是第一集",
                        onClick = { if (epIdx > 0) playEpisode(srcIdx, epIdx - 1) }
                    )
                    PlayerControlButton(
                        label = if (playing) "暂停" else "播放",
                        iconRes = if (playing) R.drawable.ic_pause else R.drawable.ic_play,
                        onClick = { togglePlay() }
                    )
                    PlayerControlButton(
                        label = "下一集",
                        iconRes = R.drawable.ic_next,
                        subLabel = if (epIdx < epCount - 1) episodeName(epIdx + 1) else "已是最后一集",
                        onClick = { if (epIdx < epCount - 1) playEpisode(srcIdx, epIdx + 1) }
                    )
                    PlayerControlButton(
                        label = "倍速",
                        iconRes = null,
                        subLabel = "%.2fx".format(speeds[speedIdx]),
                        onClick = { cycleSpeed() }
                    )
                    PlayerControlButton(
                        label = "循环",
                        iconRes = null,
                        subLabel = if (loopOn) "开" else "关",
                        onClick = { toggleLoop() }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 辅助控制行（含全屏/画面比例）
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                PlayerControlButton(
                    label = "画质",
                    iconRes = null,
                    subLabel = qualityText,
                    modifier = Modifier.focusRequester(qualityFocusRequester),
                    onClick = { panelOpen = true }
                )
                PlayerControlButton(
                    label = "字幕",
                    iconRes = null,
                    subLabel = subtitleText,
                    onClick = { cycleSubtitle() }
                )
                if (!isLive) PlayerControlButton(
                    label = "换源",
                    iconRes = null,
                    subLabel = sourceText,
                    onClick = { switchSource() }
                )
                PlayerControlButton(
                    label = "收藏",
                    iconRes = null,
                    subLabel = if (favOn) "已收藏" else "未收藏",
                    onClick = { toggleFavorite() }
                )
                PlayerControlButton(
                    label = "后台",
                    iconRes = null,
                    subLabel = if (backgroundPlay) "开" else "关",
                    onClick = { backgroundPlay = !backgroundPlay }
                )
                PlayerControlButton(
                    label = "全屏",
                    iconRes = null,
                    subLabel = resizeModes[resizeIdx].second,
                    onClick = { resizeIdx = (resizeIdx + 1) % resizeModes.size }
                )
            }
        }

        if (panelOpen) {
            PlaySettingsPanel(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 12.dp, end = 12.dp),
                videoInfo = videoInfoText,
                audioInfo = audioInfoText,
                bufferedInfo = bufferedText,
                bufferedPct = bufferedPct,
                backgroundPlay = backgroundPlay,
                autoFrameRate = autoFrameRate,
                onBackgroundPlayChange = { backgroundPlay = it },
                onAutoFrameRateChange = { autoFrameRate = it },
                onClose = { panelOpen = false }
            )
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
    modifier: Modifier = Modifier,
    requestFocus: Boolean = false,
    onClick: () -> Unit
) {
    FocusableSurface(
        onClick = onClick,
        shape = CircleShape,
        containerColor = Color(0x33FFFFFF),
        requestFocus = requestFocus,
        modifier = modifier.width(96.dp)
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

/**
 * 播放设置面板。
 *
 * 上半部分是播放器实时状态（视频格式 / 音频轨道 / 缓冲进度），每 500ms 自动刷新；
 * 下半部分是两个真正生效的开关：后台播放、自动帧率。
 * 关闭方式：遥控器返回键（由 BackHandler 统一拦截），或点击底部「返回」按钮。
 */
@Composable
private fun PlaySettingsPanel(
    modifier: Modifier = Modifier,
    videoInfo: String,
    audioInfo: String,
    bufferedInfo: String,
    bufferedPct: Int,
    backgroundPlay: Boolean,
    autoFrameRate: Boolean,
    onBackgroundPlayChange: (Boolean) -> Unit,
    onAutoFrameRateChange: (Boolean) -> Unit,
    onClose: () -> Unit
) {
    Column(
        modifier = modifier
            .width(420.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(Color(0xF214171C))
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(text = "播放设置", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color.White)
        Text(
            text = "按返回键或下方「返回」关闭",
            fontSize = 16.sp,
            color = Color.White.copy(alpha = 0.55f)
        )

        SettingsInfoRow(label = "视频格式", value = videoInfo)
        SettingsInfoRow(label = "音频轨道", value = audioInfo)
        SettingsToggleRow(
            label = "后台播放",
            value = if (backgroundPlay) "开 · 退到桌面继续播" else "关 · 退到桌面暂停",
            checked = backgroundPlay,
            onToggle = { onBackgroundPlayChange(!backgroundPlay) }
        )
        SettingsBufferRow(label = "视频缓冲", value = bufferedInfo, pct = bufferedPct)
        SettingsToggleRow(
            label = "自动帧率",
            value = if (autoFrameRate) "开 · 匹配视频帧率" else "关 · 保持当前刷新率",
            checked = autoFrameRate,
            onToggle = { onAutoFrameRateChange(!autoFrameRate) }
        )

        Spacer(modifier = Modifier.height(4.dp))

        FocusableSurface(
            onClick = onClose,
            shape = RoundedCornerShape(12.dp),
            containerColor = Color(0x33FFFFFF),
            focusScale = 1.06f,
            requestFocus = true,
            modifier = Modifier.fillMaxWidth().height(64.dp)
        ) { focused ->
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = "返回",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (focused) Color.Black else Color.White
                )
            }
        }
    }
}

/** 只读信息行：左侧标签，右侧实时值 */
@Composable
private fun SettingsInfoRow(label: String, value: String) {
    FocusableSurface(
        onClick = { },
        shape = RoundedCornerShape(12.dp),
        containerColor = Color(0x18FFFFFF),
        focusScale = 1.04f,
        modifier = Modifier.fillMaxWidth().height(64.dp)
    ) {
        SettingsRowContent(label = label, value = value, valueColor = Color.White.copy(alpha = 0.8f))
    }
}

/** 开关行：点击整行切换开 / 关 */
@Composable
private fun SettingsToggleRow(
    label: String,
    value: String,
    checked: Boolean,
    onToggle: () -> Unit
) {
    FocusableSurface(
        onClick = onToggle,
        shape = RoundedCornerShape(12.dp),
        containerColor = if (checked) Color(0x3373C8FF) else Color(0x18FFFFFF),
        focusScale = 1.04f,
        modifier = Modifier.fillMaxWidth().height(64.dp)
    ) {
        SettingsRowContent(
            label = label,
            value = value,
            valueColor = if (checked) Color(0xFF8FD6FF) else Color.White.copy(alpha = 0.7f)
        )
    }
}

@Composable
private fun SettingsRowContent(label: String, value: String, valueColor: Color) {
    Row(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, fontSize = 22.sp, color = Color.White)
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = value,
            fontSize = 20.sp,
            color = valueColor,
            textAlign = TextAlign.End,
            maxLines = 1
        )
    }
}

/** 缓冲进度行：文字 + 进度条 */
@Composable
private fun SettingsBufferRow(label: String, value: String, pct: Int) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0x18FFFFFF))
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(text = label, fontSize = 22.sp, color = Color.White)
            Text(
                text = value,
                fontSize = 20.sp,
                color = Color.White.copy(alpha = 0.8f),
                textAlign = TextAlign.End,
                maxLines = 1
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .clip(RoundedCornerShape(5.dp))
                .background(Color.White.copy(alpha = 0.15f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(pct.coerceIn(0, 100) / 100f)
                .background(Color(0xFF4FC3F7))
            )
        }
    }
}

/** 视频轨道：编码 · 分辨率 · 帧率 · 码率 */
private fun describeVideoFormat(format: Format?): String {
    if (format == null) return "未知"
    val codec = format.codecs
        ?: format.sampleMimeType?.substringAfterLast('/')?.uppercase()
        ?: return "未知"
    val parts = mutableListOf(codec)
    if (format.width > 0 && format.height > 0) parts += "${format.width}×${format.height}"
    if (format.frameRate > 0f) parts += "%.2f fps".format(format.frameRate)
    if (format.bitrate > 0) parts += "${format.bitrate / 1000} kbps"
    return parts.joinToString(" · ")
}

/** 音频轨道：编码 · 采样率 · 声道数 · 语言 */
private fun describeAudioFormat(format: Format?): String {
    if (format == null) return "未知"
    val codec = format.codecs
        ?: format.sampleMimeType?.substringAfterLast('/')?.uppercase()
        ?: return "未知"
    val parts = mutableListOf(codec)
    if (format.sampleRate > 0) parts += "${format.sampleRate} Hz"
    if (format.channelCount > 0) parts += "${format.channelCount} 声道"
    format.language?.takeIf { it.isNotBlank() }?.let { parts += it }
    return parts.joinToString(" · ")
}

/** 拼接「影片名 + 集数名」，集数名为空时按序号生成 */
private fun buildEpisodeTitle(base: String, epName: String, eIdx: Int): String {
    val ep = epName.ifEmpty { String.format("第%02d集", eIdx + 1) }
    return "$base $ep"
}

private fun formatTime(ms: Long): String {
    if (ms < 0) return "--:--"
    val totalSec = ms / 1000
    val h = totalSec / 3600
    val m = (totalSec % 3600) / 60
    val s = totalSec % 60
    return if (h > 0) String.format("%d:%02d:%02d", h, m, s) else String.format("%02d:%02d", m, s)
}
