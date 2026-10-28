package com.hansaps.spotify.rexmusic

/** Catat lagu ke riwayat (tanpa audioUrl). Dipanggil setelah lagu benar-benar mulai diputar. */
    internal fun RexPlayerController.__splitImpl_pushHistory(track: RexTrack) {
        val entry = track.copy(audioUrl = "")
        history.removeAll { sameSong(it, entry) }
        history.add(entry)
        while (history.size > MAX_HISTORY) history.removeAt(0)
        store.save(history)
        publishQueueInfo()
    }

/** index mengacu ke state.history (terbaru di depan). Lagu yang sudah diunduh diputar dari file. */
    internal fun RexPlayerController.__splitImpl_playFromHistory(index: Int) {
        val list = _state.value.history
        if (index !in list.indices) return
        startQueue(list, index)
    }

internal fun RexPlayerController.__splitImpl_clearHistory() {
        history.clear()
        store.save(history)
        publishQueueInfo()
    }
