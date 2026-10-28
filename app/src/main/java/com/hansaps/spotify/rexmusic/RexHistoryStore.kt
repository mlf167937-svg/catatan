package com.hansaps.spotify.rexmusic

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * Menyimpan riwayat lagu ke SharedPreferences.
 * Yang disimpan HANYA metadata (judul, artis, album, cover, link spotify).
 * File mp3 / audioUrl tidak pernah disimpan; audio diambil ulang lewat API saat diputar.
 */
class RexHistoryStore(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    /** Urutan kronologis: yang paling lama di depan, yang terbaru di belakang. */
    fun load(): List<RexTrack> = runCatching {
        val arr = JSONArray(prefs.getString(KEY, "[]") ?: "[]")
        (0 until arr.length()).mapNotNull { i ->
            val o = arr.optJSONObject(i) ?: return@mapNotNull null
            val title = o.optString("title")
            if (title.isBlank()) return@mapNotNull null
            RexTrack(
                id = o.optString("id").ifBlank { title },
                title = title,
                artist = o.optString("artist").ifBlank { "Unknown" },
                album = o.optString("album").ifBlank { "Single" },
                cover = o.optString("cover"),
                spotifyUrl = o.optString("spotifyUrl")
            )
        }
    }.getOrDefault(emptyList())

    fun save(list: List<RexTrack>) {
        runCatching {
            val arr = JSONArray()
            list.forEach { t ->
                arr.put(
                    JSONObject()
                        .put("id", t.id)
                        .put("title", t.title)
                        .put("artist", t.artist)
                        .put("album", t.album)
                        .put("cover", t.cover)
                        .put("spotifyUrl", t.spotifyUrl)
                )
            }
            prefs.edit().putString(KEY, arr.toString()).apply()
        }
    }

    fun clear() = prefs.edit().remove(KEY).apply()

    private companion object {
        const val PREFS = "rex_music_history"
        const val KEY = "history_v1"
    }
}
