package com.hansaps.core

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.Locale

/** API routes taken from the original HansAps HTML (Nexray). */
object NexrayApi {
    private const val BASE = "https://api.nexray.eu.cc"
    private const val SEARCH_TTL_MS = 5 * 60_000L
    private const val AUDIO_TTL_MS = 15 * 60_000L
    private const val LYRICS_TTL_MS = 24 * 60 * 60_000L
    private const val MEDIA_URL_TTL_MS = 2 * 60_000L

    private data class CacheEntry<T>(val value: T, val createdAt: Long = System.currentTimeMillis()) {
        fun isFresh(ttlMs: Long) = System.currentTimeMillis() - createdAt < ttlMs
    }
    private fun <T> lruCache(maxSize: Int) = object : LinkedHashMap<String, CacheEntry<T>>(maxSize + 1, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, CacheEntry<T>>?) = size > maxSize
    }
    private val spotifySearchCache = lruCache<List<NexraySpotifyTrack>>(30)
    private val spotifyAudioCache = lruCache<String>(64)
    private val lyricsCache = lruCache<NexrayLyrics>(40)
    private val youtubeSearchCache = lruCache<List<NexrayYoutubeVideo>>(30)
    private val youtubeMediaCache = lruCache<List<NexrayMediaFormat>>(20)

    private fun <T> cached(cache: MutableMap<String, CacheEntry<T>>, key: String, ttlMs: Long): T? =
        synchronized(cache) { cache[key]?.takeIf { it.isFresh(ttlMs) }?.value }
    private fun <T> cache(cache: MutableMap<String, CacheEntry<T>>, key: String, value: T) {
        synchronized(cache) { cache[key] = CacheEntry(value) }
    }
    private fun cacheKey(value: String) = value.trim().lowercase(Locale.ROOT)

    private const val SPOTIFY_SEARCH = "$BASE/search/spotify?q="
    private const val SPOTIFY_DOWNLOAD = "$BASE/downloader/spotify?url="
    private const val LYRICS_SEARCH = "$BASE/search/lyrics?q="
    private const val YOUTUBE_SEARCH = "$BASE/search/youtube?q="
    private const val YOUTUBE_DOWNLOAD = "$BASE/downloader/aio?url="

    suspend fun searchSpotify(query: String): List<NexraySpotifyTrack> = withContext(Dispatchers.IO) {
        val key = cacheKey(query)
        cached(spotifySearchCache, key, SEARCH_TTL_MS)?.let { return@withContext it }
        val root = getJson(SPOTIFY_SEARCH + encode(query.trim()))
        ensureSuccess(root)
        val array = root.optJSONArray("result") ?: root.optJSONArray("results") ?: JSONArray()
        val parsed = buildList {
            for (i in 0 until array.length()) {
                val item = array.optJSONObject(i) ?: continue
                val trackUrl = item.firstString("url", "spotifyUrl", "spotify_url")
                if (trackUrl.isBlank()) continue
                add(
                    NexraySpotifyTrack(
                        title = item.firstString("title", "name").ifBlank { "Lagu tanpa judul" },
                        artist = item.firstString("artist", "artists", "author").ifBlank { "Artis tidak diketahui" },
                        album = item.firstString("album"),
                        thumbnail = item.firstString("thumbnail", "image", "image_url", "cover"),
                        duration = item.firstString("duration"),
                        url = trackUrl,
                        releaseDate = item.firstString("release_date", "releaseDate"),
                    )
                )
            }
        }
        cache(spotifySearchCache, key, parsed)
        parsed
    }

    suspend fun resolveSpotifyAudio(spotifyUrl: String): String = withContext(Dispatchers.IO) {
        cached(spotifyAudioCache, spotifyUrl, AUDIO_TTL_MS)?.let { return@withContext it }
        val root = getJson(SPOTIFY_DOWNLOAD + encode(spotifyUrl))
        ensureSuccess(root)
        val result = root.optJSONObject("result") ?: root
        val audioUrl = result.firstString("url", "audio", "download", "streamUrl", "stream_url")
            .takeIf(::isHttpUrl)
            ?: throw IllegalStateException(root.optString("message").ifBlank { "Nexray tidak mengembalikan URL audio." })
        cache(spotifyAudioCache, spotifyUrl, audioUrl)
        audioUrl
    }

    suspend fun searchLyrics(title: String, artist: String): NexrayLyrics = withContext(Dispatchers.IO) {
        val query = listOf(title, artist).filter { it.isNotBlank() }.joinToString(" ")
        val key = cacheKey(query)
        cached(lyricsCache, key, LYRICS_TTL_MS)?.let { return@withContext it }
        val root = getJson(LYRICS_SEARCH + encode(query))
        ensureSuccess(root)
        val result = root.optJSONObject("result") ?: JSONObject()
        val lyrics = result.optJSONObject("lyrics") ?: result
        NexrayLyrics(
            synced = lyrics.firstString("synced_lyrics", "syncedLyrics", "synced"),
            plain = lyrics.firstString("plain_lyrics", "plainLyrics", "lyrics", "text"),
        ).also { cache(lyricsCache, key, it) }
    }

    suspend fun searchYoutube(query: String): List<NexrayYoutubeVideo> = withContext(Dispatchers.IO) {
        val key = cacheKey(query)
        cached(youtubeSearchCache, key, SEARCH_TTL_MS)?.let { return@withContext it }
        val root = getJson(YOUTUBE_SEARCH + encode(query.trim()))
        ensureSuccess(root)
        val array = root.optJSONArray("result") ?: root.optJSONArray("results") ?: JSONArray()
        val parsed = buildList {
            for (i in 0 until array.length()) {
                val item = array.optJSONObject(i) ?: continue
                if (item.optString("type").equals("channel", true)) continue
                val url = item.firstString("url", "link").ifBlank {
                    item.firstString("id", "videoId", "video_id").takeIf { it.isNotBlank() }
                        ?.let { "https://www.youtube.com/watch?v=$it" }.orEmpty()
                }
                if (url.isBlank()) continue
                add(
                    NexrayYoutubeVideo(
                        id = item.firstString("id", "videoId", "video_id"),
                        title = item.firstString("title", "name").ifBlank { "Video tanpa judul" },
                        channel = item.firstString("channel", "author", "uploader"),
                        thumbnail = item.firstString("image_url", "thumbnail", "image", "thumbnailUrl"),
                        duration = item.firstString("duration").ifBlank { item.optLong("seconds", 0L).takeIf { it > 0 }?.let(::formatDuration).orEmpty() },
                        url = url,
                        views = item.firstString("views", "view_count"),
                        uploadAt = item.firstString("upload_at", "uploadDate", "published"),
                    )
                )
            }
        }
        cache(youtubeSearchCache, key, parsed)
        parsed
    }

    suspend fun resolveYoutubeMedia(youtubeUrl: String): List<NexrayMediaFormat> = withContext(Dispatchers.IO) {
        cached(youtubeMediaCache, youtubeUrl, MEDIA_URL_TTL_MS)?.let { return@withContext it }
        val root = getJson(YOUTUBE_DOWNLOAD + encode(youtubeUrl))
        ensureSuccess(root)
        val result = root.optJSONObject("result") ?: root
        // Nexray returns "error": false for successful responses. The old check treated
        // any non-null value as an error, which prevented all quality options from showing.
        val errorValue = result.opt("error")
        if ((errorValue is Boolean && errorValue) ||
            (errorValue is String && errorValue.isNotBlank() && !errorValue.equals("false", true))) {
            throw IllegalStateException(result.optString("message").ifBlank { "Nexray gagal menyiapkan video." })
        }

        val medias = result.optJSONArray("medias") ?: result.optJSONArray("formats") ?: JSONArray()
        val parsed = buildList {
            for (i in 0 until medias.length()) {
                val item = medias.optJSONObject(i) ?: continue
                val url = item.firstString("url", "downloadUrl", "download_url")
                if (!isHttpUrl(url)) continue

                val mime = item.firstString("mimeType", "mime_type", "mime")
                val ext = item.firstString("ext", "extension")
                    .ifBlank { mime.substringAfterLast('/', "mp4").substringBefore(';') }
                    .lowercase(Locale.ROOT)
                val type = item.firstString("type", "mediaType").lowercase(Locale.ROOT)
                    .ifBlank { if (mime.startsWith("audio/", true)) "audio" else "video" }
                // We can save MP4 video tracks and M4A audio tracks. Other containers are
                // intentionally omitted because the offline library stores MP4 videos.
                if (type == "video" && ext != "mp4") continue
                if (type == "audio" && ext !in setOf("m4a", "mp4")) continue
                if (type !in setOf("video", "audio")) continue

                val qualityText = item.firstString("quality", "label")
                val height = item.optInt("height", 0).takeIf { it > 0 }
                    ?: Regex("(\\d{3,4})p", RegexOption.IGNORE_CASE).find(qualityText)?.groupValues?.get(1)?.toIntOrNull()
                val codecsHaveAudio = Regex("mp4a|aac", RegexOption.IGNORE_CASE).containsMatchIn(mime)
                val audioQuality = item.opt("audioQuality")
                val hasAudio = item.optBoolean("is_audio", false) ||
                    (audioQuality != null && audioQuality != JSONObject.NULL && audioQuality.toString().isNotBlank()) ||
                    codecsHaveAudio
                val size = item.optLong("fileSizeBytes", item.optLong("filesize", item.optLong("size", 0L))).takeIf { it > 0L }
                    ?: Regex("[?&]clen=(\\d+)").find(url)?.groupValues?.getOrNull(1)?.toLongOrNull()
                add(
                    NexrayMediaFormat(
                        label = item.firstString("label", "quality").ifBlank { "${height ?: "Auto"}p" },
                        quality = qualityText.ifBlank { "${height ?: "Auto"}p" },
                        mimeType = mime.ifBlank { if (type == "audio") "audio/mp4" else "video/mp4" },
                        directUrl = url,
                        fileSizeBytes = size,
                        height = height,
                        fps = item.optInt("fps", 0),
                        bitrate = item.optLong("bitrate", 0L),
                        hasAudio = hasAudio,
                        extension = ext,
                        formatId = item.firstString("formatId", "format_id", "itag"),
                        mediaType = type,
                    )
                )
            }
        }
        val videos = parsed.filter { it.mediaType == "video" }
            .sortedWith(compareBy<NexrayMediaFormat> { it.height ?: Int.MAX_VALUE }
                .thenByDescending { it.hasAudio }
                .thenByDescending { it.fps }
                .thenByDescending { it.bitrate })
            // Keep the muxed format when the API has one for the same resolution.
            .distinctBy { "${it.height ?: it.quality}:${it.fps}" }
        val audio = parsed.filter { it.mediaType == "audio" }
            .sortedWith(compareByDescending<NexrayMediaFormat> { it.bitrate })
        (videos + audio).also { cache(youtubeMediaCache, youtubeUrl, it) }
    }

    private fun getJson(url: String): JSONObject {
        val connection = URL(url).openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = 12_000
            connection.readTimeout = 25_000
            connection.requestMethod = "GET"
            connection.setRequestProperty("Accept", "application/json")
            connection.setRequestProperty("User-Agent", "HansAps/1.0 Android")
            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val body = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
            if (code !in 200..299) throw IllegalStateException("Nexray merespons HTTP $code${if (body.isBlank()) "" else ": ${body.take(180)}"}")
            return JSONObject(body)
        } finally {
            connection.disconnect()
        }
    }

    private fun ensureSuccess(root: JSONObject) {
        if (root.has("status") && root.opt("status") == false) {
            throw IllegalStateException(root.optString("message").ifBlank { "Permintaan Nexray gagal." })
        }
    }

    private fun JSONObject.firstString(vararg keys: String): String {
        for (key in keys) {
            val value = opt(key) ?: continue
            when (value) {
                is String -> if (value.isNotBlank() && value != "null") return value
                is Number -> return value.toString()
                is JSONArray -> {
                    val parts = buildList {
                        for (i in 0 until value.length()) {
                            val entry = value.opt(i)
                            when (entry) {
                                is String -> if (entry.isNotBlank()) add(entry)
                                is JSONObject -> entry.firstString("name", "title").takeIf { it.isNotBlank() }?.let(::add)
                            }
                        }
                    }
                    if (parts.isNotEmpty()) return parts.joinToString(", ")
                }
                is JSONObject -> value.firstString("name", "title").takeIf { it.isNotBlank() }?.let { return it }
            }
        }
        return ""
    }

    private fun encode(value: String): String = URLEncoder.encode(value, Charsets.UTF_8.name())
    private fun isHttpUrl(value: String): Boolean = value.startsWith("https://", true) || value.startsWith("http://", true)
    private fun formatDuration(totalSeconds: Long): String = "${totalSeconds / 60}:${(totalSeconds % 60).toString().padStart(2, '0')}"
}

data class NexraySpotifyTrack(
    val title: String,
    val artist: String,
    val album: String,
    val thumbnail: String,
    val duration: String,
    val url: String,
    val releaseDate: String = "",
)

data class NexrayLyrics(val synced: String = "", val plain: String = "") {
    val bestAvailable: String get() = synced.ifBlank { plain }
}

data class NexrayYoutubeVideo(
    val id: String,
    val title: String,
    val channel: String,
    val thumbnail: String,
    val duration: String,
    val url: String,
    val views: String = "",
    val uploadAt: String = "",
)

data class NexrayMediaFormat(
    val label: String,
    val quality: String,
    val mimeType: String,
    val directUrl: String,
    val fileSizeBytes: Long? = null,
    val height: Int? = null,
    val fps: Int = 0,
    val bitrate: Long = 0L,
    val hasAudio: Boolean = false,
    val extension: String = "mp4",
    val formatId: String = "",
    val mediaType: String = "video",
)
