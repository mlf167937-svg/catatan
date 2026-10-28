package com.hansaps.spotify.rexmusic.player

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hansaps.spotify.rexmusic.*

@Composable
internal fun __splitImpl_AutoplayRow(favorites: List<String>, popularCount: Int, offlineMode: Boolean, onClick: () -> Unit) {
    val subtitle = when {
        offlineMode -> "Dipilih dari lagu unduhanmu"
        favorites.isNotEmpty() -> "Dicampur dari ${favorites.joinToString(", ")} & lagu populer"
        popularCount > 0 -> "Dipilih acak dari lagu populer"
        else -> "Dipilih otomatis untukmu"
    }
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f))
            .clickable(onClick = onClick).padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.size(48.dp).clip(RoundedCornerShape(8.dp)).background(RexHeroBrush),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.onBackground)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text("AUTOPLAY PINTAR", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.tertiary)
            Text("Lagu berikutnya untukmu", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onBackground)
            Text(
                subtitle, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f), maxLines = 2, overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(Modifier.width(8.dp))
        Icon(Icons.Default.SkipNext, contentDescription = "Lagu berikutnya", tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f))
    }
}

@Composable
internal fun __splitImpl_PlayPauseButton(isPlaying: Boolean, busy: Boolean, onClick: () -> Unit) {
    val source = remember { MutableInteractionSource() }
    val scale = rememberPressScale(source, 0.92f)
    Box(
        Modifier
            .size(72.dp)
            .graphicsLayer { scaleX = scale.value; scaleY = scale.value }
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary)
            .clickable(
                interactionSource = source, indication = LocalIndication.current,
                role = Role.Button, onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Crossfade(
            targetState = when {
                busy -> 0
                isPlaying -> 1
                else -> 2
            },
            label = "playPause"
        ) { s ->
            when (s) {
                0 -> CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 3.dp, modifier = Modifier.size(28.dp)
                )
                1 -> Icon(
                    Icons.Default.Pause, contentDescription = "Jeda",
                    tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(40.dp)
                )
                else -> Icon(
                    Icons.Default.PlayArrow, contentDescription = "Putar",
                    tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(40.dp)
                )
            }
        }
    }
}

@Composable
internal fun __splitImpl_RepeatControl(total: Int, left: Int, onClick: () -> Unit) {
    val accent = MaterialTheme.colorScheme.tertiary
    val on = total > 0
    Box(
        Modifier.size(44.dp).clip(CircleShape).clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            if (on) Icons.Default.RepeatOne else Icons.Default.Repeat,
            contentDescription = if (on) "Ulangi lagu, sisa $left dari $total kali" else "Atur ulangi lagu",
            tint = if (on) accent else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
            modifier = Modifier.size(26.dp)
        )
        if (on) {
            Text(
                "$left", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onTertiary,
                modifier = Modifier.align(Alignment.TopEnd).padding(top = 2.dp)
                    .background(accent, CircleShape)
                    .padding(horizontal = 4.dp, vertical = 1.dp)
            )
            Box(
                Modifier.align(Alignment.BottomCenter).padding(bottom = 4.dp)
                    .size(4.dp).background(accent, CircleShape)
            )
        }
    }
}

@Composable
internal fun __splitImpl_NeighborRow(
    label: String,
    track: RexTrack,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f))
            .clickable(onClick = onClick).padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CoverThumb(track.cover, 48.dp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.tertiary)
            Text(
                track.title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1, overflow = TextOverflow.Ellipsis
            )
            Text(
                track.artist, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f), maxLines = 1, overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(Modifier.width(8.dp))
        Icon(icon, contentDescription = label, tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f))
    }
}

@Composable
internal fun __splitImpl_EqualizerBars(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    barWidth: Dp = 3.dp,
    height: Dp = 14.dp
) {
    val transition = rememberInfiniteTransition(label = "eq")
    val a by transition.animateFloat(
        0.3f, 1f, infiniteRepeatable(tween(420, easing = LinearEasing), RepeatMode.Reverse), label = "a"
    )
    val b by transition.animateFloat(
        0.2f, 1f, infiniteRepeatable(tween(560, easing = LinearEasing), RepeatMode.Reverse), label = "b"
    )
    val c by transition.animateFloat(
        0.35f, 1f, infiniteRepeatable(tween(340, easing = LinearEasing), RepeatMode.Reverse), label = "c"
    )
    Row(
        modifier.height(height),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        listOf(a, b, c).forEach { fraction ->
            Box(
                Modifier.width(barWidth).fillMaxHeight(fraction)
                    .clip(RoundedCornerShape(2.dp)).background(color)
            )
        }
    }
}

@Composable
internal fun __splitImpl_NeighborSection(state: RexMusicUiState, onPrevious: () -> Unit, onNext: () -> Unit) {
    val previous = state.previous
    val upNext = state.upNext
    if (previous == null && upNext == null && !state.autoplay) return
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (previous != null) {
            NeighborRow("SEBELUMNYA", previous, Icons.Default.SkipPrevious, onPrevious)
        }
        if (upNext != null) {
            NeighborRow(
                if (state.userQueue.isNotEmpty()) "DI ANTRIAN" else "SELANJUTNYA",
                upNext, Icons.Default.SkipNext, onNext
            )
        } else if (state.autoplay) {
            AutoplayRow(state.favoriteArtists, state.popularCount, state.offline.enabled, onNext)
        }
    }
}
