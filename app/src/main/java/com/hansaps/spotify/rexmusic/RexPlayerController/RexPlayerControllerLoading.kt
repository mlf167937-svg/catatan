package com.hansaps.spotify.rexmusic

import kotlinx.coroutines.flow.update

/**
     * loading = search / resolve / prepare.
     * Buffering (lag saat memutar) hanya lewat [LoadPhase] supaya halaman browse tidak ikut loading.
     */
    internal fun RexPlayerController.__splitImpl_publishLoading() = _state.update {
        val phase = when {
            resolving -> LoadPhase.Resolving
            preparing -> LoadPhase.Preparing
            buffering -> LoadPhase.Buffering
            searching -> LoadPhase.Searching
            else -> LoadPhase.Idle
        }
        it.copy(
            loading = searching || resolving || preparing,
            phase = phase,
            loadingText = when (phase) {
                LoadPhase.Resolving -> resolveText
                LoadPhase.Preparing -> "menyiapkan audio..."
                LoadPhase.Buffering -> "buffering..."
                LoadPhase.Searching -> "mencari..."
                LoadPhase.Idle -> ""
            }
        )
    }
