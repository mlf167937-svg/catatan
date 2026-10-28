package com.hansaps.spotify.rexmusic

import android.Manifest
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import androidx.core.content.ContextCompat
import java.io.File
import java.net.HttpURLConnection
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.withContext
import org.json.JSONObject

private val KEY_CLEAN = Regex("[^\\p{L}\\p{N}]+")

/** Nama folder lagu: "Iqro" + "Raim Laode" -> "Iqro-Raim-Laode". */
fun offlineKey(track: RexTrack): String {
    val raw = KEY_CLEAN.replace("${track.title} ${track.artist}", "-").trim('-')
    return raw.take(100).trim('-').ifBlank { "Lagu" }
}

/** Key folder lagu ini kalau sudah diunduh (cocok lewat nama folder, atau lewat id). */
fun OfflineState.keyFor(track: RexTrack, key: String = offlineKey(track)): String? =
    if (key in keys) key else entries.firstOrNull { it.track.id == track.id }?.key

fun OfflineState.isDownloaded(track: RexTrack, key: String = offlineKey(track)): Boolean =
    keyFor(track, key) != null

/**
 * Mengelola lagu offline di /storage/emulated/0/Download/HansAps/Spotify/<Judul-Artis>/
 *   music.mp3, thumbnail.jpg|png, meta.json
 * Unduhan berjalan paralel maks 2, lewat file .part dulu supaya tidak ada file setengah jadi.
 */
object RexOfflineManager {

    const val DISPLAY_PATH = "Download/HansAps/Spotify"

    internal const val AUDIO_NAME = "music.mp3"
    private const val META_NAME = "meta.json"
    internal const val LYRICS_NAME = "lyrics.json"
    private const val PREFS = "rex_music_offline"
    private const val KEY_OFFLINE = "offline_mode"
    internal const val USER_AGENT = "Mozilla/5.0 (Linux; Android 13) RexMusic"
    private const val MAX_PARALLEL = 2

    internal lateinit var appContext: Context
    internal val appContextInitialized: Boolean
        get() = this::appContext.isInitialized
    private lateinit var prefs: SharedPreferences
    private var initialized = false

    internal val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    internal val jobs = ConcurrentHashMap<String, Job>()
    internal val gate = Semaphore(MAX_PARALLEL)

    internal val _state = MutableStateFlow(OfflineState())
    val state: StateFlow<OfflineState> = _state.asStateFlow()

    internal val _messages = MutableSharedFlow<String>(
        extraBufferCapacity = 8,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    val root: File
        get() = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            "HansAps/Spotify"
        )

    @Synchronized
    fun init(context: Context) {
        if (initialized) return
        initialized = true
        appContext = context.applicationContext
        prefs = appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        RexDownloadNotification.init(appContext)
        _state.update {
            it.copy(
                enabled = prefs.getBoolean(KEY_OFFLINE, false),
                hasAccess = hasAccess(appContext)
            )
        }
        refresh()
    }

    //  Access & mode 

