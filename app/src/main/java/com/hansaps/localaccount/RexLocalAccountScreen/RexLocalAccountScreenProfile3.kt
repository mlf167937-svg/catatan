package com.hansaps.localaccount

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.AtomicFile
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import java.io.File
import java.io.FileOutputStream
import kotlin.math.max
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

@Composable
internal fun __splitImpl_rememberRexProfileController(): RexProfileController {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    RexPaths.initialize(context)
    val ctl = remember { RexProfileController(context, scope) }

    val bannerPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) {
        ctl.handlePick(it, CropTarget.BANNER)
    }
    val avatarPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) {
        ctl.handlePick(it, CropTarget.AVATAR)
    }
    val storyVideoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) {
        ctl.addStoryVideo(it)
    }
    val videoPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) {
        ctl.addVideo(it)
    }

    ctl.onPickBanner = { bannerPicker.launch(arrayOf("image/*", "video/*")) }
    ctl.onPickAvatar = { avatarPicker.launch(arrayOf("image/*", "video/*")) }
    ctl.onPickStoryVideo = { storyVideoPicker.launch("video/*") }
    ctl.onPickVideo = { videoPicker.launch(arrayOf("video/mp4", "image/png", "image/jpeg")) }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) { RexPaths.migrateLegacyIfNeeded() }
        ctl.load()
    }
    return ctl
}

internal fun __splitImpl_buildCropReq(ctx: Context, uri: Uri, target: CropTarget): CropReq? {
    val mime = ctx.contentResolver.getType(uri) ?: ""
    if (mime.startsWith("video")) {
        val src = VSrc(uri, null)
        val dur = videoDurationOf(ctx, src)
        val frame = videoFrameAt(ctx, src, 0) ?: return null
        return CropReq(target, frame, src, dur, false)
    }
    val photo = decodeSampled(ctx, uri) ?: return null
    if (mime == "image/jpeg") {
        val f = File(ctx.cacheDir, "live_${System.currentTimeMillis()}.mp4")
        if (extractMotionVideo(ctx, uri, f)) {
            val src = VSrc(null, f)
            val dur = videoDurationOf(ctx, src)
            val frame = videoFrameAt(ctx, src, 0)
            if (dur > 0 && frame != null) {
                val sameAspect = Math.abs(
                    photo.width.toFloat() / photo.height - frame.width.toFloat() / frame.height
                ) < 0.02f
                return CropReq(target, if (sameAspect) photo else frame, src, dur, true)
            }
        }
        f.delete()
    }
    return CropReq(target, photo, null, 0, false)
}

