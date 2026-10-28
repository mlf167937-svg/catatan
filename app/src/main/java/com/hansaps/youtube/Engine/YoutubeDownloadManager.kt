package com.hansaps.youtube.Engine

import android.content.ContentValues
import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import android.provider.MediaStore
import com.hansaps.core.DownloadStorage
import com.hansaps.core.LocalVideo
import com.hansaps.core.NexrayApi
import com.hansaps.core.NexrayMediaFormat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.nio.ByteBuffer
import java.util.Locale

/** A selectable MP4 rendition returned by Nexray's /downloader/aio endpoint. */
data class YoutubeDownloadOption(
    val label: String,
    val quality: String,
    val mimeType: String,
    val directUrl: String,
    val fileSizeBytes: Long? = null,
    val hasAudio: Boolean = false,
    val extension: String = "mp4",
    val formatId: String = "",
    /** M4A audio URL returned alongside video-only renditions. */
    val audioUrl: String? = null,
)

data class AuthorizedVideoSource(
    val title: String,
    val videoId: String,
    val directUrl: String,
    val quality: String,
    val thumbnailUrl: String? = null,
    val sourceLabel: String = "Nexray API",
    /** Used when the chosen MP4 rendition contains video only. */
    val audioUrl: String? = null,
)

object YoutubeDownloadManager {
    fun toOptions(formats: List<NexrayMediaFormat>, audioUrl: String? = null): List<YoutubeDownloadOption> =
        formats.filter { it.mediaType == "video" && it.extension.equals("mp4", true) }.map { format ->
            YoutubeDownloadOption(
                label = format.label,
                quality = format.quality,
                mimeType = format.mimeType,
                directUrl = format.directUrl,
                fileSizeBytes = format.fileSizeBytes,
                hasAudio = format.hasAudio,
                extension = format.extension,
                formatId = format.formatId,
                audioUrl = audioUrl,
            )
        }

    /** Compatibility parser for a body from /downloader/aio?url=...; no "resolution" field is expected. */
    fun parseOptionsFromApiResponse(responseJson: String): List<YoutubeDownloadOption> {
        val root = JSONObject(responseJson)
        val result = root.optJSONObject("result") ?: root
        val err = result.opt("error")
        require(!(err is Boolean && err) && !(err is String && err.isNotBlank() && !err.equals("false", true))) {
            result.optString("message").ifBlank { "Nexray gagal menyiapkan video." }
        }
        val medias = result.optJSONArray("medias") ?: result.optJSONArray("formats") ?: JSONArray()
        val audioUrl = (0 until medias.length()).asSequence()
            .mapNotNull { medias.optJSONObject(it) }
            .firstOrNull {
                it.optString("type").equals("audio", true) &&
                    it.optString("ext", it.optString("extension")).equals("m4a", true) &&
                    it.optString("url").startsWith("https://", true)
            }?.optString("url")
        val formats = (0 until medias.length()).mapNotNull { index ->
            val item = medias.optJSONObject(index) ?: return@mapNotNull null
            val type = item.optString("type", "video").lowercase(Locale.ROOT)
            val ext = item.optString("ext", item.optString("extension", "mp4"))
            val url = item.optString("url")
            if (type != "video" || !ext.equals("mp4", true) || !url.startsWith("https://", true)) return@mapNotNull null
            val mime = item.optString("mimeType", item.optString("mime_type", "video/mp4"))
            val aq = item.opt("audioQuality")
            val hasAudio = item.optBoolean("is_audio", false) ||
                (aq != null && aq != JSONObject.NULL && aq.toString().isNotBlank()) ||
                Regex("mp4a|aac", RegexOption.IGNORE_CASE).containsMatchIn(mime)
            val height = item.optInt("height", 0).takeIf { it > 0 }
                ?: Regex("(\\d{3,4})p", RegexOption.IGNORE_CASE).find(item.optString("label") + " " + item.optString("quality"))?.groupValues?.get(1)?.toIntOrNull()
            YoutubeDownloadOption(
                label = item.optString("label").ifBlank { "${height ?: "Auto"}p" },
                quality = item.optString("quality").ifBlank { "${height ?: "Auto"}p" },
                mimeType = mime,
                directUrl = url,
                fileSizeBytes = item.optLong("fileSizeBytes", item.optLong("filesize", 0L)).takeIf { it > 0L },
                hasAudio = hasAudio,
                extension = ext,
                formatId = item.optString("formatId"),
                audioUrl = audioUrl,
            )
        }
        return formats.sortedWith(compareBy<YoutubeDownloadOption> { Regex("(\\d{3,4})p", RegexOption.IGNORE_CASE).find(it.quality)?.groupValues?.get(1)?.toIntOrNull() ?: Int.MAX_VALUE }
            .thenByDescending { it.hasAudio }.thenBy { it.quality }).distinctBy { it.quality }
    }

