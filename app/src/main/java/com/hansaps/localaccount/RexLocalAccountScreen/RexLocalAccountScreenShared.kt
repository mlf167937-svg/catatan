package com.hansaps.localaccount

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.ExifInterface
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.media.MediaMuxer
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.*
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import kotlin.math.max
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

internal fun __splitImpl_trimVideo(ctx: Context, src: VSrc, startMs: Long, out: File): Boolean {
    val dur = videoDurationOf(ctx, src)
    if (dur in 1..(MAX_CLIP_MS + 300)) {
        return try {
            src.open(ctx)?.use { i -> FileOutputStream(out).use { i.copyTo(it) } }
            true
        } catch (e: Exception) { false }
    }
    val ex = MediaExtractor()
    var mux: MediaMuxer? = null
    return try {
        src.bind(ex, ctx)
        var vt = -1
        for (i in 0 until ex.trackCount) {
            if (ex.getTrackFormat(i).getString(MediaFormat.KEY_MIME)?.startsWith("video/") == true) { vt = i; break }
        }
        if (vt < 0) return false
        ex.selectTrack(vt)
        val fmt = ex.getTrackFormat(vt)
        val maxIn = if (fmt.containsKey(MediaFormat.KEY_MAX_INPUT_SIZE)) fmt.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE) else 0
        val buf = ByteBuffer.allocate(max(maxIn, 8 * 1024 * 1024))

        var rot = 0
        val r = MediaMetadataRetriever()
        try {
            src.bind(r, ctx)
            rot = r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)?.toIntOrNull() ?: 0
        } catch (_: Exception) {
        } finally { try { r.release() } catch (_: Exception) {} }

        out.delete()
        mux = MediaMuxer(out.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        val mt = mux.addTrack(fmt)
        mux.setOrientationHint(rot)
        mux.start()
        ex.seekTo(startMs * 1000, MediaExtractor.SEEK_TO_PREVIOUS_SYNC)

        val info = MediaCodec.BufferInfo()
        var first = -1L
        while (true) {
            buf.clear()
            val size = ex.readSampleData(buf, 0)
            if (size < 0) break
            val t = ex.sampleTime
            if (first < 0) first = t
            if (t - first > MAX_CLIP_MS * 1000) break
            val flags = if (ex.sampleFlags and MediaExtractor.SAMPLE_FLAG_SYNC != 0) MediaCodec.BUFFER_FLAG_KEY_FRAME else 0
            info.set(0, size, t - first, flags)
            mux.writeSampleData(mt, buf, info)
            ex.advance()
        }
        mux.stop()
        true
    } catch (e: Exception) { false }
    finally {
        try { mux?.release() } catch (_: Exception) {}
        try { ex.release() } catch (_: Exception) {}
    }
}

internal fun __splitImpl_extractMotionVideo(ctx: Context, uri: Uri, out: File): Boolean = try {
    val bytes = ctx.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: ByteArray(0)
    var start = -1
    if (bytes.size > 4096 && bytes[0] == 0xFF.toByte() && bytes[1] == 0xD8.toByte()) {
        var i = 2048
        while (i < bytes.size - 16) {
            if (bytes[i] == 'f'.code.toByte() && bytes[i + 1] == 't'.code.toByte() &&
                bytes[i + 2] == 'y'.code.toByte() && bytes[i + 3] == 'p'.code.toByte()
            ) {
                val boxSize = ((bytes[i - 4].toInt() and 0xFF) shl 24) or
                    ((bytes[i - 3].toInt() and 0xFF) shl 16) or
                    ((bytes[i - 2].toInt() and 0xFF) shl 8) or
                    (bytes[i - 1].toInt() and 0xFF)
                if (boxSize in 16..64) { start = i - 4; break }
            }
            i++
        }
    }
    if (start > 0 && bytes.size - start > 2048) {
        FileOutputStream(out).use { it.write(bytes, start, bytes.size - start) }
        true
    } else false
} catch (e: Exception) { false }

internal fun RexProfileController.__splitImpl_addVideo(uri: Uri?) {
        if (uri == null) return
        scope.launch(Dispatchers.IO) {
            try {
                RexPaths.video.mkdirs()
                val mime = context.contentResolver.getType(uri) ?: ""
                val ext = when {
                    mime.startsWith("video") -> "mp4"
                    mime.contains("png") -> "png"
                    else -> "jpg"
                }
                val f = File(RexPaths.video, "${System.currentTimeMillis()}.$ext")
                context.contentResolver.openInputStream(uri)?.use { i -> FileOutputStream(f).use { i.copyTo(it) } }
                val list = loadVideos()
                withContext(Dispatchers.Main) { videos = list }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { toast("Gagal: ${e.message}") }
            }
        }
    }

