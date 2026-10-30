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
private fun normalizedTrackPart(value: String): String =
    value.trim().lowercase().replace(Regex("[^\\p{L}\\p{N}]+"), " ").trim()

fun OfflineState.keyFor(track: RexTrack, key: String = offlineKey(track)): String? =
    entries.firstOrNull { it.track.id == track.id }?.key
        ?: entries.firstOrNull {
            normalizedTrackPart(it.track.title) == normalizedTrackPart(track.title) &&
                normalizedTrackPart(it.track.artist) == normalizedTrackPart(track.artist)
        }?.key
        ?: key.takeIf { it in keys }

fun OfflineState.isDownloaded(track: RexTrack, key: String = offlineKey(track)): Boolean =
    keyFor(track, key) != null

/**
 * Mengelola lagu offline di app-private files/HansAps/Spotify/<Judul-Artis>/
 *   music.mp3, thumbnail.jpg|png, meta.json. This works on Android 10+ without broad storage permissions.
 * Unduhan berjalan paralel maks 2, lewat file .part dulu supaya tidak ada file setengah jadi.
 */
object RexOfflineManager {

    const val DISPLAY_PATH = "HansAps/Spotify (penyimpanan aplikasi)"

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

    /** Private app storage avoids scoped-storage failures on Android 10 and ROM variants. */
    val root: File
        get() {
            check(::appContext.isInitialized) { "RexOfflineManager.init(context) harus dipanggil sebelum akses koleksi offline" }
            return File(appContext.filesDir, "HansAps/Spotify")
        }

    private fun migrateLegacyDownloadsIfPossible() {
        val legacy = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            "HansAps/Spotify"
        )
        if (!legacy.isDirectory || legacy.absolutePath == root.absolutePath) return
        runCatching {
            root.mkdirs()
            legacy.listFiles()?.forEach { old ->
                val destination = File(root, old.name)
                if (!destination.exists()) old.copyRecursively(destination, overwrite = false)
            }
        }
    }

    /** Read-only compatibility source used by RexMusic: Download/RexAps/Music. */
    internal val rexApsRoot: File
        get() = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            "RexAps/Music"
        )

    internal fun directoryFor(entry: OfflineEntry): File =
        entry.sourcePath.takeIf { it.isNotBlank() }?.let(::File) ?: File(root, entry.key.removePrefix("RexAps:"))

    internal fun findEntry(track: RexTrack): OfflineEntry? =
        _state.value.entries.firstOrNull { it.track.id == track.id }
            ?: _state.value.entries.firstOrNull {
                normalizedTrackPart(it.track.title) == normalizedTrackPart(track.title) &&
                    normalizedTrackPart(it.track.artist) == normalizedTrackPart(track.artist)
            }

    internal fun findEntryById(id: String): OfflineEntry? =
        _state.value.entries.firstOrNull { it.track.id == id }

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

    /** Offline music is written only to this app's private directory; no storage permission is required. */
    fun hasAccess(context: Context): Boolean = runCatching {
        val dir = File(context.filesDir, "HansAps/Spotify")
        if (!dir.isDirectory && !dir.mkdirs()) return@runCatching false
        // Some vendor ROMs report canWrite() even when the filesystem is read-only.
        // A tiny real write is a more reliable capability check than permission flags.
        val probe = File(dir, ".write-check-${System.nanoTime()}")
        try {
            probe.outputStream().use { it.write(0x48) }
            probe.isFile && probe.length() == 1L
        } finally {
            probe.delete()
        }
    }.getOrDefault(false)

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
        val entries = if (access) withContext(Dispatchers.IO) {
            migrateLegacyDownloadsIfPossible()
            scan()
        } else emptyList()
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
        val entry = findEntry(track) ?: return null
        val file = File(directoryFor(entry), AUDIO_NAME)
        return file.takeIf { it.isFile && it.length() > 0L }
    }

    /** Lirik offline yang sudah disimpan bersama lagu, termasuk lagu yang dibaca dari RexAps. */
    fun lyricsFile(track: RexTrack): File? {
        val entry = findEntry(track) ?: return null
        val file = File(directoryFor(entry), LYRICS_NAME)
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
        val entry = findEntry(track) ?: return
        if (entry.sourceApp != "HansAps") {
            _messages.tryEmit("Lagu ini berasal dari RexAps/Music. HansAps membacanya tanpa menghapus file asli; hapus lagu dari RexAps untuk menghilangkannya.")
            return
        }
        jobs[entry.key]?.cancel()
        scope.launch {
            withContext(Dispatchers.IO) { directoryFor(entry).deleteRecursively() }
            refreshNow()
            _messages.tryEmit("Dihapus dari offline: ${track.title}")
        }
    }

    fun deleteAll() {
        val localEntries = _state.value.entries.filter { it.sourceApp == "HansAps" }
        scope.launch {
            withContext(Dispatchers.IO) { localEntries.forEach { directoryFor(it).deleteRecursively() } }
            refreshNow()
            _messages.tryEmit("Lagu offline HansAps dihapus; file RexAps tetap aman")
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
