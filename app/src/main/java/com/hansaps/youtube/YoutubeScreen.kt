package com.hansaps.youtube

import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.VideoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.hansaps.core.HansColors
import com.hansaps.core.NexrayYoutubeVideo
import com.hansaps.core.SoftCard
import com.hansaps.youtube.Engine.YoutubeDownloadOption
import com.hansaps.youtube.screens.YoutubeCollection

private enum class VideoTab(val label: String) { SEARCH("Jelajahi"), COLLECTION("Koleksi") }

@Composable
fun YoutubeScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val vm: YoutubeViewModel = viewModel()
    val videos by vm.localVideos.collectAsState()
    val results by vm.searchResults.collectAsState()
    val selectedVideo by vm.selectedVideo.collectAsState()
    val options by vm.qualityOptions.collectAsState()
    val searching by vm.searching.collectAsState()
    val preparing by vm.preparing.collectAsState()
    val message by vm.message.collectAsState()
    var selectedTab by remember { mutableStateOf(VideoTab.SEARCH) }
    var query by remember { mutableStateOf("") }
    var selectedQuality by remember { mutableStateOf<String?>(null) }
    var playingUrl by remember { mutableStateOf<String?>(null) }
    var playingTitle by remember { mutableStateOf("") }

    LaunchedEffect(Unit) { vm.refresh(context.applicationContext) }
    LaunchedEffect(options) { selectedQuality = options.firstOrNull()?.quality }

    Column(
        modifier = Modifier.fillMaxSize().background(HansColors.Background).windowInsetsPadding(WindowInsets.safeDrawing).padding(horizontal = 18.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(13.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack) { Icon(Icons.Rounded.ArrowBack, contentDescription = "Kembali", tint = HansColors.Text) }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("HansAps YouTube", color = HansColors.Text, fontSize = 21.sp, fontWeight = FontWeight.Bold)
                Text("Search, quality picker & offline library", color = HansColors.Muted, fontSize = 11.sp)
            }
            Icon(Icons.Rounded.Movie, contentDescription = null, tint = HansColors.Coral, modifier = Modifier.size(23.dp))
        }
        Row(Modifier.fillMaxWidth().background(HansColors.Surface, RoundedCornerShape(16.dp)).padding(4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            VideoTab.entries.forEach { tab ->
                Surface(onClick = { selectedTab = tab }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp), color = if (selectedTab == tab) HansColors.Coral else Color.Transparent) {
                    Row(Modifier.padding(vertical = 11.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                        Icon(if (tab == VideoTab.SEARCH) Icons.Rounded.Search else Icons.Rounded.VideoLibrary, contentDescription = null, tint = if (selectedTab == tab) Color.White else HansColors.Muted, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.size(6.dp))
                        Text(tab.label, color = if (selectedTab == tab) Color.White else HansColors.Muted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
        message?.let {
            Surface(color = HansColors.SurfaceRaised, shape = RoundedCornerShape(13.dp)) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 9.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(it, color = HansColors.Text, fontSize = 11.sp, lineHeight = 16.sp, modifier = Modifier.weight(1f))
                    TextButton(onClick = vm::consumeMessage) { Text("OK", color = HansColors.Coral, fontSize = 10.sp) }
                }
            }
        }

        if (selectedTab == VideoTab.SEARCH) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(11.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        label = { Text("Video, channel, atau topik") },
                        shape = RoundedCornerShape(15.dp),
                    )
                    Button(
                        onClick = { vm.search(query) },
                        enabled = query.isNotBlank() && !searching,
                        modifier = Modifier.height(55.dp),
                        shape = RoundedCornerShape(15.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = HansColors.Coral, contentColor = Color.White),
                    ) {
                        if (searching) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = Color.White)
                        else Icon(Icons.Rounded.Search, contentDescription = "Cari")
                    }
                }

                if (selectedVideo != null) {
                    SelectedVideoPanel(
                        video = selectedVideo!!,
                        options = options,
                        selectedQuality = selectedQuality,
                        preparing = preparing,
                        onSelectQuality = { selectedQuality = it.quality },
                        onPlay = { option -> playingTitle = selectedVideo!!.title; playingUrl = option.directUrl },
                        onDownload = { option -> vm.downloadOption(context, selectedVideo!!, option) },
                    )
                }

                if (playingUrl != null) {
                    OnlineVideoPlayer(url = playingUrl!!, title = playingTitle, onClose = { playingUrl = null })
                }

                if (results.isEmpty() && !searching) {
                    SoftCard {
                        Icon(Icons.Rounded.Movie, contentDescription = null, tint = HansColors.Coral, modifier = Modifier.size(28.dp))
                        Text("Jelajahi video dari Nexray", color = HansColors.Text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                        Text("Cari video, pilih kualitas yang tersedia dari respons API, lalu simpan untuk ditonton offline.", color = HansColors.Muted, fontSize = 12.sp, lineHeight = 18.sp)
                    }
                } else {
                    Text("${results.size} hasil", color = HansColors.Muted, fontSize = 11.sp)
                    LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                        items(results, key = { it.url }) { video ->
                            VideoSearchRow(video = video, selected = selectedVideo?.url == video.url, onSelect = { vm.prepareVideo(video) })
                        }
                    }
                }
            }
        } else {
            YoutubeCollection(
                videos = videos,
                onRefresh = { vm.refresh(context) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun VideoSearchRow(video: NexrayYoutubeVideo, selected: Boolean, onSelect: () -> Unit) {
    Surface(
        onClick = onSelect,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(17.dp),
        color = if (selected) HansColors.Coral.copy(alpha = .10f) else HansColors.Surface,
        border = BorderStroke(1.dp, if (selected) HansColors.Coral else HansColors.Border),
    ) {
        Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            AsyncImage(model = video.thumbnail.ifBlank { null }, contentDescription = null, modifier = Modifier.size(width = 100.dp, height = 65.dp), contentScale = ContentScale.Crop)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(video.title, color = HansColors.Text, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(video.channel.ifBlank { "YouTube" }, color = HansColors.Muted, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(listOf(video.duration, video.views.takeIf { it.isNotBlank() }?.let { "$it views" }).filterNotNull().filter { it.isNotBlank() }.joinToString(" • "), color = HansColors.Muted, fontSize = 9.sp)
            }
            Text(if (selected) "DIPILIH" else "PILIH", color = HansColors.Coral, fontSize = 9.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun SelectedVideoPanel(
    video: NexrayYoutubeVideo,
    options: List<YoutubeDownloadOption>,
    selectedQuality: String?,
    preparing: Boolean,
    onSelectQuality: (YoutubeDownloadOption) -> Unit,
    onPlay: (YoutubeDownloadOption) -> Unit,
    onDownload: (YoutubeDownloadOption) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth().background(HansColors.Surface, RoundedCornerShape(18.dp)).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        Text(video.title, color = HansColors.Text, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
        if (preparing) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CircularProgressIndicator(Modifier.size(17.dp), strokeWidth = 2.dp, color = HansColors.Coral)
                Text("Memuat kualitas dari Nexray…", color = HansColors.Muted, fontSize = 11.sp)
            }
        } else if (options.isNotEmpty()) {
            Text("PILIH KUALITAS", color = HansColors.Muted, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.1.sp)
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                options.forEach { option ->
                    Surface(
                        onClick = { onSelectQuality(option) },
                        color = if (selectedQuality == option.quality) HansColors.Coral else HansColors.SurfaceRaised,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, if (selectedQuality == option.quality) HansColors.Coral else HansColors.Border),
                    ) {
                        Text(option.label, modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp), color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
            val chosen = options.firstOrNull { it.quality == selectedQuality } ?: options.first()
            Text("${chosen.mimeType}${chosen.fileSizeBytes?.let { " • ${it / (1024 * 1024)} MB" } ?: ""}${if (chosen.hasAudio) " • audio" else " • video tanpa audio"}", color = HansColors.Muted, fontSize = 9.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { onPlay(chosen) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = HansColors.SurfaceRaised, contentColor = HansColors.Text),
                ) {
                    Icon(Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.size(4.dp))
                    Text("Putar", fontSize = 11.sp)
                }
                Button(
                    onClick = { onDownload(chosen) },
                    enabled = chosen.hasAudio,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = HansColors.Coral, contentColor = Color.White),
                ) {
                    Icon(Icons.Rounded.CloudDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.size(4.dp))
                    Text("Download", fontSize = 11.sp)
                }
            }
            if (!chosen.hasAudio) Text("Format ini tidak memiliki audio gabungan, jadi tidak bisa disimpan sebagai video offline ber-suara.", color = HansColors.Muted, fontSize = 10.sp, lineHeight = 14.sp)
        } else if (!preparing) {
            Text("Nexray tidak menyediakan opsi kualitas untuk video ini.", color = HansColors.Muted, fontSize = 11.sp)
        }
    }
}

@Composable
private fun OnlineVideoPlayer(url: String, title: String, onClose: () -> Unit) {
    val context = LocalContext.current
    val player = remember(url) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(Uri.parse(url)))
            repeatMode = Player.REPEAT_MODE_OFF
            prepare()
            playWhenReady = true
        }
    }
    DisposableEffect(player) { onDispose { player.release() } }
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(title, color = HansColors.Text, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
            TextButton(onClick = onClose) { Text("Tutup", color = HansColors.Coral, fontSize = 10.sp) }
        }
        AndroidView(
            modifier = Modifier.fillMaxWidth().height(190.dp),
            factory = { viewContext -> PlayerView(viewContext).apply {
                this.player = player
                useController = true
                resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                setShutterBackgroundColor(android.graphics.Color.BLACK)
            } },
            update = { it.player = player },
        )
    }
}
