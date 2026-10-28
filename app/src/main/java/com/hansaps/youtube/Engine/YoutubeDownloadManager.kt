package com.hansaps.youtube.Engine

import android.content.ContentValues
import android.content.Context
import android.provider.MediaStore
import com.hansaps.core.DownloadStorage
import com.hansaps.core.LocalVideo
import com.hansaps.core.NexrayApi
import com.hansaps.core.NexrayMediaFormat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.URL

/** A quality option returned by Nexray's /downloader/aio endpoint. */
data class YoutubeDownloadOption(
    val label: String,
    val quality: String,
    val mimeType: String,
    val directUrl: String,
    val fileSizeBytes: Long? = null,
    val hasAudio: Boolean = false,
    val extension: String = "mp4",
)

data class AuthorizedVideoSource(
    val title: String,
    val videoId: String,
    val directUrl: String,
    val quality: String,
    val thumbnailUrl: String? = null,
    val sourceLabel: String = "Nexray API",
)

object YoutubeDownloadManager {
    fun toOptions(formats: List<NexrayMediaFormat>): List<YoutubeDownloadOption> = formats.map { format ->
        YoutubeDownloadOption(
            label = format.label,
            quality = format.quality,
            mimeType = format.mimeType,
            directUrl = format.directUrl,
            fileSizeBytes = format.fileSizeBytes,
            hasAudio = format.hasAudio,
            extension = format.extension,
        )
    }

    /** Compatibility parser for a response body from /downloader/aio?url=... */
    fun parseOptionsFromApiResponse(responseJson: String): List<YoutubeDownloadOption> {
        val root = JSONObject(responseJson)
        val result = root.optJSONObject("result") ?: root
        val formats = result.optJSONArray("medias") ?: result.optJSONArray("formats") ?: JSONArray()
        val parsed = buildList {
            for (index in 0 until formats.length()) {
                val item = formats.optJSONObject(index) ?: continue
                val type = item.optString("type", "video")
                val url = item.optString("url", "")
                if (!type.equals("video", true) || !url.startsWith("https://", true)) continue
                val mime = item.optString("mimeType", item.optString("mime_type", "video/mp4"))
                val hasAudio = item.optBoolean("is_audio", false) ||
                    (!item.isNull("audioQuality") && item.has("audioQuality")) ||
                    Regex("mp4a|aac|opus|vorbis", RegexOption.IGNORE_CASE).containsMatchIn(mime)
                val height = item.optInt("height", 0).takeIf { it > 0 }
                    ?: Regex("(\\d{3,4})p", RegexOption.IGNORE_CASE).find(item.optString("quality", item.optString("label")))?.groupValues?.get(1)?.toIntOrNull()
                add(
                    YoutubeDownloadOption(
                        label = item.optString("label").ifBlank { "${height ?: "Auto"}p" },
                        quality = item.optString("quality").ifBlank { "${height ?: "Auto"}p" },
                        mimeType = mime,
                        directUrl = url,
                        fileSizeBytes = item.optLong("fileSizeBytes", item.optLong("size", 0L)).takeIf { it > 0L },
                        hasAudio = hasAudio,
                        extension = item.optString("ext", "mp4"),
                    )
                )
            }
        }
        return parsed.distinctBy { it.quality }.sortedBy { Regex("\\d+").find(it.quality)?.value?.toIntOrNull() ?: Int.MAX_VALUE }
    }

    suspend fun download(context: Context, source: AuthorizedVideoSource): Result<LocalVideo> = withContext(Dispatchers.IO) {
        runCatching {
            require(source.directUrl.startsWith("https://", true)) {
                "Sumber video harus berupa URL HTTPS langsung dari Nexray."
            }
            val relativePath = DownloadStorage.relativeFolder("Youtube", source.title)
            val values = ContentValues().apply { put(MediaStore.Video.Media.TITLE, source.title) }
            val videoUri = DownloadStorage.createPendingFile(
                context, DownloadStorage.Kind.VIDEO, relativePath, "video.mp4", "video/mp4", values,
            )
            try {
                URL(source.directUrl).openConnection().apply {
                    connectTimeout = 15_000
                    readTimeout = 60_000
                    setRequestProperty("User-Agent", "HansAps/1.0 Android")
                }.getInputStream().use { input -> DownloadStorage.copyStream(context, videoUri, input) }
                DownloadStorage.finishPending(context, videoUri)
            } catch (t: Throwable) {
                DownloadStorage.deletePending(context, videoUri)
                throw t
            }

            DownloadStorage.writeText(
                context,
                relativePath,
                "meta.json",
                "application/json",
                JSONObject()
                    .put("title", source.title)
                    .put("videoId", source.videoId)
                    .put("quality", source.quality)
                    .put("source", source.sourceLabel)
                    .put("file", "video.mp4")
                    .put("thumbnail", if (source.thumbnailUrl != null) "thumbnail.jpg" else JSONObject.NULL)
                    .toString(2),
            )

            source.thumbnailUrl?.takeIf { it.startsWith("https://", true) }?.let { thumbnailUrl ->
                runCatching {
                    val thumbnail = DownloadStorage.createPendingFile(
                        context, DownloadStorage.Kind.IMAGE, relativePath, "thumbnail.jpg", "image/jpeg",
                    )
                    try {
                        URL(thumbnailUrl).openConnection().apply {
                            connectTimeout = 10_000
                            readTimeout = 20_000
                        }.getInputStream().use { input -> DownloadStorage.copyStream(context, thumbnail, input) }
                        DownloadStorage.finishPending(context, thumbnail)
                    } catch (t: Throwable) {
                        DownloadStorage.deletePending(context, thumbnail)
                        throw t
                    }
                }
            }
            LocalVideo(videoUri.lastPathSegment ?: source.videoId, source.title, videoUri, 0L, "video.mp4")
        }
    }
}
