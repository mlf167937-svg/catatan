package com.hansaps.spotify

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hansaps.core.DownloadStorage
import com.hansaps.core.LocalTrack
import com.hansaps.core.MusicPlaylist
import com.hansaps.core.PlaylistEntry
import com.hansaps.core.PlaylistParser
import com.hansaps.core.PlaybackConnector
import com.hansaps.spotify.Engine.QueueEngine
import com.hansaps.spotify.Engine.DownloaderMusic
import com.hansaps.spotify.Engine.AuthorizedAudioSource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.Locale

class SpotifyViewModel : ViewModel() {
    private val mutablePlaylists = MutableStateFlow<List<MusicPlaylist>>(emptyList())
    val playlists: StateFlow<List<MusicPlaylist>> = mutablePlaylists

    private val mutableTracks = MutableStateFlow<List<LocalTrack>>(emptyList())
    val localTracks: StateFlow<List<LocalTrack>> = mutableTracks

    private val mutableMessage = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = mutableMessage

    val queueEngine = QueueEngine()
    private var loaded = false

    fun load(context: Context) {
        if (loaded) return
        loaded = true
        runCatching {
            context.assets.open("Playlist.ini").bufferedReader(Charsets.UTF_8).use { it.readText() }
        }.onSuccess { mutablePlaylists.value = PlaylistParser.parse(it) }
        refreshCollection(context)
    }

    fun refreshCollection(context: Context) {
        viewModelScope.launch {
            mutableTracks.value = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                DownloadStorage.listLocalAudio(context.applicationContext)
            }
        }
    }

    fun playPlaylistEntry(context: Context, entry: PlaylistEntry) {
        val match = findLocal(entry)
        if (match == null) {
            mutableMessage.value = "${entry.artist} — ${entry.title} belum ada di Koleksi Offline. Provider musik online belum dikonfigurasi."
            return
        }
        PlaybackConnector.play(context, match)
        mutableMessage.value = "Memutar ${match.title}"
    }

    fun playPlaylist(context: Context, entries: List<PlaylistEntry>) {
        val matched = entries.mapNotNull { findLocal(it) }.distinctBy { it.id }
        if (matched.isEmpty()) {
            mutableMessage.value = "Tidak ada lagu playlist ini di Koleksi Offline. Impor file lokal atau hubungkan provider musik yang berwenang."
            return
        }
        PlaybackConnector.playQueue(context, matched)
        queueEngine.replace(matched)
        mutableMessage.value = if (matched.size == entries.size) "Memutar playlist (${matched.size} lagu)" else "Memutar ${matched.size} dari ${entries.size} lagu yang tersedia offline."
    }

    fun playTrack(context: Context, track: LocalTrack) {
        PlaybackConnector.play(context, track)
        mutableMessage.value = "Memutar ${track.title}"
    }

    fun playAll(context: Context, tracks: List<LocalTrack>, startIndex: Int = 0) {
        if (tracks.isEmpty()) return
        PlaybackConnector.playQueue(context, tracks, startIndex)
        queueEngine.replace(tracks)
        mutableMessage.value = "Antrean musik diperbarui"
    }

    fun addToQueue(context: Context, track: LocalTrack) {
        queueEngine.enqueue(track)
        PlaybackConnector.enqueue(context, track)
        mutableMessage.value = "${track.title} ditambahkan ke antrean"
    }

    fun downloadMusic(context: Context, title: String, artist: String, audioUrl: String, thumbnailUrl: String, lyrics: String) {
        viewModelScope.launch {
            val result = DownloaderMusic().download(
                context.applicationContext,
                AuthorizedAudioSource(
                    title = title,
                    artist = artist,
                    directAudioUrl = audioUrl,
                    thumbnailUrl = thumbnailUrl.ifBlank { null },
                    lyricsText = lyrics.ifBlank { null },
                ),
            )
            result.onSuccess {
                refreshCollection(context)
                mutableMessage.value = "${it.title} disimpan ke Download/HansAps/Spotify"
            }.onFailure { mutableMessage.value = it.message ?: "Download musik gagal." }
        }
    }

    fun importAudio(context: Context, uri: Uri) {
        viewModelScope.launch {
            val result = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                DownloadStorage.importAudio(context.applicationContext, uri)
            }
            result.onSuccess { track ->
                refreshCollection(context)
                mutableMessage.value = "${track.title} disimpan ke Download/HansAps/Spotify"
            }.onFailure { error ->
                mutableMessage.value = error.message ?: "Import audio gagal."
            }
        }
    }

    fun notify(message: String) { mutableMessage.value = message }
    fun consumeMessage() { mutableMessage.value = null }

    private fun findLocal(entry: PlaylistEntry): LocalTrack? {
        val keyTitle = normalize(entry.title)
        val keyArtist = normalize(entry.artist)
        return mutableTracks.value.firstOrNull { track ->
            val title = normalize(track.title)
            val artist = normalize(track.artist)
            title == keyTitle || (keyTitle.length > 4 && (title.contains(keyTitle) || keyTitle.contains(title))) ||
                (artist.contains(keyArtist) && title.contains(keyTitle))
        }
    }

    private fun normalize(value: String): String = value.lowercase(Locale.ROOT)
        .replace(Regex("[^a-z0-9]+"), " ").trim()
}
