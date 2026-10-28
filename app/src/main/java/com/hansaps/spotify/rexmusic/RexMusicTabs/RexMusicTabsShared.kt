package com.hansaps.spotify.rexmusic

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

internal fun LazyListScope.__splitImpl_onlineHome(state: RexMusicUiState, actions: RexActions) {
    val nowId = state.nowPlaying?.id
    val tiles = buildQuickTiles(state, actions)
    if (tiles.isNotEmpty()) {
        item(key = "home-grid") {
            QuickGrid(tiles, nowId, state.isPlaying, Modifier.padding(top = 8.dp))
        }
    }
    item(key = "home-mix") {
        MixHeroCard(
            state.favoriteArtists, state.popularCount, actions.playMix,
            Modifier.padding(horizontal = 16.dp, vertical = 16.dp)
        )
    }
    item(key = "home-smart-next") {
        AutoplayCard(state, actions.toggleAutoplay)
    }
    downloadsSection(state.offline.downloads, actions.cancelDownload)
    artistShelf(state, actions)
    trackShelf("popular30", "30 Lagu Pilihan", RexPopularSongs.tracks, state, actions.playPopular)
    trackShelf("hist", "Terakhir diputar", state.history, state, actions.playHistory)
    trackShelf("rec", "Rekomendasi untukmu", state.tracks, state, actions.playMain)
}

internal fun __splitImpl_buildQuickTiles(state: RexMusicUiState, actions: RexActions): List<QuickTile> {
    val tiles = mutableListOf<QuickTile>()
    if (state.offline.entries.isNotEmpty()) {
        tiles += QuickTile("special-offline", "Lagu Offline", "", special = true) { actions.goLibrary() }
    }
    if (state.playlists.isNotEmpty()) {
        tiles += QuickTile("special-playlist", "Playlist", "", special = true) { actions.goPlaylists() }
    }
    state.history.take(6).forEachIndexed { i, t ->
        tiles += QuickTile(t.id, t.title, t.cover) { actions.playHistory(i) }
    }
    state.tracks.forEachIndexed { i, t ->
        if (tiles.none { tile -> tile.id == t.id }) {
            tiles += QuickTile(t.id, t.title, t.cover) { actions.playMain(i) }
        }
    }
    return tiles.take(6)
}

internal fun LazyListScope.__splitImpl_artistShelf(state: RexMusicUiState, actions: RexActions) {
    val artists = artistPool(state, 12)
    if (artists.isEmpty()) return
    item(key = "artist-title") {
        SectionTitle(if (state.favoriteArtists.isNotEmpty()) "Artis favoritmu" else "Artis populer")
    }
    item(key = "artist-row") {
        LazyRow(
            contentPadding = PaddingValues(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(artists, key = { it }) { name ->
                ArtistCard(name, onClick = { actions.searchArtist(name) })
            }
        }
    }
}
