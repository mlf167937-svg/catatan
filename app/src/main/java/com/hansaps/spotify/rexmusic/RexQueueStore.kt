package com.hansaps.spotify.rexmusic

import android.content.Context
import java.io.File
import org.json.JSONArray
import org.json.JSONObject

/**
 * Antrian disimpan di folder cache sebagai file JSON kecil.
 * Yang disimpan HANYA metadata (id, judul, artis, album, cover, link spotify). Tidak ada mp3/audioUrl.
 * Kalau cache aplikasi dibersihkan, antrian ikut kosong (memang sifat cache).
 */
class RexQueueStore(context: Context) {

    private val file = File(context.applicationContext.cacheDir, FILE_NAME)

    fun load(): List<RexTrack> {
        if (!file.exists()) return emptyList()
        return runCatching {
            val arr = JSONArray(file.readText())
            (0 until arr.length()).mapNotNull { i -> arr.optJSONObject(i)?.toTrackOrNull() }
        }.getOrDefault(emptyList())
    }

    fun save(list: List<RexTrack>) {
        runCatching {
            val arr = JSONArray()
            list.forEach { arr.put(it.toJson()) }
            file.writeText(arr.toString())
        }
    }

    fun clear() {
        runCatching { file.delete() }
    }

    private companion object {
        const val FILE_NAME = "rex_queue.json"
    }
}

internal fun RexTrack.toJson(): JSONObject = JSONObject()
    .put("id", id)
    .put("title", title)
    .put("artist", artist)
    .put("album", album)
    .put("cover", cover)
    .put("spotifyUrl", spotifyUrl)

internal fun JSONObject.toTrackOrNull(): RexTrack? {
    val title = optString("title")
    if (title.isBlank()) return null
    return RexTrack(
        id = optString("id").ifBlank { title },
        title = title,
        artist = optString("artist").ifBlank { "Unknown" },
        album = optString("album").ifBlank { "Single" },
        cover = optString("cover"),
        spotifyUrl = optString("spotifyUrl")
    )
}
