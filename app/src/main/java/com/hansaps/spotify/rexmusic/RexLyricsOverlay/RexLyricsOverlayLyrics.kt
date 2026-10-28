package com.hansaps.spotify.rexmusic

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.Lyrics
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
internal fun __splitImpl_SyncedLyrics(
    lyrics: Lyrics,
    positionState: State<Long>,
    onSeekTo: ((Long) -> Unit)?
) {
    val listState: LazyListState = rememberLazyListState()
    var userScrollUntil by remember { mutableLongStateOf(0L) }

    LaunchedEffect(listState) {
        snapshotFlow { listState.isScrollInProgress }.collect { scrolling ->
            if (scrolling) userScrollUntil = System.currentTimeMillis() + AUTO_SCROLL_IDLE_MS
        }
    }

    // hanya berubah saat baris aktif ganti -> tidak recompose tiap tick posisi
    val activeIndex by remember(lyrics) {
        derivedStateOf {
            val list = lyrics.synced
            val pos = positionState.value
            var lo = 0
            var hi = list.size - 1
            var result = -1
            while (lo <= hi) {
                val mid = (lo + hi) ushr 1
                if (list[mid].timeMs <= pos) { result = mid; lo = mid + 1 } else hi = mid - 1
            }
            result
        }
    }

    LaunchedEffect(activeIndex) {
        if (activeIndex < 0) return@LaunchedEffect
        while (System.currentTimeMillis() < userScrollUntil) delay(300)
        val offset = -(listState.layoutInfo.viewportSize.height / 3)
        listState.animateScrollToItem(activeIndex, offset)
    }

    LazyColumn(
        Modifier.fillMaxSize().fadeEdges(72.dp),
        state = listState,
        contentPadding = PaddingValues(start = 28.dp, end = 28.dp, top = 120.dp, bottom = 220.dp)
    ) {
        itemsIndexed(lyrics.synced, key = { i, _ -> i }) { idx, line ->
            val distance = if (activeIndex < 0) 3 else kotlin.math.abs(idx - activeIndex)
            val active = idx == activeIndex
            val alpha by animateFloatAsState(
                targetValue = when {
                    active -> 1f
                    distance == 1 -> 0.55f
                    distance == 2 -> 0.4f
                    else -> 0.28f
                },
                animationSpec = tween(280), label = "lyricAlpha"
            )
            val scale by animateFloatAsState(
                if (active) 1f else 0.88f, tween(280), label = "lyricScale"
            )
            Text(
                text = line.text.ifBlank { "" },
                style = TextStyle(
                    fontSize = 26.sp,
                    lineHeight = 34.sp,
                    fontWeight = FontWeight.Bold,
                    shadow = if (active) androidx.compose.ui.graphics.Shadow(
                        color = Color.White.copy(alpha = 0.45f),
                        blurRadius = 24f
                    ) else null
                ),
                color = Color.White.copy(alpha = alpha),
                textAlign = TextAlign.Start,
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        transformOrigin = TransformOrigin(0f, 0.5f)
                    }
                    .clip(RoundedCornerShape(12.dp))
                    .then(
                        if (onSeekTo != null) Modifier.clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { onSeekTo(line.timeMs) } else Modifier
                    )
                    .padding(vertical = 10.dp)
            )
        }
    }
}

/**
 * Overlay lirik premium: background foto cover (blur + scrim), piringan hitam berputar,
 * lirik synced dengan fade edge, tap baris untuk seek (opsional lewat [onSeekTo]).
 */
@Composable
internal fun __splitImpl_RexLyricsOverlay(
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
    val base = accent ?: MaterialTheme.colorScheme.primary

    BoxWithConstraints(
        modifier
            .fillMaxSize()
            .background(RexPlayerBase)
            .pointerInput(Unit) { }
    ) {
        val landscape = maxWidth > maxHeight && maxWidth >= 560.dp
        LyricsBackdrop(track.cover, base)

        if (landscape) {
            Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
                LyricsHeader(onClose)
                Row(Modifier.weight(1f).fillMaxWidth()) {
                    Column(
                        Modifier
                            .weight(0.82f)
                            .fillMaxHeight()
                    ) {
                        NowSpinning(track, isPlaying, base)
                        Spacer(Modifier.weight(1f))
                        LyricsFooter(positionState, durationMs, base, onSeekTo)
                    }
                    LyricsBody(
                        lyrics = lyrics,
                        loading = loading,
                        error = error,
                        positionState = positionState,
                        onSeekTo = onSeekTo,
                        modifier = Modifier.weight(1.18f).fillMaxHeight()
                    )
                }
            }
        } else {
            Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
                LyricsHeader(onClose)
                NowSpinning(track, isPlaying, base)
                LyricsBody(
                    lyrics = lyrics,
                    loading = loading,
                    error = error,
                    positionState = positionState,
                    onSeekTo = onSeekTo,
                    modifier = Modifier.weight(1f).fillMaxWidth()
                )
                LyricsFooter(positionState, durationMs, base, onSeekTo)
            }
        }
    }
}
