package com.hansaps.localaccount

import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(UnstableApi::class)
@Composable
internal fun __splitImpl_RexVideo(
    path: String,
    modifier: Modifier = Modifier,
    key: Any = path,
    loop: Boolean = true,
    muted: Boolean = true,
    playing: Boolean = true,
    fit: Boolean = false
) {
    val ctx = LocalContext.current
    val player = remember(path, key) {
        ExoPlayer.Builder(ctx).build().apply {
            setMediaItem(MediaItem.fromUri(Uri.fromFile(File(path))))
            repeatMode = if (loop) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
            volume = if (muted) 0f else 1f
            prepare()
        }
    }
    DisposableEffect(player) { onDispose { player.release() } }
    LaunchedEffect(player, playing) { player.playWhenReady = playing }
    AndroidView(
        modifier = modifier,
        factory = { c ->
            PlayerView(c).apply {
                this.player = player
                setShutterBackgroundColor(android.graphics.Color.TRANSPARENT)
            }
        },
        update = {
            it.player = player
            it.resizeMode = if (fit) AspectRatioFrameLayout.RESIZE_MODE_FIT else AspectRatioFrameLayout.RESIZE_MODE_ZOOM
        }
    )
}

@Composable
internal fun __splitImpl_AddTile(modifier: Modifier, title: String, sub: String, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(12.dp)
    Box(
        modifier.pressable(onClick)
            .clip(shape)
            .background(colors.surfaceVariant.copy(alpha = .30f))
            .border(1.dp, colors.outlineVariant.copy(alpha = .58f), shape),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.padding(8.dp)
        ) {
            Box(
                Modifier.size(36.dp).clip(RoundedCornerShape(12.dp)).background(colors.primary.copy(alpha = .14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Add, null, tint = colors.primary, modifier = Modifier.size(21.dp))
            }
            Text(title, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, maxLines = 1)
            Text(sub, style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant, maxLines = 1)
        }
    }
}
@Composable
internal fun __splitImpl_MediaOption(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    sub: String,
    modifier: Modifier,
    onClick: () -> Unit
) {
    val c = MaterialTheme.colorScheme
    Surface(
        modifier.pressable(onClick),
        shape = RoundedCornerShape(20.dp),
        color = c.surfaceVariant.copy(.7f),
        border = BorderStroke(1.dp, c.outlineVariant.copy(.4f))
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Box(
                Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(c.primary.copy(.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = c.primary, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.height(4.dp))
            Text(title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
            Text(sub, style = MaterialTheme.typography.labelSmall, color = c.onSurfaceVariant)
        }
    }
}

@Composable
internal fun __splitImpl_Aurora(modifier: Modifier) {
    val c = MaterialTheme.colorScheme
    val inf = rememberInfiniteTransition(label = "aurora")
    val t by inf.animateFloat(0f, 1f, infiniteRepeatable(tween(14000, easing = LinearEasing), RepeatMode.Reverse), label = "t")
    Box(modifier.drawBehind {
        drawRect(
            Brush.radialGradient(
                listOf(c.primary.copy(.26f), Color.Transparent),
                center = Offset(size.width * (.15f + .6f * t), size.height * .30f),
                radius = size.width * .95f
            )
        )
        drawRect(
            Brush.radialGradient(
                listOf(c.tertiary.copy(.22f), Color.Transparent),
                center = Offset(size.width * (.9f - .6f * t), size.height * .75f),
                radius = size.width * .9f
            )
        )
    })
}

@Composable
internal fun __splitImpl_MediaThumb(file: File, modifier: Modifier) {
    val ctx = LocalContext.current
    val isVideo = file.extension.lowercase() == "mp4"
    val bmp by produceState<Bitmap?>(null, file.path) {
        value = withContext(Dispatchers.IO) {
            if (isVideo) videoFrameAt(ctx, VSrc(null, file), 0) else decodeFileSampled(file.path, 600)
        }
    }
    Box(modifier.background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) {
        bmp?.let { Image(it.asImageBitmap(), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop) }
        if (isVideo) {
            Icon(Icons.Default.PlayArrow, null, tint = Color.White.copy(.9f), modifier = Modifier.size(34.dp))
        }
    }
}

@Composable
internal fun __splitImpl_PermissionStep(number: Int, text: String) {
    val c = MaterialTheme.colorScheme
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(
            Modifier.size(28.dp).clip(CircleShape).background(c.primary.copy(alpha = .16f)),
            contentAlignment = Alignment.Center
        ) {
            Text("$number", color = c.primary, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
        }
        Text(text, style = MaterialTheme.typography.bodyMedium, color = c.onSurface, modifier = Modifier.weight(1f))
    }
}

@Composable
internal fun __splitImpl_RexGrid(
    count: Int,
    columns: Int,
    cellAspect: Float,
    gap: Dp = 8.dp,
    cell: @Composable (Int, Modifier) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(gap)) {
        for (r in 0 until (count + columns - 1) / columns) {
            Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                for (c in 0 until columns) {
                    val i = r * columns + c
                    if (i < count) cell(i, Modifier.weight(1f).aspectRatio(cellAspect))
                    else Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
internal fun __splitImpl_StatItem(value: Int, label: String, modifier: Modifier) {
    val v by animateIntAsState(value, tween(900), label = "stat")
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text("$v", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
internal fun __splitImpl_GlowBadge(text: String) {
    Surface(shape = CircleShape, color = Color.White.copy(.16f)) {
        Text(
            text,
            Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            color = Color.White,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
internal fun __splitImpl_GlassCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val c = MaterialTheme.colorScheme
    Surface(
        modifier,
        shape = RoundedCornerShape(22.dp),
        color = c.surface.copy(alpha = .55f),
        border = BorderStroke(1.dp, c.outlineVariant.copy(alpha = .5f))
    ) { Column(content = content) }
}

@Composable
internal fun __splitImpl_Reveal(visible: Boolean, delayMs: Int, content: @Composable () -> Unit) {
    AnimatedVisibility(
        visible,
        enter = fadeIn(tween(550, delayMs)) + slideInVertically(tween(550, delayMs)) { it / 4 }
    ) { content() }
}
