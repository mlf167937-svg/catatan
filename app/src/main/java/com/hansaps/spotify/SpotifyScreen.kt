package com.hansaps.spotify

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.filled.Lyrics
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.QueueMusic
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.session.MediaController
import coil.compose.AsyncImage
import com.hansaps.core.HansColors
import com.hansaps.core.NexrayApi
import com.hansaps.core.NexrayLyrics
import com.hansaps.core.PlaybackConnector
import com.hansaps.core.formatTime
import com.hansaps.spotify.SpotifyScreen.SpotifyCollection
import com.hansaps.spotify.SpotifyScreen.SpotifyDownloaderScreen
import com.hansaps.spotify.SpotifyScreen.SpotifyHomeScreen
import com.hansaps.spotify.SpotifyScreen.SpotifyPlaylistScreen
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

private enum class SpotifyTab(val label: String) {
    HOME("Beranda"), PLAYLISTS("Playlist"), COLLECTION("Koleksi"), DOWNLOAD("Cari"), LYRICS("Lirik"), PLAYER("Pemutar"), SYNC("Sync")
}

@Composable
fun SpotifyScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val vm: SpotifyViewModel = viewModel()
    val playlists by vm.playlists.collectAsState()
    val tracks by vm.localTracks.collectAsState()
    val onlineResults by vm.searchResults.collectAsState()
    val searching by vm.searching.collectAsState()
    val message by vm.message.collectAsState()
    val controller by PlaybackConnector.controller.collectAsState()
    var selectedTab by remember { mutableStateOf(SpotifyTab.HOME) }
    var positionMs by remember { mutableFloatStateOf(0f) }
    var durationMs by remember { mutableFloatStateOf(1f) }
    val currentTitle = controller?.mediaMetadata?.title?.toString().orEmpty().ifBlank { "Belum ada lagu" }
    val currentArtist = controller?.mediaMetadata?.artist?.toString().orEmpty().ifBlank { "Pilih lagu untuk mulai mendengarkan" }

    BackHandler(enabled = selectedTab == SpotifyTab.PLAYER || selectedTab == SpotifyTab.LYRICS || selectedTab == SpotifyTab.SYNC) { selectedTab = SpotifyTab.HOME }

    LaunchedEffect(Unit) { vm.load(context.applicationContext) }
    LaunchedEffect(controller) {
        while (isActive) {
            val active = controller
            positionMs = active?.currentPosition?.coerceAtLeast(0L)?.toFloat() ?: 0f
            durationMs = active?.duration?.takeIf { it > 0L }?.toFloat() ?: 1f
            delay(350)
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().background(HansColors.Background).windowInsetsPadding(WindowInsets.safeDrawing),
        verticalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack, modifier = Modifier.size(42.dp)) {
                Icon(Icons.Rounded.ArrowBack, contentDescription = "Kembali", tint = HansColors.Text)
            }
            Column(Modifier.weight(1f).padding(start = 3.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("HansAps Music", color = HansColors.Text, fontSize = 20.sp, fontWeight = FontWeight.Bold, letterSpacing = (-.5).sp)
                Text(
                    when (selectedTab) {
                        SpotifyTab.LYRICS -> "Lirik mengikuti sesi pemutaran"
                        SpotifyTab.PLAYER -> "Now playing"
                        SpotifyTab.SYNC -> "Sinkronisasi antar perangkat"
                        else -> "Musikmu, dalam satu ruang"
                    }, color = HansColors.Muted, fontSize = 11.sp
                )
            }
            IconButton(onClick = { selectedTab = SpotifyTab.SYNC }) {
                Icon(Icons.Rounded.Sync, contentDescription = "Sync perangkat", tint = HansColors.Lime)
            }
        }

        if (!message.isNullOrBlank()) {
            Surface(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 3.dp),
                color = HansColors.SurfaceRaised,
                shape = RoundedCornerShape(13.dp),
                border = BorderStroke(1.dp, HansColors.Border),
            ) {
                Row(Modifier.padding(start = 12.dp, end = 4.dp, top = 7.dp, bottom = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(message.orEmpty(), modifier = Modifier.weight(1f), color = HansColors.Text, fontSize = 11.sp, lineHeight = 15.sp)
                    TextButton(onClick = vm::consumeMessage) { Text("OK", color = HansColors.Lime, fontSize = 10.sp) }
                }
            }
        }

        Box(Modifier.weight(1f).fillMaxWidth()) {
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
                        else vm.notify("Cari lagu di tab Cari untuk streaming atau menyimpannya offline.")
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
                    modifier = Modifier.fillMaxSize().padding(horizontal = 18.dp, vertical = 8.dp),
                )
                SpotifyTab.PLAYLISTS -> SpotifyPlaylistScreen(
                    playlists = playlists,
                    localTracks = tracks,
                    onPlay = { vm.playPlaylistEntry(context, it) },
                    onPlayAll = { entries -> vm.playPlaylist(context, entries) },
                    modifier = Modifier.fillMaxSize().padding(horizontal = 18.dp, vertical = 8.dp),
                )
                SpotifyTab.COLLECTION -> SpotifyCollection(
                    tracks = tracks,
                    onRefresh = { vm.refreshCollection(context) },
                    onPlay = { vm.playTrack(context, it) },
                    onQueue = { vm.addToQueue(context, it) },
                    modifier = Modifier.fillMaxSize().padding(horizontal = 18.dp, vertical = 8.dp),
                )
                SpotifyTab.DOWNLOAD -> SpotifyDownloaderScreen(
                    results = onlineResults,
                    searching = searching,
                    onSearch = vm::searchOnline,
                    onPlay = { vm.playOnlineTrack(context, it) },
                    onDownload = { vm.downloadOnlineTrack(context, it) },
                    modifier = Modifier.fillMaxSize().padding(horizontal = 18.dp, vertical = 8.dp),
                )
                SpotifyTab.LYRICS -> LyricsPage(
                    title = currentTitle,
                    artist = currentArtist,
                    positionMs = positionMs.toLong(),
                    onSeek = { PlaybackConnector.seekTo(it) },
                )
                SpotifyTab.PLAYER -> NowPlayingPage(
                    controller = controller,
                    positionMs = positionMs,
                    durationMs = durationMs,
                    onToggle = { PlaybackConnector.playPause(context) },
                    onPrevious = { PlaybackConnector.skipPrevious(context) },
                    onNext = { PlaybackConnector.skipNext(context) },
                    onSeek = { PlaybackConnector.seekTo(it.toLong()) },
                    onLyrics = { selectedTab = SpotifyTab.LYRICS },
                )
                SpotifyTab.SYNC -> SpotifySyncScreen(modifier = Modifier.fillMaxSize().padding(horizontal = 18.dp, vertical = 8.dp))
            }
        }

        if (controller?.currentMediaItem != null && selectedTab != SpotifyTab.PLAYER) {
            MiniNowPlaying(
                title = currentTitle,
                artist = currentArtist,
                isPlaying = controller?.isPlaying == true,
                onOpen = { selectedTab = SpotifyTab.PLAYER },
                onToggle = { PlaybackConnector.playPause(context) },
                onNext = { PlaybackConnector.skipNext(context) },
            )
        }

        NavigationBar(
            modifier = Modifier.fillMaxWidth(),
            containerColor = Color(0xFF0C0E13),
            contentColor = HansColors.Muted,
        ) {
            val navTabs = listOf(
                Triple(SpotifyTab.HOME, Icons.Rounded.Home, "Home"),
                Triple(SpotifyTab.PLAYLISTS, Icons.Rounded.QueueMusic, "Playlist"),
                Triple(SpotifyTab.COLLECTION, Icons.Rounded.LibraryMusic, "Koleksi"),
                Triple(SpotifyTab.DOWNLOAD, Icons.Rounded.Search, "Cari"),
                Triple(SpotifyTab.LYRICS, Icons.Filled.Lyrics, "Lirik"),
            )
            navTabs.forEach { (tab, icon, label) ->
                NavigationBarItem(
                    selected = selectedTab == tab || (selectedTab == SpotifyTab.PLAYER && tab == SpotifyTab.HOME),
                    onClick = { selectedTab = tab },
                    icon = { Icon(icon, contentDescription = label, modifier = Modifier.size(21.dp)) },
                    label = { Text(label, fontSize = 9.sp, maxLines = 1) },
                    alwaysShowLabel = true,
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = HansColors.Lime,
                        selectedTextColor = HansColors.Lime,
                        indicatorColor = HansColors.Lime.copy(alpha = .12f),
                        unselectedIconColor = HansColors.Muted,
                        unselectedTextColor = HansColors.Muted,
                    ),
                )
            }
        }
    }
}

