package com.hansaps.spotify.rexmusic

import android.media.MediaPlayer
import kotlin.random.Random
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/** Pilih lagu berikutnya sendiri; coba sampai [SMART_RETRIES] kali kalau audio gagal dimuat. */
    internal fun RexPlayerController.__splitImpl_playSmart() {
        stopPlayer()
        resolving = true
        resolveText = "memilih lagu untukmu..."
        _state.update {
            it.copy(
                isPlaying = false, positionMs = 0L, durationMs = 0L, bufferedPercent = 0,
                seekVersion = it.seekVersion + 1, error = null, repeatTotal = 0, repeatLeft = 0,
                nowPlayingOffline = false
            )
        }
        publishLoading()
        RexMusicService.start(appContext)

        val job = scope.launch(start = CoroutineStart.LAZY) {
            val tried = mutableSetOf<String>()
            var attempts = 0
            while (attempts < SMART_RETRIES) {
                val pick = smartPick(tried) ?: break
                tried += songKey(pick)
                markTrack(pick)
                if (resolveAndStart(pick)) return@launch
                attempts++
            }
            if (attempts == 0) {
                resolving = false
                publishLoading()
                when {
                    offlineMode() && RexOfflineManager.state.value.entries.isEmpty() ->
                        _state.update { it.copy(error = "belum ada lagu offline. Unduh lagu dulu saat online") }
                    queue.isNotEmpty() -> playContextNext()
                    else -> _state.update {
                        it.copy(error = "belum ada lagu berikutnya. Isi RexPopularSongs atau putar dari daftar")
                    }
                }
            }
        }
        playJob = job
        job.start()
    }

/** Progress + deteksi lag + pencatatan selera (>= 15 detik dianggap benar-benar didengar). */
    internal fun RexPlayerController.__splitImpl_startProgressLoop(mp: MediaPlayer) {
        progressJob?.cancel()
        progressJob = scope.launch {
            var lastPos = -1L
            var stalledTicks = 0
            while (isActive && player === mp) {
                if (!seekPending) {
                    runCatching {
                        if (mp.isPlaying) {
                            val pos = mp.currentPosition.toLong()
                            _state.update { it.copy(positionMs = pos) }
                            if (pos == lastPos) {
                                if (++stalledTicks >= STALL_TICKS) updateBuffering(true)
                            } else {
                                stalledTicks = 0
                                lastPos = pos
                                updateBuffering(false)
                            }
                            if (!tasteCounted && pos >= TASTE_LISTEN_MS) {
                                tasteCounted = true
                                _state.value.nowPlaying?.let { taste.recordPlay(it.artist) }
                                publishTaste()
                            }
                        } else {
                            stalledTicks = 0
                        }
                    }
                }
                delay(PROGRESS_INTERVAL_MS)
            }
        }
    }

/** Debounced: aman dipanggil di tiap ketikan. Tidak dipakai saat mode offline. */
    internal fun RexPlayerController.__splitImpl_search(query: String) {
        searchJob?.cancel()
        if (offlineMode()) return
        val q = query.trim()
        if (q.isEmpty()) {
            clearSearch()
            return
        }
        searchJob = scope.launch {
            delay(SEARCH_DEBOUNCE_MS)
            searching = true
            _state.update { it.copy(error = null) }
            publishLoading()
            RexMusicApi.search(q)
                .onSuccess { list ->
                    searching = false
                    _state.update { it.copy(searchResults = list) }
                }
                .onFailure { e ->
                    searching = false
                    _state.update { it.copy(error = e.message ?: "search gagal") }
                }
            publishLoading()
        }
    }

/**
     * Kembali:
     * - Lagu sudah jalan > 3 detik (dan tidak force) -> ulang dari awal.
     * - Ada riwayat -> putar lagu sebelumnya (nama disimpan, audio diambil ulang lewat API / dari file offline).
     * - Tidak ada riwayat -> mundur di daftar asal.
     */
    internal fun RexPlayerController.__splitImpl_prev(force: Boolean = false) {
        if (!force && prepared && _state.value.positionMs > RESTART_THRESHOLD_MS) {
            seekTo(0f)
            return
        }
        val now = _state.value.nowPlaying
        val target = previousTarget(now)
        if (target != null) {
            cancelPending()
            history.removeAll { sameSong(it, target) || (now != null && sameSong(it, now)) }
            store.save(history)
            syncQueueTo(target)
            beginTrack(target)
            return
        }
        if (queue.isEmpty()) {
            seekTo(0f)
            return
        }
        cancelPending()
        queueIndex = if (queueIndex - 1 < 0) queue.size - 1 else queueIndex - 1
        queue.getOrNull(queueIndex)?.let { beginTrack(it) }
    }

