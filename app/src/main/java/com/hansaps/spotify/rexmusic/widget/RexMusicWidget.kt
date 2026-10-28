package com.hansaps.spotify.rexmusic.widget

import android.content.Context
import android.graphics.Bitmap
import com.hansaps.spotify.rexmusic.RexMusicUiState

/** Placeholder adapter: the in-app player and media notification are enabled; home-screen
 * AppWidget layouts are intentionally not shipped in HansAps V1. */
class RexMusicWidget {
    companion object {
        fun updateAll(context: Context, state: RexMusicUiState, cover: Bitmap? = null, force: Boolean = false) = Unit
    }
}
