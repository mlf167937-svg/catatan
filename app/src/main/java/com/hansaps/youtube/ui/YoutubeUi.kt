package com.hansaps.youtube.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Gradien dan warna aksen bersama untuk seluruh modul YouTube. */
internal object YtPalette {
    // Palette inspired by YouTube's neutral surfaces, typography, and signature red.
    val Background = Color(0xFFFFFFFF)
    val Surface = Color(0xFFFFFFFF)
    val SurfaceAlt = Color(0xFFF2F2F2)
    val Border = Color(0xFFE5E5E5)
    val Text = Color(0xFF0F0F0F)
    val Muted = Color(0xFF606060)
    val Red = Color(0xFFFF0033)
    val AccentBrush: Brush = Brush.linearGradient(listOf(Red, Color(0xFFE62117)))
    val AccentSoftBrush: Brush = Brush.linearGradient(
        listOf(Red.copy(alpha = .12f), Red.copy(alpha = .04f)),
    )
}

/** Tombol/kartu yang mengecil halus saat ditekan, dengan pegas agar terasa hidup. */
@Composable
internal fun PressScale(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(18.dp),
    color: Color = YtPalette.Surface,
    border: BorderStroke? = null,
    content: @Composable () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) .96f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "pressScale",
    )
    Surface(
        onClick = onClick,
        modifier = modifier.graphicsLayer { scaleX = scale; scaleY = scale },
        shape = shape,
        color = color,
        border = border,
        interactionSource = interaction,
        content = content,
    )
}

/** Ikon bulat bergaya kaca untuk aksi kecil di header. */
@Composable
internal fun GlassIconButton(
    onClick: () -> Unit,
    icon: ImageVector,
    description: String,
    modifier: Modifier = Modifier,
) {
    PressScale(
        onClick = onClick,
        modifier = modifier.size(42.dp),
        shape = CircleShape,
        color = Color.Transparent,
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = description, tint = YtPalette.Text, modifier = Modifier.size(20.dp))
        }
    }
}

/** Titik kecil yang berkedip, menandakan pemutaran aktif. */
@Composable
internal fun LiveDot() {
    val transition = rememberInfiniteTransition(label = "liveDot")
    val dotAlpha by transition.animateFloat(
        initialValue = .3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(800, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "liveDotAlpha",
    )
    Box(
        Modifier
            .size(6.dp)
            .graphicsLayer { alpha = dotAlpha }
            .clip(CircleShape)
            .background(YtPalette.Red),
    )
}

/** Nilai pergeseran shimmer yang berjalan terus; dibagi ke semua blok skeleton. */
@Composable
internal fun rememberShimmerShift(): Float {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val shift by transition.animateFloat(
        initialValue = -1f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(tween(1300, easing = LinearEasing), RepeatMode.Restart),
        label = "shimmerShift",
    )
    return shift
}

/** Blok skeleton dengan kilau yang bergerak. */
@Composable
internal fun ShimmerBlock(shift: Float, modifier: Modifier = Modifier) {
    Spacer(
        modifier
            .background(YtPalette.SurfaceAlt)
            .drawBehind {
                val w = size.width
                drawRect(
                    brush = Brush.linearGradient(
                        colors = listOf(Color.Transparent, Color.White.copy(alpha = .10f), Color.Transparent),
                        start = Offset(w * shift, 0f),
                        end = Offset(w * (shift + .5f), 0f),
                    ),
                )
            },
    )
}

/** Tampilan kosong yang konsisten: ikon berdenyut dengan teks yang muncul perlahan. */
@Composable
internal fun YtEmptyState(
    icon: ImageVector,
    title: String,
    body: String,
    modifier: Modifier = Modifier,
) {
    val transition = rememberInfiniteTransition(label = "emptyPulse")
    val pulse by transition.animateFloat(
        initialValue = 1f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(tween(1400, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "emptyPulseScale",
    )
    val entrance = remember { MutableTransitionState(false).apply { targetState = true } }
    AnimatedVisibility(
        visibleState = entrance,
        modifier = modifier.fillMaxSize(),
        enter = fadeIn(tween(500)) + scaleIn(tween(500), initialScale = .9f),
    ) {
        Column(
            Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                Modifier
                    .size(96.dp)
                    .graphicsLayer { scaleX = pulse; scaleY = pulse }
                    .clip(CircleShape)
                    .background(YtPalette.AccentSoftBrush),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = YtPalette.Red, modifier = Modifier.size(40.dp))
            }
            Spacer(Modifier.size(18.dp))
            Text(title, color = YtPalette.Text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.size(6.dp))
            Text(
                body,
                color = YtPalette.Muted,
                fontSize = 12.sp,
                lineHeight = 18.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 36.dp),
            )
        }
    }
}
