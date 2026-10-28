package com.hansaps.localaccount

import android.graphics.Bitmap
import android.os.Build
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
internal fun __splitImpl_PermissionGate(padding: PaddingValues, onAllow: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val inf = rememberInfiniteTransition(label = "gate")
    val pulse by inf.animateFloat(
        1f, 1.08f,
        infiniteRepeatable(tween(1600, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "pulse"
    )
    val steps = if (Build.VERSION.SDK_INT >= 30) {
        listOf(
            "Tekan tombol \"Izinkan akses\" di bawah",
            "Aktifkan \"Izinkan akses ke semua file\"",
            "Kembali ke HansAps, halaman akan terbuka otomatis"
        )
    } else {
        listOf(
            "Tekan tombol \"Izinkan akses\" di bawah",
            "Setujui izin penyimpanan yang muncul"
        )
    }

    Box(Modifier.fillMaxSize().padding(padding)) {
        Aurora(Modifier.fillMaxSize())
        Column(
            Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(Modifier.graphicsLayer { scaleX = pulse; scaleY = pulse }, contentAlignment = Alignment.Center) {
                Box(Modifier.size(136.dp).clip(CircleShape).background(colors.primary.copy(alpha = .12f)))
                Box(
                    Modifier
                        .size(104.dp)
                        .clip(CircleShape)
                        .background(Brush.linearGradient(listOf(colors.primary, colors.tertiary))),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Lock, null, tint = colors.onPrimary, modifier = Modifier.size(44.dp))
                }
            }
            Spacer(Modifier.height(28.dp))
            Text(
                "Izin penyimpanan diperlukan",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(10.dp))
            Text(
                "Profil, story, dan video kamu disimpan langsung di Download/HansAps/.config/ — 100% lokal, tanpa server.",
                color = colors.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(24.dp))
            GlassCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    steps.forEachIndexed { i, s -> PermissionStep(i + 1, s) }
                }
            }
            Spacer(Modifier.height(26.dp))
            Button(
                onClick = onAllow,
                Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(18.dp)
            ) {
                Icon(Icons.Default.Lock, null, Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Izinkan akses", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
internal fun __splitImpl_BannerView(acct: RexLocalAccount, rev: Int, modifier: Modifier, parallax: () -> Float) {
    val colors = MaterialTheme.colorScheme
    val bg = colors.background
    val inf = rememberInfiniteTransition(label = "banner")
    val kb by inf.animateFloat(0f, 1f, infiniteRepeatable(tween(14000, easing = LinearEasing), RepeatMode.Reverse), label = "kb")
    val shift by inf.animateFloat(0f, 900f, infiniteRepeatable(tween(9000, easing = LinearEasing), RepeatMode.Reverse), label = "shift")
    var w by remember { mutableFloatStateOf(1f) }
    var h by remember { mutableFloatStateOf(1f) }
    val bmp by produceState<Bitmap?>(null, acct.bannerPath, rev) {
        value = if (acct.bannerPath.isNotBlank()) withContext(Dispatchers.IO) { decodeFileSampled(acct.bannerPath, 2048) } else null
    }
    val hasVideo = acct.bannerVideo.isNotBlank() && File(acct.bannerVideo).exists()

    Box(modifier.clipToBounds().onSizeChanged { w = it.width.toFloat(); h = it.height.toFloat() }) {
        Box(Modifier.fillMaxSize().graphicsLayer { translationY = parallax() }) {
            val b = bmp
            if (b != null) {
                Image(
                    b.asImageBitmap(), null,
                    Modifier.fillMaxSize().graphicsLayer {
                        if (!hasVideo) {
                            val z = 1.08f + 0.08f * kb
                            scaleX = z; scaleY = z
                            translationX = (kb - .5f) * 24f
                            translationY = (.5f - kb) * 12f
                        }
                    },
                    contentScale = ContentScale.Crop
                )
            } else if (!hasVideo) {
                Box(Modifier.fillMaxSize().drawBehind {
                    drawRect(
                        Brush.linearGradient(
                            listOf(colors.primary, colors.tertiary, colors.secondary),
                            start = Offset(shift, 0f),
                            end = Offset(shift + 900f, 900f),
                            tileMode = TileMode.Mirror
                        )
                    )
                })
            }
            if (hasVideo) {
                RexVideo(
                    acct.bannerVideo,
                    Modifier.fillMaxSize().graphicsLayer {
                        scaleX = acct.bannerScale; scaleY = acct.bannerScale
                        translationX = acct.bannerX * w
                        translationY = acct.bannerY * h
                    },
                    key = rev
                )
            }
        }
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(
            colorStops = arrayOf(0f to Color.Black.copy(.25f), .25f to Color.Transparent, .6f to Color.Transparent, 1f to bg)
        )))
    }
}

@Composable
internal fun __splitImpl_FeedItem(file: File, acct: RexLocalAccount, rev: Int, active: Boolean) {
    val isVideo = file.extension.lowercase() == "mp4"
    var userPlaying by remember(active) { mutableStateOf(true) }

    Box(
        Modifier.fillMaxSize().then(
            if (isVideo) Modifier.clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { userPlaying = !userPlaying } else Modifier
        ),
        contentAlignment = Alignment.Center
    ) {
        if (isVideo) {
            RexVideo(
                file.path,
                Modifier.fillMaxSize(),
                loop = true,
                muted = false,
                playing = active && userPlaying,
                fit = false
            )
            if (active && !userPlaying) {
                Box(
                    Modifier.size(72.dp).clip(CircleShape).background(Color.Black.copy(alpha = .48f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.PlayArrow, null, tint = Color.White, modifier = Modifier.size(44.dp))
                }
            }
        } else {
            val bmp by produceState<Bitmap?>(null, file.path) {
                value = withContext(Dispatchers.IO) { decodeFileSampled(file.path, 2000) }
            }
            bmp?.let { Image(it.asImageBitmap(), null, Modifier.fillMaxSize(), contentScale = ContentScale.Fit) }
        }

        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    colorStops = arrayOf(
                        0f to Color.Black.copy(alpha = .22f),
                        .22f to Color.Transparent,
                        .62f to Color.Transparent,
                        1f to Color.Black.copy(alpha = .78f)
                    )
                )
            )
        )

        Surface(
            Modifier.align(Alignment.TopEnd).statusBarsPadding().padding(top = 14.dp, end = 16.dp),
            shape = CircleShape,
            color = Color.Black.copy(alpha = .34f),
            border = BorderStroke(1.dp, Color.White.copy(alpha = .16f))
        ) {
            Row(
                Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Icon(Icons.Default.Lock, null, tint = Color.White.copy(alpha = .9f), modifier = Modifier.size(13.dp))
                Text("LOKAL", color = Color.White, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
            }
        }

        Row(
            Modifier.align(Alignment.BottomStart)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(start = 18.dp, end = 18.dp, bottom = 24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(11.dp)
        ) {
            AvatarContent(acct, rev, false, Modifier.size(44.dp).border(1.5.dp, Color.White.copy(alpha = .88f), CircleShape))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    acct.name,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    acct.username,
                    color = Color.White.copy(alpha = .84f),
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    if (isVideo) "Video tersimpan di perangkat ini" else "Foto tersimpan di perangkat ini",
                    color = Color.White.copy(alpha = .68f),
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Surface(shape = CircleShape, color = Color.White.copy(alpha = .12f)) {
                Icon(
                    if (isVideo) Icons.Default.Movie else Icons.Default.Image,
                    null,
                    tint = Color.White,
                    modifier = Modifier.padding(10.dp).size(17.dp)
                )
            }
        }
    }
}