@Composable
private fun MiniNowPlaying(
    title: String,
    artist: String,
    isPlaying: Boolean,
    onOpen: () -> Unit,
    onToggle: () -> Unit,
    onNext: () -> Unit,
) {
    Surface(
        onClick = onOpen,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 5.dp),
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF1B2028),
        border = BorderStroke(1.dp, HansColors.Border),
    ) {
        Row(Modifier.padding(start = 10.dp, end = 5.dp, top = 7.dp, bottom = 7.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(Modifier.size(40.dp).clip(RoundedCornerShape(11.dp)).background(Brush.linearGradient(listOf(HansColors.Lime.copy(alpha = .8f), Color(0xFF222A3A)))), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.MusicNote, contentDescription = null, tint = Color(0xFF071018), modifier = Modifier.size(22.dp))
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, color = HansColors.Text, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(artist, color = HansColors.Muted, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            IconButton(onClick = onToggle) { Icon(if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, contentDescription = "Play/Pause", tint = HansColors.Text) }
            IconButton(onClick = onNext) { Icon(Icons.Rounded.SkipNext, contentDescription = "Lagu berikutnya", tint = HansColors.Lime) }
        }
    }
}

@Composable
private fun NowPlayingPage(
    controller: MediaController?,
    positionMs: Float,
    durationMs: Float,
    onToggle: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onSeek: (Float) -> Unit,
    onLyrics: () -> Unit,
) {
    val title = controller?.mediaMetadata?.title?.toString().orEmpty().ifBlank { "Belum ada lagu" }
    val artist = controller?.mediaMetadata?.artist?.toString().orEmpty().ifBlank { "Pilih lagu dari Cari atau Koleksi" }
    val artwork = controller?.mediaMetadata?.artworkUri?.toString()
    Box(
        Modifier.fillMaxSize().padding(horizontal = 14.dp, vertical = 8.dp).clip(RoundedCornerShape(27.dp))
            .background(Brush.linearGradient(listOf(Color(0xFFE04625), Color(0xFFB92D20), Color(0xFF4A211F))))
    ) {
        if (!artwork.isNullOrBlank()) {
            AsyncImage(
                model = artwork,
                contentDescription = null,
                modifier = Modifier.fillMaxSize().graphicsLayer { alpha = .20f },
                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
            )
        }
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0x33220A06), Color(0x44220A06), Color(0xAA130B0B)))))
        Column(
            Modifier.fillMaxSize().padding(horizontal = 22.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Rounded.MusicNote, contentDescription = null, tint = Color.White, modifier = Modifier.size(21.dp))
                    Text("HANSAPS  •  NOW PLAYING", color = Color.White.copy(alpha = .9f), fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
                }
                Spacer(Modifier.height(8.dp))
                Text(title, color = Color.White, fontSize = 28.sp, lineHeight = 33.sp, fontWeight = FontWeight.Bold, letterSpacing = (-.6).sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(artist, color = Color.White.copy(alpha = .82f), fontSize = 17.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Column(verticalArrangement = Arrangement.spacedBy(17.dp)) {
                Slider(
                    value = positionMs.coerceIn(0f, durationMs.coerceAtLeast(1f)),
                    onValueChange = onSeek,
                    valueRange = 0f..durationMs.coerceAtLeast(1f),
                    modifier = Modifier.fillMaxWidth(),
                    colors = androidx.compose.material3.SliderDefaults.colors(
                        thumbColor = Color.White,
                        activeTrackColor = Color.White,
                        inactiveTrackColor = Color.White.copy(alpha = .42f),
                    ),
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(formatTime(positionMs.toLong()), color = Color.White.copy(alpha = .9f), fontSize = 11.sp)
                    Text(formatTime(durationMs.toLong()), color = Color.White.copy(alpha = .9f), fontSize = 11.sp)
                }
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(22.dp)) {
                    Spacer(Modifier.weight(1f))
                    IconButton(onClick = onPrevious, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.Rounded.SkipPrevious, contentDescription = "Sebelumnya", tint = Color.White, modifier = Modifier.size(32.dp))
                    }
                    Surface(onClick = onToggle, shape = CircleShape, color = Color.White) {
                        Box(Modifier.size(66.dp), contentAlignment = Alignment.Center) {
                            Icon(if (controller?.isPlaying == true) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, contentDescription = "Putar/Jeda", tint = Color(0xFFB93222), modifier = Modifier.size(34.dp))
                        }
                    }
                    IconButton(onClick = onNext, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.Rounded.SkipNext, contentDescription = "Berikutnya", tint = Color.White, modifier = Modifier.size(32.dp))
                    }
                    Spacer(Modifier.weight(1f))
                }
                Surface(onClick = onLyrics, color = Color.White.copy(alpha = .14f), shape = RoundedCornerShape(15.dp), border = BorderStroke(1.dp, Color.White.copy(alpha = .24f))) {
                    Row(Modifier.fillMaxWidth().padding(horizontal = 15.dp, vertical = 13.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                        Icon(Icons.Filled.Lyrics, contentDescription = null, tint = Color.White, modifier = Modifier.size(19.dp))
                        Spacer(Modifier.size(8.dp))
                        Text("Buka lirik lagu", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

private data class TimedLyric(val timeMs: Long, val text: String)

@Composable
private fun LyricsPage(title: String, artist: String, positionMs: Long, onSeek: (Long) -> Unit) {
    var lyrics by remember(title, artist) { mutableStateOf<NexrayLyrics?>(null) }
    var loading by remember(title, artist) { mutableStateOf(false) }
    var error by remember(title, artist) { mutableStateOf<String?>(null) }
    val listState = androidx.compose.foundation.lazy.rememberLazyListState()

    LaunchedEffect(title, artist) {
        lyrics = null
        if (title == "Belum ada lagu" || artist == "Pilih lagu untuk mulai mendengarkan") {
            error = "Putar lagu dulu untuk melihat liriknya."
            return@LaunchedEffect
        }
        loading = true
        error = null
        try {
            lyrics = NexrayApi.searchLyrics(title, artist)
            if (lyrics?.bestAvailable.isNullOrBlank()) error = "Lirik belum tersedia untuk lagu ini."
        } catch (t: Throwable) {
            error = t.message ?: "Lirik gagal dimuat. Periksa koneksi internet."
        } finally {
            loading = false
        }
    }

    val timed = remember(lyrics?.synced) { parseTimedLyrics(lyrics?.synced.orEmpty()) }
    val activeIndex by remember(timed, positionMs) {
        derivedStateOf { timed.indexOfLast { it.timeMs <= positionMs }.coerceAtLeast(0) }
    }
    LaunchedEffect(activeIndex, timed.size) {
        if (timed.isNotEmpty()) runCatching { listState.animateScrollToItem(activeIndex, scrollOffset = -150) }
    }

    Column(Modifier.fillMaxSize().padding(horizontal = 18.dp, vertical = 8.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text("LYRICS", color = HansColors.Lime, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                Text(title, color = HansColors.Text, fontSize = 18.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(artist, color = HansColors.Muted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Icon(Icons.Filled.Lyrics, contentDescription = null, tint = HansColors.Lime, modifier = Modifier.size(30.dp))
        }
        Spacer(Modifier.height(13.dp))
        when {
            loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { androidx.compose.material3.CircularProgressIndicator(color = HansColors.Lime) }
            error != null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Filled.Lyrics, contentDescription = null, tint = HansColors.Muted, modifier = Modifier.size(34.dp))
                    Text(error.orEmpty(), color = HansColors.Muted, fontSize = 13.sp, textAlign = TextAlign.Center)
                    Text("Lirik dicari berdasarkan judul dan artis aktif.", color = HansColors.Muted.copy(alpha = .7f), fontSize = 10.sp, textAlign = TextAlign.Center)
                }
            }
            timed.isNotEmpty() -> LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                itemsIndexed(timed) { index, line ->
                    val active = index == activeIndex
                    Text(
                        text = line.text.ifBlank { "♪" },
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(if (active) HansColors.Lime.copy(alpha = .11f) else Color.Transparent)
                            .clickable { onSeek(line.timeMs) }
                            .padding(horizontal = 14.dp, vertical = 11.dp),
                        color = if (active) HansColors.Text else HansColors.Muted.copy(alpha = if (kotlin.math.abs(index - activeIndex) < 3) .78f else .42f),
                        fontSize = if (active) 21.sp else 16.sp,
                        lineHeight = if (active) 28.sp else 23.sp,
                        fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                    )
                }
            }
            !lyrics?.plain.isNullOrBlank() -> LazyColumn(modifier = Modifier.fillMaxSize()) {
                itemsIndexed(lyrics!!.plain.lines()) { _, line ->
                    Text(line.ifBlank { " " }, modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 7.dp), color = HansColors.Text.copy(alpha = .9f), fontSize = 16.sp, lineHeight = 25.sp)
                }
            }
            else -> Unit
        }
    }
}

private fun parseTimedLyrics(raw: String): List<TimedLyric> {
    val timestamp = Regex("\\[(\\d{1,2}):(\\d{2})(?:\\.(\\d{1,3}))?]")
    return raw.lineSequence().flatMap { line ->
        val matches = timestamp.findAll(line).toList()
        if (matches.isEmpty()) emptySequence()
        else {
            val text = line.substring(matches.last().range.last + 1).trim().ifBlank { "♪" }
            matches.asSequence().map { match ->
                val minutes = match.groupValues[1].toLongOrNull() ?: 0L
                val seconds = match.groupValues[2].toLongOrNull() ?: 0L
                val fraction = match.groupValues[3].padEnd(3, '0').take(3).toLongOrNull() ?: 0L
                TimedLyric((minutes * 60 + seconds) * 1000 + fraction, text)
            }
        }
    }.sortedBy { it.timeMs }.toList()
}
