package com.hansaps.spotify.SpotifyScreen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hansaps.core.HansColors
import com.hansaps.core.SoftCard

@Composable
fun SpotifyDownloaderScreen(
    onDownload: (title: String, artist: String, audioUrl: String, thumbnailUrl: String, lyrics: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var title by remember { mutableStateOf("") }
    var artist by remember { mutableStateOf("") }
    var audioUrl by remember { mutableStateOf("") }
    var thumbnailUrl by remember { mutableStateOf("") }
    var lyrics by remember { mutableStateOf("") }

    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text("Unduh musik", color = HansColors.Text, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Text("Isi URL file audio langsung dari provider yang berwenang. Metadata dan lirik disimpan bersama koleksi offline.", color = HansColors.Muted, fontSize = 12.sp, lineHeight = 18.sp)
        }
        SoftCard {
            Text("Informasi trek", color = HansColors.Text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(value = title, onValueChange = { title = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Judul lagu") }, singleLine = true, shape = RoundedCornerShape(14.dp))
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(value = artist, onValueChange = { artist = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Artis") }, singleLine = true, shape = RoundedCornerShape(14.dp))
        }
        SoftCard {
            Text("Sumber media", color = HansColors.Text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(7.dp))
            OutlinedTextField(value = audioUrl, onValueChange = { audioUrl = it }, modifier = Modifier.fillMaxWidth(), label = { Text("URL HTTPS langsung untuk audio") }, singleLine = true, shape = RoundedCornerShape(14.dp))
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(value = thumbnailUrl, onValueChange = { thumbnailUrl = it }, modifier = Modifier.fillMaxWidth(), label = { Text("URL thumbnail (opsional)") }, singleLine = true, shape = RoundedCornerShape(14.dp))
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(value = lyrics, onValueChange = { lyrics = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Lirik (opsional)") }, minLines = 3, maxLines = 7, shape = RoundedCornerShape(14.dp))
            Spacer(Modifier.height(13.dp))
            Button(
                onClick = { onDownload(title.trim(), artist.trim(), audioUrl.trim(), thumbnailUrl.trim(), lyrics) },
                enabled = title.isNotBlank() && artist.isNotBlank() && audioUrl.startsWith("https://", true),
                modifier = Modifier.fillMaxWidth().height(49.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = HansColors.Lime, contentColor = Color(0xFF15190D)),
            ) {
                Icon(Icons.Rounded.CloudDownload, contentDescription = null)
                Spacer(Modifier.height(0.dp))
                Text("Unduh ke Koleksi", fontWeight = FontWeight.Bold)
            }
        }
        Text("Struktur: Spotify/{Artis-Judul}/thumbnail.jpg • music.mp3 • meta.json • lyrics.json", color = HansColors.Muted, fontSize = 10.sp, lineHeight = 15.sp)
    }
}
