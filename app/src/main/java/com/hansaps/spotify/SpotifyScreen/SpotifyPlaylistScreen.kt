package com.hansaps.spotify.SpotifyScreen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.QueueMusic
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import com.hansaps.core.HansColors
import com.hansaps.core.LocalTrack
import com.hansaps.core.MusicPlaylist
import com.hansaps.core.PlaylistEntry
import com.hansaps.core.SectionLabel

@Composable
fun SpotifyPlaylistScreen(
    playlists: List<MusicPlaylist>,
    localTracks: List<LocalTrack>,
    onPlay: (PlaylistEntry) -> Unit,
    onPlayAll: (List<PlaylistEntry>) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (playlists.isEmpty()) {
        Column(modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SectionLabel("Playlist", "Playlist.ini")
            Text("File Playlist.ini belum terbaca atau belum berisi playlist.", color = HansColors.Muted, fontSize = 13.sp)
        }
        return
    }
    LazyColumn(modifier = modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Playlist pribadi", color = HansColors.Text, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Text("Playlist.ini menjadi sumber daftar lagu. File lokal diprioritaskan.", color = HansColors.Muted, fontSize = 12.sp, lineHeight = 18.sp)
            }
        }
        items(playlists, key = { it.name }) { playlist ->
            Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                Surface(color = HansColors.Surface, shape = RoundedCornerShape(20.dp), border = androidx.compose.foundation.BorderStroke(1.dp, HansColors.Border)) {
                    Row(Modifier.fillMaxWidth().padding(13.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Surface(shape = RoundedCornerShape(14.dp), color = HansColors.Coral.copy(alpha = 0.14f)) {
                            Icon(Icons.Rounded.QueueMusic, contentDescription = null, tint = HansColors.Coral, modifier = Modifier.padding(11.dp).size(23.dp))
                        }
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(playlist.name, color = HansColors.Text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                            Text("${playlist.tracks.size} lagu", color = HansColors.Muted, fontSize = 11.sp)
                        }
                        Button(
                            onClick = { onPlayAll(playlist.tracks) },
                            shape = CircleShape,
                            colors = ButtonDefaults.buttonColors(containerColor = HansColors.Lime, contentColor = Color(0xFF15190D)),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
                            modifier = Modifier.size(39.dp),
                        ) { Icon(Icons.Rounded.PlayArrow, contentDescription = "Putar playlist") }
                    }
                }
                playlist.tracks.forEachIndexed { index, entry ->
                    val existsOffline = localTracks.any { track ->
                        track.title.contains(entry.title, ignoreCase = true) || entry.title.contains(track.title, ignoreCase = true)
                    }
                    Surface(
                        onClick = { onPlay(entry) },
                        color = Color.Transparent,
                        shape = RoundedCornerShape(13.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, HansColors.Border.copy(alpha = 0.5f)),
                    ) {
                        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 11.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("%02d".format(index + 1), color = HansColors.Muted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                Text(entry.title, color = HansColors.Text, fontSize = 13.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(entry.artist, color = HansColors.Muted, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            Surface(shape = CircleShape, color = if (existsOffline) HansColors.Lime.copy(alpha = 0.15f) else HansColors.SurfaceRaised) {
                                Text(if (existsOffline) "OFFLINE" else "ONLINE", modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp), color = if (existsOffline) HansColors.Lime else HansColors.Muted, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
        item { Spacer(Modifier.size(5.dp)) }
    }
}
