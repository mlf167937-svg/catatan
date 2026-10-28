package com.hansaps.youtube.screens

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.hansaps.core.HansColors
import com.hansaps.core.LocalVideo
import com.hansaps.youtube.ui.PressScale
import com.hansaps.youtube.ui.YtEmptyState
import com.hansaps.youtube.ui.YtPalette
import kotlinx.coroutines.delay

@Composable
fun YoutubeCollection(
    videos: List<LocalVideo>,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedVideo by remember { mutableStateOf<LocalVideo?>(null) }

    Column(modifier = modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        CollectionHeader(count = videos.size, onRefresh = onRefresh)

        // Pemutar muncul dengan animasi; ganti video cukup dengan mengetuk baris lain.
        selectedVideo?.let { LocalVideoPlayer(video = it, onClose = { selectedVideo = null }) }

        if (videos.isEmpty()) {
            YtEmptyState(
                icon = Icons.Rounded.Movie,
                title = "Belum ada video",
                body = "Cari video di tab Jelajahi lalu tekan Download. Video yang sudah tersimpan dapat diputar tanpa internet.",
            )
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                itemsIndexed(videos, key = { _, video -> video.id }) { index, video ->
                    CollectionRow(
                        video = video,
                        index = index,
                        selected = selectedVideo?.id == video.id,
                        onClick = { selectedVideo = video },
                    )
                }
            }
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
            Text("Koleksi video", color = HansColors.Text, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Text(
                "$count video • tersimpan di Download/HansAps/Youtube",
                color = HansColors.Muted,
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
            color = HansColors.SurfaceRaised,
            border = BorderStroke(1.dp, HansColors.Border),
        ) {
            Row(
                Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Rounded.Refresh,
                    contentDescription = null,
                    tint = HansColors.Muted,
                    modifier = Modifier.size(16.dp).graphicsLayer { rotationZ = rotation },
                )
                Spacer(Modifier.size(6.dp))
                Text("Refresh", color = HansColors.Muted, fontSize = 12.sp, fontWeight = FontWeight.Medium)
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
        targetValue = if (selected) HansColors.Coral.copy(alpha = .12f) else HansColors.Surface,
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
            border = BorderStroke(1.dp, if (selected) HansColors.Coral else HansColors.Border),
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
                        tint = HansColors.Coral,
                        modifier = Modifier.size(26.dp),
                    )
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        video.title,
                        color = HansColors.Text,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        "${video.displayName} • Offline",
                        color = HansColors.Muted,
                        fontSize = 10.sp,
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
                        if (selected) "SEDANG" else "PUTAR",
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
private fun LocalVideoPlayer(video: LocalVideo, onClose: () -> Unit) {
    val context = LocalContext.current
    val exo = remember(video.uri) { buildLocalPlayer(context, video) }
    DisposableEffect(exo) { onDispose { exo.release() } }
    val entrance = remember(video.uri) { MutableTransitionState(false).apply { targetState = true } }

    AnimatedVisibility(
        visibleState = entrance,
        enter = fadeIn(tween(350)) + expandVertically(tween(420, easing = FastOutSlowInEasing)),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    video.title,
                    color = HansColors.Text,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                IconButton(onClick = onClose) {
                    Icon(Icons.Rounded.Close, contentDescription = "Tutup", tint = HansColors.Coral)
                }
            }
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(210.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.Black),
            ) {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { ctx -> createLocalPlayerView(ctx, exo) },
                    update = { it.player = exo },
                )
            }
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
        this.player = exo
        useController = true
        resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
        setShutterBackgroundColor(android.graphics.Color.BLACK)
    }
