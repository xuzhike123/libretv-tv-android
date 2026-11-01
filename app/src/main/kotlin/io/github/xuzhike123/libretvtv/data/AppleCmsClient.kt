package io.github.xuzhike123.libretvtv.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

/**
 * Apple CMS（飞飞/苹果 CMS）采集站 API 客户端。
 * 接口约定：GET {base}?ac=videolist&wd=关键词&pg=页码   （搜索）
 *           GET {base}?ac=videolist&ids=影片ID          （详情）
 * 返回 {"code":1,"list":[...]}。
 */
object AppleCmsClient {

    private val client = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    private fun buildUrl(base: String, params: Map<String, String>): String {
        val sep = if (base.contains('?')) '&' else '?'
        val query = params.entries.joinToString("&") { (k, v) ->
            "$k=${URLEncoder.encode(v, "UTF-8")}"
        }
        return "$base$sep$query"
    }

    private fun getText(url: String): String? = try {
        val req = Request.Builder().url(url).header("User-Agent", UA).build()
        val resp = client.newCall(req).execute()
        if (resp.isSuccessful) resp.body?.string() else null
    } catch (_: Exception) {
        null
    }

    /** 搜索（跨页合并，最多 maxPages 页） */
    suspend fun search(
        source: VodSource,
        keyword: String,
        maxPages: Int = 2
    ): List<CmsVideo> = withContext(Dispatchers.IO) {
        val out = mutableListOf<CmsVideo>()
        val seen = LinkedHashSet<String>()
        for (pg in 1..maxPages) {
            val url = buildUrl(source.url, mapOf("ac" to "videolist", "wd" to keyword, "pg" to pg.toString()))
            val raw = getText(url) ?: break
            val items = parseList(raw, source) ?: break
            if (items.isEmpty()) break
            items.forEach {
                if (seen.add(it.vodId)) out.add(it)
            }
            if (items.size < 10) break // 一页不满，基本到尾页
        }
        out
    }

    /** 最近更新（不带关键词，取指定页） */
    suspend fun latest(source: VodSource, page: Int = 1): List<CmsVideo> = withContext(Dispatchers.IO) {
        val url = buildUrl(source.url, mapOf("ac" to "videolist", "pg" to page.toString()))
        val raw = getText(url) ?: return@withContext emptyList()
        parseList(raw, source) ?: emptyList()
    }

    /**
     * 最近更新（合并前 N 页，按影片 ID 去重）。
     * 首页要按「电影 / 电视 / 动漫 / 综艺 / 体育 / 其他」分类展示，
     * 单页通常只有 20 条，分完类每类所剩无几，所以多拉几页再分。
     */
    suspend fun latestMulti(source: VodSource, pages: Int = 3): List<CmsVideo> =
        withContext(Dispatchers.IO) {
            // 并行取页（串行要等 N 个往返，网络差时会明显卡顿）
            val jobs = (1..pages).map { pg ->
                async {
                    try {
                        val url = buildUrl(source.url, mapOf("ac" to "videolist", "pg" to pg.toString()))
                        val raw = getText(url) ?: return@async emptyList()
                        parseList(raw, source) ?: emptyList()
                    } catch (_: Exception) {
                        emptyList()
                    }
                }
            }
            val out = mutableListOf<CmsVideo>()
            val seen = LinkedHashSet<String>()
            // 按页序合并，保证「越新越靠前」
            jobs.forEach { job ->
                job.await().forEach { if (seen.add(it.vodId)) out.add(it) }
            }
            out
        }

    /** 详情：含剧集（支持多播放源） */
    suspend fun detail(source: VodSource, vodId: String): CmsDetail? = withContext(Dispatchers.IO) {
        val url = buildUrl(source.url, mapOf("ac" to "videolist", "ids" to vodId))
        val raw = getText(url) ?: return@withContext null
        try {
            val root = JSONObject(raw)
            val list = root.optJSONArray("list") ?: return@withContext null
            if (list.length() == 0) return@withContext null
            val o = list.getJSONObject(0)
            val playFrom = o.optString("vod_play_from", "")
            val playUrl = o.optString("vod_play_url", "")
            val playSources = parsePlaySources(playFrom, playUrl)
            CmsDetail(
                vodId = o.optString("vod_id", vodId),
                name = o.optString("vod_name", ""),
                pic = o.optString("vod_pic", ""),
                remarks = o.optString("vod_remarks", ""),
                typeName = o.optString("type_name", ""),
                year = o.optString("vod_year", ""),
                desc = o.optString("vod_content", "").take(400),
                sourceUrl = source.url,
                sourceName = source.name,
                playSources = playSources
            )
        } catch (_: Exception) {
            null
        }
    }

    private fun parseList(raw: String, source: VodSource): List<CmsVideo>? = try {
        val root = JSONObject(raw)
        val list = root.optJSONArray("list") ?: JSONArray()
        val out = mutableListOf<CmsVideo>()
        for (i in 0 until list.length()) {
            val o = list.optJSONObject(i) ?: continue
            out.add(
                CmsVideo(
                    vodId = o.optString("vod_id", ""),
                    name = o.optString("vod_name", ""),
                    pic = o.optString("vod_pic", ""),
                    remarks = o.optString("vod_remarks", ""),
                    typeName = o.optString("type_name", ""),
                    year = o.optString("vod_year", ""),
                    sourceUrl = source.url,
                    sourceName = source.name
                )
            )
        }
        out
    } catch (_: Exception) {
        null
    }

    /**
     * 解析 vod_play_from / vod_play_url，返回多个播放源及其剧集。
     * playFrom: "正片$$$备用"
     * playUrl:  "第1集$url1#第2集$url2$$$第1集$urlB1#..."
     */
    private fun parsePlaySources(playFrom: String, playUrl: String): List<PlaySource> {
        if (playUrl.isEmpty()) return emptyList()
        val fromParts = playFrom.split("$$$")
        val urlParts = playUrl.split("$$$")
        val out = mutableListOf<PlaySource>()
        val count = maxOf(fromParts.size, urlParts.size)
        for (i in 0 until count) {
            val name = fromParts.getOrNull(i)?.takeIf { it.isNotEmpty() } ?: "源${i + 1}"
            val segment = urlParts.getOrNull(i) ?: ""
            val episodes = mutableListOf<CmsEpisode>()
            segment.split("#").forEach { ep ->
                val cut = ep.split("$", limit = 2)
                val epName = cut.getOrNull(0)?.trim() ?: ""
                val epUrl = cut.getOrNull(1)?.trim() ?: ""
                if (epUrl.isNotEmpty()) episodes.add(CmsEpisode(epName, epUrl))
            }
            if (episodes.isNotEmpty()) out.add(PlaySource(name, episodes))
        }
        return out
    }

    private const val UA =
        "Mozilla/5.0 (Linux; Android 9.0; TV) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0 Safari/537.36"
}
