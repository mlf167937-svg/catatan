package com.hansaps.spotify.rexmusic

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import java.util.Calendar

@Composable
internal fun __splitImpl_HomeTab(
    state: RexMusicUiState,
    actions: RexActions,
    listState: LazyListState,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    val offlineMode = state.offline.enabled
    val glow by animateColorAsState(
        if (offlineMode) scheme.tertiary else scheme.primary, tween(400), label = "homeGlow"
    )
    val hour = remember { Calendar.getInstance().get(Calendar.HOUR_OF_DAY) }

    Box(
        modifier.fillMaxSize().drawBehind {
            val h = 320.dp.toPx()
            drawRect(
                Brush.verticalGradient(
                    listOf(glow.copy(alpha = 0.30f), Color.Transparent), startY = 0f, endY = h
                ),
                size = Size(size.width, h)
            )
        }
    ) {
        LazyColumn(
            Modifier.fillMaxSize(),
            state = listState,
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            item(key = "home-header") {
                HomeHeader(
                    greeting = greetingForHour(hour),
                    offline = offlineMode,
                    onToggle = { actions.setOffline(!offlineMode) },
                    onBack = onBack
                )
            }
            if (offlineMode) {
                offlineSection(state, actions, query = "", full = true)
            } else {
                onlineHome(state, actions)
            }
        }
    }
}

@Composable
internal fun __splitImpl_LibraryTab(
    state: RexMusicUiState,
    libTab: Int,
    onLibTab: (Int) -> Unit,
    actions: RexActions,
    listState: LazyListState,
    modifier: Modifier = Modifier
) {
    val offlineMode = state.offline.enabled
    LazyColumn(
        modifier.fillMaxSize(),
        state = listState,
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        item(key = "lib-header") {
            TabHeader("Koleksi", offlineMode) { actions.setOffline(!offlineMode) }
        }
        item(key = "lib-chips") {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                LibraryChip("Unduhan", state.offline.entries.size, libTab == 0) { onLibTab(0) }
                LibraryChip("Antrian", state.userQueue.size, libTab == 1) { onLibTab(1) }
                LibraryChip("Riwayat", state.history.size, libTab == 2) { onLibTab(2) }
            }
        }
        when (libTab) {
            0 -> offlineSection(state, actions, query = "", full = true)
            1 -> queueSection(state, actions, showNow = false)
            else -> historySection(state, actions)
        }
    }
}

@Composable
internal fun __splitImpl_SearchTab(
    state: RexMusicUiState,
    query: String,
    typing: Boolean,
    onQueryChange: (String) -> Unit,
    onClear: () -> Unit,
    actions: RexActions,
    listState: LazyListState,
    modifier: Modifier = Modifier
) {
    val offlineMode = state.offline.enabled
    val q = query.trim()
    LazyColumn(
        modifier.fillMaxSize(),
        state = listState,
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        item(key = "search-header") {
            TabHeader("Cari", offlineMode) { actions.setOffline(!offlineMode) }
        }
        item(key = "search-field") {
            SearchField(
                query, onQueryChange, onClear,
                if (offlineMode) "Cari di lagu offline" else "Lagu, artis, atau album",
                Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }
        when {
            offlineMode -> offlineSection(state, actions, q, full = false)
            q.isEmpty() -> exploreSection(state, actions)
            else -> resultsSection(state, actions, typing)
        }
    }
}
