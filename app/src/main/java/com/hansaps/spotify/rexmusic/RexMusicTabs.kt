package com.hansaps.spotify.rexmusic

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector

//  Shared row helper 

@Composable
internal fun TrackRowFor(
    track: RexTrack,
    state: RexMusicUiState,
    actions: RexActions,
    subtitle: String? = null,
    onClick: () -> Unit
) {
        return __splitImpl_TrackRowFor(track, state, actions, subtitle, onClick)
    }

internal fun artistPool(state: RexMusicUiState, limit: Int): List<String> =
    (state.favoriteArtists + RexPopularSongs.tracks.map { primaryArtist(it.artist) })
        .filter { it.isNotBlank() }
        .distinct()
        .take(limit)

//  HOME 

@Composable
fun HomeTab(
    state: RexMusicUiState,
    actions: RexActions,
    listState: LazyListState,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
        return __splitImpl_HomeTab(state, actions, listState, onBack, modifier)
    }

@Composable
internal fun HomeHeader(greeting: String, offline: Boolean, onToggle: () -> Unit, onBack: () -> Unit) {
        return __splitImpl_HomeHeader(greeting, offline, onToggle, onBack)
    }

internal fun LazyListScope.onlineHome(state: RexMusicUiState, actions: RexActions) {
        return this.__splitImpl_onlineHome(state, actions)
    }

internal fun buildQuickTiles(state: RexMusicUiState, actions: RexActions): List<QuickTile> {
        return __splitImpl_buildQuickTiles(state, actions)
    }

internal fun LazyListScope.trackShelf(
    key: String,
    title: String,
    tracks: List<RexTrack>,
    state: RexMusicUiState,
    onPlay: (Int) -> Unit
) {
        return this.__splitImpl_trackShelf(key, title, tracks, state, onPlay)
    }

internal fun LazyListScope.artistShelf(state: RexMusicUiState, actions: RexActions) {
        return this.__splitImpl_artistShelf(state, actions)
    }

@Composable
internal fun MixHeroCard(
    favorites: List<String>,
    popularCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
        return __splitImpl_MixHeroCard(favorites, popularCount, onClick, modifier)
    }

//  SEARCH 

@Composable
fun SearchTab(
    state: RexMusicUiState,
    query: String,
    typing: Boolean,
    onQueryChange: (String) -> Unit,
    onClear: () -> Unit,
    actions: RexActions,
    listState: LazyListState,
    modifier: Modifier = Modifier
) {
        return __splitImpl_SearchTab(state, query, typing, onQueryChange, onClear, actions, listState, modifier)
    }

@Composable
internal fun SearchField(
    query: String,
    onChange: (String) -> Unit,
    onClear: () -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier
) {
        return __splitImpl_SearchField(query, onChange, onClear, placeholder, modifier)
    }

internal fun LazyListScope.exploreSection(state: RexMusicUiState, actions: RexActions) {
        return this.__splitImpl_exploreSection(state, actions)
    }

@Composable
internal fun ExploreTile(name: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
        return __splitImpl_ExploreTile(name, onClick, modifier)
    }

internal fun LazyListScope.resultsSection(state: RexMusicUiState, actions: RexActions, typing: Boolean) {
        return this.__splitImpl_resultsSection(state, actions, typing)
    }

//  LIBRARY 

@Composable
fun LibraryTab(
    state: RexMusicUiState,
    libTab: Int,
    onLibTab: (Int) -> Unit,
    actions: RexActions,
    listState: LazyListState,
    modifier: Modifier = Modifier
) {
        return __splitImpl_LibraryTab(state, libTab, onLibTab, actions, listState, modifier)
    }

@Composable
fun PlaylistTab(
    state: RexMusicUiState,
    actions: RexActions,
    listState: LazyListState,
    selectedPlaylistName: String?,
    onOpenPlaylist: (String) -> Unit,
    onBackFromDetail: () -> Unit,
    onRequestDeletePlaylist: (String) -> Unit,
    modifier: Modifier = Modifier
) {
        return __splitImpl_PlaylistTab(state, actions, listState, selectedPlaylistName, onOpenPlaylist, onBackFromDetail, onRequestDeletePlaylist, modifier)
    }

@Composable
internal fun PlaylistGridCard(
    playlist: RexPlaylist,
    compact: Boolean,
    onOpen: () -> Unit,
    onPlay: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
        return __splitImpl_PlaylistGridCard(playlist, compact, onOpen, onPlay, onDelete, modifier)
    }

@Composable
internal fun PlaylistArtwork(
    playlist: RexPlaylist,
    modifier: Modifier = Modifier
) {
        return __splitImpl_PlaylistArtwork(playlist, modifier)
    }

@Composable
internal fun PlaylistArtworkTile(track: RexTrack?, modifier: Modifier = Modifier) {
        return __splitImpl_PlaylistArtworkTile(track, modifier)
    }

@Composable
internal fun PlaylistDetailScreen(
    playlist: RexPlaylist,
    state: RexMusicUiState,
    actions: RexActions,
    listState: LazyListState,
    wide: Boolean,
    onBack: () -> Unit,
    onDelete: () -> Unit
) {
        return __splitImpl_PlaylistDetailScreen(playlist, state, actions, listState, wide, onBack, onDelete)
    }

@Composable
internal fun PlaylistDetailText(
    playlist: RexPlaylist,
    onPlay: () -> Unit,
    modifier: Modifier = Modifier
) {
        return __splitImpl_PlaylistDetailText(playlist, onPlay, modifier)
    }

@Composable
internal fun PlaylistEmptyState(onCreate: () -> Unit) {
        return __splitImpl_PlaylistEmptyState(onCreate)
    }


internal fun LazyListScope.historySection(state: RexMusicUiState, actions: RexActions) {
        return this.__splitImpl_historySection(state, actions)
    }

//  OFFLINE (dipakai Home offline, Cari offline, Koleksi) 

fun LazyListScope.offlineSection(
    state: RexMusicUiState,
    actions: RexActions,
    query: String,
    full: Boolean
) {
        return this.__splitImpl_offlineSection(state, actions, query, full)
    }

fun LazyListScope.downloadsSection(
    downloads: Map<String, DownloadStatus>,
    onCancel: (RexTrack) -> Unit
) {
    if (downloads.isEmpty()) return
    item(key = "dl-title") { SectionTitle("Sedang diunduh  ${downloads.size}") }
    items(downloads.entries.toList(), key = { "dl-${it.key}" }) { entry ->
        DownloadRow(entry.value) { onCancel(entry.value.track) }
    }
}

@Composable
internal fun OfflineHeroCard(
    count: Int,
    bytes: Long,
    enabled: Boolean,
    onPlayAll: () -> Unit,
    onShuffle: () -> Unit,
    modifier: Modifier = Modifier
) {
        return __splitImpl_OfflineHeroCard(count, bytes, enabled, onPlayAll, onShuffle, modifier)
    }

@Composable
internal fun HeroButton(
    label: String,
    icon: ImageVector,
    filled: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
        return __splitImpl_HeroButton(label, icon, filled, enabled, onClick, modifier)
    }

@Composable
internal fun PermissionCard(onClick: () -> Unit, modifier: Modifier = Modifier) {
        return __splitImpl_PermissionCard(onClick, modifier)
    }

@Composable
internal fun LibraryChip(
    label: String,
    count: Int,
    selected: Boolean,
    onClick: () -> Unit
) {
        return __splitImpl_LibraryChip(label, count, selected, onClick)
    }
