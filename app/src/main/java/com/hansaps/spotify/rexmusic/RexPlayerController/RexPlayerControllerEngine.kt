package com.hansaps.spotify.rexmusic

import kotlinx.coroutines.flow.update

/**
     * File offline dipakai kalau ada (tanpa internet). Kalau belum diunduh: saat mode offline gagal
     * dengan pesan jelas, saat online diambil lewat API. true = pemutaran dimulai.
     */
    internal suspend fun RexPlayerController.__splitImpl_resolveAndStart(track: RexTrack): Boolean {
        val local = RexOfflineManager.audioFile(track)
        if (local != null) {
            resolving = false
            _state.update { it.copy(nowPlayingOffline = true) }
            publishLoading()
            startPlayer(local.absolutePath, local = true)
            return true
        }
        if (offlineMode()) {
            resolving = false
            _state.update { it.copy(error = "Mode offline: \"${track.title}\" belum diunduh") }
            publishLoading()
            return false
        }

        val result = RexTrackResolver.resolve(track)
        resolving = false
        val fresh = result.getOrNull()
        if (fresh == null) {
            _state.update { it.copy(error = result.exceptionOrNull()?.message ?: "gagal memuat audio") }
            publishLoading()
            return false
        }
        replaceTrack(fresh)
        publishLoading()
        startPlayer(fresh.audioUrl)
        return true
    }