    suspend fun download(context: Context, source: AuthorizedVideoSource): Result<LocalVideo> = withContext(Dispatchers.IO) {
        runCatching {
            require(source.directUrl.startsWith("https://", true)) {
                "Sumber video harus berupa URL HTTPS langsung dari Nexray."
            }
            if (source.audioUrl.isNullOrBlank() && source.quality.isBlank()) {
                throw IOException("Info kualitas video tidak valid.")
            }

            val workDir = File(context.cacheDir, "hansaps-youtube-tmp").apply { mkdirs() }
            val token = "${System.currentTimeMillis()}-${source.videoId.hashCode().toUInt()}"
            val rawVideo = File(workDir, "$token-video.mp4.part")
            val rawAudio = File(workDir, "$token-audio.m4a.part")
            val muxedVideo = File(workDir, "$token-merged.mp4.part")
            var outputFile = rawVideo
            try {
                downloadToFile(source.directUrl, rawVideo)
                if (!source.audioUrl.isNullOrBlank()) {
                    downloadToFile(source.audioUrl, rawAudio)
                    muxMp4VideoAndM4aAudio(rawVideo, rawAudio, muxedVideo)
                    outputFile = muxedVideo
                }

                val relativePath = DownloadStorage.relativeFolder("Youtube", source.title)
                val values = ContentValues().apply { put(MediaStore.Video.Media.TITLE, source.title) }
                val videoUri = DownloadStorage.createPendingFile(
                    context, DownloadStorage.Kind.VIDEO, relativePath, "video.mp4", "video/mp4", values,
                )
                try {
                    outputFile.inputStream().use { input -> DownloadStorage.copyStream(context, videoUri, input) }
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
                        .put("hasAudio", source.audioUrl != null || source.quality.isNotBlank())
                        .put("thumbnail", if (source.thumbnailUrl != null) "thumbnail.jpg" else JSONObject.NULL)
                        .toString(2),
                )

                source.thumbnailUrl?.takeIf { it.startsWith("https://", true) }?.let { thumbnailUrl ->
                    runCatching {
                        val thumbnail = DownloadStorage.createPendingFile(
                            context, DownloadStorage.Kind.IMAGE, relativePath, "thumbnail.jpg", "image/jpeg",
                        )
                        try {
                            downloadToPendingUri(context, thumbnailUrl, thumbnail)
                            DownloadStorage.finishPending(context, thumbnail)
                        } catch (t: Throwable) {
                            DownloadStorage.deletePending(context, thumbnail)
                            throw t
                        }
                    }
                }
                LocalVideo(videoUri.lastPathSegment ?: source.videoId, source.title, videoUri, 0L, "video.mp4")
            } finally {
                rawVideo.delete()
                rawAudio.delete()
                muxedVideo.delete()
            }
        }
    }

    private fun downloadToPendingUri(context: Context, url: String, uri: android.net.Uri) {
        val conn = openDownloadConnection(url)
        try {
            conn.inputStream.use { input -> DownloadStorage.copyStream(context, uri, input) }
        } finally { conn.disconnect() }
    }

    private fun downloadToFile(url: String, destination: File) {
        destination.parentFile?.mkdirs()
        val conn = openDownloadConnection(url)
        try {
            conn.inputStream.use { input -> destination.outputStream().use { output -> input.copyTo(output, 128 * 1024) } }
        } finally { conn.disconnect() }
        if (destination.length() <= 0L) throw IOException("Nexray mengirim file kosong.")
    }

    private fun openDownloadConnection(url: String): HttpURLConnection {
        require(url.startsWith("https://", true)) { "URL unduhan bukan HTTPS." }
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.connectTimeout = 20_000
        connection.readTimeout = 90_000
        connection.instanceFollowRedirects = true
        connection.setRequestProperty("User-Agent", "HansAps/1.1 Android")
        connection.setRequestProperty("Accept", "*/*")
        val code = connection.responseCode
        val contentType = connection.contentType.orEmpty().lowercase(Locale.ROOT)
        if (code !in 200..299 || contentType.contains("application/json") || contentType.startsWith("text/html")) {
            val diagnostic = runCatching { connection.errorStream?.bufferedReader()?.use { it.readText().take(180) } }.getOrNull().orEmpty()
            connection.disconnect()
            throw IOException("Sumber media tidak bisa diunduh (HTTP $code). ${diagnostic.takeIf { it.isNotBlank() }.orEmpty()}")
        }
        return connection
    }

