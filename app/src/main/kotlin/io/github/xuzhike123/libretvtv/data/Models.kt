package io.github.xuzhike123.libretvtv.data

data class VideoItem(
    val id: String,
    val title: String,
    val source: String,
    val duration: String,
    val secondaryLine: String = "",
    val progressPercent: Int? = null,
    val pic: String = ""
)

data class ContentRow(
    val title: String,
    val subtitle: String = "",
    val items: List<VideoItem>
)

data class SourceItem(
    val name: String,
    val address: String,
    val latencyMs: Int,
    val healthy: Boolean
)

data class ChannelItem(
    val number: String,
    val name: String,
    val nowPlaying: String,
    val next: String
)
