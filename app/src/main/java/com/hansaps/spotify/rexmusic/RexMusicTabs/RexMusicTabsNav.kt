package com.hansaps.spotify.rexmusic

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
internal fun __splitImpl_PlaylistTab(
    state: RexMusicUiState,
    actions: RexActions,
    listState: LazyListState,
    selectedPlaylistName: String?,
    onOpenPlaylist: (String) -> Unit,
    onBackFromDetail: () -> Unit,
    onRequestDeletePlaylist: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showCreate by remember { mutableStateOf(false) }
    var playlistName by remember { mutableStateOf("") }
    var playlistQuery by rememberSaveable { mutableStateOf("") }
    val detailListState = androidx.compose.foundation.lazy.rememberLazyListState()
    val selectedPlaylist = state.playlists.firstOrNull { it.name == selectedPlaylistName }

    LaunchedEffect(selectedPlaylistName) {
        if (selectedPlaylistName != null) detailListState.scrollToItem(0)
    }
    LaunchedEffect(selectedPlaylistName, state.playlists) {
        if (selectedPlaylistName != null && selectedPlaylist == null) onBackFromDetail()
    }

    BoxWithConstraints(modifier.fillMaxSize()) {
        val columns = when {
            maxWidth >= 1120.dp -> 3
            maxWidth >= 650.dp -> 2
            else -> 1
        }
        val wideDetail = maxWidth >= 720.dp

        if (selectedPlaylist != null) {
            PlaylistDetailScreen(
                playlist = selectedPlaylist,
                state = state,
                actions = actions,
                listState = detailListState,
                wide = wideDetail,
                onBack = onBackFromDetail,
                onDelete = { onRequestDeletePlaylist(selectedPlaylist.name) }
            )
        } else {
            val filteredPlaylists = remember(state.playlists, playlistQuery) {
                val query = playlistQuery.trim()
                if (query.isBlank()) state.playlists
                else state.playlists.filter { playlist ->
                    playlist.name.contains(query, ignoreCase = true) ||
                        playlist.tracks.any { it.title.contains(query, true) || it.artist.contains(query, true) }
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                state = listState,
                contentPadding = PaddingValues(bottom = 30.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                item(key = "playlist-page-header") {
                    Column(
                        Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(9.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            FilledTonalButton(
                                onClick = { showCreate = true },
                                shape = RoundedCornerShape(15.dp),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 11.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(19.dp))
                                Spacer(Modifier.width(7.dp))
                                Text("Tambahkan Playlist", maxLines = 1)
                            }
                            Spacer(Modifier.weight(1f))
                            RexMusicProfileAvatarButton()
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                "KOLEKSI MUSIK",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                letterSpacing = 1.2.sp
                            )
                            Text("Playlist", style = MaterialTheme.typography.headlineLarge)
                            Text(
                                "Susun lagu sesuai suasana, aktivitas, atau momen favoritmu.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            "${state.playlists.size} playlist · tersimpan di perangkat",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                item(key = "playlist-search") {
                    OutlinedTextField(
                        value = playlistQuery,
                        onValueChange = { playlistQuery = it },
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        trailingIcon = {
                            if (playlistQuery.isNotBlank()) {
                                IconButton(onClick = { playlistQuery = "" }) {
                                    Icon(Icons.Default.Close, contentDescription = "Hapus pencarian")
                                }
                            }
                        },
                        placeholder = { Text("Cari playlist atau lagu di dalamnya") }
                    )
                }

                if (state.playlists.isEmpty()) {
                    item(key = "playlist-empty") {
                        PlaylistEmptyState(onCreate = { showCreate = true })
                    }
                } else if (filteredPlaylists.isEmpty()) {
                    item(key = "playlist-no-search-results") {
                        EmptyState(
                            Icons.Default.Search,
                            "Playlist tidak ditemukan",
                            "Coba kata kunci lain atau hapus pencarian."
                        )
                    }
                } else {
                    item(key = "playlist-list-heading") {
                        Row(
                            Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Koleksi kamu", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                            Text("${filteredPlaylists.size} item", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    items(
                        items = filteredPlaylists.chunked(columns),
                        key = { row -> "playlist-row-${row.firstOrNull()?.name.orEmpty()}" }
                    ) { rowPlaylists ->
                        Row(
                            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            rowPlaylists.forEach { playlist ->
                                PlaylistGridCard(
                                    playlist = playlist,
                                    compact = columns == 1,
                                    onOpen = { onOpenPlaylist(playlist.name) },
                                    onPlay = { actions.playPlaylist(playlist.name) },
                                    onDelete = { onRequestDeletePlaylist(playlist.name) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            repeat((columns - rowPlaylists.size).coerceAtLeast(0)) {
                                Spacer(Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreate) {
        AlertDialog(
            onDismissRequest = { showCreate = false },
            icon = { Icon(Icons.Default.QueueMusic, contentDescription = null) },
            title = { Text("Buat playlist") },
            text = {
                OutlinedTextField(
                    value = playlistName,
                    onValueChange = { playlistName = it },
                    singleLine = true,
                    label = { Text("Nama playlist") },
                    placeholder = { Text("Santai, Workout, Favorit...") }
                )
            },
            confirmButton = {
                TextButton(
                    enabled = playlistName.trim().isNotEmpty(),
                    onClick = {
                        actions.createPlaylist(playlistName.trim())
                        playlistName = ""
                        showCreate = false
                    }
                ) { Text("Buat playlist") }
            },
            dismissButton = { TextButton(onClick = { showCreate = false }) { Text("Batal") } }
        )
    }
}
