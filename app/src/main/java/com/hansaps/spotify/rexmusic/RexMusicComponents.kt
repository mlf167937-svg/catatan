package com.hansaps.spotify.rexmusic

import android.graphics.drawable.Drawable
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

//  Models & actions 

/** Target menu opsi lagu. onPlay = null berarti menu "Putar" disembunyikan. */
class MenuTarget(val track: RexTrack, val onPlay: (() -> Unit)?)

class QuickTile(
    val id: String,
    val title: String,
    val cover: String,
    val special: Boolean = false,
    val onClick: () -> Unit
)

class RexActions(
    val playSearch: (Int) -> Unit,
    val playHistory: (Int) -> Unit,
    val playMain: (Int) -> Unit,
    val playPopular: (Int) -> Unit,
    val playOffline: (RexTrack) -> Unit,
    val playOfflineAll: (Boolean) -> Unit,
    val playMix: () -> Unit,
    val playQueue: (Int) -> Unit,
    val addQueue: (RexTrack) -> Unit,
    val removeQueue: (Int) -> Unit,
    val moveQueueTop: (Int) -> Unit,
    val clearQueue: () -> Unit,
    val download: (RexTrack) -> Unit,
    val cancelDownload: (RexTrack) -> Unit,
    val delete: (RexTrack) -> Unit,
    val deleteAll: () -> Unit,
    val clearHistory: () -> Unit,
    val requestAccess: () -> Unit,
    val setOffline: (Boolean) -> Unit,
    val toggleAutoplay: () -> Unit,
    val searchArtist: (String) -> Unit,
    val goLibrary: () -> Unit,
    val goPlaylists: () -> Unit,
    val openMenu: (MenuTarget) -> Unit,
    val openPlaylistPicker: (RexTrack) -> Unit,
    val playPlaylist: (String) -> Unit,
    val playPlaylistTrack: (String, String) -> Unit,
    val createPlaylist: (String) -> Unit,
    val deletePlaylist: (String) -> Unit,
    val dismissError: () -> Unit
)

enum class CoverOverlay { None, Loading, Playing }

fun overlayFor(active: Boolean, playing: Boolean, loading: Boolean): CoverOverlay = when {
    active && loading -> CoverOverlay.Loading
    active && playing -> CoverOverlay.Playing
    else -> CoverOverlay.None
}

//  Helpers 

/** Rata-rata warna dari bitmap 16x16 (murah, tanpa Palette). */
fun Drawable.averageColor(): Color? = this.__splitImpl_averageColor()

private val TilePalette = listOf(
    Color(0xFF7E57C2), Color(0xFF1E88E5), Color(0xFFE5533D), Color(0xFF00897B),
    Color(0xFFD81B60), Color(0xFF3949AB), Color(0xFF43A047), Color(0xFFF57C00)
)

fun rexTileColor(name: String): Color =
    TilePalette[(name.hashCode() and 0x7fffffff) % TilePalette.size]

fun formatBytes(bytes: Long): String = __splitImpl_formatBytes(bytes)

fun greetingForHour(hour: Int): String = when (hour) {
    in 4..10 -> "Selamat pagi"
    in 11..14 -> "Selamat siang"
    in 15..17 -> "Selamat sore"
    else -> "Selamat malam"
}

@Composable
fun rememberPressScale(source: MutableInteractionSource, pressed: Float = 0.96f): State<Float> {
        return __splitImpl_rememberPressScale(source, pressed)
    }

//  Basic building blocks 

/** Tombol ikon bulat dengan ukuran pasti (tidak pernah melebar / bertabrakan). */
@Composable
fun RexIconButton(
    icon: ImageVector,
    description: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    iconSize: Dp = 24.dp,
    tint: Color = LocalContentColor.current,
    background: Color = Color.Unspecified,
    enabled: Boolean = true
) {
        return __splitImpl_RexIconButton(icon, description, onClick, modifier, size, iconSize, tint, background, enabled)
    }

