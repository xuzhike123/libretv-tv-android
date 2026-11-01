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

    /** 新增订阅：拉取远程 JSON 并解析 */
    suspend fun addSubscription(url: String): Subscription? = withContext(Dispatchers.IO) {
        val u = url.trim().removeSuffix("/")
        if (u.isEmpty() || !u.startsWith("http", ignoreCase = true)) return@withContext null
        _busy.value = true
        try {
            val raw = fetch(u)
            val (vods, lives) = SourceListParser.parse(raw ?: "", u)
            val name = try {
                JSONObject(raw ?: "{}").optString("name", "")
            } catch (_: Exception) { "" }
            val sub = Subscription(
                url = u,
                name = name,
                version = try { JSONObject(raw ?: "{}").optInt("version", 0) } catch (_: Exception) { 0 },
                vodSources = vods,
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
