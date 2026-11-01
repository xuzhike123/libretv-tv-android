package io.github.xuzhike123.libretvtv.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * 订阅仓库：单例，持有 Application Context。
 * 负责远程订阅的拉取、解析、本地持久化，并对外暴露实时状态。
 */
object SourceRepository {

    /** 内置公开示例订阅（真实可用的 LibreTV 源列表） */
    const val EXAMPLE_SUBSCRIPTION_URL =
        "https://raw.githubusercontent.com/haojun19980728-prog/libretv-source-list/main/libretv-sources.json"

    private lateinit var appContext: Context
    private lateinit var storeFile: File
    private lateinit var customLiveFile: File

    /** 用户单独导入的自定义直播源（M3U 直链），独立于远程订阅 */
    private val _customLive = MutableStateFlow<List<LiveSource>>(emptyList())
    val customLive: StateFlow<List<LiveSource>> = _customLive.asStateFlow()

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .build()

    private val _subscriptions = MutableStateFlow<List<Subscription>>(emptyList())
    val subscriptions: StateFlow<List<Subscription>> = _subscriptions.asStateFlow()

    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy.asStateFlow()

    fun init(ctx: Context) {
        if (::appContext.isInitialized) return
        appContext = ctx.applicationContext
        storeFile = File(appContext.filesDir, "subscriptions.json")
        customLiveFile = File(appContext.filesDir, "live_sources.json")
        loadFromDisk()
        loadCustomLive()
    }

    /** 首次运行且无任何订阅时，注入示例订阅并同步 */
    suspend fun seedIfEmpty() {
        if (_subscriptions.value.isEmpty() && !storeFile.exists()) {
            addSubscription(EXAMPLE_SUBSCRIPTION_URL)
        }
    }

    fun loadFromDisk() {
        if (!::storeFile.isInitialized || !storeFile.exists()) return
        runCatching {
            val arr = JSONArray(storeFile.readText())
            val list = mutableListOf<Subscription>()
            for (i in 0 until arr.length()) {
                list.add(fromJson(arr.getJSONObject(i)))
            }
            _subscriptions.value = list
        }
    }

    /**
     * 新增订阅：拉取远程 JSON 并解析。
     *
     * 名称优先级：手动填写 > 订阅 JSON 里的 name > 站点首页 title > 域名兜底。
     * 采集站接口本身不返回站名，所以才有后面两级兜底。
     */
    suspend fun addSubscription(url: String, preferredName: String = ""): Subscription? =
        withContext(Dispatchers.IO) {
        val u = url.trim().removeSuffix("/")
        if (u.isEmpty() || !u.startsWith("http", ignoreCase = true)) return@withContext null
        _busy.value = true
        try {
            val raw = fetch(u)
            val (vods, lives) = SourceListParser.parse(raw ?: "", u)

            // 形态 4（地址本身就是采集站接口）时 Parser 只能拿域名占位，
            // 这里补抓一次站点首页 title 换成可读站名
            val hostName = u.substringAfter("://").substringBefore('/')
            val siteName = if (vods.size == 1 && vods[0].name == hostName) {
                fetchSiteName(u)
            } else null

            val manual = preferredName.trim()
            val jsonName = try {
                JSONObject(raw ?: "{}").optString("name", "")
            } catch (_: Exception) { "" }
            val finalName = manual.ifEmpty { jsonName.ifEmpty { siteName ?: hostName } }

            val namedVods = when {
                manual.isNotEmpty() && vods.size == 1 -> vods.map { it.copy(name = manual) }
                siteName != null -> vods.map { it.copy(name = siteName) }
                else -> vods
            }

            val sub = Subscription(
                url = u,
                name = finalName,
                vodSources = namedVods,
                liveSources = lives,
                syncedAt = System.currentTimeMillis(),
                lastError = if (vods.isEmpty() && lives.isEmpty()) "解析结果为空，请检查地址格式" else "",
                enabled = true
            )
            val merged = _subscriptions.value.toMutableList()
            merged.removeAll { it.url == u }
            merged.add(sub)
            _subscriptions.value = merged
            save()
            sub
        } catch (e: Exception) {
            val sub = Subscription(url = u, lastError = e.message ?: "拉取失败", enabled = true)
            val merged = _subscriptions.value.toMutableList()
            merged.removeAll { it.url == u }
            merged.add(sub)
            _subscriptions.value = merged
            save()
            sub
        } finally {
            _busy.value = false
        }
    }

