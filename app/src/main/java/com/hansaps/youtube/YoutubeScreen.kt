package com.hansaps.youtube

import android.content.Context
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
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
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
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
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
import com.hansaps.core.HansColors
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
    var playback by remember { mutableStateOf<OnlinePlayback?>(null) }
    var shownVideo by remember { mutableStateOf<NexrayYoutubeVideo?>(null) }
    var lastMessage by remember { mutableStateOf("") }
    var autoStartedVideoKey by remember { mutableStateOf<String?>(null) }
    var requestedVideoKey by remember { mutableStateOf<String?>(null) }
    var loadingSeenVideoKey by remember { mutableStateOf<String?>(null) }

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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(HansColors.Background)
            .drawBehind {
                // Cahaya ambient di bagian atas agar layar terasa lebih berdimensi.
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(HansColors.Coral.copy(alpha = .16f), Color.Transparent),
                        startY = 0f,
                        endY = size.height * .35f,
                    ),
                )
            }
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(top = 10.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        AnimatedVisibility(
            visibleState = headerEntrance,
            enter = fadeIn(tween(450)) + slideInVertically(tween(450, easing = FastOutSlowInEasing)) { -it / 3 },
        ) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                GlassIconButton(onClick = onBack, icon = Icons.Rounded.ArrowBack, description = "Kembali")
                Spacer(Modifier.size(12.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("HansAps YouTube", color = HansColors.Text, fontSize = 21.sp, fontWeight = FontWeight.Bold)
                    Text("Cari, pilih kualitas, dan simpan offline", color = HansColors.Muted, fontSize = 11.sp)
                }
                Box(
                    Modifier.size(42.dp).clip(RoundedCornerShape(14.dp)).background(YtPalette.AccentBrush),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Rounded.Movie, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
                }
            }
        }

        AnimatedVisibility(
            visible = message != null,
            enter = fadeIn(tween(250)) + slideInVertically(tween(300)) { -it / 2 },
            exit = fadeOut(tween(200)) + slideOutVertically(tween(200)) { -it / 2 },
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp),
                color = HansColors.SurfaceRaised,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, HansColors.Border),
            ) {
                Row(
                    Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(Modifier.size(8.dp).clip(CircleShape).background(YtPalette.AccentBrush))
                    Spacer(Modifier.size(10.dp))
                    Text(
                        lastMessage,
                        color = HansColors.Text,
                        fontSize = 11.sp,
                        lineHeight = 16.sp,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(onClick = vm::consumeMessage) {
                        Text("OK", color = HansColors.Coral, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        AnimatedContent(
            targetState = selectedTab,
            modifier = Modifier.weight(1f).fillMaxWidth(),
            transitionSpec = {
                // Geser ke arah tab tujuan: kanan untuk tab berikutnya, kiri untuk sebelumnya.
                val forward = targetState.ordinal > initialState.ordinal
                (slideInHorizontally(tween(340, easing = FastOutSlowInEasing)) { if (forward) it / 6 else -it / 6 } +
                    fadeIn(tween(260)))
                    .togetherWith(
                        slideOutHorizontally(tween(340, easing = FastOutSlowInEasing)) { if (forward) -it / 6 else it / 6 } +
                            fadeOut(tween(180)),
                    )
            },
            label = "tabContent",
        ) { tab ->
            when (tab) {
                VideoTab.SEARCH -> SearchPane(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp),
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
                    modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp),
                )
            }
        }

        FloatingTabBar(
            selected = selectedTab,
            onSelect = { selectedTab = it },
            modifier = Modifier.padding(bottom = 4.dp),
        )
    }
}

@Composable
private fun FloatingTabBar(selected: VideoTab, onSelect: (VideoTab) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp)
            .clip(RoundedCornerShape(26.dp))
            .background(HansColors.SurfaceRaised.copy(alpha = .94f))
            .border(1.dp, HansColors.Border, RoundedCornerShape(26.dp))
            .padding(6.dp),
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
private fun RowScope.TabPill(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, selected: Boolean, onClick: () -> Unit) {
    // Pill aktif melebar dan menampilkan label; pill lain mengecil menjadi ikon saja.
    val rowWeight by animateFloatAsState(
        targetValue = if (selected) 1.7f else 1f,
        animationSpec = tween(350, easing = FastOutSlowInEasing),
        label = "pillWeight",
    )
    val background by animateColorAsState(
        targetValue = if (selected) HansColors.Coral.copy(alpha = .16f) else Color.Transparent,
        animationSpec = tween(300),
        label = "pillBackground",
    )
    val tint by animateColorAsState(
        targetValue = if (selected) HansColors.Coral else HansColors.Muted,
        animationSpec = tween(300),
        label = "pillTint",
    )
    Row(
        modifier = Modifier
            .weight(rowWeight)
            .height(46.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(background)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(20.dp))
        AnimatedVisibility(
            visible = selected,
            enter = fadeIn(tween(250)) + expandHorizontally(tween(300)),
            exit = fadeOut(tween(150)) + shrinkHorizontally(tween(200)),
        ) {
            Text(
                label,
                modifier = Modifier.padding(start = 8.dp),
                color = tint,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
            )
        }
    }
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
            placeholder = { Text("Cari video, channel, atau topik", color = HansColors.Muted, fontSize = 13.sp) },
            leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null, tint = HansColors.Muted) },
            shape = RoundedCornerShape(18.dp),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { onSearch() }),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = HansColors.Coral,
                unfocusedBorderColor = HansColors.Border,
                focusedContainerColor = HansColors.Surface,
                unfocusedContainerColor = HansColors.Surface,
                focusedTextColor = HansColors.Text,
                unfocusedTextColor = HansColors.Text,
                cursorColor = HansColors.Coral,
            ),
        )
        PressScale(
            onClick = { if (enabled) onSearch() },
            modifier = Modifier.size(56.dp).graphicsLayer { alpha = searchAlpha },
            shape = RoundedCornerShape(18.dp),
            color = Color.Transparent,
        ) {
            Box(
                Modifier.fillMaxSize().background(YtPalette.AccentBrush),
                contentAlignment = Alignment.Center,
            ) {
                if (searching) {
                    CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = Color.White)
                } else {
                    Icon(Icons.Rounded.Search, contentDescription = "Cari", tint = Color.White)
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
                    .background(HansColors.Surface)
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
        Text("${results.size} hasil", color = HansColors.Muted, fontSize = 11.sp, fontWeight = FontWeight.Medium)
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(bottom = 16.dp),
        ) {
            itemsIndexed(results, key = { _, video -> video.url }) { index, video ->
                VideoSearchRow(
                    video = video,
                    index = index,
                    selected = selectedVideo?.url == video.url,
                    onSelect = { onSelect(video) },
                )
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
        targetValue = if (selected) HansColors.Coral.copy(alpha = .12f) else HansColors.Surface,
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
            border = BorderStroke(1.dp, if (selected) HansColors.Coral else HansColors.Border),
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
                        .background(HansColors.SurfaceRaised),
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
                        color = HansColors.Text,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        video.channel.ifBlank { "YouTube" },
                        color = HansColors.Muted,
                        fontSize = 10.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        listOf(video.views.takeIf { it.isNotBlank() }?.let { "$it views" }).filterNotNull().filter { it.isNotBlank() }.joinToString(" • "),
                        color = HansColors.Muted,
                        fontSize = 9.sp,
                    )
                }
                Box(
                    Modifier
                        .clip(RoundedCornerShape(50))
                        .background(
                            if (selected) HansColors.Coral.copy(alpha = .18f) else Color.Transparent,
                            RoundedCornerShape(50),
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                ) {
                    Text(
                        if (selected) "DIPILIH" else "PILIH",
                        color = HansColors.Coral,
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
            .background(HansColors.Surface)
            .border(
                1.dp,
                Brush.linearGradient(listOf(HansColors.Coral.copy(alpha = .55f), Color.Transparent)),
                RoundedCornerShape(22.dp),
            )
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            video.title,
            color = HansColors.Text,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        if (preparing) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                CircularProgressIndicator(Modifier.size(17.dp), strokeWidth = 2.dp, color = HansColors.Coral)
                Text("Memuat kualitas dari Nexray…", color = HansColors.Muted, fontSize = 11.sp)
            }
        } else if (options.isNotEmpty()) {
            val chosen = options.firstOrNull { it.quality == selectedQuality } ?: options.first()
            Text(
                "PILIH KUALITAS",
                color = HansColors.Muted,
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
                color = HansColors.Muted,
                fontSize = 9.sp,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = { onPlay(chosen) },
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = HansColors.SurfaceRaised, contentColor = HansColors.Text),
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
                    colors = ButtonDefaults.buttonColors(containerColor = HansColors.Coral, contentColor = Color.White),
                ) {
                    Icon(Icons.Rounded.CloudDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.size(4.dp))
                    Text("Download", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }
            if (!chosen.hasAudio && chosen.audioUrl.isNullOrBlank()) {
                Text(
                    "Format ini tidak memiliki audio gabungan dan API tidak mengembalikan track M4A pendamping.",
                    color = HansColors.Muted,
                    fontSize = 10.sp,
                    lineHeight = 14.sp,
                )
            }
        } else {
            Text("Nexray tidak menyediakan opsi kualitas untuk video ini.", color = HansColors.Muted, fontSize = 11.sp)
        }
    }
}

@Composable
private fun QualityChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val background by animateColorAsState(
        targetValue = if (selected) HansColors.Coral else HansColors.SurfaceRaised,
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
        border = BorderStroke(1.dp, if (selected) HansColors.Coral else HansColors.Border),
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
    var fullscreen by remember(playback) { mutableStateOf(false) }
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
                        color = HansColors.Text,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        LiveDot()
                        Text(
                            "Memutar • ${playback.quality.ifBlank { "360p default" }}",
                            color = HansColors.Muted,
                            fontSize = 9.sp,
                        )
                    }
                }
                IconButton(onClick = { fullscreen = true }) {
                    Icon(Icons.Rounded.Fullscreen, contentDescription = "Layar penuh", tint = HansColors.Text)
                }
                TextButton(onClick = onClose) { Text("Tutup", color = HansColors.Coral, fontSize = 11.sp) }
            }
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(220.dp)
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
