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
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import kotlin.math.PI

// ───────────────────────── Motion tokens ─────────────────────────

internal val EmphasizedDecelerate = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)
internal val EmphasizedAccelerate = CubicBezierEasing(0.3f, 0f, 0.8f, 0.15f)
internal val TwoPi = (2.0 * PI).toFloat()
internal val DegToRad = (PI / 180.0).toFloat()

/** Seeded, stable particle coordinates prevent shimmer caused by regenerating randomness per frame. */
internal data class AmbientParticle(
    val x: Float,
    val y: Float,
    val radius: Float,
    val phase: Float,
    val speed: Float
)

internal val LandscapeParticles = List(34) { index ->
    // A deterministic low-discrepancy layout gives the canvas texture without a Random on every draw.
    val x = ((index * 37 + 13) % 101) / 100f
    val y = ((index * 61 + 29) % 97) / 96f
    AmbientParticle(x, y, 0.7f + (index % 4) * 0.24f, index * 0.71f, 0.35f + (index % 5) * 0.13f)
}

/** Dua "jam" animasi bersama (lambat + denyut) agar hanya ada satu InfiniteTransition. */
@Stable
internal class MotionClock(val slow: State<Float>, val pulse: State<Float>)

@Composable
internal fun rememberReduceMotion(): Boolean {
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
internal fun rememberMotionClock(reduceMotion: Boolean): MotionClock {
    return if (reduceMotion) {
        remember { MotionClock(mutableStateOf(0.18f), mutableStateOf(0.5f)) }
    } else {
        val transition = rememberInfiniteTransition(label = "rexLandscapeClock")
        val slow = transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(28_000, easing = LinearEasing)),
            label = "rexClockSlow"
        )
        val pulse = transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                tween(7_000, easing = FastOutSlowInEasing),
                RepeatMode.Reverse
            ),
            label = "rexClockPulse"
        )
        remember(slow, pulse) { MotionClock(slow, pulse) }
    }
}

internal data class DockItem(val tab: Int, val icon: ImageVector, val label: String)

// ───────────────────────── Root ─────────────────────────

/**
 * Landscape shell futuristik: floating navigation dock dengan indikator animasi,
 * ambient backdrop, content stage sinematik, dan floating audio dock.
 * Seluruh warna/typography berasal dari MaterialTheme; state & playback tetap milik parent.
 */
@Composable
internal fun RexMusicScreenLandscape(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    miniPlayer: @Composable () -> Unit,
    snackbarHost: @Composable () -> Unit,
    content: @Composable (Modifier) -> Unit
) {
        return __splitImpl_RexMusicScreenLandscape(selectedTab, onTabSelected, miniPlayer, snackbarHost, content)
    }

// ───────────────────────── Content stage ─────────────────────────

/**
 * Transisi tab tanpa menduplikasi / mereset content: `content` hanya dipanggil SEKALI,
 * efek sinematik (fade + slide + scale halus) dilakukan via graphicsLayer pada container.
 */
@Composable
internal fun ContentStage(
    selectedTab: Int,
    reduceMotion: Boolean,
    content: @Composable (Modifier) -> Unit
) {
        return __splitImpl_ContentStage(selectedTab, reduceMotion, content)
    }

// ───────────────────────── Ambient backdrop ─────────────────────────

@Composable
internal fun AmbientBackdrop(clock: MotionClock, modifier: Modifier) {
        return __splitImpl_AmbientBackdrop(clock, modifier)
    }

// ───────────────────────── Navigation dock ─────────────────────────

@Composable
internal fun NavigationDock(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    showLabels: Boolean,
    compactHeight: Boolean,
    width: Dp,
    clock: MotionClock
) {
        return __splitImpl_NavigationDock(selectedTab, onTabSelected, showLabels, compactHeight, width, clock)
    }

@Composable
internal fun DockStatusSignal(clock: MotionClock) {
        return __splitImpl_DockStatusSignal(clock)
    }

@Composable
internal fun DockIndicator(height: Dp, clock: MotionClock, modifier: Modifier) {
        return __splitImpl_DockIndicator(height, clock, modifier)
    }

@Composable
internal fun DockItemView(
    item: DockItem,
    selected: Boolean,
    showLabel: Boolean,
    height: Dp,
    onClick: () -> Unit
) {
        return __splitImpl_DockItemView(item, selected, showLabel, height, onClick)
    }

@Composable
internal fun RexBadge(size: Dp, compact: Boolean, clock: MotionClock) {
        return __splitImpl_RexBadge(size, compact, clock)
    }

// ───────────────────────── Floating audio dock ─────────────────────────

@Composable
internal fun FloatingAudioDock(clock: MotionClock, miniPlayer: @Composable () -> Unit) {
        return __splitImpl_FloatingAudioDock(clock, miniPlayer)
    }