package com.hansaps.spotify.rexmusic.player

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.hansaps.spotify.rexmusic.*

@Stable
class PlayerActions(
    val onClose: () -> Unit,
    val onLyrics: () -> Unit,
    val onMore: () -> Unit,
    val onPlayPause: () -> Unit,
    val onNext: () -> Unit,
    val onPrev: () -> Unit,
    val onOpenPrevious: () -> Unit,
    val onSeek: (Float) -> Unit,
    val onToggleLike: () -> Unit,
    val onRepeat: () -> Unit,
    val onAutoplay: () -> Unit,
    val onQueue: () -> Unit,
    val onDownload: () -> Unit,
    val onCancelDownload: () -> Unit,
    val onDelete: () -> Unit
)

//  Full-screen player 

@Composable
fun MusicPlayerCard(
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
        return __splitImpl_MusicPlayerCard(track, state, positionState, isLiked, downloaded, downloadStatus, accent, onAccentFound, actions, modifier)
    }

@Composable
internal fun PlayerDetails(
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
        return __splitImpl_PlayerDetails(track, state, positionState, isLiked, downloaded, downloadStatus, busy, actions, modifier)
    }

internal fun statusLabel(state: RexMusicUiState): String = when (state.phase) {
    LoadPhase.Resolving -> "MENYIAPKAN"
    LoadPhase.Preparing ->
        if (state.bufferedPercent in 1..99) "MEMUAT ${state.bufferedPercent}%" else "MEMUAT"
    LoadPhase.Buffering -> "BUFFERING"
    else -> if (state.nowPlayingOffline) "DIPUTAR OFFLINE" else "SEDANG DIPUTAR"
}

@Composable
internal fun PlayerTopBar(
    state: RexMusicUiState,
    track: RexTrack,
    onClose: () -> Unit,
    onLyrics: () -> Unit,
    onMore: () -> Unit
) {
        return __splitImpl_PlayerTopBar(state, track, onClose, onLyrics, onMore)
    }

//  Background 

@Composable
fun Modifier.playerBackground(accent: Color?): Modifier {
        return this.__splitImpl_playerBackground(accent)
    }

//  Equalizer 

@Composable
fun EqualizerBars(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    barWidth: Dp = 3.dp,
    height: Dp = 14.dp
) {
        return __splitImpl_EqualizerBars(modifier, color, barWidth, height)
    }

//  Cover 

@Composable
internal fun PlayerCover(
    cover: String,
    title: String,
    size: Dp,
    busy: Boolean,
    playing: Boolean,
    accent: Color?,
    onAccentFound: (Color) -> Unit,
    onSwipeNext: () -> Unit,
    onSwipePrev: () -> Unit
) {
        return __splitImpl_PlayerCover(cover, title, size, busy, playing, accent, onAccentFound, onSwipeNext, onSwipePrev)
    }

//  Title & like 

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun TrackInfo(
    title: String,
    artist: String,
    isLiked: Boolean,
    onToggleLike: () -> Unit,
    lyrics: Lyrics,
    positionMs: Long,
    onLyrics: () -> Unit
) {
        return __splitImpl_TrackInfo(title, artist, isLiked, onToggleLike, lyrics, positionMs, onLyrics)
    }

internal fun activeLyricText(lyrics: Lyrics, positionMs: Long): String? {
    val list = lyrics.synced
    if (list.isEmpty()) return null
    var low = 0
    var high = list.lastIndex
    var active = -1
    while (low <= high) {
        val mid = (low + high) ushr 1
        if (list[mid].timeMs <= positionMs) {
            active = mid
            low = mid + 1
        } else {
            high = mid - 1
        }
    }
    return active.takeIf { it >= 0 }?.let { list[it].text.trim() }?.takeIf { it.isNotBlank() }
}

@Composable
internal fun LikeButton(isLiked: Boolean, onToggle: () -> Unit) {
        return __splitImpl_LikeButton(isLiked, onToggle)
    }

//  Seek 

@Composable
internal fun SeekSection(
    positionState: State<Long>,
    durationMs: Long,
    bufferedPercent: Int,
    busy: Boolean,
    onSeek: (Float) -> Unit
) {
        return __splitImpl_SeekSection(positionState, durationMs, bufferedPercent, busy, onSeek)
    }

@Composable
internal fun SeekBar(progress: Float, buffered: Float, onSeek: (Float) -> Unit) {
        return __splitImpl_SeekBar(progress, buffered, onSeek)
    }

//  Controls 

@Composable
internal fun PlayerControls(state: RexMusicUiState, busy: Boolean, actions: PlayerActions) {
        return __splitImpl_PlayerControls(state, busy, actions)
    }

@Composable
internal fun PlayPauseButton(isPlaying: Boolean, busy: Boolean, onClick: () -> Unit) {
        return __splitImpl_PlayPauseButton(isPlaying, busy, onClick)
    }

@Composable
internal fun ToggleControl(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    active: Boolean,
    onClick: () -> Unit
) {
        return __splitImpl_ToggleControl(icon, description, active, onClick)
    }

@Composable
internal fun RepeatControl(total: Int, left: Int, onClick: () -> Unit) {
        return __splitImpl_RepeatControl(total, left, onClick)
    }

@Composable
internal fun BottomActions(
    state: RexMusicUiState,
    downloaded: Boolean,
    status: DownloadStatus?,
    actions: PlayerActions
) {
        return __splitImpl_BottomActions(state, downloaded, status, actions)
    }

@Composable
internal fun ActionPill(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    active: Boolean,
    onClick: () -> Unit
) {
        return __splitImpl_ActionPill(icon, label, active, onClick)
    }

//  Error & neighbors 

@Composable
internal fun PlayerError(error: String?, onRetry: () -> Unit) {
        return __splitImpl_PlayerError(error, onRetry)
    }

@Composable
internal fun NeighborSection(state: RexMusicUiState, onPrevious: () -> Unit, onNext: () -> Unit) {
        return __splitImpl_NeighborSection(state, onPrevious, onNext)
    }

@Composable
internal fun NeighborRow(
    label: String,
    track: RexTrack,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
        return __splitImpl_NeighborRow(label, track, icon, onClick)
    }

@Composable
internal fun AutoplayRow(favorites: List<String>, popularCount: Int, offlineMode: Boolean, onClick: () -> Unit) {
        return __splitImpl_AutoplayRow(favorites, popularCount, offlineMode, onClick)
    }

internal fun fmt(ms: Long): String {
    val totalSec = (ms / 1000).coerceAtLeast(0)
    return "%d:%02d".format(totalSec / 60, totalSec % 60)
}
