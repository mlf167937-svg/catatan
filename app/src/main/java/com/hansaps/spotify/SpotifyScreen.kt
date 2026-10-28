package com.hansaps.spotify

import androidx.compose.runtime.Composable
import com.hansaps.spotify.rexmusic.RexMusicScreen

/** HansAps Spotify entry point. V1's full music experience follows the supplied RexMusic source. */
@Composable
fun SpotifyScreen(onBack: () -> Unit) {
    RexMusicScreen(
        onBack = onBack,
        onOpenLocalProfile = onBack,
    )
}
