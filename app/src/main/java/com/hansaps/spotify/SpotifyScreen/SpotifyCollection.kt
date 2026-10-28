package com.hansaps.spotify.SpotifyScreen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hansaps.core.EmptyState
import com.hansaps.core.HansColors
import com.hansaps.core.LocalTrack
import com.hansaps.core.SectionLabel
import com.hansaps.spotify.SpotifyScreen.TrackRow

@Composable
fun SpotifyCollection(
    tracks: List<LocalTrack>,
    onImport: () -> Unit,
    onRefresh: () -> Unit,
    onPlay: (LocalTrack) -> Unit,
    onQueue: (LocalTrack) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(13.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Koleksi musik", color = HansColors.Text, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Text("File lokal disimpan di Download/HansAps/Spotify/.", color = HansColors.Muted, fontSize = 12.sp)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            Button(
                onClick = onImport,
                modifier = Modifier.weight(1f).height(47.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = HansColors.Lime, contentColor = Color(0xFF15190D)),
            ) {
                Icon(Icons.Rounded.Add, contentDescription = null)
                Spacer(Modifier.size(6.dp))
                Text("Impor audio", fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
            TextButton(onClick = onRefresh, modifier = Modifier.height(47.dp)) {
                Icon(Icons.Rounded.Refresh, contentDescription = "Segarkan", tint = HansColors.Muted)
                Text("Refresh", color = HansColors.Muted, fontSize = 12.sp)
            }
        }
        SectionLabel("Tersimpan", "${tracks.size} lagu")
        if (tracks.isEmpty()) {
            EmptyState(
                title = "Koleksi masih kosong",
                detail = "Impor file musik milikmu untuk memutarnya offline. Metadata dasar akan disimpan bersama file.",
            )
        } else {
            LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(tracks, key = { it.id }) { track ->
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        androidx.compose.foundation.layout.Box(Modifier.weight(1f)) {
                            TrackRow(track = track, onClick = { onPlay(track) }, showPlay = true)
                        }
                        TextButton(onClick = { onQueue(track) }) { Text("+ Queue", color = HansColors.Lime, fontSize = 10.sp) }
                    }
                }
            }
        }
        Text("Audio yang diimpor tidak diunggah ke server.", color = HansColors.Muted, fontSize = 10.sp)
    }
}
