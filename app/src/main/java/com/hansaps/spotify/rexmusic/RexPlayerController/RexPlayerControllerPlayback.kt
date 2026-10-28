package com.hansaps.spotify.rexmusic

import android.media.MediaPlayer
import android.os.PowerManager
import java.io.File
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** [source] = URL audio (online) atau path file (local = true). */
    internal fun RexPlayerController.__splitImpl_startPlayer(source: String, local: Boolean = false) {
        if (source.isBlank()) return fail("URL audio kosong")
        stopPlayer()
        if (!requestFocus()) return fail("audio sedang dipakai aplikasi lain")
        try {
            val mp = MediaPlayer()
            player = mp
            runCatching { mp.setVolume(syncVolume, syncVolume) }
            preparing = true
            publishLoading()
            mp.setAudioAttributes(audioAttrs)
            runCatching { mp.setWakeMode(appContext, PowerManager.PARTIAL_WAKE_LOCK) }
            if (local) {
                // Use the path overload for legacy Android (including Android 8).
                // Keeping an FD open only until setDataSource returns can trigger
                // vendor-specific native MediaPlayer failures during prepareAsync.
                mp.setDataSource(File(source).absolutePath)
            } else {
                mp.setDataSource(source)
            }
            mp.setOnPreparedListener {
                if (player !== mp) return@setOnPreparedListener
                prepared = true
                preparing = false
                buffering = false
                val preparePaused = syncPrepareTrackId == _state.value.nowPlaying?.id
                if (preparePaused) syncPrepareTrackId = null
                _state.update { s -> s.copy(durationMs = mp.duration.toLong(), isPlaying = !preparePaused) }
                publishLoading()
                // MediaPlayer is already stopped before its first start after onPrepared.
                // Calling pause() in this state can raise MEDIA_ERROR_INVALID_OPERATION (-38).
                // For sync preparation, leave it prepared and silent until startSyncPlayback().
                if (!preparePaused) mp.start()
                startProgressLoop(mp)
                _state.value.nowPlaying?.let { pushHistory(it) }
            }
            mp.setOnBufferingUpdateListener { _, percent ->
                if (player !== mp) return@setOnBufferingUpdateListener
                _state.update { s -> s.copy(bufferedPercent = percent.coerceIn(0, 100)) }
            }
            mp.setOnInfoListener { _, what, _ ->
                if (player === mp) {
                    when (what) {
                        MediaPlayer.MEDIA_INFO_BUFFERING_START -> updateBuffering(true)
                        MediaPlayer.MEDIA_INFO_BUFFERING_END -> updateBuffering(false)
                    }
                }
                false
            }
            mp.setOnCompletionListener {
                if (player !== mp) return@setOnCompletionListener
                val left = _state.value.repeatLeft
                if (left > 0) {
                    // ulangi lagu yang sama tanpa memanggil API lagi
                    _state.value.nowPlaying?.let { taste.recordPlay(it.artist, REPEAT_TASTE_WEIGHT) }
                    _state.update { s ->
                        s.copy(
                            repeatLeft = left - 1, positionMs = 0L, isPlaying = true,
                            seekVersion = s.seekVersion + 1
                        )
                    }
                    runCatching {
                        mp.seekTo(0)
                        mp.start()
                    }
                } else {
                    _state.update { s -> s.copy(isPlaying = false, repeatTotal = 0, repeatLeft = 0) }
                    cancelPending()
                    advance()
                }
            }
            mp.setOnErrorListener { _, what, extra ->
                // Only invalidate resolved network audio. A local MP3 error should
                // not evict any cached API URL for the same track.
                if (!local) {
                    queue.getOrNull(queueIndex)?.let { RexMusicApi.invalidateAudio(it) }
                    _state.value.nowPlaying?.let { RexMusicApi.invalidateAudio(it) }
                }
                stopPlayer() // supaya tombol play bisa mencoba ulang
                fail("gagal memutar audio (kode $what, detail $extra)")
                true
            }
            mp.prepareAsync()
        } catch (e: Exception) {
            stopPlayer()
            fail(e.message ?: "gagal memutar audio")
        }
    }