    /** Android 11+: "Akses semua file". Android 10-: izin tulis penyimpanan biasa. */
    fun hasAccess(context: Context): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        }

    fun setOfflineMode(on: Boolean) {
        prefs.edit().putBoolean(KEY_OFFLINE, on).apply()
        _state.update { it.copy(enabled = on) }
    }

    //  Library 

    fun refresh() {
        scope.launch { refreshNow() }
    }

    internal suspend fun refreshNow() {
        val access = hasAccess(appContext)
        val entries = if (access) withContext(Dispatchers.IO) { scan() } else emptyList()
        _state.update {
            it.copy(
                hasAccess = access,
                entries = entries,
                keys = entries.map { e -> e.key }.toSet(),
                totalBytes = entries.sumOf { e -> e.sizeBytes }
            )
        }
    }

    fun isDownloaded(track: RexTrack): Boolean = _state.value.isDownloaded(track)

    /** Import lagu yang diterima lewat RexMusicSync ke library RexMusic, tanpa menghapus cache sync. */
    fun importSyncTrack(track: RexTrack, sourceDir: File): Boolean {
        return this.__splitImpl_importSyncTrack(track, sourceDir)
    }

    /** File mp3 lokal untuk lagu ini, atau null kalau belum diunduh. */
    fun audioFile(track: RexTrack): File? {
        val key = _state.value.keyFor(track) ?: return null
        val file = File(root, "$key/$AUDIO_NAME")
        return file.takeIf { it.isFile && it.length() > 0L }
    }

    /** Lirik offline yang sudah disimpan bersama lagu. */
    fun lyricsFile(track: RexTrack): File? {
        val key = _state.value.keyFor(track) ?: return null
        val file = File(root, "$key/$LYRICS_NAME")
        return file.takeIf { it.isFile && it.length() > 0L }
    }

    fun readOfflineLyrics(track: RexTrack): Lyrics? = this.__splitImpl_readOfflineLyrics(track)

    //  Download 

    fun download(track: RexTrack) {
        return this.__splitImpl_download(track)
    }

    fun cancel(track: RexTrack) {
        jobs[offlineKey(track)]?.cancel()
    }

    fun delete(track: RexTrack) {
        val key = _state.value.keyFor(track) ?: return
        jobs[key]?.cancel()
        scope.launch {
            withContext(Dispatchers.IO) { File(root, key).deleteRecursively() }
            refreshNow()
            _messages.tryEmit("Dihapus dari offline: ${track.title}")
        }
    }

    fun deleteAll() {
        val keys = _state.value.entries.map { it.key }
        scope.launch {
            withContext(Dispatchers.IO) { keys.forEach { File(root, it).deleteRecursively() } }
            refreshNow()
            _messages.tryEmit("Semua lagu offline dihapus")
        }
    }

    internal suspend fun doDownload(track: RexTrack, key: String) {
        return this.__splitImpl_doDownload(track, key)
    }

    internal data class DownloadProgress(
        val percent: Int,
        val downloadedBytes: Long,
        val totalBytes: Long,
        val speedBytesPerSecond: Long
    )

    internal suspend fun downloadFile(
        url: String,
        dest: File,
        onProgress: (DownloadProgress) -> Unit
    ) {
        return this.__splitImpl_downloadFile(url, dest, onProgress)
    }

    /** Thumbnail bersifat opsional: kalau gagal, lagu tetap tersimpan tanpa cover. */
    internal fun downloadThumbnail(url: String, dir: File) {
        return this.__splitImpl_downloadThumbnail(url, dir)
    }

    internal fun writeMeta(dir: File, t: RexTrack) {
        runCatching {
            File(dir, META_NAME).writeText(
                JSONObject()
                    .put("id", t.id)
                    .put("title", t.title)
                    .put("artist", t.artist)
                    .put("album", t.album)
                    .put("spotifyUrl", t.spotifyUrl)
                    .put("durationText", t.durationText)
                    .put("coverUrl", t.cover)
                    .toString()
            )
        }
    }


    internal fun writeLyrics(dir: File, lyrics: Lyrics?, unavailable: Boolean) {
        return this.__splitImpl_writeLyrics(dir, lyrics, unavailable)
    }

    internal fun readMeta(dir: File): JSONObject? = runCatching {
        JSONObject(File(dir, META_NAME).readText())
    }.getOrNull()

    /** Buka koneksi dan ikuti redirect sendiri (termasuk http -> https). */
    internal fun openConnection(url: String): HttpURLConnection {
        return this.__splitImpl_openConnection(url)
    }

    internal fun cleanup(key: String) {
        val dir = File(root, key)
        File(dir, "$AUDIO_NAME.part").delete()
        File(dir, "thumbnail.tmp").delete()
        if (!File(dir, AUDIO_NAME).exists()) dir.deleteRecursively()
    }

    //  Scan 

    internal fun scan(): List<OfflineEntry> {
        return this.__splitImpl_scan()
    }

    internal fun findThumbnail(dir: File): File? =
        listOf("thumbnail.jpg", "thumbnail.png", "thumbnail.jpeg", "thumbnail.webp")
            .map { File(dir, it) }
            .firstOrNull { it.isFile && it.length() > 0L }

    //  Status helpers 

    internal fun setStatus(key: String, status: DownloadStatus) {
        _state.update { it.copy(downloads = it.downloads + (key to status)) }
        if (::appContext.isInitialized) RexDownloadNotification.showProgress(appContext, key, status)
    }

    internal fun clearStatus(key: String) =
        _state.update { it.copy(downloads = it.downloads - key) }
}
