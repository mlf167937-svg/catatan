package com.hansaps.spotify.rexmusic.player

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.hansaps.spotify.rexmusic.*

@Composable
internal fun __splitImpl_MusicPlayerCard(
    track: RexTrack,
    state: RexMusicUiState,
    positionState: State<Long>,
    isLiked: Boolean,
    downloaded: Boolean,
    downloadStatus: DownloadStatus?,
    accent: Color?,
    onAccentFound: (Color) -> Unit,
    actions: PlayerActions,
    modifier: Modifier = Modifier
) {
    val busy = state.phase.isPlayerBusy
    CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onBackground) {
        BoxWithConstraints(
            modifier
                .fillMaxSize()
                .playerBackground(accent)
                .pointerInput(Unit) { detectTapGestures(onTap = {}) }
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            val landscape = maxWidth > maxHeight && maxWidth >= 560.dp
            val coverSize = if (landscape) {
                minOf(300.dp, maxWidth * 0.36f, (maxHeight - 88.dp).coerceAtLeast(128.dp)).coerceAtLeast(128.dp)
            } else {
                minOf(300.dp, (maxWidth - 40.dp).coerceAtLeast(144.dp), maxHeight * 0.42f).coerceAtLeast(144.dp)
            }

            if (landscape) {
                Column(Modifier.fillMaxSize()) {
                    PlayerTopBar(state, track, actions.onClose, actions.onLyrics, actions.onMore)
                    Row(
                        Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            Modifier.weight(0.92f).fillMaxHeight(),
                            contentAlignment = Alignment.Center
                        ) {
                            PlayerCover(
                                cover = track.cover,
                                title = track.title,
                                size = coverSize,
                                busy = busy,
                                playing = state.isPlaying,
                                accent = accent,
                                onAccentFound = onAccentFound,
                                onSwipeNext = actions.onNext,
                                onSwipePrev = actions.onOpenPrevious
                            )
                        }
                        PlayerDetails(
                            track = track,
                            state = state,
                            positionState = positionState,
                            isLiked = isLiked,
                            downloaded = downloaded,
                            downloadStatus = downloadStatus,
                            busy = busy,
                            actions = actions,
                            modifier = Modifier
                                .weight(1.08f)
                                .fillMaxHeight()
                                .padding(horizontal = 20.dp)
                                .verticalScroll(rememberScrollState())
                        )
                    }
                }
            } else {
                Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                    PlayerTopBar(state, track, actions.onClose, actions.onLyrics, actions.onMore)
                    Spacer(Modifier.height(8.dp))
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        PlayerCover(
                            cover = track.cover,
                            title = track.title,
                            size = coverSize,
                            busy = busy,
                            playing = state.isPlaying,
                            accent = accent,
                            onAccentFound = onAccentFound,
                            onSwipeNext = actions.onNext,
                            onSwipePrev = actions.onOpenPrevious
                        )
                    }
                    Spacer(Modifier.height(28.dp))
                    PlayerDetails(
                        track = track,
                        state = state,
                        positionState = positionState,
                        isLiked = isLiked,
                        downloaded = downloaded,
                        downloadStatus = downloadStatus,
                        busy = busy,
                        actions = actions,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                    Spacer(Modifier.height(24.dp))
                }
            }
        }
    }
}
