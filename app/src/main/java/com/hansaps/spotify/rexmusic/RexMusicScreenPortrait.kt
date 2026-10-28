package com.hansaps.spotify.rexmusic

import android.provider.Settings
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlin.math.PI

internal val PortraitNavigationShape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp)
internal val PortraitEase = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)
internal val PortraitTwoPi = (2.0 * PI).toFloat()

internal data class PortraitMotionClock(val drift: State<Float>, val pulse: State<Float>)
internal data class PortraitDust(
    val x: Float,
    val y: Float,
    val radius: Float,
    val phase: Float,
    val velocity: Float
)

/** Stable particles are calculated once, so recomposition never changes their positions. */
internal val PortraitDustField = List(28) { index ->
    PortraitDust(
        x = ((index * 29 + 11) % 103) / 102f,
        y = ((index * 47 + 19) % 101) / 100f,
        radius = 0.6f + (index % 4) * 0.22f,
        phase = index * 0.83f,
        velocity = 0.35f + (index % 6) * 0.11f
    )
}

@Composable
internal fun portraitReduceMotion(): Boolean {
    val context = LocalContext.current
    return remember(context) {
        runCatching {
            Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1f
            ) == 0f
        }.getOrDefault(false)
    }
}

@Composable
internal fun rememberPortraitMotionClock(reduceMotion: Boolean): PortraitMotionClock {
    if (reduceMotion) {
        return remember { PortraitMotionClock(mutableStateOf(0.18f), mutableStateOf(0.5f)) }
    }
    val transition = rememberInfiniteTransition(label = "rexPortraitAmbientClock")
    val drift = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(31_000, easing = LinearEasing)),
        label = "rexPortraitAmbientDrift"
    )
    val pulse = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            tween(5_800, easing = FastOutSlowInEasing),
            RepeatMode.Reverse
        ),
        label = "rexPortraitAmbientPulse"
    )
    return remember(drift, pulse) { PortraitMotionClock(drift, pulse) }
}

/**
 * Portrait-first shell. It deliberately owns only visual chrome and layout; playback state,
 * tab data, and actions remain in the existing screen/controller. Every tint comes from the
 * host HansAps MaterialTheme, allowing the user's existing app palette to remain authoritative.
 */
@Composable
internal fun RexMusicScreenPortrait(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    miniPlayer: @Composable () -> Unit,
    snackbarHost: @Composable () -> Unit,
    content: @Composable (Modifier) -> Unit
) {
        return __splitImpl_RexMusicScreenPortrait(selectedTab, onTabSelected, miniPlayer, snackbarHost, content)
    }

@Composable
internal fun PortraitContentStage(
    selectedTab: Int,
    reduceMotion: Boolean,
    modifier: Modifier,
    content: @Composable (Modifier) -> Unit
) {
        return __splitImpl_PortraitContentStage(selectedTab, reduceMotion, modifier, content)
    }

@Composable
internal fun PortraitAmbientBackdrop(clock: PortraitMotionClock, modifier: Modifier) {
        return __splitImpl_PortraitAmbientBackdrop(clock, modifier)
    }

@Composable
internal fun PortraitBottomChrome(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    miniPlayer: @Composable () -> Unit,
    clock: PortraitMotionClock
) {
        return __splitImpl_PortraitBottomChrome(selectedTab, onTabSelected, miniPlayer, clock)
    }

internal data class PortraitNavItem(val tab: Int, val icon: ImageVector, val label: String)
internal val PortraitNavItems = listOf(
    PortraitNavItem(TAB_HOME, Icons.Default.Home, "Beranda"),
    PortraitNavItem(TAB_SEARCH, Icons.Default.Search, "Cari"),
    PortraitNavItem(TAB_PLAYLIST, Icons.Default.QueueMusic, "Playlist"),
    PortraitNavItem(TAB_LIBRARY, Icons.Default.LibraryMusic, "Koleksi"),
    PortraitNavItem(TAB_SYNC, Icons.Default.Groups, "Sync")
)

@Composable
internal fun PortraitNavigationIcon(
    image: ImageVector,
    description: String,
    selected: Boolean,
    clock: PortraitMotionClock
) {
        return __splitImpl_PortraitNavigationIcon(image, description, selected, clock)
    }

@Composable
internal fun PortraitTopSheen(clock: PortraitMotionClock, modifier: Modifier) {
    val scheme = MaterialTheme.colorScheme
    Canvas(modifier) {
        val center = size.width * (0.08f + 0.84f * clock.pulse.value)
        drawLine(
            brush = Brush.horizontalGradient(
                colors = listOf(Color.Transparent, scheme.primary.copy(alpha = 0.72f), scheme.tertiary.copy(alpha = 0.48f), Color.Transparent),
                startX = center - size.width * 0.2f,
                endX = center + size.width * 0.2f
            ),
            start = Offset(0f, size.height / 2f),
            end = Offset(size.width, size.height / 2f),
            strokeWidth = size.height
        )
    }
}

@Composable
internal fun PortraitBottomAccent(clock: PortraitMotionClock, modifier: Modifier) {
    val scheme = MaterialTheme.colorScheme
    Canvas(modifier) {
        val midY = size.height / 2f
        drawLine(
            brush = Brush.horizontalGradient(
                listOf(Color.Transparent, scheme.primary.copy(alpha = 0.13f + clock.pulse.value * 0.12f), scheme.tertiary.copy(alpha = 0.12f), Color.Transparent)
            ),
            start = Offset(0f, midY),
            end = Offset(size.width, midY),
            strokeWidth = 1.dp.toPx()
        )
    }
}
