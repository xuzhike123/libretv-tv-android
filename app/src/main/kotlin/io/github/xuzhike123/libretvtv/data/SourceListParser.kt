package io.github.xuzhike123.libretvtv.data

import org.json.JSONArray
import org.json.JSONObject

/**
 * 解析 LibreTV 订阅列表 JSON，兼容三种格式：
 *  1) LibreTV-SourceList：{ name, version, sources:[...], liveSources:[...] }
 *  2) 裸数组：[{ name, url }, ...] 视为点播源
 *  3) TVBOX 配置：{ sites:[...], lives:[...] }（仅导入可直接使用的接口）
 *
 * 返回 (点播源, 直播源)。按 url 去重；过滤非 http(s)；点播源另需为公网地址。
 */
object SourceListParser {

    fun parse(raw: String): Pair<List<VodSource>, List<LiveSource>> {
        val vodSeen = LinkedHashSet<String>(256)
        val liveSeen = LinkedHashSet<String>(128)
        val vods = mutableListOf<VodSource>()
        val lives = mutableListOf<LiveSource>()

        val root = try {
            when {
                raw.trim().startsWith("[") -> JSONArray(raw)
                else -> JSONObject(raw)
            }
        } catch (e: Exception) {
            return emptyList<VodSource>() to emptyList()
        }

        when (root) {
            is JSONArray -> {
                for (i in 0 until root.length()) {
                    val obj = root.optJSONObject(i) ?: continue
                    addVod(obj, vods, vodSeen)
                }
            }
            is JSONObject -> {
                // LibreTV-SourceList
                if (root.has("sources") || root.has("liveSources")) {
                    root.optJSONArray("sources")?.let { arr ->
                        for (i in 0 until arr.length()) addVod(arr.optJSONObject(i), vods, vodSeen)
                    }
                    root.optJSONArray("liveSources")?.let { arr ->
                        for (i in 0 until arr.length()) addLive(arr.optJSONObject(i), lives, liveSeen)
                    }
                } else if (root.has("sites")) {
                    // TVBOX
                    val sites = root.optJSONArray("sites")
                    for (i in 0 until (sites?.length() ?: 0)) {
                        val s = sites!!.optJSONObject(i) ?: continue
                        val type = s.optInt("type", -1)
                        val api = s.optString("api").ifEmpty { s.optString("url") }
                        // 仅导入 type==1 的 JSON 接口，或地址命中 Apple CMS 采集站
                        val isCms = api.contains("api.php/provide/vod", ignoreCase = true)
                        if (type == 1 || isCms) {
                            val obj = JSONObject().apply {
                                put("name", s.optString("name", "未命名源"))
                                put("url", api)
                                put("detail", s.optString("detail"))
                                put("isAdult", s.optBoolean("isAdult", false))
                            }
                            addVod(obj, vods, vodSeen)
                        }
                    }
                    val livesArr = root.optJSONArray("lives")
                    parseTvboxLives(livesArr, lives, liveSeen)
                }
            }
        }

        // 控制规模：点播最多 100、直播最多 50（与 LibreTV 服务端一致）
        return vods.take(100) to lives.take(50)
    }

    private fun parseTvboxLives(
        lives: JSONArray?,
        out: MutableList<LiveSource>,
        seen: LinkedHashSet<String>
    ) {
        for (i in 0 until (lives?.length() ?: 0)) {
            val l = lives!!.optJSONObject(i) ?: continue
            // 形态 A：{ name, url }
            val direct = l.optString("url").ifEmpty { l.optString("link") }
            if (direct.isNotEmpty()) {
                addLive(l, out, seen)
                continue
            }
            // 形态 B：{ groups:[ { name, channels:[ { name, urls:{...} } ] } ] }
            val groups = l.optJSONArray("groups")
            for (g in 0 until (groups?.length() ?: 0)) {
                val grp = groups!!.optJSONObject(g) ?: continue
                val groupName = grp.optString("name", "未分组")
                val channels = grp.optJSONArray("channels")
                for (c in 0 until (channels?.length() ?: 0)) {
                    val ch = channels!!.optJSONObject(c) ?: continue
                    val chName = ch.optString("name", "频道")
                    val urls = ch.optJSONObject("urls")
                    val chUrl = if (urls != null && urls.length() > 0) {
                        urls.optString(urls.names()?.getString(0) ?: "")
                    } else {
                        ch.optString("url")
                    }
                    if (chUrl.isNotEmpty()) {
                        addLive(
                            JSONObject().apply {
                                put("name", chName)
                                put("url", chUrl)
                            }, out, seen, groupName
                        )
                    }
                }
            }
        }
    }

    private fun addVod(obj: JSONObject?, out: MutableList<VodSource>, seen: LinkedHashSet<String>) {
        if (obj == null) return
        val url = obj.optString("url").ifEmpty { obj.optString("api") }.trim()
        if (!isHttp(url)) return
        val key = url.lowercase()
        if (!seen.add(key)) return
        if (!isPublic(url)) return // 点播源需为公网
        out.add(
            VodSource(
                name = obj.optString("name", url),
                url = url,
                detail = obj.optString("detail"),
                isAdult = obj.optBoolean("isAdult", false)
            )
        )
    }

    private fun addLive(
        obj: JSONObject?,
        out: MutableList<LiveSource>,
        seen: LinkedHashSet<String>,
        group: String = "未分组"
    ) {
        if (obj == null) return
        val url = obj.optString("url").ifEmpty { obj.optString("link") }.trim()
        if (!isHttp(url)) return
        val key = url.lowercase()
        if (!seen.add(key)) return
        out.add(
            LiveSource(
                name = obj.optString("name", url),
                url = url,
                epg = obj.optString("epg")
            )
        )
    }

    private fun isHttp(url: String): Boolean {
        return url.startsWith("http://", ignoreCase = true) ||
            url.startsWith("https://", ignoreCase = true)
    }

    private fun isPublic(url: String): Boolean {
        return try {
            val host = url.substringAfter("://").substringBefore('/').substringBefore(':')
            !host.equals("localhost", ignoreCase = true) &&
                !host.equals("127.0.0.1", ignoreCase = true) &&
                !host.startsWith("10.") &&
                !host.startsWith("192.168.") &&
                !host.startsWith("172.") &&
                !host.startsWith("169.254.")
        } catch (_: Exception) {
            false
        }
    }
}
