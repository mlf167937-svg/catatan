package com.hansaps.spotify.rexmusic

import android.content.Context
import android.os.Environment
import java.io.File
import org.json.JSONArray
import org.json.JSONObject

/** Playlist user disimpan sebagai JSON mandiri di Download/HansAps/Spotify. */
class RexPlaylistStore(context: Context) {
    companion object {
        const val DISPLAY_PATH = "Download/HansAps/Spotify"
        private const val ROOT = "HansAps/Spotify"
        private const val EXT = ".json"
    }

    private val appContext = context.applicationContext
    private val root: File
        get() = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            ROOT
        )

    fun ensureRoot(): Boolean = runCatching { root.exists() || root.mkdirs() }.getOrDefault(false)

    fun list(): List<RexPlaylist> {
        if (!ensureRoot()) return emptyList()
        return root.listFiles { f -> f.isFile && f.extension.equals("json", ignoreCase = true) }
            ?.mapNotNull(::readFile)
            ?.sortedBy { it.name.lowercase() }
            ?: emptyList()
    }

    fun load(name: String): RexPlaylist? = readFile(fileFor(name))

    fun save(playlist: RexPlaylist): Boolean {
        if (!ensureRoot()) return false
        val safe = sanitize(playlist.name)
        if (safe.isBlank()) return false
        val target = File(root, "$safe$EXT")
        val json = JSONObject()
            .put("version", 1)
            .put("name", playlist.name.trim())
            .put("createdAt", playlist.createdAt)
            .put("updatedAt", System.currentTimeMillis())
            .put("tracks", JSONArray().apply { playlist.tracks.forEach { put(it.toPlaylistJson()) } })
        return runCatching {
            val part = File(target.parentFile, ".${target.name}.part")
            part.writeText(json.toString(2))
            if (target.exists() && !target.delete()) return@runCatching false
            part.renameTo(target)
        }.getOrDefault(false)
    }

    fun create(name: String): RexPlaylist? {
        val clean = name.trim()
        if (clean.isBlank()) return null
        val existing = load(clean)
        if (existing != null) return existing
        val playlist = RexPlaylist(clean, System.currentTimeMillis(), System.currentTimeMillis(), emptyList())
        return playlist.takeIf { save(it) }
    }

    fun addTrack(name: String, track: RexTrack): Boolean {
        val playlist = load(name) ?: return false
        if (playlist.tracks.any { it.id == track.id }) return true
        return save(playlist.copy(tracks = playlist.tracks + track))
    }

    fun removeTrack(name: String, trackId: String): Boolean {
        val playlist = load(name) ?: return false
        return save(playlist.copy(tracks = playlist.tracks.filterNot { it.id == trackId }))
    }

    fun delete(name: String): Boolean = runCatching { fileFor(name).delete() }.getOrDefault(false)

    private fun readFile(file: File): RexPlaylist? = runCatching {
        if (!file.isFile) return@runCatching null
        val o = JSONObject(file.readText())
        val name = o.optString("name").ifBlank { file.nameWithoutExtension }
        val arr = o.optJSONArray("tracks") ?: JSONArray()
        val tracks = (0 until arr.length()).mapNotNull { arr.optJSONObject(it)?.toPlaylistTrack() }
        RexPlaylist(
            name = name,
            createdAt = o.optLong("createdAt", file.lastModified()),
            updatedAt = o.optLong("updatedAt", file.lastModified()),
            tracks = tracks
        )
    }.getOrNull()

    private fun fileFor(name: String): File = File(root, "${sanitize(name)}$EXT")

    private fun sanitize(name: String): String = name
        .trim()
        .replace(Regex("[\\\\/:*?\"<>|]"), "-")
        .replace(Regex("\\s+"), " ")
        .take(80)
        .trim(' ', '.')
        .ifBlank { "Playlist" }
}

data class RexPlaylist(
    val name: String,
    val createdAt: Long,
    val updatedAt: Long,
    val tracks: List<RexTrack>
)

private fun RexTrack.toPlaylistJson(): JSONObject = JSONObject()
    .put("id", id)
    .put("title", title)
    .put("artist", artist)
    .put("album", album)
    .put("cover", cover)
    .put("spotifyUrl", spotifyUrl)
    .put("durationText", durationText)

private fun JSONObject.toPlaylistTrack(): RexTrack? {
    val title = optString("title")
    if (title.isBlank()) return null
    return RexTrack(
        id = optString("id").ifBlank { title },
        title = title,
        artist = optString("artist").ifBlank { "Unknown" },
        album = optString("album").ifBlank { "Single" },
        cover = optString("cover"),
        spotifyUrl = optString("spotifyUrl"),
        durationText = optString("durationText")
    )
}
