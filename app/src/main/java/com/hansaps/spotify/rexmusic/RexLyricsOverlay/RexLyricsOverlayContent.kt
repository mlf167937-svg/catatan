package com.hansaps.spotify.rexmusic

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
internal fun __splitImpl_VinylDisc(cover: String, spinning: Boolean, accent: Color, size: Dp) {
    // sudut disimpan sebagai state, hanya dibaca di graphicsLayer -> tanpa recompose per frame
    var angle by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(spinning) {
        if (!spinning) return@LaunchedEffect
        var last = 0L
        while (true) {
            androidx.compose.runtime.withFrameNanos { now ->
                if (last != 0L) {
                    val dt = (now - last) / 1_000_000_000f
                    angle = (angle + dt * 36f) % 360f // 10 detik per putaran
                }
                last = now
            }
        }
    }

    Box(
        Modifier
            .size(size)
            .shadow(20.dp, CircleShape, ambientColor = accent, spotColor = accent)
            .graphicsLayer { rotationZ = angle }
            .clip(CircleShape)
            .background(Color(0xFF0B0B0F))
            .drawBehind {
                val r = this.size.minDimension / 2f
                // alur piringan
                var rr = r * 0.97f
                while (rr > r * 0.5f) {
                    drawCircle(
                        color = Color.White.copy(alpha = 0.06f),
                        radius = rr,
                        style = Stroke(width = 1f)
                    )
                    rr -= r * 0.045f
                }
                // kilau (sheen) diagonal
                drawCircle(
                    brush = Brush.linearGradient(
                        0f to Color.White.copy(alpha = 0.16f),
                        0.35f to Color.Transparent,
                        0.65f to Color.Transparent,
                        1f to Color.White.copy(alpha = 0.10f),
                        start = Offset(0f, 0f),
                        end = Offset(this.size.width, this.size.height)
                    ),
                    radius = r
                )
            },
        contentAlignment = Alignment.Center
    ) {
        // label tengah = cover
        Box(
            Modifier
                .size(size * 0.42f)
                .clip(CircleShape)
                .background(accent.copy(alpha = 0.6f))
        ) {
            CoverImage(cover, Modifier.fillMaxSize(), CircleShape, iconSize = 20.dp)
        }
        // lubang tengah
        Box(
            Modifier
                .size(size * 0.07f)
                .clip(CircleShape)
                .background(RexPlayerBase)
        )
    }
}
