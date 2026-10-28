package com.hansaps.youtube.Engine

import android.content.ContentValues
import android.content.Context
import android.provider.MediaStore
import com.hansaps.core.DownloadStorage
import com.hansaps.core.LocalVideo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL

/** A provider may expose only media it is authorized to distribute/download. */
data class YoutubeDownloadOption(
    val label: String,
    val quality: String,
    val mimeType: String,
    val directUrl: String,
    val fileSizeBytes: Long? = null,
    val license: String = "",
)

data class AuthorizedVideoSource(
    val title: String,
    val videoId: String,
    val directUrl: String,
    val quality: String,
    val thumbnailUrl: String? = null,
    val sourceLabel: String = "authorized-provider",
)

/**
 * Adapter contract for an authorized API response. Items are accepted only if the provider marks
 * them downloadable and labels the license/ownership as authorized or owned. This does not scrape
 * YouTube pages, resolve streaming manifests, or bypass DRM/restrictions.
 */
object YoutubeDownloadManager {
    fun parseOptionsFromApiResponse(responseJson: String): List<YoutubeDownloadOption> {
        val root = JSONObject(responseJson)
        val formats = root.optJSONArray("formats") ?: return emptyList()
        return buildList {
            for (index in 0 until formats.length()) {
                val item = formats.optJSONObject(index) ?: continue
                val downloadable = item.optBoolean("downloadable", false)
                val license = item.optString("license", "").lowercase()
                val url = item.optString("url", "")
                if (!downloadable || license !in setOf("authorized", "owned") || !url.startsWith("https://", true)) continue
                add(
                    YoutubeDownloadOption(
                        label = item.optString("label", item.optString("quality", "Video")),
                        quality = item.optString("quality", item.optString("label", "Unknown")),
                        mimeType = item.optString("mimeType", "video/mp4"),
                        directUrl = url,
                        fileSizeBytes = item.optLong("fileSizeBytes").takeIf { it > 0L },
                        license = license,
                    )
                )
            }
        }
    }

    suspend fun download(context: Context, source: AuthorizedVideoSource): Result<LocalVideo> = withContext(Dispatchers.IO) {
        runCatching {
            require(source.directUrl.startsWith("https://", true)) {
                "Sumber video harus berupa URL HTTPS langsung dari provider yang berwenang."
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
