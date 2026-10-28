package com.hansaps.spotify.Engine

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import com.hansaps.core.DownloadStorage
import com.hansaps.core.LocalTrack
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL

/** Metadata supplied by a provider that is explicitly allowed to distribute the audio file. */
data class AuthorizedAudioSource(
    val title: String,
    val artist: String,
    val directAudioUrl: String,
    val thumbnailUrl: String? = null,
    val lyricsText: String? = null,
    val lyricsSynced: String? = null,
    val lyricsPlain: String? = null,
    val album: String = "",
    val duration: String = "",
    val releaseDate: String = "",
    val sourceLabel: String = "Nexray API",
)

/**
 * Downloads a direct, HTTPS audio asset supplied by an authorized provider.
 * This class intentionally does not resolve Spotify pages, scrape streams, or bypass DRM.
 */
class DownloaderMusic {
    suspend fun download(context: Context, source: AuthorizedAudioSource): Result<LocalTrack> = withContext(Dispatchers.IO) {
        runCatching {
            require(source.directAudioUrl.startsWith("https://", ignoreCase = true)) {
                "Sumber audio harus berupa URL HTTPS langsung dari provider yang berwenang."
            }
            val folderName = "${source.artist}-${source.title}"
            val relativePath = DownloadStorage.relativeFolder("Spotify", folderName)
            val audioValues = ContentValues().apply {
                put(MediaStore.Audio.Media.TITLE, source.title)
                put(MediaStore.Audio.Media.ARTIST, source.artist)
            }
            val audioUri = DownloadStorage.createPendingFile(
                context,
                DownloadStorage.Kind.AUDIO,
                relativePath,
                "music.mp3",
                "audio/mpeg",
                audioValues,
            )
            try {
                URL(source.directAudioUrl).openConnection().apply {
                    connectTimeout = 15_000
                    readTimeout = 45_000
                    setRequestProperty("User-Agent", "HansAps/1.0 Android")
                }.getInputStream().use { input -> DownloadStorage.copyStream(context, audioUri, input) }
                DownloadStorage.finishPending(context, audioUri)
            } catch (t: Throwable) {
                DownloadStorage.deletePending(context, audioUri)
                throw t
            }

            val meta = JSONObject()
                .put("title", source.title)
                .put("artist", source.artist)
                .put("album", source.album)
                .put("duration", source.duration)
                .put("releaseDate", source.releaseDate)
                .put("source", source.sourceLabel)
                .put("file", "music.mp3")
                .put("thumbnail", if (source.thumbnailUrl != null) "thumbnail.jpg" else JSONObject.NULL)
            DownloadStorage.writeText(context, relativePath, "meta.json", "application/json", meta.toString(2))
            DownloadStorage.writeText(
                context,
                relativePath,
                "lyrics.json",
                "application/json",
                JSONObject()
                    .put("lyrics", source.lyricsText ?: "")
                    .put("syncedLyrics", source.lyricsSynced ?: "")
                    .put("plainLyrics", source.lyricsPlain ?: "")
                    .put("source", source.sourceLabel)
                    .toString(2),
            )

            source.thumbnailUrl?.takeIf { it.startsWith("https://", true) }?.let { coverUrl ->
                runCatching {
                    val thumbUri = DownloadStorage.createPendingFile(
                        context, DownloadStorage.Kind.IMAGE, relativePath, "thumbnail.jpg", "image/jpeg",
                    )
                    try {
                        URL(coverUrl).openConnection().apply {
                            connectTimeout = 10_000
                            readTimeout = 20_000
                        }.getInputStream().use { input -> DownloadStorage.copyStream(context, thumbUri, input) }
                        DownloadStorage.finishPending(context, thumbUri)
                    } catch (t: Throwable) {
                        DownloadStorage.deletePending(context, thumbUri)
                        throw t
                    }
                }
            }
            LocalTrack(
                id = audioUri.lastPathSegment ?: source.title,
                title = source.title,
                artist = source.artist,
                uri = audioUri,
                displayName = "music.mp3",
            )
        }
    }
}
