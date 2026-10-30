package com.hansaps.spotify.rexmusic

import com.hansaps.localaccount.RexPaths

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update

/**
 * Pemutar musik tingkat aplikasi. Hidup di luar ViewModel supaya musik tetap jalan
 * saat layar ditutup dan bisa dikontrol dari notifikasi / lock screen.
 * Semua fungsi dipanggil dari main thread.
 */
object RexPlayerController {
    internal lateinit var appContext: Context
    internal lateinit var audioManager: AudioManager
    internal lateinit var store: RexHistoryStore
    private lateinit var queueStore: RexQueueStore
    internal lateinit var taste: RexTasteStore
    internal lateinit var playlistStore: RexPlaylistStore
    internal var initialized = false
    internal val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    internal val _state = MutableStateFlow(
        RexMusicUiState(tracks = RexMusicRegistry.defaultTracks)
    )
    val state: StateFlow<RexMusicUiState> = _state.asStateFlow()

    // player
    internal var player: MediaPlayer? = null
    internal var syncVolume: Float = 1f
    internal var syncPrepareTrackId: String? = null
    internal var prepared = false
    internal var progressJob: Job? = null
    internal var seekJob: Job? = null
    internal var seekPending = false
    internal var tasteCounted = false

    // search / resolve / prepare / buffering flags
    internal var searchJob: Job? = null
    internal var playJob: Job? = null
    internal var searching = false
    internal var resolving = false
    internal var preparing = false
    internal var buffering = false
    internal var resolveText = ""

    // konteks: daftar asal lagu (search / rekomendasi / riwayat / perpustakaan offline)
    internal var queue: List<RexTrack> = emptyList()
    internal var queueIndex = 0

    // true = tombol next mengikuti urutan konteks (dipakai perpustakaan offline), bukan autoplay acak
    internal var followContext = false

    // antrian buatan pengguna (metadata saja) + autoplay pintar
    internal val userQueue = mutableListOf<RexTrack>()
    internal var autoplay = true

    // riwayat kronologis (terlama di depan, terbaru di belakang). Hanya metadata.
    internal val history = mutableListOf<RexTrack>()

    // audio focus
    internal var focusRequest: AudioFocusRequest? = null
    private var resumeOnGain = false
    internal val audioAttrs = AudioAttributes.Builder()
        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
        .setUsage(AudioAttributes.USAGE_MEDIA)
        .build()

    @Synchronized
    fun init(context: Context) {
        if (initialized) return
        initialized = true
        appContext = context.applicationContext
        RexPaths.initialize(appContext)
        audioManager = appContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        store = RexHistoryStore(appContext)
        queueStore = RexQueueStore(appContext)
        taste = RexTasteStore(appContext)
        playlistStore = RexPlaylistStore(appContext)
        history.addAll(store.load())
        userQueue.addAll(queueStore.load())
        autoplay = taste.autoplay
        _state.update { it.copy(popularCount = RexPopularSongs.tracks.size, playlists = playlistStore.list()) }
        RexOfflineManager.init(appContext)
        scope.launch { RexOfflineManager.state.collect { onOfflineChanged(it) } }
        scope.launch { RexOfflineManager.messages.collect { postNotice(it) } }
        publishQueueInfo()
        publishTaste()
    }

    internal fun offlineMode(): Boolean = RexOfflineManager.state.value.enabled
    
    internal fun onOfflineChanged(o: OfflineState) {
        return this.__splitImpl_onOfflineChanged(o)
    }

    // Offline mode
    fun setOfflineMode(on: Boolean) {
        return this.__splitImpl_setOfflineMode(on)
    }

    // ---------------- Playlist JSON ----------------
    fun refreshPlaylists() {
        return this.__splitImpl_refreshPlaylists()
    }

    fun createPlaylist(name: String) {
        return this.__splitImpl_createPlaylist(name)
    }

    fun addToPlaylist(name: String, track: RexTrack) {
        return this.__splitImpl_addToPlaylist(name, track)
    }

    fun removeFromPlaylist(name: String, track: RexTrack) {
        return this.__splitImpl_removeFromPlaylist(name, track)
    }

    fun deletePlaylist(name: String) {
        return this.__splitImpl_deletePlaylist(name)
    }

    fun playPlaylist(name: String, startTrackId: String? = null) {
        return this.__splitImpl_playPlaylist(name, startTrackId)
    }

