package com.hansaps.spotify

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hansaps.core.DownloadStorage
import com.hansaps.core.LocalTrack
import com.hansaps.core.MusicPlaylist
import com.hansaps.core.NexrayApi
import com.hansaps.core.NexraySpotifyTrack
import com.hansaps.core.NexrayLyrics
import com.hansaps.core.PlaybackConnector
import com.hansaps.core.PlaylistEntry
import com.hansaps.core.PlaylistParser
import com.hansaps.spotify.Engine.AuthorizedAudioSource
import com.hansaps.spotify.Engine.DownloaderMusic
import com.hansaps.spotify.Engine.QueueEngine
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

class SpotifyViewModel : ViewModel() {
    private val mutablePlaylists = MutableStateFlow<List<MusicPlaylist>>(emptyList())
    val playlists: StateFlow<List<MusicPlaylist>> = mutablePlaylists
    private val mutableTracks = MutableStateFlow<List<LocalTrack>>(emptyList())
    val localTracks: StateFlow<List<LocalTrack>> = mutableTracks
    private val mutableSearchResults = MutableStateFlow<List<NexraySpotifyTrack>>(emptyList())
    val searchResults: StateFlow<List<NexraySpotifyTrack>> = mutableSearchResults
    private val mutableSearching = MutableStateFlow(false)
    val searching: StateFlow<Boolean> = mutableSearching
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
            mutableTracks.value = withContext(Dispatchers.IO) {
                DownloadStorage.listLocalAudio(context.applicationContext)
            }
        }
    }

    fun searchOnline(query: String) {
        val clean = query.trim()
        if (clean.isBlank()) {
            mutableMessage.value = "Masukkan judul lagu atau nama artis."
            return
        }
        viewModelScope.launch {
            mutableSearching.value = true
            mutableMessage.value = null
            try {
                mutableSearchResults.value = NexrayApi.searchSpotify(clean)
                mutableMessage.value = if (mutableSearchResults.value.isEmpty()) "Lagu tidak ditemukan. Coba kata kunci lain." else "${mutableSearchResults.value.size} hasil dari Nexray."
            } catch (error: Throwable) {
                mutableMessage.value = error.message ?: "Pencarian Nexray gagal. Periksa koneksi lalu coba lagi."
                mutableSearchResults.value = emptyList()
            } finally {
                mutableSearching.value = false
            }
        }
    }

    fun playPlaylistEntry(context: Context, entry: PlaylistEntry) {
        val local = findLocal(entry)
        if (local != null) {
            PlaybackConnector.play(context, local)
            mutableMessage.value = "Memutar ${local.title} dari koleksi offline."
            return
        }
        viewModelScope.launch {
            mutableMessage.value = "Mencari ${entry.title} di Nexray…"
            runCatching { findRemote(entry) }.onSuccess { remote ->
                if (remote == null) mutableMessage.value = "${entry.artist} — ${entry.title} tidak ditemukan secara online."
                else playOnlineInternal(context, remote)
            }.onFailure { mutableMessage.value = it.message ?: "Tidak bisa memuat lagu dari Nexray." }
        }
    }

    fun playPlaylist(context: Context, entries: List<PlaylistEntry>) {
        if (entries.isEmpty()) return
        viewModelScope.launch {
            mutableMessage.value = "Mencocokkan playlist dengan koleksi offline dan Nexray…"
            val tracks = mutableListOf<LocalTrack>()
            for (entry in entries) {
                val local = findLocal(entry)
                if (local != null) {
                    tracks += local
                    continue
                }
                runCatching { findRemote(entry) }.getOrNull()?.let { remote ->
                    runCatching {
                        val directUrl = resolveAudioWithFallback(remote)
                        tracks += remote.toRemoteLocalTrack(directUrl)
                    }
                }
            }
            if (tracks.isEmpty()) {
                mutableMessage.value = "Tidak ada lagu playlist yang ditemukan secara offline maupun online."
            } else {
                PlaybackConnector.playQueue(context, tracks)
                queueEngine.replace(tracks)
                mutableMessage.value = "Memutar ${tracks.size} dari ${entries.size} lagu (offline diprioritaskan)."
            }
        }
    }

    fun playTrack(context: Context, track: LocalTrack) {
        PlaybackConnector.play(context, track)
        mutableMessage.value = "Memutar ${track.title}"
    }

    fun playOnlineTrack(context: Context, track: NexraySpotifyTrack) {
        viewModelScope.launch {
            runCatching { playOnlineInternal(context, track) }
                .onFailure { mutableMessage.value = it.message ?: "Pemutaran gagal. Coba lagu lain." }
        }
    }

    private suspend fun playOnlineInternal(context: Context, track: NexraySpotifyTrack) {
        mutableMessage.value = "Menyiapkan audio dari Nexray…"
        val stream = resolveAudioWithFallback(track)
        PlaybackConnector.play(context, track.toRemoteLocalTrack(stream))
        mutableMessage.value = "Memutar ${track.title} — ${track.artist} dari Nexray."
    }

    fun downloadOnlineTrack(context: Context, track: NexraySpotifyTrack) {
        viewModelScope.launch {
            mutableMessage.value = "Mengunduh audio dan mengambil metadata dari Nexray…"
            val result = runCatching {
                val stream = resolveAudioWithFallback(track)
                val lyrics = runCatching { NexrayApi.searchLyrics(track.title, track.artist) }.getOrDefault(NexrayLyrics())
                DownloaderMusic().download(
                    context.applicationContext,
                    AuthorizedAudioSource(
                        title = track.title,
                        artist = track.artist,
                        directAudioUrl = stream,
                        thumbnailUrl = track.thumbnail.ifBlank { null },
                        lyricsText = lyrics.bestAvailable,
                        lyricsSynced = lyrics.synced,
                        lyricsPlain = lyrics.plain,
                        album = track.album,
                        duration = track.duration,
                        releaseDate = track.releaseDate,
                        sourceLabel = "Nexray API",
                    ),
                ).getOrThrow()
            }
            result.onSuccess {
                refreshCollection(context)
                mutableMessage.value = "${it.title} disimpan ke Download/HansAps/Spotify."
            }.onFailure { mutableMessage.value = it.message ?: "Download musik gagal." }
        }
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

    fun notify(message: String) { mutableMessage.value = message }
    fun consumeMessage() { mutableMessage.value = null }

    /** Mirrors RexMusic's resolver strategy: try the selected Spotify URL first, then search by title/artist if it fails. */
    private suspend fun resolveAudioWithFallback(track: NexraySpotifyTrack): String {
        var initialError: Exception? = null
        try {
            return NexrayApi.resolveSpotifyAudio(track.url)
        } catch (cancel: CancellationException) {
            throw cancel
        } catch (error: Exception) {
            initialError = error
        }

        val candidates = try {
            NexrayApi.searchSpotify("${track.artist} ${track.title}".trim())
        } catch (cancel: CancellationException) {
            throw cancel
        } catch (error: Exception) {
            throw initialError ?: error
        }
        val best = candidates
            .map { it to matchScore(track.title, track.artist, it.title, it.artist) }
            .filter { it.second > 0 && it.first.url != track.url }
            .maxByOrNull { it.second }
            ?.first
            ?: throw (initialError ?: IllegalStateException("Audio lagu tidak ditemukan dari Nexray."))

        return try {
            NexrayApi.resolveSpotifyAudio(best.url)
        } catch (cancel: CancellationException) {
            throw cancel
        } catch (fallbackError: Exception) {
            initialError?.let(fallbackError::addSuppressed)
            throw fallbackError
        }
    }

    private suspend fun findRemote(entry: PlaylistEntry): NexraySpotifyTrack? {
        val results = NexrayApi.searchSpotify("${entry.artist} ${entry.title}")
        return results
            .map { it to matchScore(entry.title, entry.artist, it.title, it.artist) }
            .filter { it.second > 0 }
            .maxByOrNull { it.second }
            ?.first
    }

    private fun matchScore(targetTitleRaw: String, targetArtistRaw: String, candidateTitleRaw: String, candidateArtistRaw: String): Int {
        val targetTitle = normalize(targetTitleRaw)
        val targetArtist = normalize(targetArtistRaw)
        val title = normalize(candidateTitleRaw)
        val artist = normalize(candidateArtistRaw)
        val titleScore = when {
            targetTitle.isBlank() || title.isBlank() -> 0
            title == targetTitle -> 6
            title.contains(targetTitle) || targetTitle.contains(title) -> 3
            else -> 0
        }
        val artistScore = when {
            targetArtist.isBlank() || artist.isBlank() -> 0
            artist == targetArtist -> 5
            artist.contains(targetArtist) || targetArtist.contains(artist) -> 2
            else -> 0
        }
        return titleScore + artistScore
    }

    private fun NexraySpotifyTrack.toRemoteLocalTrack(streamUrl: String) = LocalTrack(
        id = "nexray:${url}",
        title = title,
        artist = artist,
        uri = Uri.parse(streamUrl),
        displayName = "$title - $artist",
        artworkUri = thumbnail.takeIf { it.startsWith("http", true) }?.let(Uri::parse),
    )

    private fun findLocal(entry: PlaylistEntry): LocalTrack? {
        val keyTitle = normalize(entry.title)
        val keyArtist = normalize(entry.artist)
        return mutableTracks.value.firstOrNull { track ->
            val title = normalize(track.title)
            val artist = normalize(track.artist)
            title == keyTitle || (keyTitle.length > 4 && (title.contains(keyTitle) || keyTitle.contains(title))) ||
                (keyArtist.isNotBlank() && artist.contains(keyArtist) && title.contains(keyTitle))
        }
    }

    private fun normalize(value: String): String = value.lowercase(Locale.ROOT)
        .replace(Regex("[^a-z0-9]+"), " ").trim()
}
