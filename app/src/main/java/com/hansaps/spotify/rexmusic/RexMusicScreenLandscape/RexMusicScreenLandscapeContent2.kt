package com.hansaps.spotify.rexmusic

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColor
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.max

/**
 * Landscape shell futuristik: floating navigation dock dengan indikator animasi,
 * ambient backdrop, content stage sinematik, dan floating audio dock.
 * Seluruh warna/typography berasal dari MaterialTheme; state & playback tetap milik parent.
 */
@Composable
internal fun __splitImpl_RexMusicScreenLandscape(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    miniPlayer: @Composable () -> Unit,
    snackbarHost: @Composable () -> Unit,
    content: @Composable (Modifier) -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val reduceMotion = rememberReduceMotion()
    val clock = rememberMotionClock(reduceMotion)
    val railVisible = selectedTab != TAB_SYNC

    BoxWithConstraints(Modifier.fillMaxSize().background(scheme.background)) {
        val showNavigationLabels = maxWidth >= 900.dp
        val compactHeight = maxHeight < 420.dp
        val railWidth = if (showNavigationLabels) 104.dp else 80.dp

        AmbientBackdrop(clock, Modifier.fillMaxSize())

        Row(Modifier.fillMaxSize()) {
            AnimatedVisibility(
                visible = railVisible,
                enter = fadeIn(tween(240)) +
                    slideInHorizontally(tween(340, easing = EmphasizedDecelerate)) { -it / 2 } +
                    expandHorizontally(tween(340, easing = EmphasizedDecelerate), expandFrom = Alignment.Start),
                exit = fadeOut(tween(140)) +
                    slideOutHorizontally(tween(220, easing = EmphasizedAccelerate)) { -it / 3 } +
                    shrinkHorizontally(tween(240, easing = EmphasizedAccelerate), shrinkTowards = Alignment.Start)
            ) {
                NavigationDock(
                    selectedTab = selectedTab,
                    onTabSelected = onTabSelected,
                    showLabels = showNavigationLabels,
                    compactHeight = compactHeight,
                    width = railWidth,
                    clock = clock
                )
            }

            Scaffold(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                containerColor = Color.Transparent,
                snackbarHost = snackbarHost,
                bottomBar = {
                    AnimatedVisibility(
                        visible = railVisible,
                        enter = fadeIn(tween(260)) +
                            slideInVertically(tween(360, easing = EmphasizedDecelerate)) { it / 2 } +
                            expandVertically(tween(320, easing = EmphasizedDecelerate), expandFrom = Alignment.Bottom) +
                            scaleIn(tween(320, easing = EmphasizedDecelerate), initialScale = 0.92f, transformOrigin = TransformOrigin(0.5f, 1f)),
                        exit = fadeOut(tween(150)) +
                            slideOutVertically(tween(220, easing = EmphasizedAccelerate)) { it / 2 } +
                            shrinkVertically(tween(240, easing = EmphasizedAccelerate), shrinkTowards = Alignment.Bottom) +
                            scaleOut(tween(220, easing = EmphasizedAccelerate), targetScale = 0.94f, transformOrigin = TransformOrigin(0.5f, 1f))
                    ) {
                        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            Box(
                                Modifier
                                    .widthIn(max = 1000.dp)
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 8.dp)
                            ) {
                                FloatingAudioDock(clock) { miniPlayer() }
                            }
                        }
                    }
                }
            ) { padding ->
                Box(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.TopCenter
                ) {
                    ContentStage(
                        selectedTab = selectedTab,
                        reduceMotion = reduceMotion,
                        content = content
                    )
                }
            }
        }
    }
}

@Composable
internal fun __splitImpl_DockItemView(
    item: DockItem,
    selected: Boolean,
    showLabel: Boolean,
    height: Dp,
    onClick: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val glowColor = scheme.primary
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (pressed) 0.93f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMedium),
        label = "dockPressScale"
    )
    val transition = updateTransition(targetState = selected, label = "dockItemTransition")
    val progress by transition.animateFloat(
        label = "dockItemProgress",
        transitionSpec = { spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow) }
    ) { if (it) 1f else 0f }
    val tint by transition.animateColor(
        label = "dockItemTint",
        transitionSpec = { tween(280) }
    ) { if (it) scheme.primary else scheme.onSurfaceVariant }

    val iconModifier = Modifier
        .size(24.dp)
        .drawBehind {
            val p = progress.coerceIn(0f, 1f)
            if (p > 0.01f) {
                val r = this.size.minDimension * 1.1f
                drawCircle(
                    brush = Brush.radialGradient(
                        listOf(glowColor.copy(alpha = 0.30f * p), Color.Transparent),
                        radius = r
                    ),
                    radius = r
                )
            }
        }
        .graphicsLayer {
            val s = 1f + 0.14f * progress.coerceIn(0f, 1f)
            scaleX = s
            scaleY = s
            if (!showLabel) translationY = -7.dp.toPx() * progress.coerceIn(0f, 1f)
        }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
            .graphicsLayer {
                scaleX = pressScale
                scaleY = pressScale
            }
            .selectable(
                selected = selected,
                interactionSource = interaction,
                indication = null,
                role = Role.Tab,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        if (showLabel) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(item.icon, contentDescription = null, tint = tint, modifier = iconModifier)
                Spacer(Modifier.height(4.dp))
                Text(item.label, style = MaterialTheme.typography.labelSmall, color = tint, maxLines = 1)
            }
        } else {
            Icon(item.icon, contentDescription = item.label, tint = tint, modifier = iconModifier)
            Text(
                item.label,
                style = MaterialTheme.typography.labelSmall,
                color = tint,
                maxLines = 1,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 6.dp)
                    .graphicsLayer { alpha = progress.coerceIn(0f, 1f) }
            )
        }
    }
}
