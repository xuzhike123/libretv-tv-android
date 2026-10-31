package io.github.xuzhike123.libretvtv.data

/**
 * 一次播放请求的完整参数。
 */
data class PlayRequest(
    val url: String = "",
    val title: String = "",
    val source: String = "",
    /** 是否为直播（M3U 频道）。直播无时长、不可拖动进度，控制条也与点播不同。 */
    val isLive: Boolean = false,
    /** 直播时为同批次频道列表，用于「上一频道 / 下一频道」切换。 */
    val liveChannels: List<LiveChannel> = emptyList(),
    /** 当前频道在 liveChannels 中的下标，-1 表示未知。 */
    val liveIndex: Int = -1
)

/**
 * 单次播放请求的临时载体。
 * 由于播放地址（m3u8）含大量特殊字符，不适合塞进导航路由，
 * 故通过本单例在「详情 → 播放」之间传递，由 PlayerScreen 读取。
 * 仅用于本次会话内的导航跳转，进程重建后会丢失（可接受）。
 */
object PendingPlay {
    var url: String = ""
    var title: String = ""
    var source: String = ""
    var videoId: String = ""
    var isLive: Boolean = false
    var liveChannels: List<LiveChannel> = emptyList()
    var liveIndex: Int = -1

    fun set(
        url: String,
        title: String,
        source: String,
        videoId: String,
        isLive: Boolean = false,
        liveChannels: List<LiveChannel> = emptyList(),
        liveIndex: Int = -1
    ) {
        this.url = url
        this.title = title
        this.source = source
        this.videoId = videoId
        this.isLive = isLive
        this.liveChannels = liveChannels
        this.liveIndex = liveIndex
    }

    fun consume(): PlayRequest = PlayRequest(
        url = url,
        title = title,
        source = source,
        isLive = isLive,
        liveChannels = liveChannels,
        liveIndex = liveIndex
    )
}
