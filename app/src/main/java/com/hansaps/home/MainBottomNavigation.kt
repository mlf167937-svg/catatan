package com.hansaps.home

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.hansaps.core.HansColors

internal enum class MainNavTab { HOME, SPOTIFY, YOUTUBE, ACCOUNT }

@Composable
internal fun HansMainBottomNavigation(
    selected: MainNavTab,
    onHome: () -> Unit,
    onSpotify: () -> Unit,
    onYoutube: () -> Unit,
    onAccount: () -> Unit,
) {
    NavigationBar(
        containerColor = HansColors.Surface,
        contentColor = HansColors.Text,
        tonalElevation = 8.dp
    ) {
        val colors = NavigationBarItemDefaults.colors(
            selectedIconColor = HansColors.Lime,
            selectedTextColor = HansColors.Text,
            indicatorColor = HansColors.Lime.copy(alpha = 0.14f),
            unselectedIconColor = HansColors.Muted,
            unselectedTextColor = HansColors.Muted
        )
        NavigationBarItem(
            selected = selected == MainNavTab.HOME,
            onClick = onHome,
            icon = { androidx.compose.material3.Icon(Icons.Rounded.Home, contentDescription = null) },
            label = { Text("Home") },
            colors = colors
        )
        NavigationBarItem(
            selected = selected == MainNavTab.SPOTIFY,
            onClick = onSpotify,
            icon = { androidx.compose.material3.Icon(Icons.Rounded.MusicNote, contentDescription = null) },
            label = { Text("Spotify") },
            colors = colors
        )
        NavigationBarItem(
            selected = selected == MainNavTab.YOUTUBE,
            onClick = onYoutube,
            icon = { androidx.compose.material3.Icon(Icons.Rounded.Movie, contentDescription = null) },
            label = { Text("YouTube") },
            colors = colors
        )
        NavigationBarItem(
            selected = selected == MainNavTab.ACCOUNT,
            onClick = onAccount,
            icon = { androidx.compose.material3.Icon(Icons.Rounded.AccountCircle, contentDescription = null) },
            label = { Text("Account") },
            colors = colors
        )
    }
}
