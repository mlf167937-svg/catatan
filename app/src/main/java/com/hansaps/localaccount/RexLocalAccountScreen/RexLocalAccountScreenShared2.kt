package com.hansaps.localaccount

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.media.MediaExtractor
import android.media.MediaMetadataRetriever
import android.os.Build
import android.os.Environment
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.*
import androidx.core.content.ContextCompat
import java.io.File
import java.io.InputStream
import kotlin.math.max
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

internal fun RexProfileController.__splitImpl_clearBanner() {
        val a = acct.copy(bannerPath = "", bannerVideo = "")
        acct = a
        rev++
        scope.launch(Dispatchers.IO) {
            File(RexPaths.account, "banner.png").delete()
            File(RexPaths.account, "banner.mp4").delete()
            writeAccount(a)
        }
    }

internal fun __splitImpl_videoDurationOf(ctx: Context, src: VSrc): Long {
    val r = MediaMetadataRetriever()
    return try {
        src.bind(r, ctx)
        r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
    } catch (e: Exception) { 0L } finally { try { r.release() } catch (_: Exception) {} }
}

internal fun __splitImpl_downscale(b: Bitmap, maxDim: Int): Bitmap {
    val m = max(b.width, b.height)
    if (m <= maxDim) return b
    val k = maxDim.toFloat() / m
    return Bitmap.createScaledBitmap(b, (b.width * k).toInt().coerceAtLeast(1), (b.height * k).toInt().coerceAtLeast(1), true)
}

internal fun __splitImpl_hasStorageAccess(ctx: Context): Boolean = runCatching {
    val dir = File(ctx.filesDir, "HansAps/.config")
    if (!dir.isDirectory && !dir.mkdirs()) return@runCatching false
    val probe = File(dir, ".write-check-${System.nanoTime()}")
    try {
        probe.outputStream().use { it.write(0x48) }
        probe.isFile && probe.length() == 1L
    } finally {
        probe.delete()
    }
}.getOrDefault(false)

internal fun __splitImpl_loadVideos(): List<File> =
    RexPaths.video.listFiles()
        ?.filter { it.extension.lowercase() in setOf("mp4", "png", "jpg", "jpeg") }
        ?.sortedByDescending { it.lastModified() }
        ?: emptyList()

internal fun __splitImpl_rotate90(b: Bitmap): Bitmap {
    val m = android.graphics.Matrix().apply { postRotate(90f) }
    return Bitmap.createBitmap(b, 0, 0, b.width, b.height, m, true)
}

internal fun VSrc.__splitImpl_bind(e: MediaExtractor, c: Context) {
        if (uri != null) e.setDataSource(c, uri, null) else e.setDataSource(file!!.absolutePath)
    }

internal fun VSrc.__splitImpl_bind(r: MediaMetadataRetriever, c: Context) {
        if (uri != null) r.setDataSource(c, uri) else r.setDataSource(file!!.absolutePath)
    }

internal fun VSrc.__splitImpl_open(c: Context): InputStream? =
        if (uri != null) c.contentResolver.openInputStream(uri) else file?.inputStream()

internal fun RexProfileController.__splitImpl_toast(m: String) = Toast.makeText(context, m, Toast.LENGTH_SHORT).show()
