package com.hansaps.spotify.rexmusic

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.max
import kotlin.math.sin

@Composable
internal fun __splitImpl_DockIndicator(height: Dp, clock: MotionClock, modifier: Modifier) {
    val scheme = MaterialTheme.colorScheme
    val primary = scheme.primary
    val tertiary = scheme.tertiary
    val shape = RoundedCornerShape(20.dp)
    Box(
        modifier
            .fillMaxWidth()
            .height(height)
            .drawBehind {
                // Glow terkontrol di belakang indikator
                val r = this.size.width * 0.95f
                drawCircle(
                    brush = Brush.radialGradient(
                        listOf(primary.copy(alpha = 0.20f + 0.07f * clock.pulse.value), Color.Transparent),
                        radius = r
                    ),
                    radius = r
                )
            }
            .clip(shape)
            .background(
                Brush.linearGradient(
                    listOf(primary.copy(alpha = 0.24f), tertiary.copy(alpha = 0.10f))
                )
            )
            .border(
                1.dp,
                Brush.linearGradient(listOf(primary.copy(alpha = 0.55f), tertiary.copy(alpha = 0.25f))),
                shape
            )
            .drawWithContent {
                drawContent()
                // Kapsul aksen di sisi kiri
                val barH = this.size.height * 0.42f
                drawRoundRect(
                    color = primary,
                    topLeft = Offset(0f, (this.size.height - barH) / 2f),
                    size = Size(3.dp.toPx(), barH),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.dp.toPx())
                )
            }
    )
}

@Composable
internal fun __splitImpl_RexBadge(size: Dp, compact: Boolean, clock: MotionClock) {
    val scheme = MaterialTheme.colorScheme
    val primary = scheme.primary
    val tertiary = scheme.tertiary
    val ringBrush = remember(primary, tertiary) {
        Brush.sweepGradient(
            listOf(Color.Transparent, primary.copy(alpha = 0.9f), tertiary.copy(alpha = 0.9f), Color.Transparent)
        )
    }
    Box(
        modifier = Modifier
            .size(size)
            .drawBehind {
                val base = this.size.minDimension
                drawCircle(primary.copy(alpha = 0.08f + 0.08f * clock.pulse.value), radius = base * 0.85f)
                rotate(clock.slow.value * 360f * 3f) {
                    drawCircle(
                        brush = ringBrush,
                        radius = base / 2f + 4.dp.toPx(),
                        style = Stroke(width = 1.6.dp.toPx())
                    )
                }
            }
            .clip(RoundedCornerShape(if (compact) 12.dp else 14.dp))
            .background(Brush.linearGradient(listOf(primary, tertiary))),
        contentAlignment = Alignment.Center
    ) {
        Text(
            "R",
            style = if (compact) MaterialTheme.typography.titleMedium else MaterialTheme.typography.titleLarge,
            color = scheme.onPrimary,
            fontWeight = FontWeight.Black
        )
    }
}

/**
 * Transisi tab tanpa menduplikasi / mereset content: `content` hanya dipanggil SEKALI,
 * efek sinematik (fade + slide + scale halus) dilakukan via graphicsLayer pada container.
 */
@Composable
internal fun __splitImpl_ContentStage(
    selectedTab: Int,
    reduceMotion: Boolean,
    content: @Composable (Modifier) -> Unit
) {
    val progress = remember { Animatable(1f) }
    var settledTab by remember { mutableStateOf(selectedTab) }
    var direction by remember { mutableStateOf(1f) }
    val shiftPx = with(LocalDensity.current) { 40.dp.toPx() }

    LaunchedEffect(selectedTab) {
        if (selectedTab != settledTab) {
            direction = if (selectedTab > settledTab) 1f else -1f
            if (reduceMotion) {
                settledTab = selectedTab
            } else {
                progress.snapTo(0f)
                settledTab = selectedTab
                progress.animateTo(1f, tween(420, easing = EmphasizedDecelerate))
            }
        }
    }

    Box(
        modifier = Modifier
            .widthIn(max = 1440.dp)
            .fillMaxSize()
            .graphicsLayer {
                // Frame pertama setelah tab berganti: tahan di alpha 0 agar tidak flicker.
                val switching = selectedTab != settledTab && !reduceMotion
                val p = if (switching) 0f else progress.value
                alpha = p
                translationX = (1f - p) * shiftPx * direction
                val s = 0.985f + 0.015f * p
                scaleX = s
                scaleY = s
            }
    ) {
        content(Modifier.fillMaxSize())
    }
}

@Composable
internal fun __splitImpl_DockStatusSignal(clock: MotionClock) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(3) { index ->
            val offset = index * 0.23f
            val height = 3.5.dp + (clock.pulse.value * (5 + index % 2 * 2) + sin(clock.slow.value * TwoPi + offset) * 2f + 2f).dp
            Box(
                Modifier
                    .padding(horizontal = 1.5.dp)
                    .width(2.5.dp)
                    .height(height.coerceIn(3.dp, 12.dp))
                    .clip(RoundedCornerShape(4.dp))
                    .background(if (index == 1) scheme.tertiary.copy(alpha = 0.8f) else scheme.primary.copy(alpha = 0.65f))
            )
        }
    }
    Text(
        text = "AUDIO ENGINE",
        style = MaterialTheme.typography.labelSmall,
        color = scheme.onSurfaceVariant.copy(alpha = 0.58f),
        maxLines = 1,
        modifier = Modifier.padding(top = 5.dp)
    )
}
