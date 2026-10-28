package com.hansaps.spotify.rexmusic

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

internal const val NOTIF_PERMISSION = "android.permission.POST_NOTIFICATIONS"

internal const val TAB_HOME = 0
internal const val TAB_SEARCH = 1
internal const val TAB_PLAYLIST = 2
internal const val TAB_LIBRARY = 3
internal const val TAB_SYNC = 4

@Composable
fun RexMusicScreen(
    onBack: () -> Unit = {},
    vm: RexMusicViewModel = viewModel(),
    onOpenLocalProfile: () -> Unit = onBack
) {
    RexMusicTheme {
        RexMusicContent(onBack, vm, onOpenLocalProfile)
    }
}

@Composable
internal fun RexMusicContent(
    onBack: () -> Unit,
    vm: RexMusicViewModel,
    onOpenLocalProfile: () -> Unit = onBack
) {
        return __splitImpl_RexMusicContent(onBack, vm, onOpenLocalProfile)
    }

@Composable
internal fun PlayerPlaceholder(
    loadingText: String,
    error: String?,
    onClose: () -> Unit
) {
    Column(
        Modifier
            .fillMaxSize()
            .background(RexPlayerBase)
            .clickable(
                interactionSource = remember {
                    MutableInteractionSource()
                },
                indication = null,
                onClick = {}
            )
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        RexIconButton(
            Icons.Default.KeyboardArrowDown,
            "Tutup player",
            onClose,
            Modifier.padding(4.dp),
            size = 48.dp,
            iconSize = 32.dp,
            tint = MaterialTheme.colorScheme.onBackground
        )

        Column(
            Modifier
                .fillMaxSize()
                .padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (error.isNullOrBlank()) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.onBackground,
                    strokeWidth = 3.dp,
                    modifier = Modifier.size(44.dp)
                )

                Spacer(Modifier.height(16.dp))

                Text(
                    loadingText.ifBlank { "Memuat..." },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                )
            } else {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = "Error",
                    tint = MaterialTheme.colorScheme.onBackground
                )

                Spacer(Modifier.height(12.dp))

                Text(
                    error,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
internal fun MiniPlayer(
    track: RexTrack,
    state: RexMusicUiState,
    positionState: State<Long>,
    onToggle: () -> Unit,
    onNext: () -> Unit,
    onOpen: () -> Unit
) {
        return __splitImpl_MiniPlayer(track, state, positionState, onToggle, onNext, onOpen)
    }

@Composable
internal fun MiniPlaybackPulse(color: Color) {
    // Infinite transitions exist only while the playing indicator is composed.
    val transition = rememberInfiniteTransition(label = "miniPlaybackPulse")
    val first by transition.animateFloat(
        initialValue = 4f,
        targetValue = 12f,
        animationSpec = infiniteRepeatable(
            animation = tween(430, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseBarOne"
    )
    val second by transition.animateFloat(
        initialValue = 10f,
        targetValue = 5f,
        animationSpec = infiniteRepeatable(
            animation = tween(360, delayMillis = 70, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseBarTwo"
    )
    val third by transition.animateFloat(
        initialValue = 5f,
        targetValue = 13f,
        animationSpec = infiniteRepeatable(
            animation = tween(390, delayMillis = 130, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseBarThree"
    )
    Row(
        modifier = Modifier.height(16.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        listOf(first, second, third).forEach { height ->
            Box(
                Modifier
                    .width(2.5.dp)
                    .height(height.dp.coerceAtLeast(3.dp))
                    .clip(RoundedCornerShape(3.dp))
                    .background(color)
            )
        }
    }
}

@Composable
internal fun MiniProgress(
    positionState: State<Long>,
    durationMs: Long,
    isPlaying: Boolean
) {
    val scheme = MaterialTheme.colorScheme
    val rawProgress = if (durationMs > 0L) {
        (positionState.value.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }
    val progress by animateFloatAsState(
        targetValue = rawProgress,
        animationSpec = tween(
            durationMillis = if (isPlaying) 260 else 150,
            easing = LinearEasing
        ),
        label = "miniPlayerProgress"
    )

    Box(
        Modifier
            .fillMaxWidth()
            .height(3.dp)
            .background(scheme.onSurface.copy(alpha = 0.11f))
    ) {
        Box(
            Modifier
                .fillMaxWidth(progress)
                .fillMaxHeight()
                .background(
                    Brush.horizontalGradient(
                        listOf(scheme.primary, scheme.tertiary)
                    )
                )
        )
    }
}
