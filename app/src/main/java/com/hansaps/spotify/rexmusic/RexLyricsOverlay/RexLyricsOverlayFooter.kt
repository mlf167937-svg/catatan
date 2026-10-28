package com.hansaps.spotify.rexmusic

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp

@Composable
internal fun __splitImpl_LyricsFooter(
    positionState: State<Long>,
    durationMs: Long,
    accent: Color,
    onSeekTo: ((Long) -> Unit)?
) {
    val pos = positionState.value
    val progress = if (durationMs > 0) (pos.toFloat() / durationMs).coerceIn(0f, 1f) else 0f
    Column(
        Modifier
            .fillMaxWidth()
            .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.6f))))
            .padding(horizontal = 28.dp)
            .padding(top = 8.dp, bottom = 16.dp)
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(24.dp)
                .pointerInput(durationMs, onSeekTo) {
                    if (onSeekTo == null || durationMs <= 0) return@pointerInput
                    detectTapGestures { o ->
                        onSeekTo((o.x / size.width * durationMs).toLong().coerceIn(0L, durationMs))
                    }
                },
            contentAlignment = Alignment.CenterStart
        ) {
            Box(
                Modifier.fillMaxWidth().height(4.dp).clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.2f))
            )
            Box(
                Modifier.fillMaxWidth(progress).height(4.dp).clip(CircleShape)
                    .background(Brush.horizontalGradient(listOf(Color.White, accent.copy(alpha = 0.9f))))
            )
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(fmtLyrics(pos), style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.6f))
            Text(
                if (durationMs > 0) fmtLyrics(durationMs) else "--:--",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.6f)
            )
        }
    }
}
