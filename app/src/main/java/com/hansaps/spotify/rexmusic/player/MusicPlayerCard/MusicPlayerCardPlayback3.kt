package com.hansaps.spotify.rexmusic.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.hansaps.spotify.rexmusic.*
import kotlin.math.roundToInt

@Composable
internal fun __splitImpl_SeekBar(progress: Float, buffered: Float, onSeek: (Float) -> Unit) {
    val currentOnSeek by rememberUpdatedState(onSeek)
    var widthPx by remember { mutableFloatStateOf(1f) }
    val thumb = 14.dp
    Box(
        Modifier
            .fillMaxWidth()
            .height(28.dp)
            .padding(horizontal = thumb / 2)
            .onSizeChanged { widthPx = it.width.toFloat().coerceAtLeast(1f) }
            .pointerInput(Unit) {
                detectTapGestures { currentOnSeek((it.x / widthPx).coerceIn(0f, 1f)) }
            }
            .pointerInput(Unit) {
                detectHorizontalDragGestures { change, _ ->
                    change.consume()
                    currentOnSeek((change.position.x / widthPx).coerceIn(0f, 1f))
                }
            }
            .semantics {
                contentDescription = "Progres lagu"
                progressBarRangeInfo = ProgressBarRangeInfo(progress, 0f..1f)
                setProgress { currentOnSeek(it.coerceIn(0f, 1f)); true }
            },
        contentAlignment = Alignment.CenterStart
    ) {
        Box(Modifier.fillMaxWidth().height(4.dp).clip(CircleShape).background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.18f)))
        Box(Modifier.fillMaxWidth(buffered.coerceIn(0f, 1f)).height(4.dp).clip(CircleShape).background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f)))
        Box(Modifier.fillMaxWidth(progress).height(4.dp).clip(CircleShape).background(MaterialTheme.colorScheme.onBackground))
        Box(
            Modifier
                .offset { IntOffset((progress * widthPx - thumb.toPx() / 2).roundToInt(), 0) }
                .size(thumb)
                .background(MaterialTheme.colorScheme.onBackground, CircleShape)
        )
    }
}

@Composable
internal fun __splitImpl_SeekSection(
    positionState: State<Long>,
    durationMs: Long,
    bufferedPercent: Int,
    busy: Boolean,
    onSeek: (Float) -> Unit
) {
    val positionMs = positionState.value
    val progress = if (durationMs > 0) positionMs.toFloat() / durationMs else 0f
    val muted = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
    Column {
        if (durationMs <= 0L && busy) {
            Box(Modifier.fillMaxWidth().height(28.dp).padding(horizontal = 7.dp), contentAlignment = Alignment.Center) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth().height(4.dp).clip(CircleShape),
                    color = MaterialTheme.colorScheme.onBackground, trackColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.2f)
                )
            }
        } else {
            SeekBar(progress.coerceIn(0f, 1f), bufferedPercent / 100f, onSeek)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(fmt(positionMs), style = MaterialTheme.typography.labelSmall, color = muted)
            Text(
                if (durationMs > 0) fmt(durationMs) else "--:--",
                style = MaterialTheme.typography.labelSmall, color = muted
            )
        }
    }
}

@Composable
internal fun __splitImpl_PlayerControls(state: RexMusicUiState, busy: Boolean, actions: PlayerActions) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        ToggleControl(
            icon = Icons.Default.AutoAwesome,
            description = if (state.autoplay) "Autoplay pintar aktif" else "Autoplay pintar mati",
            active = state.autoplay, onClick = actions.onAutoplay
        )
        RexIconButton(
            Icons.Default.SkipPrevious, "Lagu sebelumnya", actions.onPrev,
            size = 52.dp, iconSize = 36.dp, tint = MaterialTheme.colorScheme.onBackground
        )
        PlayPauseButton(state.isPlaying, busy, actions.onPlayPause)
        RexIconButton(
            Icons.Default.SkipNext, "Lagu berikutnya", actions.onNext,
            size = 52.dp, iconSize = 36.dp, tint = MaterialTheme.colorScheme.onBackground
        )
        RepeatControl(state.repeatTotal, state.repeatLeft, actions.onRepeat)
    }
}

