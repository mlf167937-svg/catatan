package com.hansaps.spotify.rexmusic

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
internal fun __splitImpl_NavigationDock(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    showLabels: Boolean,
    compactHeight: Boolean,
    width: Dp,
    clock: MotionClock
) {
    val scheme = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(28.dp)
    val items = remember {
        listOf(
            DockItem(TAB_HOME, Icons.Default.Home, "Beranda"),
            DockItem(TAB_SEARCH, Icons.Default.Search, "Cari"),
            DockItem(TAB_PLAYLIST, Icons.Default.QueueMusic, "Playlist"),
            DockItem(TAB_LIBRARY, Icons.Default.LibraryMusic, "Koleksi"),
            DockItem(TAB_SYNC, Icons.Default.Groups, "Sync")
        )
    }
    val itemHeight = if (compactHeight) 52.dp else 62.dp
    val gap = 6.dp
    val density = LocalDensity.current
    val selectedIndex = items.indexOfFirst { it.tab == selectedTab }
    val indicatorTarget = with(density) { (itemHeight + gap).toPx() } * selectedIndex.coerceAtLeast(0)
    val indicatorY by animateFloatAsState(
        targetValue = indicatorTarget,
        animationSpec = spring(dampingRatio = 0.78f, stiffness = Spring.StiffnessMediumLow),
        label = "dockIndicatorY"
    )
    val indicatorAlpha by animateFloatAsState(
        targetValue = if (selectedIndex >= 0) 1f else 0f,
        animationSpec = tween(200),
        label = "dockIndicatorAlpha"
    )

    Column(
        modifier = Modifier
            .fillMaxHeight()
            .windowInsetsPadding(
                WindowInsets.systemBars.union(WindowInsets.displayCutout)
                    .only(WindowInsetsSides.Start + WindowInsetsSides.Vertical)
            )
            .padding(start = 10.dp, top = 10.dp, bottom = 10.dp, end = 4.dp)
            .width(width)
            .shadow(
                elevation = 12.dp,
                shape = shape,
                ambientColor = scheme.primary.copy(alpha = 0.18f),
                spotColor = scheme.primary.copy(alpha = 0.24f)
            )
            .background(
                Brush.verticalGradient(
                    listOf(
                        scheme.surface.copy(alpha = 0.93f),
                        scheme.surfaceVariant.copy(alpha = 0.62f)
                    )
                ),
                shape
            )
            .border(
                1.dp,
                Brush.verticalGradient(
                    listOf(
                        scheme.primary.copy(alpha = 0.42f),
                        scheme.outlineVariant.copy(alpha = 0.18f),
                        scheme.tertiary.copy(alpha = 0.34f)
                    )
                ),
                shape
            ),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(if (compactHeight) 12.dp else 18.dp))
        RexBadge(size = if (compactHeight) 34.dp else 42.dp, compact = compactHeight, clock = clock)
        if (!compactHeight) {
            Text(
                "Rex",
                style = MaterialTheme.typography.labelMedium,
                color = scheme.primary,
                modifier = Modifier.padding(top = 6.dp)
            )
        }
        Box(
            Modifier
                .padding(vertical = if (compactHeight) 8.dp else 12.dp)
                .width(26.dp)
                .height(1.dp)
                .background(
                    Brush.horizontalGradient(
                        listOf(Color.Transparent, scheme.outlineVariant.copy(alpha = 0.7f), Color.Transparent)
                    )
                )
        )

        Box(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 8.dp)
            ) {
                DockIndicator(
                    height = itemHeight,
                    clock = clock,
                    modifier = Modifier.graphicsLayer {
                        translationY = indicatorY
                        alpha = indicatorAlpha
                    }
                )
                Column(verticalArrangement = Arrangement.spacedBy(gap)) {
                    items.forEach { item ->
                        DockItemView(
                            item = item,
                            selected = item.tab == selectedTab,
                            showLabel = showLabels,
                            height = itemHeight,
                            onClick = { onTabSelected(item.tab) }
                        )
                    }
                }
            }
        }
        if (!compactHeight) {
            Spacer(Modifier.height(7.dp))
            DockStatusSignal(clock)
            Spacer(Modifier.height(12.dp))
        } else {
            Spacer(Modifier.height(10.dp))
        }
    }
}
