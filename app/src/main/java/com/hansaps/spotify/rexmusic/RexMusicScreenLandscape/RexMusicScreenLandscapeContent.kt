package com.hansaps.spotify.rexmusic

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

@Composable
internal fun __splitImpl_AmbientBackdrop(clock: MotionClock, modifier: Modifier) {
    val scheme = MaterialTheme.colorScheme
    val primary = scheme.primary
    val secondary = scheme.secondary
    val tertiary = scheme.tertiary

    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val slow = clock.slow.value
        val pulse = clock.pulse.value
        val a = slow * TwoPi
        val big = max(w, h)

        // Ambient glows yang melayang pelan
        val c1 = Offset(w * (0.16f + 0.05f * sin(a)), h * (0.18f + 0.06f * cos(a)))
        val r1 = big * (0.52f + 0.03f * pulse)
        drawCircle(
            brush = Brush.radialGradient(listOf(primary.copy(alpha = 0.14f), Color.Transparent), center = c1, radius = r1),
            radius = r1,
            center = c1
        )
        val c2 = Offset(w * (0.86f + 0.04f * cos(a)), h * (0.84f + 0.05f * sin(a)))
        val r2 = big * (0.46f + 0.03f * (1f - pulse))
        drawCircle(
            brush = Brush.radialGradient(listOf(tertiary.copy(alpha = 0.11f), Color.Transparent), center = c2, radius = r2),
            radius = r2,
            center = c2
        )
        val c3 = Offset(w * (0.55f + 0.03f * sin(a + 1.7f)), h * 0.5f)
        val r3 = big * 0.28f
        drawCircle(
            brush = Brush.radialGradient(listOf(secondary.copy(alpha = 0.06f), Color.Transparent), center = c3, radius = r3),
            radius = r3,
            center = c3
        )

        // Dekorasi geometris: cincin konsentris + busur berputar + titik orbit
        val ringCenter = Offset(w * 0.84f, h * 0.26f)
        val hairline = Stroke(width = 1.dp.toPx())
        drawCircle(primary.copy(alpha = 0.06f), radius = h * 0.22f, center = ringCenter, style = hairline)
        drawCircle(primary.copy(alpha = 0.05f), radius = h * 0.34f, center = ringCenter, style = hairline)
        drawCircle(primary.copy(alpha = 0.035f), radius = h * 0.48f, center = ringCenter, style = hairline)

        val arcR = h * 0.34f
        val arcStart = slow * 720f
        drawArc(
            color = primary.copy(alpha = 0.16f),
            startAngle = arcStart,
            sweepAngle = 64f,
            useCenter = false,
            topLeft = Offset(ringCenter.x - arcR, ringCenter.y - arcR),
            size = Size(arcR * 2f, arcR * 2f),
            style = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round)
        )
        val dotAngle = (arcStart + 64f) * DegToRad
        drawCircle(
            color = tertiary.copy(alpha = 0.45f),
            radius = 2.5.dp.toPx(),
            center = Offset(ringCenter.x + arcR * cos(dotAngle), ringCenter.y + arcR * sin(dotAngle))
        )

        val ring2 = Offset(w * 0.1f, h * 0.92f)
        drawCircle(tertiary.copy(alpha = 0.05f), radius = h * 0.18f, center = ring2, style = hairline)
        drawCircle(tertiary.copy(alpha = 0.035f), radius = h * 0.3f, center = ring2, style = hairline)

        // A barely visible drafting grid gives the interface depth without competing with song art.
        val gridStep = 42.dp.toPx()
        val gridColor = scheme.outlineVariant.copy(alpha = 0.045f)
        var gridX = (w % gridStep) / 2f
        while (gridX < w) {
            drawLine(gridColor, Offset(gridX, 0f), Offset(gridX, h), 0.55.dp.toPx())
            gridX += gridStep
        }
        var gridY = (h % gridStep) / 2f
        while (gridY < h) {
            drawLine(gridColor, Offset(0f, gridY), Offset(w, gridY), 0.55.dp.toPx())
            gridY += gridStep
        }

        // Slow dust particles are intentionally dim; motion remains fluid and palette-aware.
        LandscapeParticles.forEach { particle ->
            val drift = slow * TwoPi * particle.speed + particle.phase
            val x = w * particle.x + sin(drift) * w * 0.009f
            val y = h * particle.y + cos(drift * 0.73f) * h * 0.012f
            val alpha = (0.035f + 0.055f * ((sin(drift) + 1f) / 2f)) * (0.7f + pulse * 0.3f)
            drawCircle(
                color = if (particle.phase.toInt() % 2 == 0) primary.copy(alpha = alpha)
                else tertiary.copy(alpha = alpha * 0.8f),
                radius = particle.radius.dp.toPx(),
                center = Offset(x, y)
            )
        }

        // A discreet audio-spectrum signature lives near the lower edge of the backdrop.
        val spectrumBase = h * 0.94f
        val spectrumWidth = w * 0.34f
        val spectrumStart = w * 0.57f
        val bars = 48
        val barGap = 3.dp.toPx()
        val barWidth = (spectrumWidth - barGap * (bars - 1)) / bars
        for (index in 0 until bars) {
            val envelope = sin((index.toFloat() / bars) * PI.toFloat()).toFloat()
            val wave = abs(sin(index * 0.39f + slow * TwoPi * 1.5f))
            val barHeight = (2.5f + envelope * (5f + 13f * wave) * (0.45f + pulse * 0.55f)).dp.toPx()
            drawRoundRect(
                brush = Brush.verticalGradient(
                    listOf(primary.copy(alpha = 0.13f), tertiary.copy(alpha = 0.035f))
                ),
                topLeft = Offset(spectrumStart + index * (barWidth + barGap), spectrumBase - barHeight),
                size = Size(barWidth.coerceAtLeast(1f), barHeight),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(barWidth / 2f)
            )
        }
    }
}
