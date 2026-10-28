package com.hansaps.spotify.rexmusic

import com.hansaps.core.NexrayApi
import com.hansaps.core.NexrayLyrics
import com.hansaps.core.NexraySpotifyTrack
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.concurrent.ConcurrentHashMap

/** Nexray adapter for the RexMusic engine. UI/player logic stays aligned with RexMusic; the
 * network transport is routed through HansAps' already verified Nexray client. */
object RexMusicApi {
    private val audioCache = ConcurrentHashMap<String, String>()
    private val lyricsCache = ConcurrentHashMap<String, Lyrics>()

    suspend fun search(query: String): Result<List<RexTrack>> = try {
        val tracks = NexrayApi.searchSpotify(query).mapNotNull { it.toRexTrack() }
            .distinctBy { it.id }
        Result.success(tracks)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(Exception(friendlyMessage(e), e))
    }

    suspend fun resolveAudio(track: RexTrack): Result<RexTrack> = try {
        if (track.spotifyUrl.isBlank()) error("tautan lagu tidak tersedia")
        val audio = audioCache[track.spotifyUrl] ?: NexrayApi.resolveSpotifyAudio(track.spotifyUrl)
            .also { audioCache[track.spotifyUrl] = it }
        Result.success(track.copy(audioUrl = audio))
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(Exception(friendlyMessage(e), e))
    }

    suspend fun play(query: String): Result<RexTrack> {
        val track = search(query).getOrElse { return Result.failure(it) }.firstOrNull()
            ?: return Result.failure(IllegalStateException("Lagu tidak ditemukan."))
        return resolveAudio(track)
    }

    suspend fun fetchLyrics(query: String): Result<Lyrics> {
        val key = query.trim().lowercase()
        if (key.isBlank()) return Result.failure(IllegalArgumentException("Query lirik kosong."))
        lyricsCache[key]?.let { return Result.success(it) }
        return try {
            val lyrics = withContext(Dispatchers.IO) {
                val encoded = URLEncoder.encode(query.trim(), Charsets.UTF_8.name())
                val connection = (URL("https://api.nexray.eu.cc/search/lyrics?q=$encoded").openConnection() as HttpURLConnection).apply {
                    connectTimeout = 12_000
                    readTimeout = 18_000
                    requestMethod = "GET"
                    setRequestProperty("Accept", "application/json")
                    setRequestProperty("User-Agent", "HansAps/1.0 Android")
                }
                try {
                    val code = connection.responseCode
                    val stream = if (code in 200..299) connection.inputStream else connection.errorStream
                    val body = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
                    if (code !in 200..299) error("Server lirik merespons HTTP $code.")
                    parseLyrics(JSONObject(body))
                } finally {
                    connection.disconnect()
                }
            }
            lyricsCache[key] = lyrics
            Result.success(lyrics)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(Exception(friendlyMessage(e), e))
        }
    }

    fun invalidateAudio(track: RexTrack) {
        audioCache.remove(track.spotifyUrl)
    }

    fun clearCache() {
        audioCache.clear()
        lyricsCache.clear()
    }

    private fun NexraySpotifyTrack.toRexTrack(): RexTrack? {
        if (url.isBlank() || title.isBlank()) return null
        return RexTrack(
            id = url,
            title = title,
            artist = artist.ifBlank { "Artis tidak diketahui" },
            album = album.ifBlank { "Single" },
            cover = thumbnail,
            spotifyUrl = url,
            durationText = duration,
        )
    }

    private fun parseLyrics(root: JSONObject): Lyrics {
        val result = root.optJSONObject("result") ?: JSONObject()
        val data = result.optJSONObject("lyrics") ?: result
        val plain = data.optString("plain_lyrics").takeUnless { it == "null" }.orEmpty()
        val syncedText = data.optString("synced_lyrics").takeUnless { it == "null" }.orEmpty()
        val pattern = Regex("""^\s*\[(\d{1,2}):(\d{2})(?:[.:](\d{1,3}))?]\s*(.*?)\s*$""")
        val synced = syncedText.lineSequence().mapNotNull { line ->
            val match = pattern.matchEntire(line) ?: return@mapNotNull null
            val minutes = match.groupValues[1].toLongOrNull() ?: return@mapNotNull null
            val seconds = match.groupValues[2].toLongOrNull() ?: return@mapNotNull null
            val rawFraction = match.groupValues[3]
            val fraction = rawFraction.toLongOrNull() ?: 0L
            val fractionMs = when (rawFraction.length) { 1 -> fraction * 100L; 2 -> fraction * 10L; 3 -> fraction; else -> 0L }
            val text = match.groupValues[4].trim()
            if (text.isBlank()) null else LyricLine(minutes * 60_000L + seconds * 1_000L + fractionMs, text, match.groupValues[0].substringAfter('[').substringBefore(']'))
        }.sortedBy { it.timeMs }
        return Lyrics(plain = plain, synced = synced, durationSeconds = data.optInt("duration", 0).coerceAtLeast(0))
    }

    private fun friendlyMessage(error: Exception): String = when {
        error.message.isNullOrBlank() -> "Permintaan Nexray gagal. Coba lagi."
        error.message!!.contains("UnknownHost", true) || error.message!!.contains("resolve host", true) -> "Server Nexray tidak dapat dijangkau. Periksa koneksi internet."
        else -> error.message!!
    }
}