    /** Putar lagu dari perpustakaan offline; Next mengikuti urutan perpustakaan. */
    fun playFromOffline(track: RexTrack) {
        return this.__splitImpl_playFromOffline(track)
    }

    /** Prepare a local track for synchronized playback without emitting audio before the room target time. */
    fun prepareFromOfflineForSync(track: RexTrack) {
        return this.__splitImpl_prepareFromOfflineForSync(track)
    }

    /** "Putar semua" / "Acak" di perpustakaan offline. */
    fun playOfflineAll(shuffle: Boolean) {
        return this.__splitImpl_playOfflineAll(shuffle)
    }

    // Search
    /** Debounced: aman dipanggil di tiap ketikan. Tidak dipakai saat mode offline. */
    fun search(query: String) {
        return this.__splitImpl_search(query)
    }

    fun clearSearch() {
        return this.__splitImpl_clearSearch()
    }

    fun dismissError() = _state.update { it.copy(error = null) }
    fun consumeNotice() = _state.update { it.copy(notice = null) }
    internal fun postNotice(message: String) = _state.update { it.copy(notice = message) }

    fun clearHistory() {
        return this.__splitImpl_clearHistory()
    }

    // Play from list
    fun playFromMain(index: Int) {
        return this.__splitImpl_playFromMain(index)
    }

    /** Main Home shelf: the 30 explicitly curated songs from RexMusic. */
    fun playPopular(index: Int) {
        val list = RexPopularSongs.tracks
        if (index !in list.indices) return
        startQueue(list, index)
    }

    fun playFromSearch(index: Int) {
        return this.__splitImpl_playFromSearch(index)
    }

    /** index mengacu ke state.history (terbaru di depan). Lagu yang sudah diunduh diputar dari file. */
    fun playFromHistory(index: Int) {
        return this.__splitImpl_playFromHistory(index)
    }

    internal fun startQueue(list: List<RexTrack>, index: Int, follow: Boolean = false) {
        return this.__splitImpl_startQueue(list, index, follow)
    }

    /** Mix pintar: langsung pilih lagu dari artis favorit / populer (atau dari unduhan saat offline). */
    fun playMix() {
        return this.__splitImpl_playMix()
    }

    // User queue
    fun addToQueue(track: RexTrack) {
        return this.__splitImpl_addToQueue(track)
    }

    fun removeFromQueue(index: Int) {
        return this.__splitImpl_removeFromQueue(index)
    }

    fun moveQueueToTop(index: Int) {
        return this.__splitImpl_moveQueueToTop(index)
    }

    fun clearQueue() {
        return this.__splitImpl_clearQueue()
    }

    /** Putar lagu dari antrian sekarang juga (lagunya keluar dari antrian). */
    fun playFromQueue(index: Int) {
        return this.__splitImpl_playFromQueue(index)
    }

    internal fun persistQueue() = queueStore.save(userQueue.toList())

    // Repeat & autoplay
    /** Ulangi lagu yang sedang diputar sebanyak [times] kali lagi (0 = mati, maks 50). */
    fun setRepeat(times: Int) {
        return this.__splitImpl_setRepeat(times)
    }

    fun setAutoplay(on: Boolean) {
        return this.__splitImpl_setAutoplay(on)
    }

    // Next / previous
    /** Tombol next dari pengguna. */
    fun next() {
        return this.__splitImpl_next()
    }

    /**
     * Kembali:
     * - Lagu sudah jalan > 3 detik (dan tidak force) -> ulang dari awal.
     * - Ada riwayat -> putar lagu sebelumnya (nama disimpan, audio diambil ulang lewat API / dari file offline).
     * - Tidak ada riwayat -> mundur di daftar asal.
     */
    fun prev(force: Boolean = false) {
        return this.__splitImpl_prev(force)
    }

    fun currentTrack(): RexTrack? = _state.value.nowPlaying

    /**
     * Urutan: antrian pengguna -> autoplay pintar -> daftar asal.
     * Saat offline, lagu yang belum diunduh dilewati.
     */
    internal fun advance() {
        return this.__splitImpl_advance()
    }

    internal fun playContextNext() {
        return this.__splitImpl_playContextNext()
    }

