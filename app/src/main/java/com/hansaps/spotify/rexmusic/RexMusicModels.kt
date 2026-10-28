package com.hansaps.spotify.rexmusic

data class RexTrack(
    val id: String,
    val title: String,
    val artist: String,
    val album: String = "Single",
    val cover: String = "",
    val audioUrl: String = "",
    val spotifyUrl: String = "",
    val durationText: String = ""
)

/** Fase loading real-time. */
enum class LoadPhase { Idle, Searching, Resolving, Preparing, Buffering }

/** True kalau player sedang sibuk (mencari audio, menyiapkan, atau buffering/lag). */
val LoadPhase.isPlayerBusy: Boolean
    get() = this == LoadPhase.Resolving ||
        this == LoadPhase.Preparing ||
        this == LoadPhase.Buffering

//  Offline / download 

enum class DownloadStage { Queued, Resolving, Downloading, FetchingLyrics, Finalizing }

/** Status satu unduhan yang sedang berjalan. percent = -1 kalau ukuran file belum diketahui. */
data class DownloadStatus(
    val track: RexTrack,
    val stage: DownloadStage = DownloadStage.Queued,
    val percent: Int = -1,
    /** Byte yang sudah diterima dari network untuk file audio. */
    val downloadedBytes: Long = 0L,
    /** Ukuran total dari response server, 0L kalau server tidak mengirim Content-Length. */
    val totalBytes: Long = 0L,
    /** Kecepatan download terbaru dalam byte/detik. */
    val speedBytesPerSecond: Long = 0L
)

/** Satu lagu yang sudah tersimpan di Download/HansAps/Spotify. key = nama folder. */
data class OfflineEntry(
    /** Unique row key. RexAps entries use the prefix "RexAps:" to avoid folder-name collisions. */
    val key: String,
    val track: RexTrack,
    val sizeBytes: Long = 0L,
    val savedAt: Long = 0L,
    /** Absolute source folder. Primary library: Download/HansAps/Spotify; hybrid import: Download/RexAps/Music. */
    val sourcePath: String = "",
    /** "HansAps" or "RexAps"; external entries are read-only from HansAps. */
    val sourceApp: String = "HansAps"
)

data class OfflineState(
    /** Mode offline: hanya lagu unduhan yang ditampilkan dan diputar. */
    val enabled: Boolean = false,
    /** Izin akses penyimpanan sudah diberikan. */
    val hasAccess: Boolean = false,
    val entries: List<OfflineEntry> = emptyList(),
    val keys: Set<String> = emptySet(),
    val downloads: Map<String, DownloadStatus> = emptyMap(),
    val totalBytes: Long = 0L
)

data class RexMusicUiState(
    val loading: Boolean = false,
    val loadingText: String = "",
    val phase: LoadPhase = LoadPhase.Idle,
    val bufferedPercent: Int = 0,
    val tracks: List<RexTrack> = emptyList(),
    val searchResults: List<RexTrack> = emptyList(),
    /** Riwayat lagu (terbaru di depan, tanpa lagu yang sedang diputar). Hanya metadata. */
    val history: List<RexTrack> = emptyList(),
    /** Antrian buatan pengguna (metadata saja, disimpan di cache). */
    val userQueue: List<RexTrack> = emptyList(),
    /** Autoplay pintar: lagu berikutnya dipilih dari populer + artis favorit. */
    val autoplay: Boolean = true,
    /** Ulangi lagu yang sedang diputar: total (0 = mati, maks 50) dan sisa. */
    val repeatTotal: Int = 0,
    val repeatLeft: Int = 0,
    /** Artis favorit hasil belajar dari kebiasaan mendengar. */
    val favoriteArtists: List<String> = emptyList(),
    /** Jumlah lagu yang terisi di RexPopularSongs. */
    val popularCount: Int = 0,
    /** Data offline: daftar unduhan, progres unduhan, mode offline. */
    val offline: OfflineState = OfflineState(),
    /** Playlist pengguna, disimpan sebagai file JSON di Download/HansAps/Spotify. */
    val playlists: List<RexPlaylist> = emptyList(),
    /** True kalau lagu yang sedang diputar dibuka dari file offline. */
    val nowPlayingOffline: Boolean = false,
    /** Pesan singkat satu kali (snackbar). */
    val notice: String? = null,
    val currentIndex: Int = 0,
    val nowPlaying: RexTrack? = null,
    val isPlaying: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    /** Naik tiap seek / ganti lagu, dipakai notifikasi untuk sinkron posisi. */
    val seekVersion: Int = 0,
    /** Lagu berikutnya yang sudah pasti (antrian atau daftar). Null kalau autoplay yang memilih. */
    val upNext: RexTrack? = null,
    /** Lagu sebelumnya dari riwayat (null kalau belum ada). */
    val previous: RexTrack? = null,
    val error: String? = null,
    val lyrics: Lyrics = Lyrics(),
    val lyricsLoading: Boolean = false,
    val lyricsError: String? = null,
    val lyricsVisible: Boolean = false
)

data class SpotifySearchResponse(
    val status: Boolean? = null,
    val result: List<SpotifySearchItem>? = null
)

data class SpotifySearchItem(
    val title: String? = null,
    val artist: String? = null,
    val album: String? = null,
    val url: String? = null,
    val thumbnail: String? = null,
    val duration: String? = null
)

data class SpotifyDownloadResponse(
    val status: Boolean? = null,
    val result: SpotifyDownloadResult? = null
)

data class SpotifyDownloadResult(
    val url: String? = null,
    val title: String? = null,
    val artist: String? = null
)

//  Lyrics 

data class LyricLine(
    /** Waktu asli dari synced_lyrics dalam milidetik. Tidak digeser / dinormalisasi. */
    val timeMs: Long,
    val text: String,
    /** Token timestamp LRC asli, mis. "00:12.63". Dipertahankan untuk offline JSON. */
    val sourceTimestamp: String? = null
)

data class Lyrics(
    val plain: String = "",
    val synced: List<LyricLine> = emptyList(),
    /** duration dari respons API dalam detik. Hanya metadata, bukan sumber offset. */
    val durationSeconds: Int = 0
) {
    val hasSynced: Boolean get() = synced.isNotEmpty()
    val isEmpty: Boolean get() = plain.isBlank() && synced.isEmpty()
}

data class LyricsResponse(
    val status: Boolean? = null,
    val author: String? = null,
    val result: LyricsResult? = null,
    val timestamp: String? = null,
    @com.google.gson.annotations.SerializedName("response_time")
    val responseTime: String? = null
)

data class LyricsResult(
    val title: String? = null,
    val artist: String? = null,
    val thumbnail: String? = null,
    val url: String? = null,
    val lyrics: LyricsData? = null
)

data class LyricsData(
    val id: Long? = null,
    val name: String? = null,
    @com.google.gson.annotations.SerializedName("track_name")
    val trackName: String? = null,
    @com.google.gson.annotations.SerializedName("artist_name")
    val artistName: String? = null,
    @com.google.gson.annotations.SerializedName("album_name")
    val albumName: String? = null,
    val duration: Int? = null,
    val instrumental: String? = null,
    @com.google.gson.annotations.SerializedName("plain_lyrics")
    val plainLyrics: String? = null,
    @com.google.gson.annotations.SerializedName("synced_lyrics")
    val syncedLyrics: String? = null
)