@Composable
internal fun __splitImpl_ProfileStats(ctl: RexProfileController) {
    val colors = MaterialTheme.colorScheme
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = colors.surface.copy(alpha = .52f),
        border = BorderStroke(1.dp, colors.outlineVariant.copy(alpha = .42f))
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            StatItem(ctl.videos.size, "Video", Modifier.weight(1f))
            Box(Modifier.width(1.dp).height(30.dp).background(colors.outlineVariant.copy(.55f)))
            StatItem(ctl.stories.size, "Story", Modifier.weight(1f))
            Box(Modifier.width(1.dp).height(30.dp).background(colors.outlineVariant.copy(.55f)))
            Column(
                Modifier.weight(1.1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(Icons.Default.Lock, null, tint = colors.primary, modifier = Modifier.size(17.dp))
                Text("LOKAL", style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

internal fun __splitImpl_readAccount(): RexLocalAccount? = try {
    val f = RexPaths.config
    if (!f.exists()) null else JSONObject(f.readText()).let { j ->
        var bp = j.optString("banner", "")
        var bv = j.optString("bannerVideo", "")
        if (bp.endsWith(".mp4")) { bv = bp; bp = "" }
        RexLocalAccount(
            j.optString("name", "HansAps User"), j.optString("username", "@hansuser"), j.optString("bio", ""),
            bp, bv,
            j.optDouble("bannerScale", 1.0).toFloat(), j.optDouble("bannerX", 0.0).toFloat(), j.optDouble("bannerY", 0.0).toFloat(),
            j.optString("profilePicture", ""), j.optString("profileVideo", ""),
            j.optDouble("pictureScale", 1.0).toFloat(), j.optDouble("pictureX", 0.0).toFloat(), j.optDouble("pictureY", 0.0).toFloat()
        )
    }
} catch (e: Exception) { null }

@Composable
internal fun __splitImpl_RexProfileRoot(padding: PaddingValues) {
    val ctl = rememberRexProfileController()
    val isLandscape = LocalConfiguration.current.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE

    Box(Modifier.fillMaxSize().padding(padding)) {
        if (isLandscape) {
            RexAccountScreenLandscape(ctl)
        } else {
            RexAccountScreenPortrait(ctl)
        }
    }

    RexProfileOverlays(ctl)
}

/** Responsive profile layout for portrait phones. */
@Composable
internal fun RexAccountScreenPortrait(ctl: RexProfileController) {
    Box(Modifier.fillMaxSize()) {
        Aurora(Modifier.fillMaxSize())

        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(Modifier.fillMaxWidth().height(218.dp)) {
                BannerView(
                    acct = ctl.acct,
                    rev = ctl.rev,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                        .height(176.dp)
                        .clip(RoundedCornerShape(22.dp)),
                    parallax = { 0f }
                )
                ProfileEditButton(ctl, Modifier.align(Alignment.TopEnd).padding(10.dp))
                StoryRingAvatar(
                    acct = ctl.acct,
                    rev = ctl.rev,
                    storyCount = ctl.stories.size,
                    dia = 96.dp,
                    modifier = Modifier.align(Alignment.BottomStart).padding(start = 12.dp),
                    onClick = {
                        if (ctl.stories.isNotEmpty()) ctl.storyIndex = 0
                        else ctl.showStoryCompose = true
                    },
                    onAdd = { ctl.showStoryCompose = true }
                )
            }

            ProfileIdentity(ctl.acct, centered = false)
            ProfileStats(ctl)
            ProfileActions(ctl)
            ProfileTabs(ctl, columns = 3, cellAspect = 0.86f)
        }
    }
}

/** Two-pane profile layout for landscape screens, keeping the collection independently scrollable. */
@Composable
internal fun RexAccountScreenLandscape(ctl: RexProfileController) {
    Box(Modifier.fillMaxSize()) {
        Aurora(Modifier.fillMaxSize())

        Row(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column(
                Modifier
                    .width(300.dp)
                    .fillMaxHeight()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(Modifier.fillMaxWidth().height(184.dp)) {
                    BannerView(
                        acct = ctl.acct,
                        rev = ctl.rev,
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .fillMaxWidth()
                            .height(145.dp)
                            .clip(RoundedCornerShape(20.dp)),
                        parallax = { 0f }
                    )
                    ProfileEditButton(ctl, Modifier.align(Alignment.TopEnd).padding(8.dp))
                    StoryRingAvatar(
                        acct = ctl.acct,
                        rev = ctl.rev,
                        storyCount = ctl.stories.size,
                        dia = 82.dp,
                        modifier = Modifier.align(Alignment.BottomStart).padding(start = 10.dp),
                        onClick = {
                            if (ctl.stories.isNotEmpty()) ctl.storyIndex = 0
                            else ctl.showStoryCompose = true
                        },
                        onAdd = { ctl.showStoryCompose = true }
                    )
                }
                ProfileIdentity(ctl.acct, centered = false)
                ProfileStats(ctl)
                ProfileActions(ctl)
            }

            Column(
                Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 8.dp)
            ) {
                ProfileTabs(ctl, columns = 4, cellAspect = 0.9f)
            }
        }
    }
}

@Composable
internal fun ProfileEditButton(ctl: RexProfileController, modifier: Modifier = Modifier) {
    Surface(
        modifier,
        shape = CircleShape,
        color = Color.Black.copy(alpha = .42f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = .18f))
    ) {
        Row(
            Modifier.clickable { ctl.showEdit = true }.padding(horizontal = 13.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            Icon(Icons.Default.Edit, null, tint = Color.White, modifier = Modifier.size(16.dp))
            Text("Edit profil", color = Color.White, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
internal fun __splitImpl_ProfileActions(ctl: RexProfileController) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Button(
            onClick = { ctl.showStoryCompose = true },
            modifier = Modifier.weight(1f).height(46.dp),
            shape = RoundedCornerShape(14.dp),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
        ) {
            Icon(Icons.Default.AutoAwesome, null, Modifier.size(17.dp))
            Spacer(Modifier.width(7.dp))
            Text("Buat story", maxLines = 1)
        }
        OutlinedButton(
            onClick = { ctl.onPickVideo() },
            modifier = Modifier.weight(1f).height(46.dp),
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = .7f)),
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp)
        ) {
            Icon(Icons.Default.Add, null, Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text("Tambah video", maxLines = 1)
        }
    }
}

internal fun RexProfileController.__splitImpl_addStoryVideo(uri: Uri?) {
        if (uri == null) return
        scope.launch(Dispatchers.IO) {
            try {
                RexPaths.story.mkdirs()
                val f = File(RexPaths.story, "${System.currentTimeMillis()}.mp4")
                context.contentResolver.openInputStream(uri)?.use { i -> FileOutputStream(f).use { i.copyTo(it) } }
                val list = loadStories()
                withContext(Dispatchers.Main) {
                    stories = list
                    toast("Story diposting (hilang dalam 24 jam)")
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { toast("Gagal: ${e.message}") }
            }
        }
    }

internal fun __splitImpl_writeAccount(a: RexLocalAccount) {
    val config = RexPaths.config
    val parent = config.parentFile ?: throw java.io.IOException("Folder akun tidak tersedia")
    if (!parent.isDirectory && !parent.mkdirs()) {
        throw java.io.IOException("Folder akun tidak dapat dibuat di penyimpanan aplikasi")
    }
    val j = JSONObject()
        .put("name", a.name).put("username", a.username).put("bio", a.bio)
        .put("banner", a.bannerPath).put("bannerVideo", a.bannerVideo)
        .put("bannerScale", a.bannerScale.toDouble()).put("bannerX", a.bannerX.toDouble()).put("bannerY", a.bannerY.toDouble())
        .put("profilePicture", a.picturePath).put("profileVideo", a.pictureVideo)
        .put("pictureScale", a.pictureScale.toDouble()).put("pictureX", a.pictureX.toDouble()).put("pictureY", a.pictureY.toDouble())
        .put("updatedAt", System.currentTimeMillis())
    val atomic = AtomicFile(config)
    var stream: java.io.FileOutputStream? = null
    try {
        stream = atomic.startWrite()
        stream.write(j.toString(2).toByteArray(Charsets.UTF_8))
        atomic.finishWrite(stream)
    } catch (error: Throwable) {
        stream?.let { runCatching { atomic.failWrite(it) } }
        throw error
    }
}

internal fun __splitImpl_renderCrop(src: Bitmap, aspect: Float, outW: Int, s: Float, ox: Float, oy: Float): Bitmap {
    val outH = (outW / aspect).toInt()
    val out = Bitmap.createBitmap(outW, outH, Bitmap.Config.ARGB_8888)
    val base = max(outW.toFloat() / src.width, outH.toFloat() / src.height) * s
    val m = android.graphics.Matrix().apply {
        postTranslate(-src.width / 2f, -src.height / 2f)
        postScale(base, base)
        postTranslate(outW / 2f + ox * outW, outH / 2f + oy * outH)
    }
    android.graphics.Canvas(out).drawBitmap(
        src, m,
        android.graphics.Paint(android.graphics.Paint.FILTER_BITMAP_FLAG or android.graphics.Paint.DITHER_FLAG)
    )
    return out
}
