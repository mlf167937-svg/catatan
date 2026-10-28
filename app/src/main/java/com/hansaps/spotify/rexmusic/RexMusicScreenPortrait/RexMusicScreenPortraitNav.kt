package com.hansaps.spotify.rexmusic

import androidx.compose.animation.animateColor
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

@Composable
internal fun __splitImpl_PortraitNavigationIcon(
    image: ImageVector,
    description: String,
    selected: Boolean,
    clock: PortraitMotionClock
) {
    val scheme = MaterialTheme.colorScheme
    val transition = updateTransition(targetState = selected, label = "portraitNavItemTransition")
    val scale by transition.animateFloat(
        transitionSpec = { spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow) },
        label = "portraitNavScale"
    ) { active -> if (active) 1.12f else 1f }
    val halo by transition.animateFloat(
        transitionSpec = { tween(260, easing = FastOutSlowInEasing) },
        label = "portraitNavHalo"
    ) { active -> if (active) 1f else 0f }
    val tint by transition.animateColor(
        transitionSpec = { tween(230) },
        label = "portraitNavTint"
    ) { active -> if (active) scheme.primary else scheme.onSurfaceVariant }

    Box(
        Modifier
            .size(width = 43.dp, height = 31.dp)
            .drawBehind {
                if (halo > 0.005f) {
                    val radius = size.minDimension * (0.42f + clock.pulse.value * 0.04f)
                    drawCircle(
                        brush = Brush.radialGradient(
                            listOf(scheme.primary.copy(alpha = 0.19f * halo), Color.Transparent),
                            radius = radius
                        ),
                        radius = radius
                    )
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = image,
            contentDescription = description,
            tint = tint,
            modifier = Modifier.graphicsLayer {
                scaleX = scale
                scaleY = scale
                translationY = if (selected) -0.5.dp.toPx() else 0f
            }
        )
        if (selected) {
            Box(
                Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 1.dp)
                    .size(width = 11.dp, height = 2.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        Brush.horizontalGradient(listOf(scheme.primary, scheme.tertiary))
                    )
            )
        }
    }
}
