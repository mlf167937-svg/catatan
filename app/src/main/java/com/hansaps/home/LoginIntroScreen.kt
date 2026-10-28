package com.hansaps.home

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.graphics.Color as AndroidColor
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.datasource.RawResourceDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.hansaps.R

/** Plays intro.mp4 as a fullscreen post-login sequence, never as a home/login banner. */
@Composable
fun LoginIntroScreen(onFinished: () -> Unit) {
    val context = LocalContext.current
    val latestOnFinished = rememberUpdatedState(onFinished)
    val player = remember {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(RawResourceDataSource.buildRawResourceUri(R.raw.intro)))
            repeatMode = Player.REPEAT_MODE_OFF
            volume = 1f
            prepare()
            playWhenReady = true
        }
    }
    val activity = remember(context) { context.findActivity() }

    DisposableEffect(player, activity) {
        val window = activity?.window
        val controller = window?.let { WindowInsetsControllerCompat(it, it.decorView) }
        val oldBehavior = controller?.systemBarsBehavior
        controller?.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller?.hide(WindowInsetsCompat.Type.systemBars())
        window?.let { WindowCompat.setDecorFitsSystemWindows(it, false) }

        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED) latestOnFinished.value()
            }
            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                latestOnFinished.value()
            }
        }
        player.addListener(listener)
        onDispose {
            player.removeListener(listener)
            player.release()
            controller?.show(WindowInsetsCompat.Type.systemBars())
            if (oldBehavior != null) controller?.systemBarsBehavior = oldBehavior
            window?.let { WindowCompat.setDecorFitsSystemWindows(it, true) }
        }
    }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { viewContext ->
                PlayerView(viewContext).apply {
                    this.player = player
                    useController = false
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                    setShutterBackgroundColor(AndroidColor.BLACK)
                }
            },
            update = { it.player = player },
        )
        TextButton(
            onClick = onFinished,
            modifier = Modifier.align(Alignment.TopEnd).padding(top = 22.dp, end = 18.dp),
        ) { Text("Lewati  ›", color = Color.White, fontSize = 13.sp) }
    }
}

private fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
