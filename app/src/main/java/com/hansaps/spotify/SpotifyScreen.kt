package com.hansaps.spotify

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.QueueMusic
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import androidx.media3.session.MediaController
import com.hansaps.core.HansColors
import com.hansaps.core.LocalTrack
import com.hansaps.core.PlaybackConnector
import com.hansaps.core.SoftCard
import com.hansaps.spotify.SpotifyScreen.SpotifyCollection
import com.hansaps.spotify.SpotifyScreen.SpotifyDownloaderScreen
import com.hansaps.spotify.SpotifyScreen.SpotifyHomeScreen
import com.hansaps.spotify.SpotifyScreen.SpotifyPlaylistScreen
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

private enum class SpotifyTab(val label: String) {
    HOME("Dengar"), PLAYLISTS("Playlist"), COLLECTION("Koleksi"), DOWNLOAD("Unduh")
}

@Composable
fun SpotifyScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val vm: SpotifyViewModel = viewModel()
    val playlists by vm.playlists.collectAsState()
    val tracks by vm.localTracks.collectAsState()
    val message by vm.message.collectAsState()
    val controller by PlaybackConnector.controller.collectAsState()
    var selectedTab by remember { mutableStateOf(SpotifyTab.HOME) }
    var positionMs by remember { mutableFloatStateOf(0f) }
    var durationMs by remember { mutableFloatStateOf(1f) }

    LaunchedEffect(Unit) { vm.load(context.applicationContext) }
    LaunchedEffect(controller) {
        while (isActive) {
            val active = controller
            if (active != null) {
                positionMs = active.currentPosition.coerceAtLeast(0L).toFloat()
                durationMs = active.duration.takeIf { it > 0L }?.toFloat() ?: 1f
            } else {
                positionMs = 0f
                durationMs = 1f
            }
            delay(500)
        }
    }

    val importAudio = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) vm.importAudio(context, uri)
    }

    Column(
        modifier = Modifier.fillMaxSize().background(HansColors.Background).windowInsetsPadding(WindowInsets.safeDrawing).padding(horizontal = 18.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(13.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack) {
                Icon(Icons.Rounded.ArrowBack, contentDescription = "Kembali", tint = HansColors.Text)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("HansAps Spotify", color = HansColors.Text, fontSize = 21.sp, fontWeight = FontWeight.Bold)
                Text("Music, without the clutter.", color = HansColors.Muted, fontSize = 11.sp)
            }
            Icon(Icons.Rounded.MusicNote, contentDescription = null, tint = HansColors.Lime, modifier = Modifier.size(23.dp))
        }

        Row(Modifier.fillMaxWidth().background(HansColors.Surface, RoundedCornerShape(16.dp)).padding(4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            SpotifyTab.entries.forEach { tab ->
                Surface(
                    onClick = { selectedTab = tab },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    color = if (selectedTab == tab) HansColors.Lime else Color.Transparent,
                ) {
                    Row(Modifier.padding(vertical = 11.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            when (tab) {
                                SpotifyTab.HOME -> Icons.Rounded.MusicNote
                                SpotifyTab.PLAYLISTS -> Icons.Rounded.QueueMusic
                                SpotifyTab.COLLECTION -> Icons.Rounded.LibraryMusic
                                SpotifyTab.DOWNLOAD -> Icons.Rounded.CloudDownload
                            },
                            contentDescription = null,
                            tint = if (selectedTab == tab) Color(0xFF15190D) else HansColors.Muted,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(Modifier.size(5.dp))
                        Text(tab.label, color = if (selectedTab == tab) Color(0xFF15190D) else HansColors.Muted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        if (!message.isNullOrBlank()) {
            SoftCard {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(message.orEmpty(), modifier = Modifier.weight(1f), color = HansColors.Text, fontSize = 12.sp, lineHeight = 17.sp)
                    TextButton(onClick = vm::consumeMessage) { Text("OK", color = HansColors.Lime, fontSize = 11.sp) }
                }
            }
        }

        when (selectedTab) {
            SpotifyTab.HOME -> SpotifyHomeScreen(
                tracks = tracks,
                playlists = playlists,
                controller = controller,
                positionMs = positionMs,
                durationMs = durationMs,
                onPlayTrack = { vm.playTrack(context, it) },
                onToggle = {
                    if (controller?.currentMediaItem != null) PlaybackConnector.playPause(context)
                    else if (tracks.isNotEmpty()) vm.playTrack(context, tracks.first())
                    else vm.notify("Koleksi masih kosong. Impor audio di tab Koleksi terlebih dahulu.")
                },
                onPrevious = {
                    if (controller?.currentMediaItem == null && tracks.isNotEmpty()) vm.playTrack(context, tracks.first())
                    else PlaybackConnector.skipPrevious(context)
                },
                onNext = {
                    if (controller?.currentMediaItem == null && tracks.isNotEmpty()) vm.playTrack(context, tracks.first())
                    else PlaybackConnector.skipNext(context)
                },
                onSeek = { PlaybackConnector.seekTo(it.toLong()) },
                onOpenPlaylists = { selectedTab = SpotifyTab.PLAYLISTS },
                onOpenCollection = { selectedTab = SpotifyTab.COLLECTION },
                modifier = Modifier.weight(1f),
            )
            SpotifyTab.PLAYLISTS -> SpotifyPlaylistScreen(
                playlists = playlists,
                localTracks = tracks,
                onPlay = { vm.playPlaylistEntry(context, it) },
                onPlayAll = { entries -> vm.playPlaylist(context, entries) },
                modifier = Modifier.weight(1f),
            )
            SpotifyTab.COLLECTION -> SpotifyCollection(
                tracks = tracks,
                onImport = { importAudio.launch(arrayOf("audio/*")) },
                onRefresh = { vm.refreshCollection(context) },
                onPlay = { vm.playTrack(context, it) },
                onQueue = { vm.addToQueue(context, it) },
                modifier = Modifier.weight(1f),
            )
            SpotifyTab.DOWNLOAD -> SpotifyDownloaderScreen(
                onDownload = { title, artist, audioUrl, thumbnailUrl, lyrics ->
                    vm.downloadMusic(context, title, artist, audioUrl, thumbnailUrl, lyrics)
                },
                modifier = Modifier.weight(1f),
            )
        }

        if (selectedTab != SpotifyTab.HOME && controller?.currentMediaItem != null) {
            CompactPlayer(controller = controller, isPlaying = controller?.isPlaying == true, onToggle = { PlaybackConnector.playPause(context) }, onNext = { PlaybackConnector.skipNext(context) })
        }
    }
}

@Composable
private fun CompactPlayer(controller: MediaController?, isPlaying: Boolean, onToggle: () -> Unit, onNext: () -> Unit) {
    if (controller == null) return
    Surface(color = HansColors.SurfaceRaised, shape = RoundedCornerShape(17.dp), border = androidx.compose.foundation.BorderStroke(1.dp, HansColors.Border)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 11.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(Icons.Rounded.MusicNote, contentDescription = null, tint = HansColors.Lime)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(controller.mediaMetadata.title?.toString() ?: "HansAps Player", color = HansColors.Text, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                Text(controller.mediaMetadata.artist?.toString() ?: "Offline audio", color = HansColors.Muted, fontSize = 10.sp, maxLines = 1)
            }
            TextButton(onClick = onToggle) { Text(if (isPlaying) "Pause" else "Play", color = HansColors.Lime, fontSize = 11.sp) }
            TextButton(onClick = onNext) { Text("Next", color = HansColors.Text, fontSize = 11.sp) }
        }
    }
}