/** Gambar cover dengan placeholder ikon. Kalau [onColor] diisi, warna rata-rata dikirim balik. */
@Composable
fun CoverImage(
    url: String,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(8.dp),
    onColor: ((Color) -> Unit)? = null,
    contentDescription: String? = null,
    iconSize: Dp = 24.dp
) {
        return __splitImpl_CoverImage(url, modifier, shape, onColor, contentDescription, iconSize)
    }

/** Thumbnail kotak dengan overlay loading / equalizer. */
@Composable
fun CoverThumb(
    url: String,
    size: Dp,
    modifier: Modifier = Modifier,
    overlay: CoverOverlay = CoverOverlay.None,
    shape: Shape = RoundedCornerShape(8.dp)
) {
        return __splitImpl_CoverThumb(url, size, modifier, overlay, shape)
    }

@Composable
fun DownloadRing(status: DownloadStatus, size: Dp = 22.dp, modifier: Modifier = Modifier) {
        return __splitImpl_DownloadRing(status, size, modifier)
    }

@Composable
fun SectionTitle(
    title: String,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null
) {
        return __splitImpl_SectionTitle(title, modifier, trailing)
    }

/** Saklar Online / Offline berbentuk pill. */
@Composable
fun ModeChip(offline: Boolean, onToggle: () -> Unit, modifier: Modifier = Modifier) {
        return __splitImpl_ModeChip(offline, onToggle, modifier)
    }

/** Header untuk tab Cari & Koleksi. */
@Composable
fun TabHeader(title: String, offline: Boolean, onToggle: () -> Unit) {
        return __splitImpl_TabHeader(title, offline, onToggle)
    }

@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
        return __splitImpl_EmptyState(icon, title, message, modifier, actionLabel, onAction)
    }

@Composable
fun ErrorCard(error: String?, onDismiss: () -> Unit, modifier: Modifier = Modifier) {
        return __splitImpl_ErrorCard(error, onDismiss, modifier)
    }

//  Skeleton 

@Composable
fun SkeletonList(modifier: Modifier = Modifier) {
        return __splitImpl_SkeletonList(modifier)
    }

//  Track row (list) 

@Composable
fun TrackRow(
    track: RexTrack,
    active: Boolean,
    playing: Boolean,
    loading: Boolean,
    downloaded: Boolean,
    status: DownloadStatus?,
    onClick: () -> Unit,
    onMore: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null
) {
        return __splitImpl_TrackRow(track, active, playing, loading, downloaded, status, onClick, onMore, modifier, subtitle)
    }

//  Quick grid (Home) 

@Composable
fun QuickGrid(tiles: List<QuickTile>, nowId: String?, playing: Boolean, modifier: Modifier = Modifier) {
        return __splitImpl_QuickGrid(tiles, nowId, playing, modifier)
    }

@Composable
internal fun QuickTileCard(tile: QuickTile, active: Boolean, playing: Boolean, modifier: Modifier) {
        return __splitImpl_QuickTileCard(tile, active, playing, modifier)
    }

//  Shelf cards 

@Composable
fun ShelfCard(
    track: RexTrack,
    active: Boolean,
    playing: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 148.dp
) {
        return __splitImpl_ShelfCard(track, active, playing, onClick, modifier, size)
    }

@Composable
fun ArtistCard(name: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
        return __splitImpl_ArtistCard(name, onClick, modifier)
    }

//  Downloads & queue rows 

@Composable
fun DownloadRow(status: DownloadStatus, onCancel: () -> Unit) {
        return __splitImpl_DownloadRow(status, onCancel)
    }

@Composable
fun QueueRow(
    index: Int,
    track: RexTrack,
    onPlay: () -> Unit,
    onMoveTop: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
        return __splitImpl_QueueRow(index, track, onPlay, onMoveTop, onRemove, modifier)
    }

@Composable
fun AutoplayCard(state: RexMusicUiState, onToggle: () -> Unit) {
        return __splitImpl_AutoplayCard(state, onToggle)
    }
