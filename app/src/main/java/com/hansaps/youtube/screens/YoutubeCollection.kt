package com.hansaps.youtube.screens

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
import androidx.compose.foundation.background
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.hansaps.core.EmptyState
import com.hansaps.core.HansColors
import com.hansaps.core.LocalVideo

@Composable
fun YoutubeCollection(
    videos: List<LocalVideo>,
    onImport: () -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedVideo by remember { mutableStateOf<LocalVideo?>(null) }

    Column(modifier = modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(13.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Koleksi video", color = HansColors.Text, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Text("Video impor dan video yang diunduh tersimpan di Download/HansAps/Youtube/.", color = HansColors.Muted, fontSize = 12.sp, lineHeight = 18.sp)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = onImport,
                modifier = Modifier.weight(1f).height(47.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = HansColors.Coral, contentColor = Color.White),
            ) {
                Icon(Icons.Rounded.Add, contentDescription = null)
                Spacer(Modifier.size(6.dp))
                Text("Impor video", fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
            TextButton(onClick = onRefresh) {
                Icon(Icons.Rounded.Refresh, contentDescription = null, tint = HansColors.Muted)
                Text("Refresh", color = HansColors.Muted, fontSize = 12.sp)
            }
        }
        selectedVideo?.let { LocalVideoPlayer(it, onClose = { selectedVideo = null }) }
        if (videos.isEmpty()) {
            EmptyState("Belum ada video", "Impor video yang kamu miliki atau simpan melalui provider yang berwenang. Koleksi bisa diputar tanpa internet.")
        } else {
            LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                items(videos, key = { it.id }) { video ->
                    Surface(
                        onClick = { selectedVideo = video },
                        color = HansColors.Surface,
                        shape = RoundedCornerShape(17.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, HansColors.Border),
                    ) {
                        Row(Modifier.fillMaxWidth().padding(13.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(11.dp)) {
                            Box(Modifier.size(58.dp).background(Color(0xFF2E2324), RoundedCornerShape(13.dp)), contentAlignment = Alignment.Center) {
                                Icon(Icons.Rounded.Movie, contentDescription = null, tint = HansColors.Coral, modifier = Modifier.size(25.dp))
                            }
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(video.title, color = HansColors.Text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                Text("${video.displayName} • Offline", color = HansColors.Muted, fontSize = 10.sp)
                            }
                            Text("PUTAR", color = HansColors.Coral, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LocalVideoPlayer(video: LocalVideo, onClose: () -> Unit) {
    val context = LocalContext.current
    val player = remember(video.uri) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(video.uri))
            repeatMode = Player.REPEAT_MODE_OFF
            prepare()
            playWhenReady = true
        }
    }
    DisposableEffect(player) { onDispose { player.release() } }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Text(video.title, color = HansColors.Text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
            TextButton(onClick = onClose) { Text("Tutup", color = HansColors.Coral, fontSize = 11.sp) }
        }
        AndroidView(
            modifier = Modifier.fillMaxWidth().height(205.dp),
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
