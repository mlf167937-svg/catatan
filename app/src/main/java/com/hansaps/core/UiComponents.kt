package com.hansaps.core

import androidx.annotation.RawRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import com.hansaps.R
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import android.net.Uri
import android.content.Context
import androidx.compose.ui.platform.LocalContext
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView

@Composable
fun HansWordmark(modifier: Modifier = Modifier) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
        androidx.compose.foundation.Image(
            painter = painterResource(id = R.drawable.ic_hansaps),
            contentDescription = "Logo HansAps",
            modifier = Modifier.size(42.dp).clip(RoundedCornerShape(11.dp)),
            contentScale = androidx.compose.ui.layout.ContentScale.Crop,
        )
        Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
            Text("HansAps", color = HansColors.Text, fontWeight = FontWeight.Bold, fontSize = 18.sp, letterSpacing = (-0.4).sp)
            Text("PERSONAL MEDIA SPACE", color = HansColors.Muted, fontSize = 8.sp, letterSpacing = 1.6.sp)
        }
    }
}

@Composable
fun SectionLabel(title: String, trailing: String? = null) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, color = HansColors.Text, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
        trailing?.let { Text(it, color = HansColors.Muted, fontSize = 12.sp) }
    }
}

@Composable
fun SoftCard(modifier: Modifier = Modifier, content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = HansColors.Surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, HansColors.Border.copy(alpha = 0.7f)),
    ) {
        Column(modifier = Modifier.padding(17.dp), content = content)
    }
}

@Composable
fun InlineVideoBanner(
    @RawRes resourceId: Int,
    modifier: Modifier = Modifier,
    muted: Boolean = true,
) {
    val context = LocalContext.current
    val exoPlayer = remember(resourceId) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(Uri.parse("android.resource://${context.packageName}/$resourceId")))
            repeatMode = Player.REPEAT_MODE_ONE
            volume = if (muted) 0f else 1f
            playWhenReady = true
            prepare()
        }
    }
    DisposableEffect(exoPlayer) {
        onDispose { exoPlayer.release() }
    }
    AndroidView(
        modifier = modifier,
        factory = { viewContext: Context ->
            PlayerView(viewContext).apply {
                player = exoPlayer
                useController = false
                resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                setShutterBackgroundColor(android.graphics.Color.TRANSPARENT)
                setKeepContentOnPlayerReset(true)
            }
        },
        update = { it.player = exoPlayer },
    )
}

@Composable
fun RolePill(role: UserRole) {
    val color = when (role) {
        UserRole.DEVELOPER -> HansColors.Lime
        UserRole.OWNER -> HansColors.Coral
        UserRole.USER -> HansColors.Muted
    }
    Surface(shape = CircleShape, color = color.copy(alpha = 0.13f)) {
        Text(role.label.uppercase(), modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp), color = color, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.7.sp)
    }
}

@Composable
fun EmptyState(title: String, detail: String, @RawRes iconResource: Int? = null) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 34.dp, horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier = Modifier.size(56.dp).clip(CircleShape).background(HansColors.SurfaceRaised),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Rounded.PlayArrow, contentDescription = null, tint = HansColors.Lime, modifier = Modifier.size(29.dp))
        }
        Text(title, fontWeight = FontWeight.SemiBold, color = HansColors.Text)
        Text(detail, color = HansColors.Muted, fontSize = 13.sp, lineHeight = 19.sp)
    }
}
