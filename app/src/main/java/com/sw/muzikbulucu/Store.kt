package com.sw.muzikbulucu

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale

data class Song(
    val id: String,
    val title: String,
    val artist: String,
    val album: String,
    val releaseDate: String,
    val label: String,
    val artwork: String,
    val spotifyUrl: String,
    val appleUrl: String,
    val youtubeUrl: String,
    val songLink: String,
    val lyrics: String,
    val foundAt: Long
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id); put("title", title); put("artist", artist); put("album", album)
        put("releaseDate", releaseDate); put("label", label); put("artwork", artwork)
        put("spotifyUrl", spotifyUrl); put("appleUrl", appleUrl); put("youtubeUrl", youtubeUrl)
        put("songLink", songLink); put("lyrics", lyrics); put("foundAt", foundAt)
    }

    companion object {
        fun fromJson(o: JSONObject) = Song(
            o.optString("id"), o.optString("title"), o.optString("artist"), o.optString("album"),
            o.optString("releaseDate"), o.optString("label"), o.optString("artwork"),
            o.optString("spotifyUrl"), o.optString("appleUrl"), o.optString("youtubeUrl"),
            o.optString("songLink"), o.optString("lyrics"), o.optLong("foundAt")
        )
    }
}

/** Türkçe / İngilizce metin seçici. Dil Store.lang'a göre değişir. */
fun T(tr: String, en: String): String = if (Store.lang == "tr") tr else en

object Store {
    private lateinit var p: SharedPreferences

    var token by mutableStateOf("")
        private set
    var theme by mutableStateOf("system")
        private set
    var lang by mutableStateOf("tr")
        private set
    var notif by mutableStateOf(true)
        private set
    var history by mutableStateOf(listOf<Song>())
        private set
    var bubbleOn by mutableStateOf(false)

    fun init(ctx: Context) {
        p = ctx.getSharedPreferences("sw", Context.MODE_PRIVATE)
        token = p.getString("token", "") ?: ""
        theme = p.getString("theme", "system") ?: "system"
        lang = p.getString("lang", null) ?: if (Locale.getDefault().language == "tr") "tr" else "en"
        notif = p.getBoolean("notif", true)
        history = try {
            val a = JSONArray(p.getString("history", "[]"))
            List(a.length()) { Song.fromJson(a.getJSONObject(it)) }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun setToken(v: String) { token = v; p.edit().putString("token", v).apply() }
    fun setTheme(v: String) { theme = v; p.edit().putString("theme", v).apply() }
    fun setLang(v: String) { lang = v; p.edit().putString("lang", v).apply() }
    fun setNotif(v: Boolean) { notif = v; p.edit().putBoolean("notif", v).apply() }

    private fun saveHistory() {
        val a = JSONArray()
        history.forEach { a.put(it.toJson()) }
        p.edit().putString("history", a.toString()).apply()
    }

    fun add(s: Song) { history = listOf(s) + history; saveHistory() }
    fun remove(id: String) { history = history.filter { it.id != id }; saveHistory() }
    fun clear() { history = emptyList(); saveHistory() }
    fun updateLyrics(id: String, lyrics: String) {
        history = history.map { if (it.id == id) it.copy(lyrics = lyrics) else it }
        saveHistory()
    }
    fun find(id: String): Song? = history.firstOrNull { it.id == id }
}