@Composable
internal fun __splitImpl_PlayerError(error: String?, onRetry: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    AnimatedVisibility(visible = !error.isNullOrBlank()) {
        Surface(
            Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp),
            shape = RoundedCornerShape(12.dp), color = scheme.errorContainer
        ) {
            Row(
                Modifier.padding(start = 12.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Warning, contentDescription = "Error", tint = scheme.onErrorContainer)
                Spacer(Modifier.width(10.dp))
                Text(
                    error.orEmpty(), modifier = Modifier.weight(1f),
                    color = scheme.onErrorContainer, style = MaterialTheme.typography.bodySmall
                )
                TextButton(onClick = onRetry) { Text("Coba lagi") }
            }
        }
    }
}

@Composable
internal fun __splitImpl_PlayerDetails(
    track: RexTrack,
    state: RexMusicUiState,
    positionState: State<Long>,
    isLiked: Boolean,
    downloaded: Boolean,
    downloadStatus: DownloadStatus?,
    busy: Boolean,
    actions: PlayerActions,
    modifier: Modifier = Modifier
) {
    Column(modifier) {
        TrackInfo(
            title = track.title,
            artist = track.artist,
            isLiked = isLiked,
            onToggleLike = actions.onToggleLike,
            lyrics = state.lyrics,
            positionMs = positionState.value,
            onLyrics = actions.onLyrics
        )
        Spacer(Modifier.height(12.dp))
        SeekSection(positionState, state.durationMs, state.bufferedPercent, busy, actions.onSeek)
        Spacer(Modifier.height(8.dp))
        PlayerControls(state, busy, actions)
        Spacer(Modifier.height(16.dp))
        BottomActions(state, downloaded, downloadStatus, actions)
        PlayerError(state.error, actions.onPlayPause)
        LyricsInlineSection(state.lyrics, positionState.value, actions.onLyrics)
        NeighborSection(state, actions.onOpenPrevious, actions.onNext)
    }
}

@Composable
internal fun Modifier.__splitImpl_playerBackground(accent: Color?): Modifier {
    val base = accent ?: MaterialTheme.colorScheme.primary
    val playerBase = RexPlayerBase
    val animated = animateColorAsState(base, tween(700), label = "playerAccent")
    return this.drawBehind {
        val c = animated.value
        drawRect(
            Brush.verticalGradient(
                colorStops = arrayOf(
                    0f to lerp(playerBase, c, 0.14f),
                    0.55f to lerp(playerBase, c, 0.07f),
                    1f to playerBase
                )
            )
        )
    }
}


@Composable
internal fun LyricsInlineSection(lyrics: Lyrics, positionMs: Long, onOpenFull: () -> Unit) {
    // An unavailable lyric intentionally removes the entire section, including its divider and spacing.
    if (lyrics.isEmpty) return

    val colors = MaterialTheme.colorScheme
    val activeIndex = if (lyrics.synced.isNotEmpty()) {
        lyrics.synced.indexOfLast { it.timeMs <= positionMs }
    } else -1

    Column(
        Modifier.fillMaxWidth().padding(top = 26.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("Lirik", style = MaterialTheme.typography.titleLarge, color = colors.onBackground)
                Text(
                    if (lyrics.hasSynced) "Tersinkron dengan lagu" else "Lirik lagu",
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.onBackground.copy(alpha = .58f)
                )
            }
            TextButton(onClick = onOpenFull) {
                Text("Lihat penuh")
                Spacer(Modifier.width(3.dp))
                Icon(Icons.Default.OpenInFull, contentDescription = null, modifier = Modifier.size(16.dp))
            }
        }
        HorizontalDivider(color = colors.outlineVariant.copy(alpha = .6f))

        if (lyrics.synced.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                lyrics.synced.forEachIndexed { index, line ->
                    val active = index == activeIndex
                    Text(
                        text = line.text,
                        style = if (active) MaterialTheme.typography.bodyLarge else MaterialTheme.typography.bodyMedium,
                        fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
                        color = when {
                            active -> colors.primary
                            index < activeIndex -> colors.onBackground.copy(alpha = .52f)
                            else -> colors.onBackground.copy(alpha = .78f)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (active) colors.primary.copy(alpha = .11f) else Color.Transparent)
                            .clickable(onClick = onOpenFull)
                            .padding(horizontal = if (active) 12.dp else 4.dp, vertical = 8.dp)
                    )
                }
            }
        } else {
            Text(
                text = lyrics.plain.trim(),
                style = MaterialTheme.typography.bodyLarge,
                color = colors.onBackground.copy(alpha = .84f),
                lineHeight = 28.sp
            )
        }
    }
}
