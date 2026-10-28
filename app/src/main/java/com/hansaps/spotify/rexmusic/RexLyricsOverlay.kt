package com.hansaps.spotify.rexmusic

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Lyrics
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

internal const val AUTO_SCROLL_IDLE_MS = 3000L

/**
 * Overlay lirik premium: background foto cover (blur + scrim), piringan hitam berputar,
 * lirik synced dengan fade edge, tap baris untuk seek (opsional lewat [onSeekTo]).
 */
@Composable
fun RexLyricsOverlay(
    track: RexTrack,
    lyrics: Lyrics,
    loading: Boolean,
    error: String?,
    accent: Color?,
    positionState: State<Long>,
    durationMs: Long,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    isPlaying: Boolean = true,
    onSeekTo: ((Long) -> Unit)? = null
) {
        return __splitImpl_RexLyricsOverlay(track, lyrics, loading, error, accent, positionState, durationMs, onClose, modifier, isPlaying, onSeekTo)
    }


@Composable
internal fun LyricsBody(
    lyrics: Lyrics,
    loading: Boolean,
    error: String?,
    positionState: State<Long>,
    onSeekTo: ((Long) -> Unit)?,
    modifier: Modifier = Modifier
) {
    Box(modifier) {
        when {
            loading -> LyricsLoading()
            !error.isNullOrBlank() -> LyricsMessage(error)
            lyrics.isEmpty -> LyricsMessage("Lirik tidak tersedia untuk lagu ini", showIcon = true)
            lyrics.hasSynced -> SyncedLyrics(lyrics, positionState, onSeekTo)
            else -> PlainLyrics(lyrics.plain)
        }
    }
}

//  Background 

@Composable
internal fun LyricsBackdrop(cover: String, accent: Color) {
    Box(Modifier.fillMaxSize()) {
        if (cover.isNotBlank()) {
            CoverImage(
                url = cover,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { scaleX = 1.5f; scaleY = 1.5f; alpha = 0.9f }
                    .blur(48.dp),
                shape = RoundedCornerShape(0.dp)
            )
        }
        // tint warna cover + scrim supaya teks tetap terbaca
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    0f to androidx.compose.ui.graphics.lerp(Color.Black, accent, 0.20f),
                    0.45f to Color.Black.copy(alpha = 0.82f),
                    1f to Color.Black.copy(alpha = 0.94f)
                )
            )
        )
    }
}

//  Header 

@Composable
internal fun LyricsHeader(onClose: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RexIconButton(
            Icons.Default.KeyboardArrowDown, "Tutup lirik", onClose,
            size = 48.dp, iconSize = 32.dp, tint = Color.White
        )
        Text(
            "LIRIK",
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.7f),
            letterSpacing = 3.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f)
        )
        Spacer(Modifier.size(48.dp))
    }
}

//  Vinyl 

@Composable
internal fun NowSpinning(track: RexTrack, isPlaying: Boolean, accent: Color) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        VinylDisc(track.cover, isPlaying, accent, size = 112.dp)
        Spacer(Modifier.width(18.dp))
        Column(Modifier.weight(1f)) {
            Text(
                track.title,
                style = MaterialTheme.typography.titleLarge,
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(4.dp))
            Text(
                track.artist,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.72f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (track.album.isNotBlank() && track.album != "Single") {
                Text(
                    track.album,
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White.copy(alpha = 0.5f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
internal fun VinylDisc(cover: String, spinning: Boolean, accent: Color, size: Dp) {
        return __splitImpl_VinylDisc(cover, spinning, accent, size)
    }

//  States 

@Composable
private fun LyricsLoading() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = Color.White, strokeWidth = 3.dp, modifier = Modifier.size(40.dp))
    }
}

@Composable
private fun LyricsMessage(message: String, showIcon: Boolean = false) {
    Column(
        Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (showIcon) {
            Icon(
                Icons.Default.Lyrics, contentDescription = null,
                tint = Color.White.copy(alpha = 0.4f), modifier = Modifier.size(40.dp)
            )
            Spacer(Modifier.height(12.dp))
        }
        Text(
            message,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.7f),
            textAlign = TextAlign.Center
        )
    }
}

//  Lyrics 

/** Fade halus di tepi atas & bawah daftar lirik. */
internal fun Modifier.fadeEdges(height: Dp = 56.dp): Modifier = this
    .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
    .drawWithContent {
        drawContent()
        val h = height.toPx()
        drawRect(
            Brush.verticalGradient(0f to Color.Transparent, 1f to Color.Black, endY = h),
            size = androidx.compose.ui.geometry.Size(size.width, h),
            blendMode = BlendMode.DstIn
        )
        drawRect(
            Brush.verticalGradient(
                0f to Color.Black, 1f to Color.Transparent,
                startY = size.height - h, endY = size.height
            ),
            topLeft = Offset(0f, size.height - h),
            size = androidx.compose.ui.geometry.Size(size.width, h),
            blendMode = BlendMode.DstIn
        )
    }

@Composable
private fun PlainLyrics(text: String) {
    val lines = remember(text) { text.split("\n").filter { it.isNotBlank() } }
    LazyColumn(
        Modifier.fillMaxSize().fadeEdges(),
        contentPadding = PaddingValues(horizontal = 28.dp, vertical = 32.dp)
    ) {
        itemsIndexed(lines) { _, line ->
            Text(
                line,
                style = MaterialTheme.typography.titleMedium,
                color = Color.White.copy(alpha = 0.9f),
                modifier = Modifier.padding(vertical = 6.dp)
            )
        }
    }
}

@Composable
internal fun SyncedLyrics(
    lyrics: Lyrics,
    positionState: State<Long>,
    onSeekTo: ((Long) -> Unit)?
) {
        return __splitImpl_SyncedLyrics(lyrics, positionState, onSeekTo)
    }

//  Footer 

@Composable
internal fun LyricsFooter(
    positionState: State<Long>,
    durationMs: Long,
    accent: Color,
    onSeekTo: ((Long) -> Unit)?
) {
        return __splitImpl_LyricsFooter(positionState, durationMs, accent, onSeekTo)
    }

internal fun fmtLyrics(ms: Long): String {
    val sec = (ms / 1000).coerceAtLeast(0)
    return "%d:%02d".format(sec / 60, sec % 60)
}
