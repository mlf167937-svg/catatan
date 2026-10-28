package com.hansaps.spotify.rexmusic

import android.os.Build
import kotlinx.coroutines.flow.update

/** Hentikan total (tombol tutup di notifikasi). Lagu tetap tercatat, bisa diputar ulang. */
    internal fun RexPlayerController.__splitImpl_stop() {
        cancelPending()
        resolving = false
        stopPlayer()
        _state.update {
            it.copy(
                isPlaying = false, positionMs = 0L, durationMs = 0L, bufferedPercent = 0,
                repeatTotal = 0, repeatLeft = 0
            )
        }
        publishLoading()
    }

internal fun RexPlayerController.__splitImpl_popularPick(avoid: Set<String>): RexTrack? {
        val all = RexPopularSongs.tracks
        if (all.isEmpty()) return null
        val nowKey = _state.value.nowPlaying?.let { songKey(it) }
        return all.filter { songKey(it) !in avoid }.randomOrNull()
            ?: all.filter { songKey(it) != nowKey }.randomOrNull()
    }

internal fun RexPlayerController.__splitImpl_abandonFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            focusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
        } else {
            @Suppress("DEPRECATION")
            audioManager.abandonAudioFocus(focusListener)
        }
    }

/** "Putar semua" / "Acak" di perpustakaan offline. */
    internal fun RexPlayerController.__splitImpl_playOfflineAll(shuffle: Boolean) {
        val list = RexOfflineManager.state.value.entries.map { it.track }
        if (list.isEmpty()) {
            postNotice("Belum ada lagu offline")
            return
        }
        startQueue(if (shuffle) list.shuffled() else list, 0, follow = true)
    }

/** Prepare a local track for synchronized playback without emitting audio before the room target time. */
    internal fun RexPlayerController.__splitImpl_prepareFromOfflineForSync(track: RexTrack) {
        val list = RexOfflineManager.state.value.entries.map { it.track }
        val index = list.indexOfFirst { it.id == track.id }
        if (index < 0) return
        syncPrepareTrackId = track.id
        startQueue(list, index, follow = true)
    }

internal suspend fun RexPlayerController.__splitImpl_pickFromArtist(artist: String, avoid: Set<String>): RexTrack? {
        val list = RexMusicApi.search(artist).getOrNull() ?: return null
        val key = artist.lowercase()
        return list
            .filter { it.artist.lowercase().contains(key) && songKey(it) !in avoid }
            .randomOrNull()
    }

internal fun RexPlayerController.__splitImpl_penalizeEarlySkip() {
        val now = _state.value.nowPlaying ?: return
        if (prepared && !tasteCounted && _state.value.positionMs < EARLY_SKIP_MS) {
            taste.recordSkip(now.artist)
            publishTaste()
        }
    }

/** Putar lagu dari perpustakaan offline; Next mengikuti urutan perpustakaan. */
    internal fun RexPlayerController.__splitImpl_playFromOffline(track: RexTrack) {
        val list = RexOfflineManager.state.value.entries.map { it.track }
        val index = list.indexOfFirst { it.id == track.id }
        if (index < 0) return
        startQueue(list, index, follow = true)
    }

internal fun RexPlayerController.__splitImpl_setOfflineMode(on: Boolean) {
        RexOfflineManager.setOfflineMode(on)
        clearSearch()
        postNotice(if (on) "Mode offline aktif. Hanya lagu unduhan yang diputar" else "Kembali online")
    }

internal fun RexPlayerController.__splitImpl_recentKeys(): Set<String> {
        val keys = history.takeLast(RECENT_WINDOW).map { songKey(it) }.toMutableSet()
        _state.value.nowPlaying?.let { keys += songKey(it) }
        return keys
    }

internal fun RexPlayerController.__splitImpl_setAutoplay(on: Boolean) {
        autoplay = on
        taste.autoplay = on
        publishQueueInfo()
        postNotice(if (on) "Autoplay pintar aktif" else "Mengikuti urutan daftar")
    }

internal fun RexPlayerController.__splitImpl_onOfflineChanged(o: OfflineState) {
        val modeChanged = _state.value.offline.enabled != o.enabled
        _state.update { it.copy(offline = o) }
        if (modeChanged) publishQueueInfo()
    }

internal fun RexPlayerController.__splitImpl_pauseInternal() {
        if (prepared) runCatching { player?.pause() }
        buffering = false
        _state.update { it.copy(isPlaying = false) }
        publishLoading()
    }

internal fun RexPlayerController.__splitImpl_fail(message: String) {
        preparing = false
        buffering = false
        _state.update { it.copy(error = message, isPlaying = false) }
        publishLoading()
    }

internal fun RexPlayerController.__splitImpl_clearSearch() {
        searchJob?.cancel()
        searching = false
        _state.update { it.copy(searchResults = emptyList()) }
        publishLoading()
    }

internal fun RexPlayerController.__splitImpl_playFromSearch(index: Int) {
        val list = _state.value.searchResults
        if (index !in list.indices) return
        startQueue(list, index)
    }

internal fun RexPlayerController.__splitImpl_playFromMain(index: Int) {
        val list = _state.value.tracks
        if (index !in list.indices) return
        startQueue(list, index)
    }

/** Ulangi lagu yang sedang diputar sebanyak [times] kali lagi (0 = mati, maks 50). */
    internal fun RexPlayerController.__splitImpl_setRepeat(times: Int) {
        val n = times.coerceIn(0, MAX_REPEAT)
        _state.update { it.copy(repeatTotal = n, repeatLeft = n) }
    }

internal fun RexPlayerController.__splitImpl_sameSong(a: RexTrack, b: RexTrack): Boolean =
        a.id == b.id ||
            (a.title.equals(b.title, ignoreCase = true) && a.artist.equals(b.artist, ignoreCase = true))

internal fun RexPlayerController.__splitImpl_setBuffering(value: Boolean) {
        if (buffering == value) return
        buffering = value
        publishLoading()
    }

/** Mix pintar: langsung pilih lagu dari artis favorit / populer (atau dari unduhan saat offline). */
    internal fun RexPlayerController.__splitImpl_playMix() {
        cancelPending()
        followContext = false
        playSmart()
    }

internal fun RexPlayerController.__splitImpl_publishTaste() {
        _state.update { it.copy(favoriteArtists = taste.favorites(3)) }
    }

/** Tombol next dari pengguna. */
    internal fun RexPlayerController.__splitImpl_next() {
        penalizeEarlySkip()
        cancelPending()
        advance()
    }
