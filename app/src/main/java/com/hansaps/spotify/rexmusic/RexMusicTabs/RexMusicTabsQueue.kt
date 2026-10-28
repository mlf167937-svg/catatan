package com.hansaps.spotify.rexmusic

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp

@Composable
internal fun __splitImpl_PlaylistDetailScreen(
    playlist: RexPlaylist,
    state: RexMusicUiState,
    actions: RexActions,
    listState: LazyListState,
    wide: Boolean,
    onBack: () -> Unit,
    onDelete: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        state = listState,
        contentPadding = PaddingValues(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        item(key = "playlist-detail-toolbar") {
            Row(
                Modifier.fillMaxWidth().padding(start = 12.dp, end = 16.dp, top = 12.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali ke playlist")
                }
                Text("Detail playlist", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.DeleteOutline, contentDescription = "Hapus playlist", tint = scheme.onSurfaceVariant)
                }
                RexMusicProfileAvatarButton()
            }
        }
        item(key = "playlist-detail-hero") {
            BoxWithConstraints(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp)) {
                val detailArtworkSize = minOf(maxWidth * 0.31f, 236.dp)
                if (wide) {
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(26.dp))
                            .background(scheme.surfaceVariant).padding(22.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(24.dp)
                    ) {
                        PlaylistArtwork(playlist, Modifier.size(detailArtworkSize))
                        PlaylistDetailText(playlist, onPlay = { actions.playPlaylist(playlist.name) }, modifier = Modifier.weight(1f))
                    }
                } else {
                    Column(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(26.dp))
                            .background(scheme.surfaceVariant).padding(18.dp),
                        horizontalAlignment = Alignment.Start,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                            PlaylistArtwork(playlist, Modifier.fillMaxWidth(0.76f).aspectRatio(1f))
                        }
                        PlaylistDetailText(playlist, onPlay = { actions.playPlaylist(playlist.name) }, modifier = Modifier.fillMaxWidth())
                    }
                }
            }
        }
        item(key = "playlist-detail-list-heading") {
            Row(
                Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Lagu di playlist", style = MaterialTheme.typography.titleLarge)
                    Text(
                        "${playlist.tracks.size} lagu · pilih lagu untuk mulai mendengarkan",
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.onSurfaceVariant
                    )
                }
                if (playlist.tracks.isNotEmpty()) {
                    TextButton(onClick = { actions.playPlaylist(playlist.name) }) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text("Putar semua")
                    }
                }
            }
        }
        if (playlist.tracks.isEmpty()) {
            item(key = "playlist-detail-empty") {
                EmptyState(
                    Icons.Default.QueueMusic,
                    "Playlist masih kosong",
                    "Tambahkan lagu lewat menu ⋮ pada lagu di Beranda, Cari, atau Koleksi."
                )
            }
        } else {
            itemsIndexed(playlist.tracks, key = { index, track -> "detail-${playlist.name}-${track.id}-$index" }) { _, track ->
                TrackRowFor(track, state, actions) {
                    actions.playPlaylistTrack(playlist.name, track.id)
                }
            }
        }
    }
}
