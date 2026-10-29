package com.hansaps.youtube

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.net.Uri
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Fullscreen
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.ScreenRotation
import androidx.compose.material.icons.rounded.VideoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.source.MergingMediaSource
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.hansaps.core.NexrayYoutubeVideo
import com.hansaps.youtube.Engine.YoutubeDownloadOption
import com.hansaps.youtube.screens.YoutubeCollection
import com.hansaps.youtube.ui.GlassIconButton
import com.hansaps.youtube.ui.LiveDot
import com.hansaps.youtube.ui.PressScale
import com.hansaps.youtube.ui.ShimmerBlock
import com.hansaps.youtube.ui.YtEmptyState
import com.hansaps.youtube.ui.YtPalette
import com.hansaps.youtube.ui.rememberShimmerShift
import kotlinx.coroutines.delay

private enum class VideoTab(val label: String) { SEARCH("Jelajahi"), COLLECTION("Koleksi") }

/** Data pemutaran online yang sedang aktif. Dibuat sebagai satu objek agar state mudah dikelola. */
private data class OnlinePlayback(
    val url: String,
    val audioUrl: String?,
    val title: String,
    val quality: String,
)

/** Keeps the active media selection when Android recreates the Activity after rotation. */
private val OnlinePlaybackStateSaver = Saver<OnlinePlayback?, List<String>>(
    save = { value -> value?.let { listOf(it.url, it.audioUrl.orEmpty(), it.title, it.quality) } ?: emptyList() },
    restore = { saved ->
        if (saved.size < 4) null else OnlinePlayback(
            url = saved[0],
            audioUrl = saved[1].ifBlank { null },
            title = saved[2],
            quality = saved[3],
        )
    },
)

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

    var selectedTab by rememberSaveable { mutableStateOf(VideoTab.SEARCH) }
    var query by rememberSaveable { mutableStateOf("") }
    var selectedQuality by rememberSaveable { mutableStateOf<String?>(null) }
    var playback by rememberSaveable(stateSaver = OnlinePlaybackStateSaver) { mutableStateOf<OnlinePlayback?>(null) }
    // The ViewModel survives configuration changes, so the currently resolved video can be restored.
    var shownVideo by remember { mutableStateOf<NexrayYoutubeVideo?>(selectedVideo) }
    var lastMessage by rememberSaveable { mutableStateOf("") }
    var autoStartedVideoKey by rememberSaveable { mutableStateOf<String?>(null) }
    var requestedVideoKey by rememberSaveable { mutableStateOf<String?>(null) }
    var loadingSeenVideoKey by rememberSaveable { mutableStateOf<String?>(null) }

    // Animasi masuk header; dipicu sekali saat layar dibuka.
    val headerEntrance = remember { MutableTransitionState(false).apply { targetState = true } }

    LaunchedEffect(Unit) { vm.refresh(context.applicationContext) }
    // Simpan pesan terakhir agar teks toast tetap tampil saat animasi keluarnya berjalan.
    LaunchedEffect(message) { message?.let { lastMessage = it } }
    LaunchedEffect(selectedVideo?.url, options, preparing, requestedVideoKey) {
        val video = selectedVideo ?: return@LaunchedEffect
        if (video.url != requestedVideoKey) return@LaunchedEffect
        if (preparing) { loadingSeenVideoKey = video.url; return@LaunchedEffect }
        if (loadingSeenVideoKey != video.url || options.isEmpty() || autoStartedVideoKey == video.url) return@LaunchedEffect
        val chosen = default360pOption(options) ?: return@LaunchedEffect
        selectedQuality = chosen.quality
        playback = OnlinePlayback(
            url = chosen.directUrl,
            audioUrl = chosen.audioUrl.takeUnless { chosen.hasAudio },
            title = video.title,
            quality = chosen.quality,
        )
        autoStartedVideoKey = video.url
    }

    val configuration = LocalConfiguration.current
    val orientationDescription = if (configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
        "Ubah ke landscape"
    } else {
        "Ubah ke portrait"
    }

    BoxWithConstraints(
        modifier = Modifier.fillMaxSize().background(YtPalette.Background),
    ) {
        val useNavigationRail = maxWidth >= 760.dp
        Row(Modifier.fillMaxSize().background(YtPalette.Background)) {
            if (useNavigationRail) {
                SideNavigationRail(
                    selected = selectedTab,
                    onSelect = { selectedTab = it },
                )
            }

            Column(
                modifier = (if (useNavigationRail) Modifier.weight(1f) else Modifier.fillMaxWidth())
                    .fillMaxHeight()
                    .background(YtPalette.Background)
                    .drawBehind {
                        drawRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(YtPalette.Red.copy(alpha = .035f), Color.Transparent),
                                startY = 0f,
                                endY = size.height * .22f,
                            ),
                        )
                    }
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .padding(top = 6.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                AnimatedVisibility(
                    visibleState = headerEntrance,
                    enter = fadeIn(tween(350)) + slideInVertically(tween(400, easing = FastOutSlowInEasing)) { -it / 4 },
                ) {
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = if (useNavigationRail) 24.dp else 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        GlassIconButton(onClick = onBack, icon = Icons.Rounded.ArrowBack, description = "Kembali")
                        Spacer(Modifier.size(10.dp))
                        Box(
                            Modifier.size(width = 38.dp, height = 28.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(YtPalette.AccentBrush),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(Icons.Rounded.PlayArrow, contentDescription = "YouTube", tint = Color.White, modifier = Modifier.size(22.dp))
                        }
                        Spacer(Modifier.size(9.dp))
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                            Text("YouTube", color = YtPalette.Text, fontSize = 21.sp, fontWeight = FontWeight.Bold, letterSpacing = (-.65).sp)
                            Text("HansAps · video dan offline", color = YtPalette.Muted, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        GlassIconButton(
                            onClick = {
                                context.findActivity()?.requestedOrientation = if (
                                    configuration.orientation == Configuration.ORIENTATION_PORTRAIT
                                ) ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE else ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                            },
                            icon = Icons.Rounded.ScreenRotation,
                            description = orientationDescription,
                        )
                    }
                }

                AnimatedVisibility(
                    visible = message != null,
                    enter = fadeIn(tween(220)) + slideInVertically(tween(250)) { -it / 3 },
                    exit = fadeOut(tween(180)) + slideOutVertically(tween(180)) { -it / 3 },
                ) {
                    Surface(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = if (useNavigationRail) 24.dp else 14.dp),
                        color = YtPalette.SurfaceAlt,
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Row(
                            Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(Modifier.size(7.dp).clip(CircleShape).background(YtPalette.Red))
                            Spacer(Modifier.size(9.dp))
                            Text(
                                lastMessage,
                                color = YtPalette.Text,
                                fontSize = 11.sp,
                                lineHeight = 15.sp,
                                modifier = Modifier.weight(1f),
                            )
                            TextButton(onClick = vm::consumeMessage) {
                                Text("OK", color = YtPalette.Red, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }

                AnimatedContent(
                    targetState = selectedTab,
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    transitionSpec = {
                        val forward = targetState.ordinal > initialState.ordinal
                        (slideInHorizontally(tween(280, easing = FastOutSlowInEasing)) { if (forward) it / 10 else -it / 10 } +
                            fadeIn(tween(220)))
                            .togetherWith(
                                slideOutHorizontally(tween(260, easing = FastOutSlowInEasing)) { if (forward) -it / 10 else it / 10 } +
                                    fadeOut(tween(160)),
                            )
                    },
                    label = "tabContent",
                ) { tab ->
                    when (tab) {
                        VideoTab.SEARCH -> SearchPane(
                            modifier = Modifier.fillMaxSize().padding(horizontal = if (useNavigationRail) 24.dp else 14.dp),
                            query = query,
                            onQueryChange = { query = it },
                            onSearch = { if (query.isNotBlank()) vm.search(query) },
                            searching = searching,
                            results = results,
                            selectedVideo = selectedVideo,
                            shownVideo = shownVideo,
                            options = options,
                            preparing = preparing,
                            selectedQuality = selectedQuality,
                            playback = playback,
                            onSelectVideo = { video ->
                                autoStartedVideoKey = null
                                requestedVideoKey = video.url
                                loadingSeenVideoKey = null
                                playback = null
                                shownVideo = video
                                vm.prepareVideo(video)
                            },
                            onSelectQuality = { selectedQuality = it.quality },
                            onPlay = { option ->
                                shownVideo?.let { video ->
                                    selectedQuality = option.quality
                                    playback = OnlinePlayback(
                                        url = option.directUrl,
                                        audioUrl = option.audioUrl.takeUnless { option.hasAudio },
                                        title = video.title,
                                        quality = option.quality,
                                    )
                                }
                            },
                            onDownload = { option -> selectedVideo?.let { vm.downloadOption(context, it, option) } },
                            onClosePlayer = { playback = null; autoStartedVideoKey = null },
                        )
                        VideoTab.COLLECTION -> YoutubeCollection(
                            videos = videos,
                            onRefresh = { vm.refresh(context) },
                            modifier = Modifier.fillMaxSize().padding(horizontal = if (useNavigationRail) 24.dp else 14.dp),
                        )
                    }
                }

                if (!useNavigationRail) {
                    FloatingTabBar(
                        selected = selectedTab,
                        onSelect = { selectedTab = it },
                        modifier = Modifier.padding(bottom = 2.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun FloatingTabBar(selected: VideoTab, onSelect: (VideoTab) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(YtPalette.Background)
            .border(BorderStroke(1.dp, YtPalette.Border))
            .padding(horizontal = 8.dp, vertical = 3.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        TabPill(
            label = VideoTab.SEARCH.label,
            icon = Icons.Rounded.Search,
            selected = selected == VideoTab.SEARCH,
            onClick = { onSelect(VideoTab.SEARCH) },
        )
        TabPill(
            label = VideoTab.COLLECTION.label,
            icon = Icons.Rounded.VideoLibrary,
            selected = selected == VideoTab.COLLECTION,
            onClick = { onSelect(VideoTab.COLLECTION) },
        )
    }
}

@Composable
private fun SideNavigationRail(selected: VideoTab, onSelect: (VideoTab) -> Unit) {
    Column(
        modifier = Modifier
            .width(82.dp)
            .fillMaxHeight()
            .background(YtPalette.Background)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .border(BorderStroke(1.dp, YtPalette.Border))
            .padding(horizontal = 7.dp, vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(
            Modifier.size(width = 38.dp, height = 28.dp).clip(RoundedCornerShape(8.dp)).background(YtPalette.AccentBrush),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Rounded.PlayArrow, contentDescription = "YouTube", tint = Color.White, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.size(4.dp))
        NavigationRailItem("Jelajahi", Icons.Rounded.Search, selected == VideoTab.SEARCH) { onSelect(VideoTab.SEARCH) }
        NavigationRailItem("Koleksi", Icons.Rounded.VideoLibrary, selected == VideoTab.COLLECTION) { onSelect(VideoTab.COLLECTION) }
    }
}

@Composable
private fun NavigationRailItem(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) YtPalette.Red.copy(alpha = .08f) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(vertical = 11.dp, horizontal = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Icon(icon, contentDescription = label, tint = if (selected) YtPalette.Red else YtPalette.Text, modifier = Modifier.size(23.dp))
        Text(label, color = if (selected) YtPalette.Red else YtPalette.Text, fontSize = 9.sp, maxLines = 1, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal)
    }
}

@Composable
private fun RowScope.TabPill(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val tint by animateColorAsState(
        targetValue = if (selected) YtPalette.Red else YtPalette.Text,
        animationSpec = tween(220),
        label = "tabTint",
    )
    Column(
        modifier = Modifier
            .weight(1f)
            .height(52.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) YtPalette.Red.copy(alpha = .07f) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(vertical = 5.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(21.dp))
        Text(label, color = tint, fontSize = 10.sp, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal, maxLines = 1)
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

@Composable
private fun SearchPane(
    modifier: Modifier,
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    searching: Boolean,
    results: List<NexrayYoutubeVideo>,
    selectedVideo: NexrayYoutubeVideo?,
    shownVideo: NexrayYoutubeVideo?,
    options: List<YoutubeDownloadOption>,
    preparing: Boolean,
    selectedQuality: String?,
    playback: OnlinePlayback?,
    onSelectVideo: (NexrayYoutubeVideo) -> Unit,
    onSelectQuality: (YoutubeDownloadOption) -> Unit,
    onPlay: (YoutubeDownloadOption) -> Unit,
    onDownload: (YoutubeDownloadOption) -> Unit,
    onClosePlayer: () -> Unit,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SearchBar(query = query, onQueryChange = onQueryChange, searching = searching, onSearch = onSearch)

        AnimatedVisibility(
            visible = selectedVideo != null && shownVideo != null,
            enter = fadeIn(tween(260)) + expandVertically(tween(340, easing = FastOutSlowInEasing)),
            exit = fadeOut(tween(160)) + shrinkVertically(tween(260)),
        ) {
            shownVideo?.let { video ->
                SelectedVideoPanel(
                    video = video,
                    options = options,
                    selectedQuality = selectedQuality,
                    preparing = preparing,
                    onSelectQuality = onSelectQuality,
                    onPlay = onPlay,
                    onDownload = onDownload,
                )
            }
        }

        playback?.let { OnlineVideoPlayer(playback = it, onClose = onClosePlayer) }

        // Fase: 0 = memuat, 1 = kosong, 2 = ada hasil. Crossfade menjaga perpindahan tetap halus.
        val phase = when {
            searching -> 0
            results.isEmpty() -> 1
            else -> 2
        }
        Box(Modifier.weight(1f).fillMaxWidth()) {
            Crossfade(targetState = phase, animationSpec = tween(320), label = "resultsPhase") { current ->
                when (current) {
                    0 -> ShimmerList()
                    1 -> YtEmptyState(
                        icon = Icons.Rounded.Movie,
                        title = "Jelajahi video dari Nexray",
                        body = "Ketuk video untuk langsung diputar pada 360p. Ganti kualitas atau simpan offline dari panel video.",
                    )
                    else -> ResultsList(results = results, selectedVideo = selectedVideo, onSelect = onSelectVideo)
                }
            }
        }
    }
}

@Composable
private fun SearchBar(query: String, onQueryChange: (String) -> Unit, searching: Boolean, onSearch: () -> Unit) {
    val enabled = query.isNotBlank() && !searching
    val searchAlpha by animateFloatAsState(
        targetValue = if (enabled) 1f else .45f,
        animationSpec = tween(250),
        label = "searchAlpha",
    )
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.weight(1f),
            singleLine = true,
            placeholder = { Text("Cari video, channel, atau topik", color = YtPalette.Muted, fontSize = 13.sp) },
            leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null, tint = YtPalette.Muted) },
            shape = RoundedCornerShape(28.dp),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { onSearch() }),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = YtPalette.Red,
                unfocusedBorderColor = YtPalette.Border,
                focusedContainerColor = YtPalette.Surface,
                unfocusedContainerColor = YtPalette.Surface,
                focusedTextColor = YtPalette.Text,
                unfocusedTextColor = YtPalette.Text,
                cursorColor = YtPalette.Red,
            ),
        )
        PressScale(
            onClick = { if (enabled) onSearch() },
            modifier = Modifier.size(52.dp).graphicsLayer { alpha = searchAlpha },
            shape = RoundedCornerShape(26.dp),
            color = YtPalette.SurfaceAlt,
            border = BorderStroke(1.dp, YtPalette.Border),
        ) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                if (searching) {
                    CircularProgressIndicator(Modifier.size(19.dp), strokeWidth = 2.dp, color = YtPalette.Red)
                } else {
                    Icon(Icons.Rounded.Search, contentDescription = "Cari", tint = YtPalette.Text)
                }
            }
        }
    }
}

@Composable
private fun ShimmerList() {
    val shift = rememberShimmerShift()
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        repeat(5) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(YtPalette.Surface)
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                ShimmerBlock(shift, Modifier.size(width = 108.dp, height = 68.dp).clip(RoundedCornerShape(14.dp)))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    ShimmerBlock(shift, Modifier.fillMaxWidth(.92f).height(12.dp).clip(RoundedCornerShape(6.dp)))
                    ShimmerBlock(shift, Modifier.fillMaxWidth(.6f).height(10.dp).clip(RoundedCornerShape(5.dp)))
                    ShimmerBlock(shift, Modifier.fillMaxWidth(.35f).height(8.dp).clip(RoundedCornerShape(4.dp)))
                }
            }
        }
    }
}