    /** Pastikan target ada di antrian konteks & queueIndex menunjuk ke sana. */
    internal fun syncQueueTo(target: RexTrack) {
        return this.__splitImpl_syncQueueTo(target)
    }

    internal fun cancelPending() {
        playJob?.cancel()
        playJob = null
    }

    // Begin / resolve
    internal fun beginTrack(track: RexTrack) {
        return this.__splitImpl_beginTrack(track)
    }

    /** Set state "lagu baru" (reset ulangi, posisi, dsb). Tidak menyentuh player. */
    internal fun markTrack(track: RexTrack) {
        return this.__splitImpl_markTrack(track)
    }

    /**
     * File offline dipakai kalau ada (tanpa internet). Kalau belum diunduh: saat mode offline gagal
     * dengan pesan jelas, saat online diambil lewat API. true = pemutaran dimulai.
     */
    internal suspend fun resolveAndStart(track: RexTrack): Boolean {
        return this.__splitImpl_resolveAndStart(track)
    }

    // Lyrics
    internal var lyricsJob: Job? = null
    internal fun loadLyrics(track: RexTrack) {
        return this.__splitImpl_loadLyrics(track)
    }

    /**
     * Paksa widget re-render tiap kali state penting berubah (lirik selesai dimuat, lagu ganti, dll).
     * Service akan mengirim cover bitmap-nya sendiri saat siap.
     */
    internal fun pushWidgetUpdate() {
        return this.__splitImpl_pushWidgetUpdate()
    }

    fun toggleLyrics() = this.__splitImpl_toggleLyrics()
    fun closeLyrics() = _state.update { it.copy(lyricsVisible = false) }

    /** Update track di queue + semua list berdasarkan id (bukan index, jadi anti out-of-bounds). */
    internal fun replaceTrack(fresh: RexTrack) {
        return this.__splitImpl_replaceTrack(fresh)
    }

    // Smart autoplay
    /** Pilih lagu berikutnya sendiri; coba sampai [SMART_RETRIES] kali kalau audio gagal dimuat. */
    internal fun playSmart() {
        return this.__splitImpl_playSmart()
    }

    /**
     * Online: ~65% dari artis favorit (kalau selera sudah terbaca), selain itu acak dari populer.
     * Offline: sama, tapi hanya dari lagu yang sudah diunduh (tanpa internet).
     * Lagu yang baru diputar dihindari.
     */
    internal suspend fun smartPick(exclude: Set<String>): RexTrack? {
        return this.__splitImpl_smartPick(exclude)
    }

    internal fun offlinePick(avoid: Set<String>): RexTrack? {
        return this.__splitImpl_offlinePick(avoid)
    }

    internal suspend fun pickFromArtist(artist: String, avoid: Set<String>): RexTrack? {
        return this.__splitImpl_pickFromArtist(artist, avoid)
    }

    internal fun popularPick(avoid: Set<String>): RexTrack? {
        return this.__splitImpl_popularPick(avoid)
    }

    internal fun recentKeys(): Set<String> {
        return this.__splitImpl_recentKeys()
    }

    internal fun penalizeEarlySkip() {
        return this.__splitImpl_penalizeEarlySkip()
    }

    internal fun publishTaste() {
        return this.__splitImpl_publishTaste()
    }

    // History
    internal fun sameSong(a: RexTrack, b: RexTrack): Boolean = this.__splitImpl_sameSong(a, b)

    /** Catat lagu ke riwayat (tanpa audioUrl). Dipanggil setelah lagu benar-benar mulai diputar. */
    internal fun pushHistory(track: RexTrack) {
        return this.__splitImpl_pushHistory(track)
    }

    /** Lagu tepat sebelum lagu yang sedang diputar di riwayat (saat offline: hanya yang sudah diunduh). */
    internal fun previousTarget(now: RexTrack?): RexTrack? {
        return this.__splitImpl_previousTarget(now)
    }

    internal fun publishQueueInfo() {
        return this.__splitImpl_publishQueueInfo()
    }

    // Player
    /** [source] = URL audio (online) atau path file (local = true). */
    internal fun startPlayer(source: String, local: Boolean = false) {
        return this.__splitImpl_startPlayer(source, local)
    }

    internal fun updateBuffering(value: Boolean) {
        return this.__splitImpl_setBuffering(value)
    }

    internal fun stopPlayer() {
        return this.__splitImpl_stopPlayer()
    }