    /** Remuxes a video-only MP4 with the M4A audio track, without re-encoding. */
    private fun muxMp4VideoAndM4aAudio(videoFile: File, audioFile: File, outputFile: File) {
        val videoExtractor = MediaExtractor()
        val audioExtractor = MediaExtractor()
        var muxer: MediaMuxer? = null
        var started = false
        try {
            videoExtractor.setDataSource(videoFile.absolutePath)
            audioExtractor.setDataSource(audioFile.absolutePath)
            val videoIndex = findTrack(videoExtractor, "video/")
                ?: throw IOException("File video Nexray tidak memiliki track video.")
            val audioIndex = findTrack(audioExtractor, "audio/")
                ?: throw IOException("File audio M4A Nexray tidak memiliki track audio.")
            videoExtractor.selectTrack(videoIndex)
            audioExtractor.selectTrack(audioIndex)
            val videoFormat = videoExtractor.getTrackFormat(videoIndex)
            val audioFormat = audioExtractor.getTrackFormat(audioIndex)
            outputFile.delete()
            muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            val outputVideoTrack = muxer.addTrack(videoFormat)
            val outputAudioTrack = muxer.addTrack(audioFormat)
            muxer.start()
            started = true

            val videoBuffer = ByteBuffer.allocateDirect(bufferSize(videoFormat))
            val audioBuffer = ByteBuffer.allocateDirect(bufferSize(audioFormat))
            val videoInfo = MediaCodec.BufferInfo()
            val audioInfo = MediaCodec.BufferInfo()
            var videoDone = false
            var audioDone = false
            while (!videoDone || !audioDone) {
                val vTime = if (videoDone) -1L else videoExtractor.sampleTime
                val aTime = if (audioDone) -1L else audioExtractor.sampleTime
                val useVideo = !videoDone && (audioDone || vTime >= 0L && (aTime < 0L || vTime <= aTime))
                if (useVideo) {
                    videoBuffer.clear()
                    val size = videoExtractor.readSampleData(videoBuffer, 0)
                    if (size < 0) videoDone = true else {
                        videoInfo.set(0, size, videoExtractor.sampleTime, videoExtractor.sampleFlags)
                        videoBuffer.position(0); videoBuffer.limit(size)
                        muxer.writeSampleData(outputVideoTrack, videoBuffer, videoInfo)
                        videoExtractor.advance()
                        if (videoExtractor.sampleTime < 0L) videoDone = true
                    }
                } else if (!audioDone) {
                    audioBuffer.clear()
                    val size = audioExtractor.readSampleData(audioBuffer, 0)
                    if (size < 0) audioDone = true else {
                        audioInfo.set(0, size, audioExtractor.sampleTime, audioExtractor.sampleFlags)
                        audioBuffer.position(0); audioBuffer.limit(size)
                        muxer.writeSampleData(outputAudioTrack, audioBuffer, audioInfo)
                        audioExtractor.advance()
                        if (audioExtractor.sampleTime < 0L) audioDone = true
                    }
                }
            }
            muxer.stop()
            started = false
            if (outputFile.length() <= 0L) throw IOException("Penggabungan audio-video gagal menghasilkan MP4.")
        } finally {
            if (started) runCatching { muxer?.stop() }
            runCatching { muxer?.release() }
            videoExtractor.release()
            audioExtractor.release()
        }
    }

    private fun findTrack(extractor: MediaExtractor, prefix: String): Int? {
        for (i in 0 until extractor.trackCount) {
            val mime = runCatching { extractor.getTrackFormat(i).getString(MediaFormat.KEY_MIME) }.getOrNull().orEmpty()
            if (mime.startsWith(prefix, true)) return i
        }
        return null
    }

    private fun bufferSize(format: MediaFormat): Int {
        val requested = runCatching { format.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE) }.getOrNull() ?: 4 * 1024 * 1024
        return requested.coerceIn(512 * 1024, 16 * 1024 * 1024)
    }
}