internal fun __splitImpl_applyExif(ctx: Context, uri: Uri, b: Bitmap): Bitmap {
    val o = try {
        ctx.contentResolver.openInputStream(uri)?.use {
            ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
        }
    } catch (e: Exception) { null } ?: ExifInterface.ORIENTATION_NORMAL

    val m = android.graphics.Matrix()
    when (o) {
        ExifInterface.ORIENTATION_ROTATE_90 -> m.postRotate(90f)
        ExifInterface.ORIENTATION_ROTATE_180 -> m.postRotate(180f)
        ExifInterface.ORIENTATION_ROTATE_270 -> m.postRotate(270f)
        ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> m.postScale(-1f, 1f)
        ExifInterface.ORIENTATION_FLIP_VERTICAL -> m.postScale(1f, -1f)
        else -> return b
    }
    return try { Bitmap.createBitmap(b, 0, 0, b.width, b.height, m, true) } catch (e: Exception) { b }
}

internal fun __splitImpl_loadStories(): List<RexStory> {
    val now = System.currentTimeMillis()
    val files = RexPaths.story.listFiles() ?: return emptyList()
    return files.mapNotNull { f ->
        val t = f.nameWithoutExtension.toLongOrNull() ?: return@mapNotNull null
        if (now - t > DAY_MS) { f.delete(); return@mapNotNull null }
        when (f.extension.lowercase()) {
            "mp4" -> RexStory(f, t, true, "", 0)
            "json" -> try {
                val j = JSONObject(f.readText())
                RexStory(f, t, false, j.optString("text"), j.optInt("gradient"))
            } catch (e: Exception) { null }
            else -> null
        }
    }.sortedBy { it.time }
}

internal fun __splitImpl_decodeSampled(ctx: Context, uri: Uri, maxDim: Int = 2400): Bitmap? = try {
    val o = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    ctx.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, o) }
    var s = 1
    while (max(o.outWidth, o.outHeight) / s > maxDim) s *= 2
    val raw = ctx.contentResolver.openInputStream(uri)?.use {
        BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = s })
    }
    raw?.let { applyExif(ctx, uri, it) }
} catch (e: Exception) { null }

internal fun Modifier.__splitImpl_pressable(onClick: () -> Unit): Modifier = composed {
    val src = remember { MutableInteractionSource() }
    val pressed by src.collectIsPressedAsState()
    val sc by animateFloatAsState(
        if (pressed) .93f else 1f,
        spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium),
        label = "press"
    )
    this.graphicsLayer { scaleX = sc; scaleY = sc }
        .clickable(interactionSource = src, indication = null, onClick = onClick)
}

suspend internal fun RexProfileController.__splitImpl_load() {
        withContext(Dispatchers.IO) {
            RexPaths.story.mkdirs()
            RexPaths.video.mkdirs()
        }
        withContext(Dispatchers.IO) { readAccount() }?.let { acct = it; rev++ }
        videos = withContext(Dispatchers.IO) { loadVideos() }
        visible = true
        while (true) {
            stories = withContext(Dispatchers.IO) { loadStories() }
            delay(60_000)
        }
    }

internal fun __splitImpl_videoFrameAt(ctx: Context, src: VSrc, ms: Long): Bitmap? {
    val r = MediaMetadataRetriever()
    return try {
        src.bind(r, ctx)
        val f = r.getFrameAtTime(ms * 1000, MediaMetadataRetriever.OPTION_PREVIOUS_SYNC) ?: r.getFrameAtTime(0)
        f?.let { downscale(it, 1600) }
    } catch (e: Exception) { null } finally { try { r.release() } catch (_: Exception) {} }
}

internal fun RexProfileController.__splitImpl_handlePick(uri: Uri?, target: CropTarget) {
        if (uri == null) return
        scope.launch {
            busy = "Membaca media…"
            val req = withContext(Dispatchers.IO) { buildCropReq(context, uri, target) }
            busy = null
            if (req == null) toast("Media tidak bisa dibaca") else crop = req
        }
    }
