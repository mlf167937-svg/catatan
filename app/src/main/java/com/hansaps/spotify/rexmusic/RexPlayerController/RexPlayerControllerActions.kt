package com.hansaps.spotify.rexmusic

/**
     * Paksa widget re-render tiap kali state penting berubah (lirik selesai dimuat, lagu ganti, dll).
     * Service akan mengirim cover bitmap-nya sendiri saat siap.
     */
    internal fun RexPlayerController.__splitImpl_pushWidgetUpdate() {
        if (!initialized) return
        runCatching {
            com.hansaps.spotify.rexmusic.widget.RexMusicWidget.updateAll(
                appContext,
                _state.value,
                force = true
            )
        }
    }
