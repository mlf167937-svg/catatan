package com.hansaps.spotify.SpotifyScreen

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import com.hansaps.core.HansColors
import com.hansaps.core.NexraySpotifyTrack
import com.hansaps.core.SoftCard

@Composable
fun SpotifyDownloaderScreen(
    results: List<NexraySpotifyTrack>,
    searching: Boolean,
    onSearch: (String) -> Unit,
    onPlay: (NexraySpotifyTrack) -> Unit,
    onDownload: (NexraySpotifyTrack) -> Unit,
    modifier: Modifier = Modifier,
) {
    var query by remember { mutableStateOf("") }

    Column(modifier = modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(13.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text("Cari musik", color = HansColors.Text, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Text("Ditenagai Nexray API • putar online atau simpan untuk offline.", color = HansColors.Muted, fontSize = 12.sp, lineHeight = 18.sp)
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.weight(1f),
                singleLine = true,
                label = { Text("Judul lagu atau artis") },
                shape = RoundedCornerShape(15.dp),
            )
            Button(
                onClick = { onSearch(query) },
                enabled = query.isNotBlank() && !searching,
                modifier = Modifier.height(55.dp),
                shape = RoundedCornerShape(15.dp),
                colors = ButtonDefaults.buttonColors(containerColor = HansColors.Lime, contentColor = Color(0xFF15190D)),
            ) {
                if (searching) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = Color(0xFF15190D))
                else Icon(Icons.Rounded.Search, contentDescription = "Cari")
            }
        }

        if (results.isEmpty() && !searching) {
            SoftCard {
                Icon(Icons.Rounded.MusicNote, contentDescription = null, tint = HansColors.Lime, modifier = Modifier.size(28.dp))
                Spacer(Modifier.height(4.dp))
                Text("Cari lagu dari katalog Nexray", color = HansColors.Text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                Text("Hasil yang cocok akan muncul di sini. Ketuk Play untuk streaming atau Download untuk menyimpannya bersama metadata, lirik, dan thumbnail.", color = HansColors.Muted, fontSize = 12.sp, lineHeight = 18.sp)
            }
        } else {
            Text("${results.size} hasil", color = HansColors.Muted, fontSize = 11.sp)
            LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                items(results, key = { it.url }) { track ->
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(17.dp),
                        color = HansColors.Surface,
                        border = androidx.compose.foundation.BorderStroke(1.dp, HansColors.Border),
                    ) {
                        Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            AsyncImage(
                                model = track.thumbnail.ifBlank { null },
                                contentDescription = null,
                                modifier = Modifier.size(58.dp),
                                contentScale = ContentScale.Crop,
                            )
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(track.title, color = HansColors.Text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                Text(track.artist, color = HansColors.Muted, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                if (track.album.isNotBlank() || track.duration.isNotBlank()) {
                                    Text(listOf(track.album, track.duration).filter { it.isNotBlank() }.joinToString(" • "), color = HansColors.Muted, fontSize = 9.sp, maxLines = 1)
                                }
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(1.dp)) {
                                IconButton(onClick = { onPlay(track) }) { Icon(Icons.Rounded.PlayArrow, contentDescription = "Putar", tint = HansColors.Lime) }
                                IconButton(onClick = { onDownload(track) }) { Icon(Icons.Rounded.CloudDownload, contentDescription = "Download", tint = HansColors.Text) }
                            }
                        }
                    }
                }
            }
        }
    }
}
