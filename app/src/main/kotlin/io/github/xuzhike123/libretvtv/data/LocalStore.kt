package io.github.xuzhike123.libretvtv.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * 本机收藏与观看历史（持久化到 filesDir/localstore.json）。
 * 仅保存在设备本地，不上传。
 */
object LocalStore {

    private lateinit var file: File
    private var favorites = LinkedHashMap<String, String>() // id -> title
    private var history = mutableListOf<HistoryEntry>()

    fun init(ctx: Context) {
        if (::file.isInitialized) return
        file = File(ctx.applicationContext.filesDir, "localstore.json")
        load()
    }

    private fun load() {
        if (!file.exists()) return
        runCatching {
            val o = JSONObject(file.readText())
            o.optJSONArray("favorites")?.let { a ->
                for (i in 0 until a.length()) {
                    val e = a.getJSONObject(i)
                    favorites[e.optString("id")] = e.optString("title")
                }
            }
            o.optJSONArray("history")?.let { a ->
                for (i in 0 until a.length()) {
                    val e = a.getJSONObject(i)
                    history.add(
                        HistoryEntry(
                            videoId = e.optString("videoId"),
                            title = e.optString("title"),
                            source = e.optString("source"),
                            positionMs = e.optLong("positionMs", 0),
                            durationMs = e.optLong("durationMs", 0),
                            updatedAt = e.optLong("updatedAt", 0)
                        )
                    )
                }
            }
        }
    }

    private fun save() {
        runCatching {
            val o = JSONObject()
            val f = JSONArray()
            favorites.forEach { (id, title) ->
                f.put(JSONObject().apply { put("id", id); put("title", title) })
            }
            o.put("favorites", f)
            val h = JSONArray()
            history.sortedByDescending { it.updatedAt }.take(50).forEach {
                h.put(JSONObject().apply {
                    put("videoId", it.videoId); put("title", it.title); put("source", it.source)
                    put("positionMs", it.positionMs); put("durationMs", it.durationMs)
                    put("updatedAt", it.updatedAt)
                })
            }
            o.put("history", h)
            file.writeText(o.toString())
        }
    }

    fun isFavorite(id: String): Boolean = favorites.containsKey(id)

    fun toggleFavorite(id: String, title: String) {
        if (favorites.remove(id) == null) favorites[id] = title
        save()
    }

    fun getFavorites(): List<Pair<String, String>> = favorites.map { it.key to it.value }

    fun addHistory(entry: HistoryEntry) {
        history.removeAll { it.videoId == entry.videoId }
        history.add(0, entry)
        if (history.size > 50) history = history.take(50).toMutableList()
        save()
    }

    fun getHistory(): List<HistoryEntry> = history.sortedByDescending { it.updatedAt }
}

data class HistoryEntry(
    val videoId: String,
    val title: String,
    val source: String,
    val positionMs: Long,
    val durationMs: Long,
    val updatedAt: Long
)
