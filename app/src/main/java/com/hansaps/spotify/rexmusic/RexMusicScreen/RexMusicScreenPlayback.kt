package com.hansaps.spotify.rexmusic

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
internal fun __splitImpl_MiniPlayer(
    track: RexTrack,
    state: RexMusicUiState,
    positionState: State<Long>,
    onToggle: () -> Unit,
    onNext: () -> Unit,
    onOpen: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val busy = state.phase.isPlayerBusy
    val shape = RoundedCornerShape(18.dp)
    val elevation by animateDpAsState(
        targetValue = if (state.isPlaying) 9.dp else 3.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "miniPlayerElevation"
    )
    val artworkScale by animateFloatAsState(
        targetValue = if (state.isPlaying) 1.035f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "miniPlayerArtworkScale"
    )
    val cardBrush = if (state.isPlaying) {
        Brush.horizontalGradient(
            listOf(
                scheme.primary.copy(alpha = 0.11f),
                scheme.surfaceVariant.copy(alpha = 0.98f),
                scheme.tertiary.copy(alpha = 0.07f)
            )
        )
    } else {
        Brush.horizontalGradient(listOf(scheme.surfaceVariant, scheme.surfaceVariant))
    }

    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 9.dp, vertical = 6.dp)
            .shadow(elevation, shape, clip = false)
            .clip(shape)
            .background(cardBrush)
            .border(1.dp, scheme.outlineVariant.copy(alpha = 0.42f), shape)
            .animateContentSize(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            )
            .clickable(onClick = onOpen)
    ) {
        AnimatedContent(
            targetState = track,
            transitionSpec = {
                (fadeIn(tween(170)) +
                    slideInVertically(tween(220, easing = FastOutSlowInEasing)) { it / 5 }) togetherWith
                    (fadeOut(tween(110)) +
                        slideOutVertically(tween(170, easing = FastOutSlowInEasing)) { -it / 6 })
            },
            label = "miniPlayerTrackChange"
        ) { displayedTrack ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(start = 10.dp, end = 5.dp, top = 8.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CoverThumb(
                    displayedTrack.cover,
                    46.dp,
                    modifier = Modifier.graphicsLayer {
                        scaleX = artworkScale
                        scaleY = artworkScale
                    },
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(Modifier.width(12.dp))

                Column(Modifier.weight(1f)) {
                    Text(
                        displayedTrack.title,
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            displayedTrack.artist,
                            style = MaterialTheme.typography.bodySmall,
                            color = scheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        if (state.isPlaying) {
                            Spacer(Modifier.width(7.dp))
                            MiniPlaybackPulse(scheme.primary)
                        }
                    }
                }

                if (busy) {
                    Box(
                        Modifier.size(48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                } else {
                    AnimatedContent(
                        targetState = state.isPlaying,
                        transitionSpec = {
                            (fadeIn(tween(120)) + scaleIn(initialScale = 0.78f, animationSpec = tween(150))) togetherWith
                                (fadeOut(tween(80)) + scaleOut(targetScale = 0.82f, animationSpec = tween(110)))
                        },
                        label = "miniPlayerPlayPauseMorph"
                    ) { playing ->
                        RexIconButton(
                            icon = if (playing) Icons.Default.Pause else Icons.Default.PlayArrow,
                            description = if (playing) "Jeda" else "Putar",
                            onClick = onToggle,
                            size = 48.dp,
                            iconSize = 25.dp,
                            tint = scheme.onPrimary,
                            background = scheme.primary
                        )
                    }
                }

                RexIconButton(
                    icon = Icons.Default.SkipNext,
                    description = "Lagu berikutnya",
                    onClick = onNext,
                    size = 48.dp,
                    iconSize = 24.dp,
                    tint = scheme.onSurface
                )
            }
        }

        MiniProgress(
            positionState = positionState,
            durationMs = state.durationMs,
            isPlaying = state.isPlaying
        )
    }
}
