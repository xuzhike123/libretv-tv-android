package io.github.xuzhike123.libretvtv.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/**
 * 解析 IPTV / 直播 M3U 播放列表。
 * 每条频道形如：
 *   #EXTINF:-1 tvg-id="x" tvg-logo="u" group-title="央视",频道名
 *   https://.../xxx.m3u8
 */
object M3uParser {

    private val client = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .build()

    /** 拉取并解析某个直播源的 M3U 文本 */
    suspend fun fetchAndParse(source: LiveSource): List<LiveChannel> = withContext(Dispatchers.IO) {
        val raw = try {
            val req = Request.Builder().url(source.url)
                .header("User-Agent", "LibreTV/AndroidTV")
                .build()
            val resp = client.newCall(req).execute()
            if (resp.isSuccessful) resp.body?.string() else null
        } catch (_: Exception) {
            null
        } ?: return@withContext emptyList()
        parse(raw)
    }

    fun parse(text: String): List<LiveChannel> {
        val lines = text.lineSequence().map { it.trimEnd() }.filter { it.isNotEmpty() }
        val out = mutableListOf<LiveChannel>()
        var pending: Pending? = null
        for (line in lines) {
            if (line.startsWith("#EXTINF", ignoreCase = true)) {
                pending = parseExtInf(line)
            } else if (line.startsWith("#")) {
                // 其它注释行（如 #EXTGRP）忽略，但若有 #EXTGRP 可补充分组
                continue
            } else {
                // 数据行：频道地址
                val url = line
                val p = pending
                if (p != null && url.startsWith("http", ignoreCase = true)) {
                    out.add(LiveChannel(name = p.name, url = url, group = p.group, logo = p.logo))
                }
                pending = null
            }
        }
        return out
    }

    private fun parseExtInf(line: String): Pending {
        val commaIdx = line.indexOf(',')
        val attrsPart = if (commaIdx >= 0) line.substring(line.indexOf(' ') + 1, commaIdx) else ""
        val name = if (commaIdx >= 0) line.substring(commaIdx + 1).trim() else "频道"
        val group = attr(attrsPart, "group-title") ?: "未分组"
        val logo = attr(attrsPart, "tvg-logo") ?: ""
        return Pending(name = name.ifEmpty { "频道" }, group = group, logo = logo)
    }

    private fun attr(part: String, key: String): String? {
        val i = part.indexOf("$key=", ignoreCase = true)
        if (i < 0) return null
        val start = part.indexOf('"', i)
        if (start < 0) return null
        val end = part.indexOf('"', start + 1)
        if (end < 0) return null
        return part.substring(start + 1, end)
    }

    private data class Pending(val name: String, val group: String, val logo: String)
}
