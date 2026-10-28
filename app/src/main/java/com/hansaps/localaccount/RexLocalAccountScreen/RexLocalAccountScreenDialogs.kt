package com.hansaps.localaccount

import android.graphics.Bitmap
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
internal fun __splitImpl_CropDialog(
    req: CropReq,
    aspect: Float,
    circle: Boolean,
    title: String,
    onCancel: () -> Unit,
    onConfirm: (Bitmap, Float, Float, Float, Long) -> Unit
) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var bmp by remember { mutableStateOf(req.bitmap) }
    var zoom by remember { mutableFloatStateOf(1f) }
    var off by remember { mutableStateOf(Offset.Zero) }
    var vw by remember { mutableFloatStateOf(1f) }
    var vh by remember { mutableFloatStateOf(1f) }
    var startMs by remember { mutableFloatStateOf(0f) }

    val canTrim = req.video != null && req.durationMs > MAX_CLIP_MS
    val maxStart = (req.durationMs - MAX_CLIP_MS).coerceAtLeast(0L).toFloat()
    val fw = min(vw * .9f, vh * .9f * aspect)
    val fh = fw / aspect

    fun clamp(z: Float, o: Offset): Offset {
        val base = max(fw / bmp.width, fh / bmp.height)
        val mx = max(0f, (bmp.width * base * z - fw) / 2f)
        val my = max(0f, (bmp.height * base * z - fh) / 2f)
        return Offset(o.x.coerceIn(-mx, mx), o.y.coerceIn(-my, my))
    }

    if (canTrim) {
        LaunchedEffect(startMs) {
            if (startMs > 0f) delay(220)
            val f = withContext(Dispatchers.IO) { videoFrameAt(ctx, req.video!!, startMs.toLong()) }
            if (f != null && f.width == bmp.width && f.height == bmp.height) bmp = f
        }
    }

    Dialog(onDismissRequest = onCancel, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize(), color = Color(0xFF09090D)) {
            Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onCancel) { Icon(Icons.Default.Close, null, tint = Color.White) }
                    Column(Modifier.weight(1f)) {
                        Text(title, color = Color.White, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("Cubit & geser • ketuk 2x untuk zoom", color = Color.White.copy(.6f), style = MaterialTheme.typography.labelSmall)
                    }
                    if (req.live) GlowBadge("LIVE") else if (req.video != null) GlowBadge("VIDEO")
                    Spacer(Modifier.width(8.dp))
                }

                val img = remember(bmp) { bmp.asImageBitmap() }
                Canvas(
                    Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .onSizeChanged { vw = it.width.toFloat(); vh = it.height.toFloat() }
                        .pointerInput(fw, fh, bmp) {
                            detectTransformGestures { centroid, pan, gz, _ ->
                                val nz = (zoom * gz).coerceIn(1f, 8f)
                                val c = centroid - Offset(size.width / 2f, size.height / 2f)
                                val q = (c - off) / zoom
                                off = clamp(nz, c - q * nz + pan)
                                zoom = nz
                            }
                        }
                        .pointerInput(fw, fh, bmp) {
                            detectTapGestures(onDoubleTap = {
                                scope.launch {
                                    val from = zoom
                                    val to = if (from > 1.05f) 1f else 2.5f
                                    animate(from, to, animationSpec = tween(280)) { v, _ ->
                                        zoom = v
                                        off = clamp(v, off)
                                    }
                                }
                            })
                        }
                ) {
                    val base = max(fw / img.width, fh / img.height)
                    val s = base * zoom
                    val dw = img.width * s
                    val dh = img.height * s
                    val cx = size.width / 2f + off.x
                    val cy = size.height / 2f + off.y
                    drawImage(
                        img,
                        dstOffset = IntOffset((cx - dw / 2f).roundToInt(), (cy - dh / 2f).roundToInt()),
                        dstSize = IntSize(dw.roundToInt(), dh.roundToInt()),
                        filterQuality = FilterQuality.High
                    )
                    val left = (size.width - fw) / 2f
                    val top = (size.height - fh) / 2f
                    val frame = Rect(left, top, left + fw, top + fh)
                    val corner = CornerRadius(18.dp.toPx())
                    val hole = Path().apply {
                        fillType = PathFillType.EvenOdd
                        addRect(Rect(0f, 0f, size.width, size.height))
                        if (circle) addOval(frame) else addRoundRect(RoundRect(frame, corner))
                    }
                    drawPath(hole, Color.Black.copy(.72f))
                    if (circle) {
                        drawOval(Color.White, frame.topLeft, frame.size, style = Stroke(2.dp.toPx()))
                    } else {
                        drawRoundRect(Color.White, frame.topLeft, frame.size, corner, style = Stroke(2.dp.toPx()))
                        val gl = Color.White.copy(.28f)
                        for (k in 1..2) {
                            drawLine(gl, Offset(left + fw * k / 3f, top), Offset(left + fw * k / 3f, top + fh), 1.dp.toPx())
                            drawLine(gl, Offset(left, top + fh * k / 3f), Offset(left + fw, top + fh * k / 3f), 1.dp.toPx())
                        }
                    }
                }

                Column(
                    Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (canTrim) {
                        Text(
                            "Pilih 4 detik: ${secs(startMs.toLong())} – ${secs(startMs.toLong() + MAX_CLIP_MS)}  (total ${secs(req.durationMs)})",
                            color = Color.White,
                            style = MaterialTheme.typography.labelLarge
                        )
                        Slider(value = startMs, onValueChange = { startMs = it }, valueRange = 0f..maxStart)
                    } else if (req.video != null) {
                        Text(
                            if (req.live) "Live photo • gerakan ${secs(req.durationMs)} ikut disimpan"
                            else "Video ${secs(req.durationMs)} • sudah ≤ 4 detik",
                            color = Color.White.copy(.8f),
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                    Text("Zoom", color = Color.White.copy(.7f), style = MaterialTheme.typography.labelMedium)
                    Slider(
                        value = zoom,
                        valueRange = 1f..8f,
                        onValueChange = { zoom = it; off = clamp(it, off) }
                    )
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        IconButton(
                            enabled = req.video == null,
                            onClick = { bmp = rotate90(bmp); zoom = 1f; off = Offset.Zero }
                        ) {
                            Icon(Icons.Default.RotateRight, null, tint = Color.White.copy(if (req.video == null) 1f else .3f))
                        }
                        IconButton(onClick = { zoom = 1f; off = Offset.Zero }) {
                            Icon(Icons.Default.Refresh, null, tint = Color.White)
                        }
                        Spacer(Modifier.weight(1f))
                        OutlinedButton(onClick = onCancel) { Text("Batal") }
                        Button(onClick = { onConfirm(bmp, zoom, off.x / fw, off.y / fh, startMs.toLong()) }) { Text("Terapkan") }
                    }
                }
            }
        }
    }
}