    /** 重新同步某个订阅 */
    suspend fun resync(sub: Subscription): Subscription? = addSubscription(sub.url)

    fun removeSubscription(url: String) {
        _subscriptions.value = _subscriptions.value.filter { it.url != url }
        save()
    }

    fun setEnabled(url: String, enabled: Boolean) {
        _subscriptions.value = _subscriptions.value.map {
            if (it.url == url) it.copy(enabled = enabled) else it
        }
        save()
    }

    fun enabledVodSources(): List<VodSource> =
        _subscriptions.value.filter { it.enabled }.flatMap { it.vodSources }

    /** 合并「订阅内的直播源」与「用户单独导入的自定义直播源」 */
    fun enabledLiveSources(): List<LiveSource> =
        _subscriptions.value.filter { it.enabled }.flatMap { it.liveSources } + _customLive.value

    /** 单独导入一个 M3U 直播源（返回是否成功） */
    suspend fun addLiveSource(name: String, url: String): Boolean = withContext(Dispatchers.IO) {
        val u = url.trim().removeSuffix("/")
        if (u.isEmpty() || !u.startsWith("http", ignoreCase = true)) return@withContext false
        val n = name.trim().ifEmpty { "自定义直播源" }
        val list = _customLive.value.toMutableList()
        if (list.any { it.url == u }) return@withContext true
        list.add(LiveSource(name = n, url = u))
        _customLive.value = list
        saveCustomLive()
        true
    }

    fun removeLiveSource(url: String) {
        _customLive.value = _customLive.value.filter { it.url != url }
        saveCustomLive()
    }

    private fun loadCustomLive() {
        if (!::customLiveFile.isInitialized || !customLiveFile.exists()) return
        runCatching {
            val arr = JSONArray(customLiveFile.readText())
            val list = mutableListOf<LiveSource>()
            for (i in 0 until arr.length()) {
                val o = arr.optJSONObject(i) ?: continue
                list.add(LiveSource(name = o.optString("name"), url = o.optString("url"), epg = o.optString("epg")))
            }
            _customLive.value = list
        }
    }

    private fun saveCustomLive() {
        runCatching {
            val arr = JSONArray()
            _customLive.value.forEach {
                arr.put(JSONObject().apply {
                    put("name", it.name); put("url", it.url); put("epg", it.epg)
                })
            }
            customLiveFile.writeText(arr.toString())
        }
    }

    fun firstEnabledVodSource(): VodSource? = enabledVodSources().firstOrNull()

    private fun fetch(url: String): String? = try {
        val req = Request.Builder().url(url)
            .header("User-Agent", "LibreTV/AndroidTV")
            .build()
        val resp = client.newCall(req).execute()
        if (resp.isSuccessful) resp.body?.string() else null
    } catch (_: Exception) {
        null
    }

