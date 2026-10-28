package com.hansaps.spotify.SpotifyScreen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.QueueMusic
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.session.MediaController
import com.hansaps.core.HansColors
import com.hansaps.core.LocalTrack
import com.hansaps.core.MusicPlaylist
import com.hansaps.core.SectionLabel
import com.hansaps.core.SoftCard
import com.hansaps.core.formatTime

@Composable
fun SpotifyHomeScreen(
    tracks: List<LocalTrack>,
    playlists: List<MusicPlaylist>,
    controller: MediaController?,
    positionMs: Float,
    durationMs: Float,
    onPlayTrack: (LocalTrack) -> Unit,
    onToggle: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onSeek: (Float) -> Unit,
    onOpenPlaylists: () -> Unit,
    onOpenCollection: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(modifier = modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(17.dp)) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("THE HANSAPS SOUND", color = HansColors.Lime, fontSize = 10.sp, letterSpacing = 1.8.sp, fontWeight = FontWeight.Bold)
                Text("Made for your\nown rhythm.", color = HansColors.Text, fontSize = 29.sp, lineHeight = 32.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.8).sp)
                Text("Play locally. Keep your library yours.", color = HansColors.Muted, fontSize = 13.sp)
            }
        }
        item {
            PlayerFeatureCard(
                title = controller?.currentMediaMetadata?.title?.toString() ?: "Lesung Pipi",
                artist = controller?.currentMediaMetadata?.artist?.toString() ?: "Raim Laode",
                isPlaying = controller?.isPlaying == true,
                positionMs = positionMs,
                durationMs = durationMs,
                onToggle = onToggle,
                onPrevious = onPrevious,
                onNext = onNext,
                onSeek = onSeek,
            )
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text("Playlist kamu", color = HansColors.Text, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                    Text("${playlists.size} daftar", color = HansColors.Muted, fontSize = 11.sp)
                }
                TextButton(onClick = onOpenPlaylists) { Text("Lihat semua", color = HansColors.Lime, fontSize = 11.sp) }
            }
        }
        items(playlists.take(3), key = { it.name }) { playlist ->
            Surface(
                onClick = onOpenPlaylists,
                shape = RoundedCornerShape(18.dp),
                color = HansColors.Surface,
                border = androidx.compose.foundation.BorderStroke(1.dp, HansColors.Border),
            ) {
                Row(Modifier.fillMaxWidth().padding(13.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(Modifier.size(46.dp).clip(RoundedCornerShape(14.dp)).background(Brush.linearGradient(listOf(HansColors.Coral, Color(0xFF7D2F28)))), contentAlignment = Alignment.Center) {
                        Icon(Icons.Rounded.QueueMusic, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(playlist.name, color = HansColors.Text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        Text("${playlist.tracks.size} lagu • pilih untuk membuka", color = HansColors.Muted, fontSize = 11.sp)
                    }
                }
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text("Koleksi offline", color = HansColors.Text, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                    Text("${tracks.size} lagu", color = HansColors.Muted, fontSize = 11.sp)
                }
                TextButton(onClick = onOpenCollection) { Text("Buka koleksi", color = HansColors.Lime, fontSize = 11.sp) }
            }
        }
        if (tracks.isEmpty()) {
            item {
                SoftCard {
                    Text("Belum ada musik offline", color = HansColors.Text, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(4.dp))
                    Text("Impor file audio di tab Koleksi. HansAps akan menyimpannya di Download/HansAps/Spotify/.", color = HansColors.Muted, fontSize = 12.sp, lineHeight = 18.sp)
                }
            }
        } else {
            items(tracks.take(4), key = { it.id }) { track ->
                TrackRow(track = track, onClick = { onPlayTrack(track) }, showPlay = true)
            }
        }
    }
}

@Composable
private fun PlayerFeatureCard(
    title: String,
    artist: String,
    isPlaying: Boolean,
    positionMs: Float,
    durationMs: Float,
    onToggle: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onSeek: (Float) -> Unit,
) {
    Box(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(
            Brush.linearGradient(listOf(Color(0xFFCB351E), Color(0xFFDA4328), Color(0xFF8B2D24)))
        ),
    ) {
        Box(Modifier.align(Alignment.TopEnd).size(165.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.045f)))
        Column(Modifier.padding(horizontal = 17.dp, vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    Icon(Icons.Rounded.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
                    Text("HANSAPS  •  POPULER", color = Color.White.copy(alpha = 0.92f), fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.0.sp)
                }
                Surface(color = Color.White.copy(alpha = 0.16f), shape = CircleShape) {
                    Text("LOCAL PLAYER", modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp), color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, color = Color.White, fontSize = 23.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(artist, color = Color.White.copy(alpha = 0.85f), fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                IconButton(onClick = onPrevious, modifier = Modifier.size(42.dp)) { Icon(Icons.Rounded.SkipPrevious, contentDescription = "Sebelumnya", tint = Color.White, modifier = Modifier.size(28.dp)) }
                Surface(onClick = onToggle, shape = CircleShape, color = Color.White) {
                    Box(Modifier.size(49.dp), contentAlignment = Alignment.Center) {
                        Icon(if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, contentDescription = if (isPlaying) "Jeda" else "Putar", tint = Color(0xFFB83221), modifier = Modifier.size(30.dp))
                    }
                }
                IconButton(onClick = onNext, modifier = Modifier.size(42.dp)) { Icon(Icons.Rounded.SkipNext, contentDescription = "Berikutnya", tint = Color.White, modifier = Modifier.size(28.dp)) }
            }
            Slider(
                value = positionMs.coerceIn(0f, durationMs.coerceAtLeast(1f)),
                onValueChange = onSeek,
                valueRange = 0f..durationMs.coerceAtLeast(1f),
                modifier = Modifier.fillMaxWidth().height(20.dp),
                colors = androidx.compose.material3.SliderDefaults.colors(
                    thumbColor = Color.White,
                    activeTrackColor = Color.White,
                    inactiveTrackColor = Color.White.copy(alpha = 0.35f),
                ),
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(formatTime(positionMs.toLong()), color = Color.White, fontSize = 11.sp)
                Text(formatTime(durationMs.toLong()), color = Color.White, fontSize = 11.sp)
            }
        }
    }
}

@Composable
fun TrackRow(track: LocalTrack, onClick: () -> Unit, showPlay: Boolean = false) {
    Surface(
        onClick = onClick,
        color = HansColors.Surface,
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, HansColors.Border.copy(alpha = 0.6f)),
    ) {
        Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.spacedBy(11.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(43.dp).clip(RoundedCornerShape(13.dp)).background(Brush.linearGradient(listOf(Color(0xFF2A3440), Color(0xFF171B22)))), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.LibraryMusic, contentDescription = null, tint = HansColors.Lime, modifier = Modifier.size(21.dp))
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(track.title, color = HansColors.Text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(track.artist, color = HansColors.Muted, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            if (showPlay) Icon(Icons.Rounded.PlayArrow, contentDescription = "Putar ${track.title}", tint = HansColors.Lime, modifier = Modifier.size(22.dp))
        }
    }
}
