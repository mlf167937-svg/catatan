package com.hansaps.youtube

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.VideoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hansaps.core.HansColors
import com.hansaps.core.SoftCard
import com.hansaps.youtube.Engine.YoutubeDownloadOption
import com.hansaps.youtube.screens.YoutubeCollection

private enum class VideoTab(val label: String) { DOWNLOADER("Provider"), COLLECTION("Koleksi") }

@Composable
fun YoutubeScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val vm: YoutubeViewModel = viewModel()
    val videos by vm.localVideos.collectAsState()
    val options by vm.qualityOptions.collectAsState()
    val message by vm.message.collectAsState()
    var selectedTab by remember { mutableStateOf(VideoTab.DOWNLOADER) }
    var responseJson by remember { mutableStateOf("") }
    var title by remember { mutableStateOf("") }
    var videoId by remember { mutableStateOf("") }
    var thumbnailUrl by remember { mutableStateOf("") }
    var selectedQuality by remember { mutableStateOf<String?>(null) }

    val importVideo = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) vm.importVideo(context, uri)
    }

    androidx.compose.runtime.LaunchedEffect(Unit) { vm.refresh(context.applicationContext) }

    Column(
        modifier = Modifier.fillMaxSize().background(HansColors.Background).windowInsetsPadding(WindowInsets.safeDrawing).padding(horizontal = 18.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(13.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack) { Icon(Icons.Rounded.ArrowBack, contentDescription = "Kembali", tint = HansColors.Text) }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("HansAps YouTube", color = HansColors.Text, fontSize = 21.sp, fontWeight = FontWeight.Bold)
                Text("Video library & offline downloads", color = HansColors.Muted, fontSize = 11.sp)
            }
            Icon(Icons.Rounded.Movie, contentDescription = null, tint = HansColors.Coral, modifier = Modifier.size(23.dp))
        }
        Row(Modifier.fillMaxWidth().background(HansColors.Surface, RoundedCornerShape(16.dp)).padding(4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            VideoTab.entries.forEach { tab ->
                Surface(onClick = { selectedTab = tab }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp), color = if (selectedTab == tab) HansColors.Coral else Color.Transparent) {
                    Row(Modifier.padding(vertical = 11.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                        Icon(if (tab == VideoTab.DOWNLOADER) Icons.Rounded.CloudDownload else Icons.Rounded.VideoLibrary, contentDescription = null, tint = if (selectedTab == tab) Color.White else HansColors.Muted, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.size(6.dp))
                        Text(tab.label, color = if (selectedTab == tab) Color.White else HansColors.Muted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
        message?.let {
            SoftCard {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(it, color = HansColors.Text, fontSize = 12.sp, lineHeight = 17.sp, modifier = Modifier.weight(1f))
                    TextButton(onClick = vm::consumeMessage) { Text("OK", color = HansColors.Coral, fontSize = 11.sp) }
                }
            }
        }

        if (selectedTab == VideoTab.DOWNLOADER) {
            Column(
                modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(13.dp),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text("Authorized downloads", color = HansColors.Text, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    Text("Pilih kualitas berdasarkan response API provider. Untuk unduhan, provider harus mengembalikan URL file langsung yang memang diizinkan untuk diunduh.", color = HansColors.Muted, fontSize = 12.sp, lineHeight = 18.sp)
                }
                SoftCard {
                    Text("1. Data video", color = HansColors.Text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(value = title, onValueChange = { title = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Judul video") }, singleLine = true, shape = RoundedCornerShape(14.dp))
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(value = videoId, onValueChange = { videoId = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Video ID / referensi") }, singleLine = true, shape = RoundedCornerShape(14.dp))
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(value = thumbnailUrl, onValueChange = { thumbnailUrl = it }, modifier = Modifier.fillMaxWidth(), label = { Text("URL thumbnail (opsional)") }, singleLine = true, shape = RoundedCornerShape(14.dp))
                }
                SoftCard {
                    Text("2. Response API", color = HansColors.Text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(5.dp))
                    Text("Tempel JSON dari provider. HansAps tidak mengubah link YouTube menjadi stream atau melewati DRM.", color = HansColors.Muted, fontSize = 11.sp, lineHeight = 16.sp)
                    Spacer(Modifier.height(9.dp))
                    OutlinedTextField(
                        value = responseJson,
                        onValueChange = { responseJson = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("JSON response") },
                        minLines = 5,
                        maxLines = 10,
                        shape = RoundedCornerShape(14.dp),
                    )
                    Spacer(Modifier.height(10.dp))
                    Button(
                        onClick = {
                            val parsed = vm.acceptProviderResponse(responseJson)
                            if (parsed != null) {
                                if (parsed.title.isNotBlank()) title = parsed.title
                                if (parsed.videoId.isNotBlank()) videoId = parsed.videoId
                                if (!parsed.thumbnailUrl.isNullOrBlank()) thumbnailUrl = parsed.thumbnailUrl
                                selectedQuality = null
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(47.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = HansColors.SurfaceRaised, contentColor = HansColors.Text),
                    ) { Text("Baca opsi kualitas", fontWeight = FontWeight.SemiBold) }
                }
                SoftCard {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Rounded.VideoLibrary, contentDescription = null, tint = HansColors.Coral)
                        Text("3. Pilih kualitas", color = HansColors.Text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    }
                    Spacer(Modifier.height(10.dp))
                    if (options.isEmpty()) {
                        Text("Belum ada kualitas dari API. Tempel response yang berisi formats[].", color = HansColors.Muted, fontSize = 12.sp, lineHeight = 18.sp)
                    } else {
                        options.forEach { option ->
                            QualityRow(option = option, selected = selectedQuality == option.quality, onClick = { selectedQuality = option.quality })
                            Spacer(Modifier.height(7.dp))
                        }
                        Button(
                            onClick = {
                                val chosen = options.firstOrNull { it.quality == selectedQuality }
                                if (chosen != null) vm.downloadOption(context, title, videoId, thumbnailUrl.ifBlank { null }, chosen)
                            },
                            enabled = options.any { it.quality == selectedQuality } && title.isNotBlank(),
                            modifier = Modifier.fillMaxWidth().height(49.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = HansColors.Coral, contentColor = Color.White),
                        ) {
                            Icon(Icons.Rounded.CloudDownload, contentDescription = null)
                            Spacer(Modifier.size(8.dp))
                            Text("Download ke koleksi", fontWeight = FontWeight.Bold)
                        }
                    }
                }
                Text("Storage: Download/HansAps/Youtube/{judul}/video.mp4 + meta.json + thumbnail.jpg", color = HansColors.Muted, fontSize = 10.sp, lineHeight = 15.sp)
            }
        } else {
            YoutubeCollection(
                videos = videos,
                onImport = { importVideo.launch(arrayOf("video/*")) },
                onRefresh = { vm.refresh(context) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun QualityRow(option: YoutubeDownloadOption, selected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = if (selected) HansColors.Coral.copy(alpha = 0.13f) else HansColors.SurfaceRaised,
        border = androidx.compose.foundation.BorderStroke(1.dp, if (selected) HansColors.Coral else HansColors.Border),
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(option.label, color = HansColors.Text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Text("${option.mimeType}${option.fileSizeBytes?.let { " • ${it / (1024 * 1024)} MB" } ?: ""}", color = HansColors.Muted, fontSize = 10.sp)
            }
            Text(if (selected) "DIPILIH" else option.quality, color = if (selected) HansColors.Coral else HansColors.Muted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
    }
}
