package io.github.xuzhike123.libretvtv.data

/**
 * 真实数据源模型（取代 DemoData 的占位结构）。
 */

/** 点播源：Apple CMS 采集站，url 形如 https://xxx/api.php/provide/vod */
data class VodSource(
    val name: String,
    val url: String,
    val detail: String = "",
    val isAdult: Boolean = false
)

/** 直播源：M3U / M3U8 播放列表地址 */
data class LiveSource(
    val name: String,
    val url: String,
    val epg: String = ""
)

/**
 * 一条远程订阅（LibreTV-SourceList JSON）。
 * 拉取后解析出 vodSources / liveSources 并持久化。
 */
data class Subscription(
    val url: String,
    val name: String = "",
    val version: Int = 0,
    val vodSources: List<VodSource> = emptyList(),
    val liveSources: List<LiveSource> = emptyList(),
    val syncedAt: Long = 0L,
    val lastError: String = "",
    val enabled: Boolean = true
) {
    val vodCount: Int get() = vodSources.size
    val liveCount: Int get() = liveSources.size
}

/** 搜索 / 列表返回的影片条目 */
data class CmsVideo(
    val vodId: String,
    val name: String,
    val pic: String = "",
    val remarks: String = "",
    val typeName: String = "",
    val year: String = "",
    val sourceUrl: String,
    val sourceName: String
) {
    /** 用 sourceUrl 编码出可在详情页还原的 id */
    val id: String get() = "$vodId@$sourceUrl"
}

/** 单集播放地址 */
data class CmsEpisode(
    val name: String,
    val url: String
)

/** 一个播放源下的剧集列表（如"正片""备用"） */
data class PlaySource(
    val name: String,
    val episodes: List<CmsEpisode>
)

/** 详情：含多个播放源与剧集 */
data class CmsDetail(
    val vodId: String,
    val name: String,
    val pic: String = "",
    val remarks: String = "",
    val typeName: String = "",
    val year: String = "",
    val desc: String = "",
    val sourceUrl: String,
    val sourceName: String,
    val playSources: List<PlaySource> = emptyList()
) {
    val episodes: List<CmsEpisode> get() = playSources.firstOrNull()?.episodes ?: emptyList()
}

/** 直播频道（来自 M3U 解析） */
data class LiveChannel(
    val name: String,
    val url: String,
    val group: String = "未分组",
    val logo: String = ""
)
