package io.github.xuzhike123.libretvtv.data

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

    fun set(url: String, title: String, source: String, videoId: String) {
        this.url = url
        this.title = title
        this.source = source
        this.videoId = videoId
    }

    fun consume(): Triple<String, String, String> = Triple(url, title, source)
}