/** Start the already-prepared player at the requested sync position. Must be called on main. */
    internal fun RexPlayerController.__splitImpl_startSyncPlayback(positionMs: Long) {
        val mp = player ?: return
        if (!prepared || !requestFocus()) return
        val d = _state.value.durationMs
        val target = positionMs.coerceIn(0L, d).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
        seekJob?.cancel()
        seekPending = false
        if (kotlin.math.abs(_state.value.positionMs - target.toLong()) <= 25L) {
            runCatching { if (!mp.isPlaying) mp.start() }
            _state.update { it.copy(isPlaying = true) }
            return
        }
        runCatching {
            mp.setOnSeekCompleteListener { completed ->
                if (player === completed) {
                    runCatching { if (!completed.isPlaying) completed.start() }
                    _state.update { it.copy(positionMs = target.toLong(), isPlaying = true) }
                    completed.setOnSeekCompleteListener(null)
                }
            }
            mp.seekTo(target)
        }.onFailure {
            runCatching { if (!mp.isPlaying) mp.start() }
            _state.update { it.copy(isPlaying = true) }
        }
    }

/** Set state "lagu baru" (reset ulangi, posisi, dsb). Tidak menyentuh player. */
    internal fun RexPlayerController.__splitImpl_markTrack(track: RexTrack) {
        tasteCounted = false
        resolving = true
        resolveText = when {
            RexOfflineManager.isDownloaded(track) -> "membuka dari penyimpanan..."
            track.spotifyUrl.isBlank() -> "mencari ${track.title}..."
            else -> "menyiapkan audio..."
        }
        _state.update {
            it.copy(
                nowPlaying = track, currentIndex = queueIndex,
                isPlaying = false, positionMs = 0L, durationMs = 0L,
                bufferedPercent = 0, seekVersion = it.seekVersion + 1, error = null,
                repeatTotal = 0, repeatLeft = 0, nowPlayingOffline = false
            )
        }
        publishLoading()
        publishQueueInfo()
        RexMusicService.start(appContext)
    }

/** Seek absolut, dipakai synced lyrics supaya baris yang diketuk tepat ke timestamp API. */
    internal fun RexPlayerController.__splitImpl_seekToPosition(positionMs: Long) {
        val mp = player ?: return
        val d = _state.value.durationMs
        if (!prepared || d <= 0L) return
        val target = positionMs.coerceIn(0L, d)
        seekPending = true
        _state.update { it.copy(positionMs = target, seekVersion = it.seekVersion + 1) }
        seekJob?.cancel()
        seekJob = scope.launch {
            delay(SEEK_DEBOUNCE_MS)
            runCatching { mp.seekTo(target.coerceAtMost(Int.MAX_VALUE.toLong()).toInt()) }
            delay(180)
            seekPending = false
        }
    }

/** Immediate seek for sync correction; unlike the UI drag path, this is not debounced. */
    internal fun RexPlayerController.__splitImpl_seekToPositionImmediate(positionMs: Long) {
        val mp = player ?: return
        val d = _state.value.durationMs
        if (!prepared || d <= 0L) return
        val target = positionMs.coerceIn(0L, d).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
        seekJob?.cancel()
        seekPending = false
        runCatching { mp.seekTo(target) }
        _state.update { it.copy(positionMs = target.toLong(), seekVersion = it.seekVersion + 1) }
    }

/** Update track di queue + semua list berdasarkan id (bukan index, jadi anti out-of-bounds). */
    internal fun RexPlayerController.__splitImpl_replaceTrack(fresh: RexTrack) {
        queue = queue.replaceById(fresh)
        _state.update {
            it.copy(
                nowPlaying = fresh,
                tracks = it.tracks.replaceById(fresh),
                searchResults = it.searchResults.replaceById(fresh),
                history = it.history.replaceById(fresh)
            )
        }
    }

internal fun RexPlayerController.__splitImpl_stopPlayer() {
        progressJob?.cancel()
        seekJob?.cancel()
        seekPending = false
        prepared = false
        preparing = false
        buffering = false
        runCatching { player?.release() }
        player = null
        abandonFocus()
    }

internal fun RexPlayerController.__splitImpl_beginTrack(track: RexTrack) {
        stopPlayer()
        markTrack(track)
        loadLyrics(track)
        playJob = scope.launch { resolveAndStart(track) }
    }

/** Posisi UI langsung update; seek asli didebounce supaya drag tidak membebani MediaPlayer. */
    internal fun RexPlayerController.__splitImpl_seekTo(ratio: Float) {
        val d = _state.value.durationMs
        if (d <= 0L) return
        seekToPosition((d * ratio.coerceIn(0f, 1f)).toLong())
    }

/** Local playback gain for RexMusicSync; does not change Android system volume. */
    internal fun RexPlayerController.__splitImpl_setSyncVolume(volume: Float) {
        syncVolume = volume.coerceIn(0f, 1f)
        runCatching { player?.setVolume(syncVolume, syncVolume) }
    }
