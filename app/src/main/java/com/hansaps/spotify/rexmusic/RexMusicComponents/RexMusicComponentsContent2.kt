package com.hansaps.spotify.rexmusic

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.hansaps.spotify.rexmusic.player.EqualizerBars

/** Saklar Online / Offline berbentuk pill. */
@Composable
internal fun __splitImpl_ModeChip(offline: Boolean, onToggle: () -> Unit, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    val fg by animateColorAsState(
        if (offline) scheme.tertiary else scheme.onSurfaceVariant, label = "chipFg"
    )
    val bg by animateColorAsState(
        if (offline) scheme.tertiary.copy(alpha = 0.16f) else scheme.onSurface.copy(alpha = 0.08f),
        label = "chipBg"
    )
    Row(
        modifier.height(36.dp).clip(CircleShape).background(bg)
            .clickable(role = Role.Switch, onClick = onToggle)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            if (offline) Icons.Default.CloudOff else Icons.Default.Cloud,
            contentDescription = null, tint = fg, modifier = Modifier.size(16.dp)
        )
        Text(
            if (offline) "Offline" else "Online", color = fg,
            style = MaterialTheme.typography.labelLarge, maxLines = 1
        )
    }
}

@Composable
internal fun __splitImpl_ArtistCard(name: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val c = rexTileColor(name)
    Column(
        modifier.width(108.dp).clip(RoundedCornerShape(12.dp)).clickable(onClick = onClick).padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            Modifier.size(96.dp).clip(CircleShape)
                .background(Brush.linearGradient(listOf(c, c.copy(alpha = 0.55f)))),
            contentAlignment = Alignment.Center
        ) {
            Text(
                name.take(1).uppercase(), color = Color.White,
                style = MaterialTheme.typography.headlineLarge
            )
        }
        Text(
            name, style = MaterialTheme.typography.labelLarge,
            textAlign = TextAlign.Center, maxLines = 2, overflow = TextOverflow.Ellipsis
        )
    }
}

/** Thumbnail kotak dengan overlay loading / equalizer. */
@Composable
internal fun __splitImpl_CoverThumb(
    url: String,
    size: Dp,
    modifier: Modifier = Modifier,
    overlay: CoverOverlay = CoverOverlay.None,
    shape: Shape = RoundedCornerShape(8.dp)
) {
    Box(modifier.size(size)) {
        CoverImage(url, Modifier.fillMaxSize(), shape, iconSize = size / 2.5f)
        if (overlay != CoverOverlay.None) {
            Box(
                Modifier.fillMaxSize().clip(shape).background(Color.Black.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                when (overlay) {
                    CoverOverlay.Loading -> CircularProgressIndicator(
                        color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(size / 2.4f)
                    )
                    CoverOverlay.Playing -> EqualizerBars(
                        color = Color.White, height = size / 3f, barWidth = 3.dp
                    )
                    CoverOverlay.None -> Unit
                }
            }
        }
    }
}

/** Tombol ikon bulat dengan ukuran pasti (tidak pernah melebar / bertabrakan). */
@Composable
internal fun __splitImpl_RexIconButton(
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
    Box(
        modifier
            .size(size)
            .clip(CircleShape)
            .then(if (background.isSpecified) Modifier.background(background) else Modifier)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            tint = if (enabled) tint else tint.copy(alpha = 0.38f),
            modifier = Modifier.size(iconSize)
        )
    }
}

@Composable
internal fun __splitImpl_QuickGrid(tiles: List<QuickTile>, nowId: String?, playing: Boolean, modifier: Modifier = Modifier) {
    Column(
        modifier.fillMaxWidth().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        tiles.chunked(2).forEach { pair ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                pair.forEach { tile ->
                    QuickTileCard(tile, tile.id == nowId, playing, Modifier.weight(1f))
                }
                if (pair.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
internal fun __splitImpl_SectionTitle(
    title: String,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null
) {
    Row(
        modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 24.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            title, style = MaterialTheme.typography.titleLarge,
            maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f)
        )
        trailing?.invoke()
    }
}

@Composable
internal fun __splitImpl_rememberPressScale(source: MutableInteractionSource, pressed: Float = 0.96f): State<Float> {
    val isPressed by source.collectIsPressedAsState()
    return animateFloatAsState(
        targetValue = if (isPressed) pressed else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "pressScale"
    )
}