@Composable
private fun ResultsList(
    results: List<NexrayYoutubeVideo>,
    selectedVideo: NexrayYoutubeVideo?,
    onSelect: (NexrayYoutubeVideo) -> Unit,
) {
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("${results.size} hasil", color = YtPalette.Muted, fontSize = 12.sp, fontWeight = FontWeight.Medium)
        BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
            if (maxWidth >= 620.dp) {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 270.dp),
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(18.dp),
                    verticalArrangement = Arrangement.spacedBy(22.dp),
                    contentPadding = PaddingValues(start = 2.dp, end = 2.dp, top = 4.dp, bottom = 18.dp),
                ) {
                    items(results, key = { it.url }) { video ->
                        VideoSearchCard(
                            video = video,
                            selected = selectedVideo?.url == video.url,
                            onSelect = { onSelect(video) },
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(17.dp),
                    contentPadding = PaddingValues(start = 1.dp, end = 1.dp, bottom = 18.dp, top = 3.dp),
                ) {
                    items(results, key = { it.url }) { video ->
                        VideoSearchCard(
                            video = video,
                            selected = selectedVideo?.url == video.url,
                            onSelect = { onSelect(video) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun VideoSearchCard(video: NexrayYoutubeVideo, selected: Boolean, onSelect: () -> Unit) {
    PressScale(
        onClick = onSelect,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(13.dp),
        color = if (selected) YtPalette.Red.copy(alpha = .035f) else YtPalette.Background,
        border = if (selected) BorderStroke(1.dp, YtPalette.Red.copy(alpha = .65f)) else null,
    ) {
        Column(Modifier.fillMaxWidth().padding(3.dp)) {
            Box(
                Modifier.fillMaxWidth().aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(11.dp))
                    .background(YtPalette.SurfaceAlt),
            ) {
                AsyncImage(
                    model = video.thumbnail.ifBlank { null },
                    contentDescription = "Thumbnail ${video.title}",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
                if (video.duration.isNotBlank()) {
                    Surface(
                        modifier = Modifier.align(Alignment.BottomEnd).padding(6.dp),
                        shape = RoundedCornerShape(4.dp),
                        color = Color.Black.copy(alpha = .82f),
                    ) {
                        Text(video.duration, modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp), color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
            Row(
                Modifier.fillMaxWidth().padding(top = 11.dp, start = 3.dp, end = 3.dp, bottom = 6.dp),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(9.dp),
            ) {
                Box(
                    Modifier.size(35.dp).clip(CircleShape).background(YtPalette.SurfaceAlt),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Rounded.Movie, contentDescription = null, tint = YtPalette.Muted, modifier = Modifier.size(19.dp))
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(video.title, color = YtPalette.Text, fontSize = 13.sp, lineHeight = 18.sp, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text(video.channel.ifBlank { "YouTube" }, color = YtPalette.Muted, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(video.views.takeIf { it.isNotBlank() } ?: "Video", color = YtPalette.Muted, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

@Composable
private fun VideoSearchRow(video: NexrayYoutubeVideo, index: Int, selected: Boolean, onSelect: () -> Unit) {
    // Setiap baris masuk bertahap; jeda kecil per indeks menciptakan efek berurutan.
    val entrance = remember(video.url) { MutableTransitionState(false) }
    LaunchedEffect(video.url) {
        delay(index.coerceAtMost(8) * 60L)
        entrance.targetState = true
    }
    val background by animateColorAsState(
        targetValue = if (selected) YtPalette.Red.copy(alpha = .12f) else YtPalette.Surface,
        animationSpec = tween(300),
        label = "rowBackground",
    )
    AnimatedVisibility(
        visibleState = entrance,
        enter = fadeIn(tween(400)) + slideInVertically(tween(420, easing = FastOutSlowInEasing)) { it / 4 },
    ) {
        PressScale(
            onClick = onSelect,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            color = background,
            border = BorderStroke(1.dp, if (selected) YtPalette.Red else YtPalette.Border),
        ) {
            Row(
                Modifier.padding(10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    Modifier
                        .size(width = 108.dp, height = 68.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(YtPalette.SurfaceAlt),
                ) {
                    AsyncImage(
                        model = video.thumbnail.ifBlank { null },
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    )
                    // Gradasi halus di bawah thumbnail agar badge durasi lebih terbaca.
                    Box(
                        Modifier
                            .fillMaxSize()
                            .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = .45f)))),
                    )
                    if (video.duration.isNotBlank()) {
                        Surface(
                            modifier = Modifier.align(Alignment.BottomEnd).padding(5.dp),
                            shape = RoundedCornerShape(6.dp),
                            color = Color.Black.copy(alpha = .72f),
                        ) {
                            Text(
                                video.duration,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                                color = Color.White,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        video.title,
                        color = YtPalette.Text,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        video.channel.ifBlank { "YouTube" },
                        color = YtPalette.Muted,
                        fontSize = 10.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        listOf(video.views.takeIf { it.isNotBlank() }?.let { "$it views" }).filterNotNull().filter { it.isNotBlank() }.joinToString(" • "),
                        color = YtPalette.Muted,
                        fontSize = 9.sp,
                    )
                }
                Box(
                    Modifier
                        .clip(RoundedCornerShape(50))
                        .background(
                            if (selected) YtPalette.Red.copy(alpha = .18f) else Color.Transparent,
                            RoundedCornerShape(50),
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                ) {
                    Text(
                        if (selected) "DIPILIH" else "PILIH",
                        color = YtPalette.Red,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
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
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
            .clip(RoundedCornerShape(22.dp))
            .background(YtPalette.Surface)
            .border(
                1.dp,
                Brush.linearGradient(listOf(YtPalette.Red.copy(alpha = .55f), Color.Transparent)),
                RoundedCornerShape(22.dp),
            )
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            video.title,
            color = YtPalette.Text,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        if (preparing) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                CircularProgressIndicator(Modifier.size(17.dp), strokeWidth = 2.dp, color = YtPalette.Red)
                Text("Memuat kualitas dari Nexray…", color = YtPalette.Muted, fontSize = 11.sp)
            }
        } else if (options.isNotEmpty()) {
            val chosen = options.firstOrNull { it.quality == selectedQuality } ?: options.first()
            Text(
                "PILIH KUALITAS",
                color = YtPalette.Muted,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.1.sp,
            )
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                options.forEach { option ->
                    QualityChip(
                        label = option.label,
                        selected = chosen.quality == option.quality,
                        onClick = { onSelectQuality(option) },
                    )
                }
            }
            Text(
                "${chosen.mimeType}${chosen.fileSizeBytes?.let { " • ${it / (1024 * 1024)} MB" } ?: ""}" +
                    if (chosen.hasAudio) " • audio included" else if (!chosen.audioUrl.isNullOrBlank()) " • video + M4A" else " • video only",
                color = YtPalette.Muted,
                fontSize = 9.sp,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = { onPlay(chosen) },
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = YtPalette.SurfaceAlt, contentColor = YtPalette.Text),
                ) {
                    Icon(Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.size(4.dp))
                    Text("Putar", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
                Button(
                    onClick = { onDownload(chosen) },
                    enabled = chosen.hasAudio || !chosen.audioUrl.isNullOrBlank(),
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = YtPalette.Red, contentColor = Color.White),
                ) {
                    Icon(Icons.Rounded.CloudDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.size(4.dp))
                    Text("Download", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }
            if (!chosen.hasAudio && chosen.audioUrl.isNullOrBlank()) {
                Text(
                    "Format ini tidak memiliki audio gabungan dan API tidak mengembalikan track M4A pendamping.",
                    color = YtPalette.Muted,
                    fontSize = 10.sp,
                    lineHeight = 14.sp,
                )
            }
        } else {
            Text("Nexray tidak menyediakan opsi kualitas untuk video ini.", color = YtPalette.Muted, fontSize = 11.sp)
        }
    }
}

@Composable
private fun QualityChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val background by animateColorAsState(
        targetValue = if (selected) YtPalette.Red else YtPalette.SurfaceAlt,
        animationSpec = tween(260),
        label = "chipBackground",
    )
    val scale by animateFloatAsState(
        targetValue = if (selected) 1.05f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "chipScale",
    )
    PressScale(
        onClick = onClick,
        modifier = Modifier.graphicsLayer { scaleX = scale; scaleY = scale },
        shape = RoundedCornerShape(12.dp),
        color = background,
        border = BorderStroke(1.dp, if (selected) YtPalette.Red else YtPalette.Border),
    ) {
        Text(
            label,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

private fun buildOnlinePlayer(context: Context, url: String, audioUrl: String?): ExoPlayer =
    ExoPlayer.Builder(context).build().apply {
        val factory = DefaultMediaSourceFactory(context)
        val videoSource = factory.createMediaSource(MediaItem.fromUri(Uri.parse(url)))
        val source = if (!audioUrl.isNullOrBlank()) {
            MergingMediaSource(videoSource, factory.createMediaSource(MediaItem.fromUri(Uri.parse(audioUrl))))
        } else {
            videoSource
        }
        setMediaSource(source)
        repeatMode = Player.REPEAT_MODE_OFF
        prepare()
        playWhenReady = true
    }

private fun createPlayerView(context: Context, exo: ExoPlayer): PlayerView =
    PlayerView(context).apply {
        this.player = exo
        useController = true
        resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
        setShutterBackgroundColor(android.graphics.Color.BLACK)
        setShowBuffering(PlayerView.SHOW_BUFFERING_ALWAYS)
    }

@Composable
private fun OnlineVideoPlayer(playback: OnlinePlayback, onClose: () -> Unit) {
    val context = LocalContext.current
    var fullscreen by rememberSaveable(playback.url) { mutableStateOf(false) }
    val configuration = LocalConfiguration.current
    val exo = remember(playback.url, playback.audioUrl) { buildOnlinePlayer(context, playback.url, playback.audioUrl) }
    DisposableEffect(exo) { onDispose { exo.release() } }
    val entrance = remember(playback) { MutableTransitionState(false).apply { targetState = true } }

    AnimatedVisibility(
        visibleState = entrance,
        enter = fadeIn(tween(350)) + expandVertically(tween(420, easing = FastOutSlowInEasing)),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(
                        playback.title,
                        color = YtPalette.Text,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        LiveDot()
                        Text(
                            "Memutar • ${playback.quality.ifBlank { "360p default" }}",
                            color = YtPalette.Muted,
                            fontSize = 9.sp,
                        )
                    }
                }
                IconButton(onClick = { fullscreen = true }) {
                    Icon(Icons.Rounded.Fullscreen, contentDescription = "Layar penuh", tint = YtPalette.Text)
                }
                TextButton(onClick = onClose) { Text("Tutup", color = YtPalette.Red, fontSize = 11.sp) }
            }
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(if (configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) 150.dp else 220.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.Black),
            ) {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { ctx -> createPlayerView(ctx, exo) },
                    update = { it.player = exo },
                )
            }
        }
    }

    if (fullscreen) {
        Dialog(
            onDismissRequest = { fullscreen = false },
            properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
        ) {
            Box(Modifier.fillMaxSize().background(Color.Black)) {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { ctx -> createPlayerView(ctx, exo) },
                    update = { it.player = exo },
                )
                Surface(
                    modifier = Modifier.align(Alignment.TopStart).padding(18.dp),
                    shape = CircleShape,
                    color = Color.Black.copy(alpha = .55f),
                ) {
                    IconButton(onClick = { fullscreen = false }) {
                        Icon(Icons.Rounded.Close, contentDescription = "Keluar dari layar penuh", tint = Color.White)
                    }
                }
                Surface(
                    modifier = Modifier.align(Alignment.TopEnd).padding(18.dp),
                    shape = CircleShape,
                    color = Color.Black.copy(alpha = .55f),
                ) {
                    IconButton(
                        onClick = {
                            context.findActivity()?.requestedOrientation = if (
                                configuration.orientation == Configuration.ORIENTATION_PORTRAIT
                            ) ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE else ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                        },
                    ) {
                        Icon(Icons.Rounded.ScreenRotation, contentDescription = "Ubah orientasi layar", tint = Color.White)
                    }
                }
                Surface(
                    modifier = Modifier.align(Alignment.TopCenter).padding(top = 22.dp),
                    shape = RoundedCornerShape(50),
                    color = Color.Black.copy(alpha = .55f),
                ) {
                    Text(
                        playback.title,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        color = Color.White,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

private fun default360pOption(options: List<YoutubeDownloadOption>): YoutubeDownloadOption? {
    if (options.isEmpty()) return null
    fun height(option: YoutubeDownloadOption): Int? =
        Regex("x(\\d{3,4})", RegexOption.IGNORE_CASE).find(option.quality + " " + option.label)?.groupValues?.get(1)?.toIntOrNull()
            ?: Regex("(\\d{3,4})p", RegexOption.IGNORE_CASE).find(option.quality + " " + option.label)?.groupValues?.get(1)?.toIntOrNull()
            ?: Regex("(\\d{3,4})").find(option.quality)?.groupValues?.get(1)?.toIntOrNull()
    return options.sortedWith(
        compareBy<YoutubeDownloadOption> { option -> height(option)?.let { kotlin.math.abs(it - 360) } ?: Int.MAX_VALUE }
            .thenByDescending { it.hasAudio }
            .thenBy { height(it) ?: Int.MAX_VALUE }
    ).first()
}