    /**
     * 采集站接口本身不返回站点名（实测顶层只有 code/msg/page/pagecount/limit/total/list，
     * 其中 msg 恒为「数据列表」这类通用文案），所以退一步抓站点首页的 <title> 来推断名称。
     *
     * 实测样本：
     *   360zy.com        → "360资源站【360zy.com】 - 苹果cms - 海洋cms - 影视站采集" → 360资源站
     *   iqiyizyapi.com   → "爱奇艺资源站-iqiyizy.com"                              → 爱奇艺资源站
     *   api.maoyanapi.top→ "功能模块及系统标签等等介绍 - 苹果CMS"（通用文案，无效）→ 回退域名
     */
    private fun fetchSiteName(url: String): String? = try {
        val home = url.substringBefore("?").let { p ->
            val i = p.indexOf("/api.php")
            val j = p.indexOf("/provide/vod")
            val cut = listOf(i, j).filter { it > 0 }.minOrNull()
            (if (cut != null) p.substring(0, cut) else p).trimEnd('/')
        }
        val host = home.substringAfter("://").substringBefore('/')
        if (host.isEmpty()) return null
        val html = fetch("$home/") ?: return null

        val raw = Regex("<title>(.*?)</title>", RegexOption.IGNORE_CASE)
            .find(html)?.groupValues?.get(1)?.trim() ?: return null

        // 取分隔符前的第一段作为站名候选，再去掉括号里的域名尾巴
        var name = raw.split("-", "—", "｜", "|")[0].trim()
        name = Regex("【[^】]*】|\\[[^]]*]|\\([^)]*\\)|（[^）]*）").replace(name, "").trim()

        // 只丢弃「CMS 默认文案」：如「功能模块及系统标签等等介绍 - 苹果CMS」。
        // 注意别把正常站名误杀——实测「最大资源采集网」是有效站名，
        // 早期版本因含"资源采集"被判为噪音而退回域名，这里已剔除该词。
        val cmsDefaults = listOf("苹果cms", "苹果CMS", "海洋CMS", "海洋cms",
            "cms", "CMS", "功能模块", "系统标签", "模板")
        if (cmsDefaults.any { name.contains(it, ignoreCase = true) }) return null
        if (name.isEmpty() || name.equals(host, ignoreCase = true)) return null
        name.take(24)
    } catch (_: Exception) {
        null
    }

    private fun save() {
        runCatching {
            val arr = JSONArray()
            _subscriptions.value.forEach { arr.put(toJson(it)) }
            storeFile.writeText(arr.toString())
        }
    }

    private fun toJson(s: Subscription): JSONObject = JSONObject().apply {
        put("url", s.url)
        put("name", s.name)
        put("version", s.version)
        put("syncedAt", s.syncedAt)
        put("lastError", s.lastError)
        put("enabled", s.enabled)
        val v = JSONArray()
        s.vodSources.forEach {
            v.put(JSONObject().apply {
                put("name", it.name); put("url", it.url)
                put("detail", it.detail); put("isAdult", it.isAdult)
            })
        }
        put("vodSources", v)
        val l = JSONArray()
        s.liveSources.forEach {
            l.put(JSONObject().apply {
                put("name", it.name); put("url", it.url); put("epg", it.epg)
            })
        }
        put("liveSources", l)
    }

    private fun jsonArrayToList(arr: JSONArray?): List<JSONObject> {
        val out = mutableListOf<JSONObject>()
        val a = arr ?: return out
        for (i in 0 until a.length()) {
            val o = a.optJSONObject(i) ?: continue
            out.add(o)
        }
        return out
    }

    private fun fromJson(o: JSONObject): Subscription = Subscription(
        url = o.optString("url"),
        name = o.optString("name"),
        version = o.optInt("version", 0),
        syncedAt = o.optLong("syncedAt", 0L),
        lastError = o.optString("lastError"),
        enabled = o.optBoolean("enabled", true),
        vodSources = jsonArrayToList(o.optJSONArray("vodSources")).map { j ->
            VodSource(
                name = j.optString("name"),
                url = j.optString("url"),
                detail = j.optString("detail"),
                isAdult = j.optBoolean("isAdult", false)
            )
        },
        liveSources = jsonArrayToList(o.optJSONArray("liveSources")).map { j ->
            LiveSource(
                name = j.optString("name"),
                url = j.optString("url"),
                epg = j.optString("epg")
            )
        }
    )
}
