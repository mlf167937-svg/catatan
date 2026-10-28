package com.hansaps.spotify.rexmusic

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
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
import kotlin.math.*

@Composable
internal fun __splitImpl_PortraitAmbientBackdrop(clock: PortraitMotionClock, modifier: Modifier) {
    val scheme = MaterialTheme.colorScheme
    val primary = scheme.primary
    val secondary = scheme.secondary
    val tertiary = scheme.tertiary

    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val drift = clock.drift.value
        val pulse = clock.pulse.value
        val angle = drift * PortraitTwoPi
        val maxDimension = max(w, h)

        // Layered aurora clouds blend the host palette instead of introducing new brand colors.
        val firstCenter = Offset(w * (0.12f + 0.035f * sin(angle)), h * (0.16f + 0.03f * cos(angle)))
        val firstRadius = maxDimension * (0.43f + 0.025f * pulse)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(primary.copy(alpha = 0.13f), primary.copy(alpha = 0.035f), Color.Transparent),
                center = firstCenter,
                radius = firstRadius
            ),
            center = firstCenter,
            radius = firstRadius
        )

        val secondCenter = Offset(w * (0.94f - 0.025f * cos(angle * 0.8f)), h * (0.72f + 0.045f * sin(angle)))
        val secondRadius = maxDimension * (0.48f + 0.02f * (1f - pulse))
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(tertiary.copy(alpha = 0.095f), Color.Transparent),
                center = secondCenter,
                radius = secondRadius
            ),
            center = secondCenter,
            radius = secondRadius
        )

        val center = Offset(w * (0.55f + 0.025f * sin(angle + 1.4f)), h * 0.48f)
        val centerRadius = maxDimension * 0.27f
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(secondary.copy(alpha = 0.055f), Color.Transparent),
                center = center,
                radius = centerRadius
            ),
            center = center,
            radius = centerRadius
        )

        // Hairline grid provides a crafted canvas texture and fades into the screen edges.
        val gridStep = 38.dp.toPx()
        val gridAlpha = 0.035f
        val lineColor = scheme.outlineVariant.copy(alpha = gridAlpha)
        var x = (w % gridStep) / 2f
        while (x < w) {
            drawLine(lineColor, Offset(x, 0f), Offset(x, h), 0.5.dp.toPx())
            x += gridStep
        }
        var y = (h % gridStep) / 2f
        while (y < h) {
            drawLine(lineColor, Offset(0f, y), Offset(w, y), 0.5.dp.toPx())
            y += gridStep
        }

        // Three orbit rings are intentionally oversized and partially clipped for depth.
        val orbitCenter = Offset(w * 0.91f, h * 0.24f)
        val orbitStroke = Stroke(width = 0.8.dp.toPx())
        drawCircle(primary.copy(alpha = 0.055f), h * 0.115f, orbitCenter, style = orbitStroke)
        drawCircle(primary.copy(alpha = 0.042f), h * 0.17f, orbitCenter, style = orbitStroke)
        drawCircle(tertiary.copy(alpha = 0.03f), h * 0.235f, orbitCenter, style = orbitStroke)
        val arcRadius = h * 0.17f
        val arcStart = drift * 720f + 26f
        drawArc(
            color = primary.copy(alpha = 0.17f),
            startAngle = arcStart,
            sweepAngle = 48f,
            useCenter = false,
            topLeft = Offset(orbitCenter.x - arcRadius, orbitCenter.y - arcRadius),
            size = Size(arcRadius * 2f, arcRadius * 2f),
            style = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round)
        )
        val dotAngle = (arcStart + 48f) * (PI / 180.0).toFloat()
        drawCircle(
            tertiary.copy(alpha = 0.52f),
            radius = 2.dp.toPx(),
            center = Offset(orbitCenter.x + arcRadius * cos(dotAngle), orbitCenter.y + arcRadius * sin(dotAngle))
        )

        PortraitDustField.forEach { dust ->
            val particleAngle = drift * PortraitTwoPi * dust.velocity + dust.phase
            val px = w * dust.x + sin(particleAngle) * w * 0.008f
            val py = h * dust.y + cos(particleAngle * 0.7f) * h * 0.009f
            val alpha = (0.025f + 0.05f * ((sin(particleAngle) + 1f) / 2f)) * (0.7f + pulse * 0.3f)
            drawCircle(
                color = if (dust.phase.toInt() % 2 == 0) primary.copy(alpha = alpha) else tertiary.copy(alpha = alpha * 0.75f),
                radius = dust.radius.dp.toPx(),
                center = Offset(px, py)
            )
        }

        // A slim reactive trace adds music identity to the negative space at the bottom.
        val baseY = h * 0.985f
        val traceWidth = w * 0.62f
        val traceStart = w * 0.19f
        val points = 52
        val gap = 2.2.dp.toPx()
        val barWidth = (traceWidth - gap * (points - 1)) / points
        for (index in 0 until points) {
            val normalized = index.toFloat() / (points - 1).coerceAtLeast(1)
            val envelope = sin(normalized * PI.toFloat()).toFloat()
            val wave = abs(sin(index * 0.43f + drift * PortraitTwoPi * 1.8f))
            val barHeight = (1.4f + envelope * (4f + 9f * wave) * (0.48f + pulse * 0.52f)).dp.toPx()
            drawRoundRect(
                brush = Brush.verticalGradient(
                    listOf(primary.copy(alpha = 0.10f), tertiary.copy(alpha = 0.025f))
                ),
                topLeft = Offset(traceStart + index * (barWidth + gap), baseY - barHeight),
                size = Size(barWidth.coerceAtLeast(1f), barHeight),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(barWidth / 2f)
            )
        }
    }
}
