package com.hansaps.core

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import org.json.JSONObject
import java.io.InputStream
import java.util.Locale

/**
 * Writes downloaded media into the public Download/HansAps tree using MediaStore's scoped-storage API.
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

}
