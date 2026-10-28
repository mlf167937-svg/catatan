package com.hansaps.spotify.rexmusic

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
internal fun __splitImpl_PortraitBottomChrome(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    miniPlayer: @Composable () -> Unit,
    clock: PortraitMotionClock
) {
    val scheme = MaterialTheme.colorScheme
    Column(
        Modifier
            .fillMaxWidth()
            .drawWithContent {
                drawContent()
                val y = 0.75.dp.toPx()
                drawLine(
                    brush = Brush.horizontalGradient(
                        listOf(Color.Transparent, scheme.primary.copy(alpha = 0.56f), scheme.tertiary.copy(alpha = 0.46f), Color.Transparent)
                    ),
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = 1.dp.toPx()
                )
            }
            .shadow(
                elevation = 19.dp,
                shape = PortraitNavigationShape,
                ambientColor = scheme.primary.copy(alpha = 0.12f),
                spotColor = scheme.tertiary.copy(alpha = 0.13f)
            )
            .clip(PortraitNavigationShape)
            .background(
                Brush.verticalGradient(
                    listOf(
                        scheme.surface.copy(alpha = 0.96f),
                        scheme.surfaceVariant.copy(alpha = 0.97f),
                        scheme.surface.copy(alpha = 0.99f)
                    )
                )
            )
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    listOf(scheme.primary.copy(alpha = 0.18f), scheme.outlineVariant.copy(alpha = 0.32f))
                ),
                shape = PortraitNavigationShape
            )
    ) {
        Box(Modifier.fillMaxWidth()) {
            PortraitTopSheen(clock, Modifier.fillMaxWidth().height(3.dp))
        }

        Box(Modifier.fillMaxWidth()) {
            // The mini player remains the original composable, wrapped rather than reimplemented.
            miniPlayer()
        }

        NavigationBar(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 7.dp)
                .drawBehind {
                    val edge = scheme.outlineVariant.copy(alpha = 0.22f)
                    drawLine(edge, Offset(12.dp.toPx(), 0f), Offset(size.width - 12.dp.toPx(), 0f), 0.7.dp.toPx())
                },
            containerColor = Color.Transparent,
            tonalElevation = 0.dp
        ) {
            val itemColors = NavigationBarItemDefaults.colors(
                selectedIconColor = scheme.primary,
                selectedTextColor = scheme.primary,
                indicatorColor = scheme.primary.copy(alpha = 0.12f),
                unselectedIconColor = scheme.onSurfaceVariant,
                unselectedTextColor = scheme.onSurfaceVariant
            )
            PortraitNavItems.forEach { item ->
                val selected = selectedTab == item.tab
                NavigationBarItem(
                    selected = selected,
                    onClick = { onTabSelected(item.tab) },
                    icon = {
                        PortraitNavigationIcon(
                            image = item.icon,
                            description = item.label,
                            selected = selected,
                            clock = clock
                        )
                    },
                    label = { Text(item.label, maxLines = 1, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal) },
                    colors = itemColors,
                    alwaysShowLabel = true
                )
            }
        }
        PortraitBottomAccent(clock, Modifier.fillMaxWidth().height(4.dp))
    }
}

/**
 * Portrait-first shell. It deliberately owns only visual chrome and layout; playback state,
 * tab data, and actions remain in the existing screen/controller. Every tint comes from the
 * host HansAps MaterialTheme, allowing the user's existing app palette to remain authoritative.
 */
@Composable
internal fun __splitImpl_RexMusicScreenPortrait(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    miniPlayer: @Composable () -> Unit,
    snackbarHost: @Composable () -> Unit,
    content: @Composable (Modifier) -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val reduceMotion = portraitReduceMotion()
    val clock = rememberPortraitMotionClock(reduceMotion)

    Box(
        Modifier
            .fillMaxSize()
            .background(scheme.background)
    ) {
        PortraitAmbientBackdrop(clock, Modifier.fillMaxSize())

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = Color.Transparent,
            snackbarHost = snackbarHost,
            bottomBar = {
                AnimatedVisibility(
                    visible = selectedTab != TAB_SYNC,
                    enter = fadeIn(tween(220)) +
                        slideInVertically(tween(360, easing = PortraitEase)) { it / 2 } +
                        scaleIn(
                            animationSpec = tween(360, easing = PortraitEase),
                            initialScale = 0.975f,
                            transformOrigin = TransformOrigin(0.5f, 1f)
                        ),
                    exit = fadeOut(tween(140)) +
                        slideOutVertically(tween(210, easing = PortraitEase)) { it / 3 } +
                        scaleOut(
                            animationSpec = tween(200, easing = PortraitEase),
                            targetScale = 0.985f,
                            transformOrigin = TransformOrigin(0.5f, 1f)
                        )
                ) {
                    PortraitBottomChrome(
                        selectedTab = selectedTab,
                        onTabSelected = onTabSelected,
                        miniPlayer = miniPlayer,
                        clock = clock
                    )
                }
            }
        ) { padding ->
            PortraitContentStage(
                selectedTab = selectedTab,
                reduceMotion = reduceMotion,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                content = content
            )
        }
    }
}

@Composable
internal fun __splitImpl_PortraitContentStage(
    selectedTab: Int,
    reduceMotion: Boolean,
    modifier: Modifier,
    content: @Composable (Modifier) -> Unit
) {
    val progress = remember { Animatable(1f) }
    var settledTab by remember { mutableStateOf(selectedTab) }
    var direction by remember { mutableStateOf(1f) }
    val shiftPx = with(LocalDensity.current) { 24.dp.toPx() }

    LaunchedEffect(selectedTab) {
        if (selectedTab != settledTab) {
            direction = if (selectedTab >= settledTab) 1f else -1f
            if (reduceMotion) {
                progress.snapTo(1f)
                settledTab = selectedTab
            } else {
                progress.snapTo(0f)
                settledTab = selectedTab
                progress.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = 330, easing = PortraitEase)
                )
            }
        }
    }

    Box(
        modifier = modifier.graphicsLayer {
            val p = if (reduceMotion) 1f else progress.value
            alpha = p
            translationX = (1f - p) * shiftPx * direction
            val scale = 0.992f + p * 0.008f
            scaleX = scale
            scaleY = scale
        }
    ) {
        content(Modifier.fillMaxSize())
    }
}