internal fun RexPlayerController.__splitImpl_offlinePick(avoid: Set<String>): RexTrack? {
        val all = RexOfflineManager.state.value.entries.map { it.track }
        if (all.isEmpty()) return null
        val favorite = taste.pickFavorite()
        if (favorite != null && Random.nextFloat() < TASTE_CHANCE) {
            all.filter {
                primaryArtist(it.artist).equals(favorite, ignoreCase = true) && songKey(it) !in avoid
            }.randomOrNull()?.let { return it }
        }
        val nowKey = _state.value.nowPlaying?.let { songKey(it) }
        return all.filter { songKey(it) !in avoid }.randomOrNull()
            ?: all.filter { songKey(it) != nowKey }.randomOrNull()
            ?: all.randomOrNull()
    }

/** Play/pause. Kalau player sudah mati (error / ditutup) -> putar ulang lagu ini. */
    internal fun RexPlayerController.__splitImpl_togglePlay() {
        val mp = player
        if (mp == null) {
            val now = _state.value.nowPlaying
            if (!resolving && now != null) {
                cancelPending()
                beginTrack(now)
            }
            return
        }
        if (!prepared) return
        if (runCatching { mp.isPlaying }.getOrDefault(false)) {
            pauseInternal()
        } else if (requestFocus()) {
            runCatching { mp.start() }
            _state.update { it.copy(isPlaying = true) }
        }
    }

internal fun RexPlayerController.__splitImpl_playContextNext() {
        if (queue.isEmpty()) return
        val offline = offlineMode()
        for (i in queue.indices) {
            queueIndex = (queueIndex + 1) % queue.size
            val track = queue[queueIndex]
            if (!offline || RexOfflineManager.isDownloaded(track)) {
                beginTrack(track)
                return
            }
        }
        _state.update { it.copy(error = "tidak ada lagu offline lain untuk diputar") }
    }

/**
     * Online: ~65% dari artis favorit (kalau selera sudah terbaca), selain itu acak dari populer.
     * Offline: sama, tapi hanya dari lagu yang sudah diunduh (tanpa internet).
     * Lagu yang baru diputar dihindari.
     */
    internal suspend fun RexPlayerController.__splitImpl_smartPick(exclude: Set<String>): RexTrack? {
        val avoid = recentKeys() + exclude
        if (offlineMode()) return offlinePick(avoid)

        val favorite = taste.pickFavorite()
        if (favorite != null && Random.nextFloat() < TASTE_CHANCE) {
            pickFromArtist(favorite, avoid)?.let { return it }
        }
        popularPick(avoid)?.let { return it }
        if (favorite != null) pickFromArtist(favorite, avoid)?.let { return it }
        return null
    }

/**
     * Urutan: antrian pengguna -> autoplay pintar -> daftar asal.
     * Saat offline, lagu yang belum diunduh dilewati.
     */
    internal fun RexPlayerController.__splitImpl_advance() {
        val offline = offlineMode()
        val queued = userQueue.indexOfFirst { !offline || RexOfflineManager.isDownloaded(it) }
        if (queued >= 0) {
            val track = userQueue.removeAt(queued)
            persistQueue()
            beginTrack(track)
            return
        }
        if (autoplay && !followContext) {
            playSmart()
            return
        }
        playContextNext()
    }

/** Lagu tepat sebelum lagu yang sedang diputar di riwayat (saat offline: hanya yang sudah diunduh). */
    internal fun RexPlayerController.__splitImpl_previousTarget(now: RexTrack?): RexTrack? {
        val last = history.lastOrNull()
        val list = if (now != null && last != null && sameSong(last, now)) {
            history.dropLast(1)
        } else {
            history
        }
        val offline = offlineMode()
        return list.lastOrNull { !offline || RexOfflineManager.isDownloaded(it) }
    }
