package com.hansaps.localaccount

import android.Manifest
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal fun RexProfileController.__splitImpl_applyCrop(req: CropReq, bmp: Bitmap, s: Float, ox: Float, oy: Float, startMs: Long) {
        crop = null
        scope.launch {
            busy = "Memproses…"
            val prev = acct
            val result = withContext(Dispatchers.IO) {
                try {
                    RexPaths.account.mkdirs()
                    val isAvatar = req.target == CropTarget.AVATAR
                    val still = if (isAvatar) renderCrop(bmp, 1f, 768, s, ox, oy)
                    else renderCrop(bmp, BANNER_ASPECT, 1600, s, ox, oy)
                    val png = File(RexPaths.account, if (isAvatar) "profile-pictures.png" else "banner.png")
                    FileOutputStream(png).use { still.compress(Bitmap.CompressFormat.PNG, 100, it) }

                    val vf = File(RexPaths.account, if (isAvatar) "profile-video.mp4" else "banner.mp4")
                    var vpath = ""
                    var videoFailed = false
                    val v = req.video
                    if (v != null) {
                        if (trimVideo(context, v, startMs, vf)) vpath = vf.absolutePath
                        else { vf.delete(); videoFailed = true }
                    } else vf.delete()

                    val a = if (isAvatar)
                        prev.copy(picturePath = png.absolutePath, pictureVideo = vpath, pictureScale = s, pictureX = ox, pictureY = oy)
                    else
                        prev.copy(bannerPath = png.absolutePath, bannerVideo = vpath, bannerScale = s, bannerX = ox, bannerY = oy)

                    writeAccount(a)
                    req.video?.file?.delete()
                    a to videoFailed
                } catch (e: Exception) { null }
            }
            busy = null
            if (result == null) {
                toast("Gagal menyimpan (cek izin)")
            } else {
                acct = result.first
                rev++
                if (result.second) toast("Video tidak bisa diproses, dipakai sebagai foto")
            }
        }
    }

@Composable
internal fun __splitImpl_StoryTile(st: RexStory, acct: RexLocalAccount, rev: Int, modifier: Modifier, onClick: () -> Unit) {
    val left = (DAY_MS - (System.currentTimeMillis() - st.time)).coerceAtLeast(0)
    val shape = RoundedCornerShape(18.dp)
    Box(modifier.pressable(onClick).clip(shape).border(1.dp, Color.White.copy(alpha = .08f), shape)) {
        if (st.isVideo) {
            MediaThumb(st.file, Modifier.fillMaxSize())
        } else {
            Box(
                Modifier.fillMaxSize().background(
                    Brush.linearGradient(storyGradients[st.gradient.mod(storyGradients.size)])
                ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    st.text,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    maxLines = 5,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(10.dp),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(
            colorStops = arrayOf(0f to Color.Black.copy(.35f), .3f to Color.Transparent, .7f to Color.Transparent, 1f to Color.Black.copy(.6f))
        )))
        AvatarContent(
            acct, rev, false,
            Modifier.align(Alignment.TopStart).padding(8.dp).size(28.dp).border(2.dp, Color.White, CircleShape)
        )
        Text(
            "${left / 3_600_000}j lagi",
            color = Color.White,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.align(Alignment.BottomStart).padding(8.dp)
        )
    }
}

@Composable
internal fun __splitImpl_RexLocalAccountScreen(padding: PaddingValues) {
    val context = LocalContext.current
    var granted by remember { mutableStateOf(hasStorageAccess(context)) }
    val lifecycle = LocalLifecycleOwner.current.lifecycle

    DisposableEffect(lifecycle) {
        val ob = LifecycleEventObserver { _, e ->
            if (e == Lifecycle.Event.ON_RESUME) granted = hasStorageAccess(context)
        }
        lifecycle.addObserver(ob)
        onDispose { lifecycle.removeObserver(ob) }
    }

    val legacyLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { granted = hasStorageAccess(context) }

    if (!granted) {
        PermissionGate(padding) {
            if (Build.VERSION.SDK_INT >= 30) {
                try {
                    context.startActivity(
                        Intent(
                            Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                            Uri.parse("package:${context.packageName}")
                        )
                    )
                } catch (e: Exception) {
                    context.startActivity(Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION))
                }
            } else {
                legacyLauncher.launch(
                    arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                )
            }
        }
    } else {
        RexProfileRoot(padding)
    }
}

@Composable
internal fun __splitImpl_AvatarContent(acct: RexLocalAccount, rev: Int, animate: Boolean, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    var sz by remember { mutableStateOf(IntSize.Zero) }
    val bmp by produceState<Bitmap?>(null, acct.picturePath, rev) {
        value = if (acct.picturePath.isBlank()) null
        else withContext(Dispatchers.IO) { decodeFileSampled(acct.picturePath, 768) }
    }
    val hasVideo = animate && acct.pictureVideo.isNotBlank() && File(acct.pictureVideo).exists()

    Box(
        modifier.clip(CircleShape).background(colors.surfaceVariant).onSizeChanged { sz = it },
        contentAlignment = Alignment.Center
    ) {
        val b = bmp
        if (b != null) Image(b.asImageBitmap(), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        else Icon(Icons.Default.AccountCircle, null, Modifier.fillMaxSize(0.8f), tint = colors.primary)

        if (hasVideo) {
            RexVideo(
                acct.pictureVideo,
                Modifier.fillMaxSize().graphicsLayer {
                    scaleX = acct.pictureScale; scaleY = acct.pictureScale
                    translationX = acct.pictureX * sz.width
                    translationY = acct.pictureY * sz.height
                },
                key = rev
            )
        }
    }
}

@Composable
internal fun __splitImpl_ProfileIdentity(acct: RexLocalAccount, centered: Boolean) {
    val colors = MaterialTheme.colorScheme
    val align = if (centered) Alignment.CenterHorizontally else Alignment.Start
    val textAlign = if (centered) TextAlign.Center else TextAlign.Start

    Column(
        Modifier.fillMaxWidth(),
        horizontalAlignment = align,
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Text(
            acct.name,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = textAlign,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            acct.username,
            color = colors.primary,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            textAlign = textAlign,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        if (acct.bio.isNotBlank()) {
            Text(
                acct.bio,
                color = colors.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = textAlign,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
