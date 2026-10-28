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
    private const val SPOTIFY_SEARCH = "$BASE/search/spotify?q="
    private const val SPOTIFY_DOWNLOAD = "$BASE/downloader/spotify?url="
    private const val LYRICS_SEARCH = "$BASE/search/lyrics?q="
    private const val YOUTUBE_SEARCH = "$BASE/search/youtube?q="
    private const val YOUTUBE_DOWNLOAD = "$BASE/downloader/aio?url="

    suspend fun searchSpotify(query: String): List<NexraySpotifyTrack> = withContext(Dispatchers.IO) {
        val root = getJson(SPOTIFY_SEARCH + encode(query.trim()))
        ensureSuccess(root)
        val array = root.optJSONArray("result") ?: root.optJSONArray("results") ?: JSONArray()
        buildList {
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
    }

    suspend fun resolveSpotifyAudio(spotifyUrl: String): String = withContext(Dispatchers.IO) {
        val root = getJson(SPOTIFY_DOWNLOAD + encode(spotifyUrl))
        ensureSuccess(root)
        val result = root.optJSONObject("result") ?: root
        result.firstString("url", "audio", "download", "streamUrl", "stream_url")
            .takeIf(::isHttpUrl)
            ?: throw IllegalStateException(root.optString("message").ifBlank { "Nexray tidak mengembalikan URL audio." })
    }

    suspend fun searchLyrics(title: String, artist: String): NexrayLyrics = withContext(Dispatchers.IO) {
        val query = listOf(title, artist).filter { it.isNotBlank() }.joinToString(" ")
        val root = getJson(LYRICS_SEARCH + encode(query))
        ensureSuccess(root)
        val result = root.optJSONObject("result") ?: JSONObject()
        val lyrics = result.optJSONObject("lyrics") ?: result
        NexrayLyrics(
            synced = lyrics.firstString("synced_lyrics", "syncedLyrics", "synced"),
            plain = lyrics.firstString("plain_lyrics", "plainLyrics", "lyrics", "text"),
        )
    }

    suspend fun searchYoutube(query: String): List<NexrayYoutubeVideo> = withContext(Dispatchers.IO) {
        val root = getJson(YOUTUBE_SEARCH + encode(query.trim()))
        ensureSuccess(root)
        val array = root.optJSONArray("result") ?: root.optJSONArray("results") ?: JSONArray()
        buildList {
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
    }

    suspend fun resolveYoutubeMedia(youtubeUrl: String): List<NexrayMediaFormat> = withContext(Dispatchers.IO) {
        val root = getJson(YOUTUBE_DOWNLOAD + encode(youtubeUrl))
        ensureSuccess(root)
        val result = root.optJSONObject("result") ?: root
        if (result.has("error") && !result.isNull("error")) {
            throw IllegalStateException(result.optString("error").ifBlank { "Nexray gagal menyiapkan video." })
        }
        val medias = result.optJSONArray("medias") ?: result.optJSONArray("formats") ?: JSONArray()
        buildList {
            for (i in 0 until medias.length()) {
                val item = medias.optJSONObject(i) ?: continue
                val url = item.firstString("url", "downloadUrl", "download_url")
                if (!isHttpUrl(url)) continue
                val type = item.firstString("type", "mediaType").lowercase(Locale.ROOT)
                val mime = item.firstString("mimeType", "mime_type", "mime")
                val ext = item.firstString("ext", "extension").ifBlank { mime.substringAfterLast('/', "mp4").substringBefore(';') }
                val height = item.optInt("height", 0).takeIf { it > 0 }
                    ?: Regex("(\\d{3,4})p", RegexOption.IGNORE_CASE).find(item.firstString("quality", "label"))?.groupValues?.get(1)?.toIntOrNull()
                val hasAudio = item.optBoolean("is_audio", false) || (!item.isNull("audioQuality") && item.has("audioQuality")) ||
                    Regex("mp4a|aac|opus|vorbis", RegexOption.IGNORE_CASE).containsMatchIn(mime)
                // This app stores videos as video.mp4, so exclude non-MP4 containers rather than mislabel them.
                if (type.isNotBlank() && type != "video") continue
                if (!ext.equals("mp4", true) || type == "audio") continue
                add(
                    NexrayMediaFormat(
                        label = item.firstString("label", "quality").ifBlank { "${height ?: "Auto"}p" },
                        quality = item.firstString("quality", "label").ifBlank { "${height ?: "Auto"}p" },
                        mimeType = mime.ifBlank { "video/${ext.ifBlank { "mp4" }}" },
                        directUrl = url,
                        fileSizeBytes = item.optLong("fileSizeBytes", item.optLong("size", 0L)).takeIf { it > 0L },
                        height = height,
                        fps = item.optInt("fps", 0),
                        bitrate = item.optLong("bitrate", 0L),
                        hasAudio = hasAudio,
                        extension = ext,
                    )
                )
            }
        }.sortedWith(compareBy<NexrayMediaFormat> { it.height ?: Int.MAX_VALUE }.thenByDescending { it.hasAudio }.thenByDescending { it.bitrate })
            .distinctBy { it.height ?: it.quality }
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
)
