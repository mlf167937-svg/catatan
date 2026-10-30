package com.hansaps.youtube.screens

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.ScreenRotation
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
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
import com.hansaps.core.LocalVideo
import com.hansaps.youtube.ui.PressScale
import com.hansaps.youtube.ui.YtEmptyState
import com.hansaps.youtube.ui.YtPalette
import com.hansaps.youtube.notifications.YoutubePlaybackNotification
import com.hansaps.youtube.notifications.YoutubeDownloadUiState
import kotlinx.coroutines.delay

@Composable
fun YoutubeCollection(
    videos: List<LocalVideo>,
    downloads: List<YoutubeDownloadUiState> = emptyList(),
    onRefresh: () -> Unit,
    onSelectVideo: (LocalVideo) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        CollectionHeader(count = videos.size, onRefresh = onRefresh)
        downloads.forEach { item -> CollectionDownloadCard(item) }

        if (videos.isEmpty()) {
            if (downloads.isEmpty()) {
                YtEmptyState(
                    icon = Icons.Rounded.Movie,
                    title = "Belum ada video",
                    body = "Cari video di tab Jelajahi lalu tekan Download. Video yang sudah tersimpan dapat diputar tanpa internet.",
                )
            } else {
                Box(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "Video yang selesai diunduh akan muncul di sini.",
                        color = YtPalette.Muted,
                        fontSize = 12.sp,
                    )
                }
            }
        } else {
            BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
                if (maxWidth >= 620.dp) {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = if (maxWidth >= 1000.dp) 260.dp else 225.dp),
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        contentPadding = PaddingValues(start = 2.dp, end = 2.dp, top = 4.dp, bottom = 18.dp),
                    ) {
                        items(videos, key = { it.id }) { video ->
                            CollectionGridCard(video = video, onClick = { onSelectVideo(video) })
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(top = 3.dp, bottom = 18.dp),
                    ) {
                        itemsIndexed(videos, key = { _, video -> video.id }) { index, video ->
                            CollectionRow(video = video, index = index, selected = false, onClick = { onSelectVideo(video) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CollectionDownloadCard(item: YoutubeDownloadUiState) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = if (item.failed) Color(0xFFFFF1F1) else YtPalette.Surface,
        border = BorderStroke(1.dp, if (item.failed) Color(0xFFFFD6D6) else YtPalette.Border),
        shadowElevation = 1.dp,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 13.dp, vertical = 11.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Rounded.Movie,
                    contentDescription = null,
                    tint = if (item.failed) Color(0xFFB3261E) else YtPalette.Red,
                    modifier = Modifier.size(19.dp),
                )
                Spacer(Modifier.size(8.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                    Text(
                        item.title,
                        color = YtPalette.Text,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        "YouTube · ${item.quality}",
                        color = YtPalette.Muted,
                        fontSize = 10.sp,
                        maxLines = 1,
                    )
                }
                Text(
                    when {
                        item.failed -> "Gagal"
                        item.finished -> "Selesai"
                        item.indeterminate -> "Proses"
                        else -> "${item.percent.coerceIn(0, 100)}%"
                    },
                    color = if (item.failed) Color(0xFFB3261E) else YtPalette.Red,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
            if (item.indeterminate && !item.finished) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth().height(5.dp),
                    color = YtPalette.Red,
                    trackColor = YtPalette.SurfaceAlt,
                )
            } else {
                LinearProgressIndicator(
                    progress = { if (item.failed) 0f else item.percent.coerceIn(0, 100) / 100f },
                    modifier = Modifier.fillMaxWidth().height(5.dp),
                    color = if (item.failed) Color(0xFFB3261E) else YtPalette.Red,
                    trackColor = YtPalette.SurfaceAlt,
                )
            }
            Text(
                item.message,
                color = if (item.failed) Color(0xFFB3261E) else YtPalette.Muted,
                fontSize = 10.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun CollectionGridCard(video: LocalVideo, onClick: () -> Unit) {
    PressScale(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = YtPalette.Surface,
        border = BorderStroke(1.dp, YtPalette.Border),
    ) {
        Column(Modifier.fillMaxWidth().padding(8.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Box(
                Modifier.fillMaxWidth().aspectRatio(16f / 9f).clip(RoundedCornerShape(11.dp)).background(YtPalette.AccentSoftBrush),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.Movie, contentDescription = null, tint = YtPalette.Red, modifier = Modifier.size(35.dp))
                Surface(
                    modifier = Modifier.align(Alignment.BottomEnd).padding(6.dp),
                    shape = RoundedCornerShape(5.dp),
                    color = Color.Black.copy(alpha = .78f),
                ) {
                    Text("OFFLINE", modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp), color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }
                Surface(
                    modifier = Modifier.align(Alignment.Center),
                    shape = CircleShape,
                    color = Color.White.copy(alpha = .92f),
                ) {
                    Icon(Icons.Rounded.PlayArrow, contentDescription = "Putar ${video.title}", tint = YtPalette.Red, modifier = Modifier.padding(9.dp).size(23.dp))
                }
            }
            Text(video.title, color = YtPalette.Text, fontSize = 13.sp, lineHeight = 18.sp, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(video.displayName.ifBlank { "Tersimpan di perangkat" }, color = YtPalette.Muted, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun CollectionHeader(count: Int, onRefresh: () -> Unit) {
    var spins by remember { mutableStateOf(0) }
    val rotation by animateFloatAsState(
        targetValue = spins * 360f,
        animationSpec = tween(650, easing = FastOutSlowInEasing),
        label = "refreshSpin",
    )
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text("Koleksi video", color = YtPalette.Text, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Text(
                "$count video • tersimpan di Download/HansAps/Youtube",
                color = YtPalette.Muted,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        PressScale(
            onClick = {
                spins += 1
                onRefresh()
            },
            shape = RoundedCornerShape(50),
            color = YtPalette.SurfaceAlt,
            border = BorderStroke(1.dp, YtPalette.Border),
        ) {
            Row(
                Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Rounded.Refresh,
                    contentDescription = null,
                    tint = YtPalette.Muted,
                    modifier = Modifier.size(16.dp).graphicsLayer { rotationZ = rotation },
                )
                Spacer(Modifier.size(6.dp))
                Text("Refresh", color = YtPalette.Muted, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
private fun CollectionRow(video: LocalVideo, index: Int, selected: Boolean, onClick: () -> Unit) {
    // Baris masuk bertahap dari bawah, dengan jeda kecil per indeks.
    val entrance = remember(video.id) { MutableTransitionState(false) }
    LaunchedEffect(video.id) {
        delay(index.coerceAtMost(8) * 55L)
        entrance.targetState = true
    }
    val background by animateColorAsState(
        targetValue = if (selected) YtPalette.Red.copy(alpha = .12f) else YtPalette.Surface,
        animationSpec = tween(300),
        label = "collectionRowBackground",
    )
    AnimatedVisibility(
        visibleState = entrance,
        enter = fadeIn(tween(380)) + slideInVertically(tween(400, easing = FastOutSlowInEasing)) { it / 3 },
    ) {
        PressScale(
            onClick = onClick,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            color = background,
            border = BorderStroke(1.dp, if (selected) YtPalette.Red else YtPalette.Border),
        ) {
            Row(
                Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(YtPalette.AccentSoftBrush),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        if (selected) Icons.Rounded.PlayArrow else Icons.Rounded.Movie,
                        contentDescription = null,
                        tint = YtPalette.Red,
                        modifier = Modifier.size(26.dp),
                    )
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        video.title,
                        color = YtPalette.Text,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        "${video.displayName} • Offline",
                        color = YtPalette.Muted,
                        fontSize = 10.sp,
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
                        if (selected) "SEDANG" else "PUTAR",
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
fun YoutubeLocalWatchScreen(video: LocalVideo, onBack: () -> Unit) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val landscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val exo = remember(video.uri) { buildLocalPlayer(context, video) }
    DisposableEffect(exo) {
        YoutubePlaybackNotification.bind(context, exo)
        onDispose {
            YoutubePlaybackNotification.unbind(context, exo)
            exo.release()
        }
    }
    LaunchedEffect(exo, video.id, video.title) {
        YoutubePlaybackNotification.updateMetadata(context, exo, video.title, null, "Offline")
    }

    BoxWithConstraints(Modifier.fillMaxSize().background(YtPalette.Background)) {
        val wide = maxWidth >= 900.dp
        val playerWidth = if (wide) maxWidth * .64f else maxWidth
        val playerHeight = minOf(playerWidth * 9f / 16f, maxHeight * (if (landscape) .58f else .52f)).coerceAtLeast(145.dp)

        Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
            Row(
                Modifier.fillMaxWidth().heightIn(min = 52.dp).padding(horizontal = if (wide) 24.dp else 12.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                androidx.compose.material3.IconButton(onClick = onBack) {
                    Icon(Icons.Rounded.ArrowBack, contentDescription = "Kembali ke koleksi", tint = YtPalette.Text)
                }
                Box(Modifier.size(width = 38.dp, height = 28.dp).clip(RoundedCornerShape(8.dp)).background(YtPalette.AccentSoftBrush), contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.PlayArrow, contentDescription = "YouTube offline", tint = YtPalette.Red, modifier = Modifier.size(22.dp))
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                    Text("Pemutar offline", color = YtPalette.Text, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text("Tersimpan di perangkat", color = YtPalette.Muted, fontSize = 10.sp)
                }
                androidx.compose.material3.IconButton(onClick = {
                    context.findHostActivity()?.requestedOrientation = if (landscape) ActivityInfo.SCREEN_ORIENTATION_PORTRAIT else ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                }) {
                    Icon(Icons.Rounded.ScreenRotation, contentDescription = if (landscape) "Ubah ke portrait" else "Ubah ke landscape", tint = YtPalette.Text)
                }
            }

            if (wide) {
                Row(Modifier.weight(1f).fillMaxWidth().padding(horizontal = 24.dp, vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(22.dp)) {
                    Column(Modifier.weight(1.8f).fillMaxHeight().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        LocalVideoSurface(video, exo, playerHeight, onBack)
                        LocalVideoDetails(video)
                    }
                    Column(Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Video offline", color = YtPalette.Text, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Surface(shape = RoundedCornerShape(16.dp), color = YtPalette.SurfaceAlt, border = BorderStroke(1.dp, YtPalette.Border)) {
                            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Icon(Icons.Rounded.Movie, contentDescription = null, tint = YtPalette.Red, modifier = Modifier.size(30.dp))
                                Text("Siap ditonton kapan saja", color = YtPalette.Text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                                Text("Video dimainkan langsung dari penyimpanan lokal. Koneksi internet tidak diperlukan.", color = YtPalette.Muted, fontSize = 12.sp, lineHeight = 18.sp)
                                Text(video.displayName, color = YtPalette.Muted, fontSize = 10.sp, maxLines = 3, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
            } else {
                Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 12.dp).padding(top = 6.dp, bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    LocalVideoSurface(video, exo, playerHeight, onBack)
                    LocalVideoDetails(video)
                    Surface(shape = RoundedCornerShape(14.dp), color = YtPalette.SurfaceAlt, border = BorderStroke(1.dp, YtPalette.Border)) {
                        Row(Modifier.fillMaxWidth().padding(13.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Movie, contentDescription = null, tint = YtPalette.Red, modifier = Modifier.size(22.dp))
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                Text("Tersedia offline", color = YtPalette.Text, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                Text(video.displayName, color = YtPalette.Muted, fontSize = 10.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LocalVideoSurface(video: LocalVideo, exo: ExoPlayer, playerHeight: androidx.compose.ui.unit.Dp, onBack: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("SEDANG DIPUTAR", color = YtPalette.Muted, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                Text("Video lokal", color = YtPalette.Text, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
            androidx.compose.material3.IconButton(onClick = onBack) {
                Icon(Icons.Rounded.Close, contentDescription = "Tutup pemutar", tint = YtPalette.Text)
            }
        }
        Box(Modifier.fillMaxWidth().height(playerHeight).clip(RoundedCornerShape(14.dp)).background(Color.Black)) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx -> createLocalPlayerView(ctx, exo) },
                update = { it.player = exo },
            )
        }
    }
}

@Composable
private fun LocalVideoDetails(video: LocalVideo) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 2.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(video.title, color = YtPalette.Text, fontSize = 18.sp, lineHeight = 24.sp, fontWeight = FontWeight.Bold, maxLines = 3, overflow = TextOverflow.Ellipsis)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(color = YtPalette.Red.copy(alpha = .09f), shape = RoundedCornerShape(8.dp)) {
                Text("OFFLINE", modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp), color = YtPalette.Red, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
            Text(video.displayName, color = YtPalette.Muted, fontSize = 11.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}

private fun buildLocalPlayer(context: Context, video: LocalVideo): ExoPlayer =
    ExoPlayer.Builder(context).build().apply {
        setMediaItem(MediaItem.fromUri(video.uri))
        repeatMode = Player.REPEAT_MODE_OFF
        prepare()
        playWhenReady = true
    }

private fun createLocalPlayerView(context: Context, exo: ExoPlayer): PlayerView =
    PlayerView(context).apply {
        player = exo
        useController = true
        resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
        setShutterBackgroundColor(android.graphics.Color.BLACK)
        setShowBuffering(PlayerView.SHOW_BUFFERING_ALWAYS)
        setKeepContentOnPlayerReset(true)
    }

private tailrec fun Context.findHostActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findHostActivity()
    else -> null
}
