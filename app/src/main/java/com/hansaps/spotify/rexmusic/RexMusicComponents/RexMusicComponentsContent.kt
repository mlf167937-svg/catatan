package com.hansaps.spotify.rexmusic

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.hansaps.spotify.rexmusic.player.EqualizerBars

@Composable
internal fun __splitImpl_ShelfCard(
    track: RexTrack,
    active: Boolean,
    playing: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 148.dp
) {
    val scheme = MaterialTheme.colorScheme
    val source = remember { MutableInteractionSource() }
    val scale = rememberPressScale(source)
    Column(
        modifier.width(size)
            .graphicsLayer { scaleX = scale.value; scaleY = scale.value }
            .clip(RoundedCornerShape(12.dp))
            .clickable(interactionSource = source, indication = LocalIndication.current, onClick = onClick),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(Modifier.size(size)) {
            CoverImage(track.cover, Modifier.fillMaxSize(), RoundedCornerShape(10.dp), iconSize = 44.dp)
            if (active && playing) {
                Box(
                    Modifier.align(Alignment.BottomEnd).padding(8.dp).size(28.dp)
                        .clip(CircleShape).background(Color.Black.copy(alpha = 0.65f)),
                    contentAlignment = Alignment.Center
                ) {
                    EqualizerBars(color = Color.White, barWidth = 2.dp, height = 12.dp)
                }
            }
        }
        Column(Modifier.padding(horizontal = 2.dp)) {
            Text(
                track.title, style = MaterialTheme.typography.titleSmall,
                color = if (active) scheme.primary else scheme.onSurface,
                maxLines = 1, overflow = TextOverflow.Ellipsis
            )
            Text(
                track.artist, style = MaterialTheme.typography.bodySmall,
                color = scheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
internal fun __splitImpl_AutoplayCard(state: RexMusicUiState, onToggle: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val subtitle = when {
        !state.autoplay -> "Mati: lagu berikutnya mengikuti urutan daftar"
        state.offline.enabled -> "Dipilih dari lagu unduhanmu"
        state.favoriteArtists.isNotEmpty() ->
            "Dicampur dari ${state.favoriteArtists.joinToString(", ")} & lagu populer"
        state.popularCount > 0 -> "Dipilih acak dari lagu populer"
        else -> "Dipilih otomatis untukmu"
    }
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(scheme.primary.copy(alpha = if (state.autoplay) 0.14f else 0.06f))
            .clickable(role = Role.Switch, onClick = onToggle)
            .padding(start = 14.dp, end = 10.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = scheme.primary)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text("Autoplay pintar", style = MaterialTheme.typography.titleSmall)
            Text(
                subtitle, style = MaterialTheme.typography.bodySmall,
                color = scheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(Modifier.width(8.dp))
        Switch(checked = state.autoplay, onCheckedChange = { onToggle() })
    }
}

@Composable
internal fun __splitImpl_QuickTileCard(tile: QuickTile, active: Boolean, playing: Boolean, modifier: Modifier) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier.height(56.dp).clip(RoundedCornerShape(8.dp))
            .background(scheme.surfaceVariant)
            .clickable(onClick = tile.onClick),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (tile.special) {
            Box(
                Modifier.size(56.dp).background(RexOfflineBrush),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.OfflinePin, contentDescription = null, tint = Color.White)
            }
        } else {
            CoverImage(tile.cover, Modifier.size(56.dp), RoundedCornerShape(0.dp))
        }
        Text(
            tile.title,
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
            color = if (active) scheme.primary else scheme.onSurface,
            maxLines = 2, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f).padding(horizontal = 10.dp)
        )
        if (active && playing) {
            EqualizerBars(
                Modifier.padding(end = 10.dp), color = scheme.primary,
                barWidth = 3.dp, height = 14.dp
            )
        }
    }
}

@Composable
internal fun __splitImpl_SkeletonList(modifier: Modifier = Modifier) {
    val base = MaterialTheme.colorScheme.surfaceVariant
    val x by rememberInfiniteTransition(label = "shimmer").animateFloat(
        initialValue = 0f, targetValue = 900f,
        animationSpec = infiniteRepeatable(tween(1200, easing = LinearEasing)),
        label = "shimmerX"
    )
    val brush = Brush.linearGradient(
        colors = listOf(base.copy(alpha = 0.35f), base.copy(alpha = 0.9f), base.copy(alpha = 0.35f)),
        start = Offset(x - 300f, 0f), end = Offset(x, 0f)
    )
    Column(modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        repeat(7) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(52.dp).clip(RoundedCornerShape(8.dp)).background(brush))
                Spacer(Modifier.width(12.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(Modifier.fillMaxWidth(0.6f).height(14.dp).clip(RoundedCornerShape(6.dp)).background(brush))
                    Box(Modifier.fillMaxWidth(0.4f).height(12.dp).clip(RoundedCornerShape(6.dp)).background(brush))
                }
            }
        }
    }
}

/** Gambar cover dengan placeholder ikon. Kalau [onColor] diisi, warna rata-rata dikirim balik. */
@Composable
internal fun __splitImpl_CoverImage(
    url: String,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(8.dp),
    onColor: ((Color) -> Unit)? = null,
    contentDescription: String? = null,
    iconSize: Dp = 24.dp
) {
    val context = LocalContext.current
    val scheme = MaterialTheme.colorScheme
    val callback by rememberUpdatedState(onColor)
    Box(
        modifier.clip(shape).background(scheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            Icons.Default.MusicNote, contentDescription = null,
            tint = scheme.onSurfaceVariant.copy(alpha = 0.6f),
            modifier = Modifier.size(iconSize)
        )
        if (url.isNotBlank()) {
            val needColor = onColor != null
            val request = remember(url, needColor) {
                ImageRequest.Builder(context).data(url)
                    .allowHardware(!needColor).crossfade(true).build()
            }
            AsyncImage(
                model = request,
                contentDescription = contentDescription,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
                onSuccess = { s -> callback?.let { cb -> s.result.drawable.averageColor()?.let(cb) } }
            )
        }
    }
}
