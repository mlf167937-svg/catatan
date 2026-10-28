package com.hansaps.core

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

object HansColors {
    val Background = Color(0xFF08090D)
    val Surface = Color(0xFF13151C)
    val SurfaceRaised = Color(0xFF1B1E27)
    val Border = Color(0xFF2C303A)
    val Lime = Color(0xFF83C8FF)
    val Coral = Color(0xFFFF654D)
    val Orange = Color(0xFFF59A54)
    val Text = Color(0xFFF5F6F0)
    val Muted = Color(0xFF9CA2AE)
    val Error = Color(0xFFFF7777)
}

private val DarkColors = darkColorScheme(
    primary = HansColors.Lime,
    onPrimary = Color(0xFF171B0E),
    secondary = HansColors.Coral,
    onSecondary = Color.White,
    background = HansColors.Background,
    onBackground = HansColors.Text,
    surface = HansColors.Surface,
    onSurface = HansColors.Text,
    surfaceVariant = HansColors.SurfaceRaised,
    onSurfaceVariant = HansColors.Muted,
    outline = HansColors.Border,
    error = HansColors.Error,
)

@Composable
fun HansApsTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = DarkColors, content = content)
}
