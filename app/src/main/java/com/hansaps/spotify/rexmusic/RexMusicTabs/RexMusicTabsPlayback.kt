package com.hansaps.spotify.rexmusic

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp

internal fun LazyListScope.__splitImpl_trackShelf(
    key: String,
    title: String,
    tracks: List<RexTrack>,
    state: RexMusicUiState,
    onPlay: (Int) -> Unit
) {
    if (tracks.isEmpty()) return
    item(key = "$key-title") { SectionTitle(title) }
    item(key = "$key-row") {
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            itemsIndexed(tracks, key = { _, t -> "$key-${t.id}" }) { idx, t ->
                val active = t.id == state.nowPlaying?.id
                ShelfCard(t, active, active && state.isPlaying, onClick = { onPlay(idx) })
            }
        }
    }
}

@Composable
internal fun __splitImpl_TrackRowFor(
    track: RexTrack,
    state: RexMusicUiState,
    actions: RexActions,
    subtitle: String? = null,
    onClick: () -> Unit
) {
    val key = remember(track.title, track.artist) { offlineKey(track) }
    val active = track.id == state.nowPlaying?.id
    TrackRow(
        track = track,
        active = active,
        playing = active && state.isPlaying,
        loading = active && state.phase.isPlayerBusy,
        downloaded = state.offline.isDownloaded(track, key),
        status = state.offline.downloads[key],
        subtitle = subtitle,
        onClick = onClick,
        onMore = { actions.openMenu(MenuTarget(track, onClick)) }
    )
}
