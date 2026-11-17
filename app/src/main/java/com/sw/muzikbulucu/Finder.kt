package com.sw.muzikbulucu

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaRecorder
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.net.URLEncoder
import java.util.UUID
import java.util.concurrent.TimeUnit

sealed class Outcome {
    data class Found(val song: Song) : Outcome()
    data object NotFound : Outcome()
    data class Error(val msg: String) : Outcome()
}

object Recorder {
    /** Mikrofondan [seconds] saniye AAC (m4a) kaydı alır. */
    suspend fun record(ctx: Context, seconds: Int, onLevel: (Int) -> Unit): File =
        withContext(Dispatchers.IO) {
            val file = File(ctx.cacheDir, "rec_${System.currentTimeMillis()}.m4a")
            val main = Handler(Looper.getMainLooper())
            @Suppress("DEPRECATION")
            val rec = if (Build.VERSION.SDK_INT >= 31) MediaRecorder(ctx) else MediaRecorder()
            try {
                rec.setAudioSource(MediaRecorder.AudioSource.MIC)
                rec.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                rec.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                rec.setAudioChannels(1)
                rec.setAudioSamplingRate(44100)
                rec.setAudioEncodingBitRate(128000)
                rec.setOutputFile(file.absolutePath)
                rec.prepare()
                rec.start()
                val end = SystemClock.elapsedRealtime() + seconds * 1000L
                while (SystemClock.elapsedRealtime() < end) {
                    delay(100)
                    val a = rec.maxAmplitude
                    main.post { onLevel(a) }
                }
                rec.stop()
            } catch (e: Exception) {
                file.delete()
                throw e
            } finally {
                rec.release()
            }
            file
        }
}

object Finder {
    private const val API = "https://api.audd.io/"

    val http: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .build()

    fun extractUrl(text: String): String? =
        Regex("https?://\\S+").find(text.trim())?.value

    /** Mikrofonla dinle, sonra tanı. */
    suspend fun listen(
        ctx: Context,
        seconds: Int = 10,
        onLevel: (Int) -> Unit = {},
        onRecorded: () -> Unit = {}
    ): Outcome {
        val file = try {
            Recorder.record(ctx, seconds, onLevel)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            return Outcome.Error(T("Mikrofon açılamadı: ", "Could not open microphone: ") + (e.message ?: ""))
        }
        onRecorded()
        return try {
            recognize(file, null)
        } finally {
            file.delete()
        }
    }

    /** YouTube / Instagram / TikTok vb. bağlantıdaki sesi AudD sunucusu analiz eder. */
    suspend fun fromLink(url: String): Outcome = recognize(null, url)

    private suspend fun recognize(file: File?, url: String?): Outcome = withContext(Dispatchers.IO) {
        if (Store.token.isBlank()) {
            return@withContext Outcome.Error(
                T("AudD API anahtarı girilmemiş. Ayarlar'dan ekle.", "AudD API token is missing. Add it in Settings.")
            )
        }
        try {
            val form = MultipartBody.Builder().setType(MultipartBody.FORM)
                .addFormDataPart("api_token", Store.token)
                .addFormDataPart("return", "apple_music,spotify")
            if (file != null) form.addFormDataPart("file", "rec.m4a", file.asRequestBody("audio/mp4".toMediaType()))
            if (url != null) form.addFormDataPart("url", url)
            val req = Request.Builder().url(API).post(form.build()).build()
            http.newCall(req).execute().use { resp ->
                val body = resp.body?.string().orEmpty()
                val json = JSONObject(body)
                if (json.optString("status") != "success") {
                    val msg = json.optJSONObject("error")?.optString("error_message").orEmpty()
                    return@withContext Outcome.Error(msg.ifBlank { "HTTP ${resp.code}" })
                }
                val r = json.optJSONObject("result") ?: return@withContext Outcome.NotFound
                Outcome.Found(parse(r))
            }
        } catch (e: IOException) {
            Outcome.Error(T("İnternet bağlantısı hatası: ", "Network error: ") + (e.message ?: ""))
        } catch (e: Exception) {
            Outcome.Error(T("Beklenmeyen hata: ", "Unexpected error: ") + (e.message ?: ""))
        }
    }

    private fun enc(s: String) = URLEncoder.encode(s, "UTF-8")

    private fun parse(r: JSONObject): Song {
        val artist = r.optString("artist")
        val title = r.optString("title")
        val q = enc("$artist $title")
        val am = r.optJSONObject("apple_music")
        val sp = r.optJSONObject("spotify")
        val link = r.optString("song_link")

        var art = am?.optJSONObject("artwork")?.optString("url").orEmpty()
            .replace("{w}", "600").replace("{h}", "600")
        if (art.isBlank()) {
            art = sp?.optJSONObject("album")?.optJSONArray("images")?.optJSONObject(0)?.optString("url").orEmpty()
        }
        val album = r.optString("album").ifBlank { sp?.optJSONObject("album")?.optString("name").orEmpty() }

        val spotify = sp?.optJSONObject("external_urls")?.optString("spotify").orEmpty().ifBlank {
            if (link.isNotBlank()) "$link?spotify" else "https://open.spotify.com/search/$q"
        }
        val apple = am?.optString("url").orEmpty().ifBlank {
            if (link.isNotBlank()) "$link?apple_music" else "https://music.apple.com/search?term=$q"
        }
        val youtube = if (link.isNotBlank()) "$link?youtube" else "https://www.youtube.com/results?search_query=$q"

        return Song(
            id = UUID.randomUUID().toString(), title = title, artist = artist, album = album,
            releaseDate = r.optString("release_date"), label = r.optString("label"),
            artwork = art, spotifyUrl = spotify, appleUrl = apple, youtubeUrl = youtube,
            songLink = link, lyrics = "", foundAt = System.currentTimeMillis()
        )
    }

    /** AudD findLyrics ile şarkı sözü arar. Bulunamazsa boş döner. */
    suspend fun lyrics(song: Song): String = withContext(Dispatchers.IO) {
        try {
            val body = FormBody.Builder()
                .add("api_token", Store.token)
                .add("q", "${song.artist} ${song.title}")
                .build()
            val req = Request.Builder().url(API + "findLyrics/").post(body).build()
            http.newCall(req).execute().use { resp ->
                val json = JSONObject(resp.body?.string().orEmpty())
                if (json.optString("status") != "success") return@withContext ""
                val arr = json.optJSONArray("result") ?: return@withContext ""
                if (arr.length() == 0) "" else arr.getJSONObject(0).optString("lyrics").trim()
            }
        } catch (e: Exception) {
            ""
        }
    }

    suspend fun bitmap(url: String): Bitmap? = withContext(Dispatchers.IO) {
        if (url.isBlank()) return@withContext null
        try {
            http.newCall(Request.Builder().url(url).build()).execute().use { r ->
                r.body?.byteStream()?.let { BitmapFactory.decodeStream(it) }
            }
        } catch (e: Exception) {
            null
        }
    }
}
