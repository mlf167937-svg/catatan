package com.hansaps.spotify.rexmusic

import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal fun RexPlayerController.__splitImpl_loadLyrics(track: RexTrack) {
        lyricsJob?.cancel()
        val offline = RexOfflineManager.readOfflineLyrics(track)
        if (offline != null && !offline.isEmpty) {
            _state.update { it.copy(lyrics = offline, lyricsLoading = false, lyricsError = null) }
            pushWidgetUpdate()
            return
        }
        if (offlineMode()) {
            _state.update {
                it.copy(
                    lyrics = Lyrics(),
                    lyricsLoading = false,
                    lyricsError = "Lirik offline belum tersedia untuk lagu ini"
                )
            }
            pushWidgetUpdate()
            return
        }
        _state.update {
            it.copy(lyrics = Lyrics(), lyricsLoading = true, lyricsError = null)
        }
        pushWidgetUpdate()
        val q = "${track.artist} ${track.title}".trim()
        lyricsJob = scope.launch {
            RexMusicApi.fetchLyrics(q)
                .onSuccess { lyr ->
                    _state.update { it.copy(lyrics = lyr, lyricsLoading = false, lyricsError = null) }
                    pushWidgetUpdate()
                }
                .onFailure { e ->
                    _state.update {
                        it.copy(
                            lyricsLoading = false,
                            lyricsError = e.message ?: "gagal memuat lirik"
                        )
                    }
                    pushWidgetUpdate()
                }
        }
    }

internal fun RexPlayerController.__splitImpl_toggleLyrics() = _state.update { it.copy(lyricsVisible = !it.lyricsVisible) }
