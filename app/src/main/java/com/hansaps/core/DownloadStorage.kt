package com.hansaps.core

import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.provider.OpenableColumns
import org.json.JSONObject
import java.io.InputStream
import java.util.Locale

/**
 * Writes owned/imported media into the public Download/HansAps tree using MediaStore's scoped-storage API.
 * No MANAGE_EXTERNAL_STORAGE permission is required on supported Android versions.
 */
object DownloadStorage {
    const val ROOT = "Download/HansAps/"
    private const val SPOTIFY_PATH = "${ROOT}Spotify/"
    private const val YOUTUBE_PATH = "${ROOT}Youtube/"

    enum class Kind { AUDIO, VIDEO, IMAGE, DOCUMENT }

    fun relativeFolder(service: String, itemName: String): String {
        val safeService = if (service.equals("Youtube", true)) "Youtube" else "Spotify"
        return "${Environment.DIRECTORY_DOWNLOADS}/HansAps/$safeService/${slugify(itemName)}/"
    }

    fun slugify(value: String): String = value
        .lowercase(Locale.ROOT)
        .replace(Regex("[^a-z0-9]+"), "-")
        .trim('-')
        .take(72)
        .ifBlank { "media-${System.currentTimeMillis()}" }

    fun listLocalAudio(context: Context): List<LocalTrack> {
        val result = mutableListOf<LocalTrack>()
        val resolver = context.contentResolver
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.DURATION,
            MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.MediaColumns.RELATIVE_PATH,
        )
        val selection = "${MediaStore.MediaColumns.RELATIVE_PATH} LIKE ?"
        resolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            arrayOf("$SPOTIFY_PATH%"),
            "${MediaStore.MediaColumns.DATE_ADDED} DESC",
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val displayCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
            val pathCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.RELATIVE_PATH)
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                val display = cursor.getString(displayCol).orEmpty()
                val trackPath = cursor.getString(pathCol).orEmpty()
                if (!display.endsWith(".mp3", true) && !display.endsWith(".m4a", true) && !display.endsWith(".aac", true)) continue
                val contentUri = Uri.withAppendedPath(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id.toString())
                result += LocalTrack(
                    id = id.toString(),
                    title = cursor.getString(titleCol)?.takeIf { it.isNotBlank() } ?: display.substringBeforeLast('.'),
                    artist = cursor.getString(artistCol)?.takeIf { it.isNotBlank() && it != "<unknown>" } ?: "Koleksi offline",
                    uri = contentUri,
                    durationMs = cursor.getLong(durationCol),
                    displayName = display,
                    artworkUri = findThumbnail(context, trackPath),
                )
            }
        }
        return result
    }

    private fun findThumbnail(context: Context, relativePath: String): Uri? {
        if (relativePath.isBlank()) return null
        val projection = arrayOf(MediaStore.Images.Media._ID)
        return context.contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection,
            "${MediaStore.MediaColumns.RELATIVE_PATH} = ? AND ${MediaStore.MediaColumns.DISPLAY_NAME} = ?",
            arrayOf(relativePath, "thumbnail.jpg"),
            null,
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                Uri.withAppendedPath(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)).toString(),
                )
            } else null
        }
    }

    fun listLocalVideos(context: Context): List<LocalVideo> {
        val result = mutableListOf<LocalVideo>()
        val resolver = context.contentResolver
        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.TITLE,
            MediaStore.Video.Media.DURATION,
            MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.MediaColumns.RELATIVE_PATH,
        )
        resolver.query(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            projection,
            "${MediaStore.MediaColumns.RELATIVE_PATH} LIKE ?",
            arrayOf("$YOUTUBE_PATH%"),
            "${MediaStore.MediaColumns.DATE_ADDED} DESC",
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
            val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.TITLE)
            val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
            val displayCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                val display = cursor.getString(displayCol).orEmpty()
                if (!display.endsWith(".mp4", true) && !display.endsWith(".mkv", true) && !display.endsWith(".webm", true)) continue
                result += LocalVideo(
                    id = id.toString(),
                    title = cursor.getString(titleCol)?.takeIf { it.isNotBlank() } ?: display.substringBeforeLast('.'),
                    uri = Uri.withAppendedPath(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id.toString()),
                    durationMs = cursor.getLong(durationCol),
                    displayName = display,
                )
            }
        }
        return result
    }

    fun importAudio(context: Context, source: Uri): Result<LocalTrack> = runCatching {
        val metadata = readMediaMetadata(context, source, isVideo = false)
        val fallbackName = getDisplayName(context.contentResolver, source).substringBeforeLast('.')
        val title = metadata.title?.takeIf { it.isNotBlank() } ?: fallbackName
        val artist = metadata.artist?.takeIf { it.isNotBlank() } ?: "Imported"
        val folderName = "$artist-$title"
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, "music.mp3")
            put(MediaStore.MediaColumns.MIME_TYPE, "audio/mpeg")
            put(MediaStore.MediaColumns.RELATIVE_PATH, relativeFolder("Spotify", folderName))
            put(MediaStore.Audio.Media.TITLE, title)
            put(MediaStore.Audio.Media.ARTIST, artist)
            put(MediaStore.Audio.Media.DURATION, metadata.durationMs)
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }
        val outputUri = requireNotNull(context.contentResolver.insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, values)) {
            "Tidak bisa membuat file musik di Download/HansAps."
        }
        try {
            copyUri(context, source, outputUri)
            finishPending(context, outputUri)
        } catch (t: Throwable) {
            deletePending(context, outputUri)
            throw t
        }
        writeText(context, relativeFolder("Spotify", folderName), "meta.json", "application/json", JSONObject()
            .put("title", title).put("artist", artist).put("durationMs", metadata.durationMs)
            .put("source", "imported-local").toString(2))
        LocalTrack(outputUri.lastPathSegment ?: title, title, artist, outputUri, metadata.durationMs, "music.mp3")
    }

    fun importVideo(context: Context, source: Uri, titleInput: String? = null): Result<LocalVideo> = runCatching {
        val metadata = readMediaMetadata(context, source, isVideo = true)
        val fallbackName = getDisplayName(context.contentResolver, source).substringBeforeLast('.')
        val title = titleInput?.takeIf { it.isNotBlank() } ?: metadata.title?.takeIf { it.isNotBlank() } ?: fallbackName
        val folder = relativeFolder("Youtube", title)
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, "video.mp4")
            put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
            put(MediaStore.MediaColumns.RELATIVE_PATH, folder)
            put(MediaStore.Video.Media.TITLE, title)
            put(MediaStore.Video.Media.DURATION, metadata.durationMs)
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }
        val outputUri = requireNotNull(context.contentResolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values)) {
            "Tidak bisa membuat file video di Download/HansAps."
        }
        try {
            copyUri(context, source, outputUri)
            finishPending(context, outputUri)
        } catch (t: Throwable) {
            deletePending(context, outputUri)
            throw t
        }
        writeText(context, folder, "meta.json", "application/json", JSONObject()
            .put("title", title).put("durationMs", metadata.durationMs).put("source", "imported-local").toString(2))
        LocalVideo(outputUri.lastPathSegment ?: title, title, outputUri, metadata.durationMs, "video.mp4")
    }

    fun createPendingFile(
        context: Context,
        kind: Kind,
        relativePath: String,
        displayName: String,
        mimeType: String,
        extraValues: ContentValues? = null,
    ): Uri {
        val collection = when (kind) {
            Kind.AUDIO -> MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
            Kind.VIDEO -> MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            Kind.IMAGE -> MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            Kind.DOCUMENT -> MediaStore.Downloads.EXTERNAL_CONTENT_URI
        }
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
            put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
            put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath)
            put(MediaStore.MediaColumns.IS_PENDING, 1)
            extraValues?.let { putAll(it) }
        }
        return requireNotNull(context.contentResolver.insert(collection, values)) { "Gagal membuat file $displayName." }
    }

    fun finishPending(context: Context, uri: Uri) {
        val values = ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) }
        context.contentResolver.update(uri, values, null, null)
    }

    fun deletePending(context: Context, uri: Uri) {
        runCatching { context.contentResolver.delete(uri, null, null) }
    }

    fun copyUri(context: Context, source: Uri, destination: Uri) {
        val input = requireNotNull(context.contentResolver.openInputStream(source)) { "File sumber tidak dapat dibaca." }
        input.use { copyStream(context, destination, it) }
    }

    fun copyStream(context: Context, destination: Uri, input: InputStream) {
        val output = requireNotNull(context.contentResolver.openOutputStream(destination, "w")) { "Tujuan penyimpanan tidak dapat dibuka." }
        output.use { out -> input.copyTo(out, 128 * 1024) }
    }

    fun writeText(context: Context, relativePath: String, displayName: String, mimeType: String, text: String): Uri {
        val uri = createPendingFile(context, Kind.DOCUMENT, relativePath, displayName, mimeType)
        try {
            context.contentResolver.openOutputStream(uri, "w")!!.bufferedWriter(Charsets.UTF_8).use { it.write(text) }
            finishPending(context, uri)
        } catch (t: Throwable) {
            deletePending(context, uri)
            throw t
        }
        return uri
    }

    fun getDisplayName(resolver: ContentResolver, uri: Uri): String {
        resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index >= 0) cursor.getString(index)?.let { return it }
            }
        }
        return uri.lastPathSegment ?: "media"
    }

    private data class ReadMetadata(val title: String?, val artist: String?, val durationMs: Long)

    private fun readMediaMetadata(context: Context, uri: Uri, isVideo: Boolean): ReadMetadata {
        return runCatching {
            MediaMetadataRetriever().use { retriever ->
                retriever.setDataSource(context, uri)
                ReadMetadata(
                    title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE),
                    artist = if (isVideo) null else retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST),
                    durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L,
                )
            }
        }.getOrDefault(ReadMetadata(null, null, 0L))
    }
}