    /** Hentikan total (tombol tutup di notifikasi). Lagu tetap tercatat, bisa diputar ulang. */
    fun stop() {
        return this.__splitImpl_stop()
    }

    /** Progress + deteksi lag + pencatatan selera (>= 15 detik dianggap benar-benar didengar). */
    internal fun startProgressLoop(mp: MediaPlayer) {
        return this.__splitImpl_startProgressLoop(mp)
    }

    /** Play/pause. Kalau player sudah mati (error / ditutup) -> putar ulang lagu ini. */
    fun togglePlay() {
        return this.__splitImpl_togglePlay()
    }

    fun play() {
        if (!_state.value.isPlaying) togglePlay()
    }

    fun pause() {
        if (_state.value.isPlaying) togglePlay()
    }

    /** Local playback gain for RexMusicSync; does not change Android system volume. */
    fun setSyncVolume(volume: Float) {
        return this.__splitImpl_setSyncVolume(volume)
    }

    /** Posisi UI langsung update; seek asli didebounce supaya drag tidak membebani MediaPlayer. */
    fun seekTo(ratio: Float) {
        return this.__splitImpl_seekTo(ratio)
    }

    /** Immediate seek for sync correction; unlike the UI drag path, this is not debounced. */
    fun seekToPositionImmediate(positionMs: Long) {
        return this.__splitImpl_seekToPositionImmediate(positionMs)
    }

    /** Start the already-prepared player at the requested sync position. Must be called on main. */
    fun startSyncPlayback(positionMs: Long) {
        return this.__splitImpl_startSyncPlayback(positionMs)
    }

    /** Seek absolut, dipakai synced lyrics supaya baris yang diketuk tepat ke timestamp API. */
    fun seekToPosition(positionMs: Long) {
        return this.__splitImpl_seekToPosition(positionMs)
    }

    internal fun pauseInternal() {
        return this.__splitImpl_pauseInternal()
    }

    internal fun fail(message: String) {
        return this.__splitImpl_fail(message)
    }

    // Audio focus
    internal val focusListener = AudioManager.OnAudioFocusChangeListener { change ->
        when (change) {
            AudioManager.AUDIOFOCUS_LOSS -> {
                resumeOnGain = false
                pauseInternal()
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                resumeOnGain = _state.value.isPlaying
                pauseInternal()
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> player?.setVolume(0.3f, 0.3f)
            AudioManager.AUDIOFOCUS_GAIN -> {
                player?.setVolume(1f, 1f)
                if (resumeOnGain && prepared) {
                    resumeOnGain = false
                    runCatching { player?.start() }
                    _state.update { it.copy(isPlaying = true) }
                }
            }
        }
    }

    internal fun requestFocus(): Boolean {
        return this.__splitImpl_requestFocus()
    }

    internal fun abandonFocus() {
        return this.__splitImpl_abandonFocus()
    }

    // Misc
    /**
     * loading = search / resolve / prepare.
     * Buffering (lag saat memutar) hanya lewat [LoadPhase] supaya halaman browse tidak ikut loading.
     */
    internal fun publishLoading() = this.__splitImpl_publishLoading()

    internal fun List<RexTrack>.replaceById(t: RexTrack) = map { if (it.id == t.id) t else it }

    internal const val SEARCH_DEBOUNCE_MS = 400L
    internal const val SEEK_DEBOUNCE_MS = 80L
    internal const val PROGRESS_INTERVAL_MS = 100L
    internal const val RESTART_THRESHOLD_MS = 3000L
    internal const val STALL_TICKS = 6
    internal const val MAX_HISTORY = 50
    internal const val MAX_QUEUE = 100
    internal const val MAX_REPEAT = 50

    // smart autoplay (silakan ubah sesuai selera)
    internal const val TASTE_CHANCE = 0.65f        // peluang lagu berikutnya dari artis favorit
    internal const val TASTE_LISTEN_MS = 15_000L   // didengar segini lama = dihitung "suka"
    internal const val EARLY_SKIP_MS = 10_000L     // di-skip sebelum ini = dianggap kurang suka
    internal const val REPEAT_TASTE_WEIGHT = 0.5
    internal const val RECENT_WINDOW = 8           // hindari lagu yang baru diputar
    internal const val SMART_RETRIES = 3
}
