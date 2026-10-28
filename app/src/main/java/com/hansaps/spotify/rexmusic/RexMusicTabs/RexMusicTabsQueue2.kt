package com.hansaps.spotify.rexmusic

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
internal fun __splitImpl_PlaylistGridCard(
    playlist: RexPlaylist,
    compact: Boolean,
    onOpen: () -> Unit,
    onPlay: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    ElevatedCard(
        modifier = modifier,
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = scheme.surfaceVariant),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp)
    ) {
        if (compact) {
            Row(
                Modifier.fillMaxWidth().clickable(onClick = onOpen).padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                PlaylistArtwork(playlist, Modifier.size(88.dp))
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text(playlist.name, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text(
                        "${playlist.tracks.size} lagu",
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.onSurfaceVariant
                    )
                    Text(
                        if (playlist.tracks.isEmpty()) "Playlist kosong" else playlist.tracks.take(2).joinToString(" · ") { it.title },
                        style = MaterialTheme.typography.labelSmall,
                        color = scheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    IconButton(onClick = onPlay, enabled = playlist.tracks.isNotEmpty()) {
                        Icon(Icons.Default.PlayCircle, contentDescription = "Putar ${playlist.name}", tint = scheme.primary, modifier = Modifier.size(30.dp))
                    }
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = "Hapus playlist ${playlist.name}", tint = scheme.onSurfaceVariant)
                    }
                }
            }
        } else {
            Column(Modifier.fillMaxWidth()) {
                Box(Modifier.fillMaxWidth().clickable(onClick = onOpen).padding(12.dp)) {
                    PlaylistArtwork(playlist, Modifier.fillMaxWidth().aspectRatio(1.12f))
                    Box(
                        Modifier.align(Alignment.BottomEnd).padding(22.dp).size(48.dp)
                            .clip(CircleShape).background(scheme.primary)
                            .clickable(enabled = playlist.tracks.isNotEmpty(), onClick = onPlay),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = "Putar ${playlist.name}", tint = scheme.onPrimary, modifier = Modifier.size(28.dp))
                    }
                }
                Row(
                    Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, bottom = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f).clickable(onClick = onOpen), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(playlist.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(
                            if (playlist.tracks.isEmpty()) "Playlist kosong" else "${playlist.tracks.size} lagu",
                            style = MaterialTheme.typography.bodySmall,
                            color = scheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = "Hapus playlist ${playlist.name}", tint = scheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
internal fun __splitImpl_PlaylistArtwork(
    playlist: RexPlaylist,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    val tracks = playlist.tracks.take(4)
    Box(
        modifier.clip(RoundedCornerShape(17.dp)).background(scheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        when {
            tracks.isEmpty() -> {
                Box(Modifier.fillMaxSize().background(RexHeroBrush), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.QueueMusic, contentDescription = null, tint = Color.White, modifier = Modifier.size(44.dp))
                }
            }
            tracks.size == 1 -> CoverImage(
                tracks.first().cover,
                Modifier.fillMaxSize(),
                shape = RoundedCornerShape(17.dp),
                contentDescription = "Sampul ${tracks.first().title}"
            )
            else -> {
                Column(
                    Modifier.fillMaxSize().padding(2.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        PlaylistArtworkTile(tracks.getOrNull(0), Modifier.weight(1f).fillMaxHeight())
                        PlaylistArtworkTile(tracks.getOrNull(1), Modifier.weight(1f).fillMaxHeight())
                    }
                    Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        PlaylistArtworkTile(tracks.getOrNull(2), Modifier.weight(1f).fillMaxHeight())
                        PlaylistArtworkTile(tracks.getOrNull(3), Modifier.weight(1f).fillMaxHeight())
                    }
                }
            }
        }
    }
}

@Composable
internal fun __splitImpl_PlaylistDetailText(
    playlist: RexPlaylist,
    onPlay: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    Column(modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            "REX MUSIC · PLAYLIST PRIBADI",
            style = MaterialTheme.typography.labelMedium,
            color = scheme.primary,
            letterSpacing = 1.1.sp
        )
        Text(
            playlist.name,
            style = MaterialTheme.typography.headlineMedium,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            "${playlist.tracks.size} lagu · koleksi pribadimu",
            style = MaterialTheme.typography.bodyMedium,
            color = scheme.onSurfaceVariant
        )
        Text(
            if (playlist.tracks.isEmpty()) "Mulai bangun playlist ini dengan lagu favoritmu." else playlist.tracks.take(3).joinToString("  ·  ") { it.title },
            style = MaterialTheme.typography.bodySmall,
            color = scheme.onSurfaceVariant,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Button(
            onClick = onPlay,
            enabled = playlist.tracks.isNotEmpty(),
            shape = RoundedCornerShape(15.dp),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 13.dp)
        ) {
            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(8.dp))
            Text("Putar playlist")
        }
    }
}
