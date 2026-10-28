package com.hansaps.spotify

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Devices
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material.icons.rounded.WifiTethering
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hansaps.core.HansColors
import kotlinx.coroutines.delay

@Composable
fun SpotifySyncScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val state by SpotifySyncEngine.state.collectAsState()
    var room by remember { mutableStateOf("HansRoom") }
    var password by remember { mutableStateOf("hansaps") }
    var hostIp by remember { mutableStateOf(SpotifySyncEngine.detectIp()) }
    var deviceName by remember { mutableStateOf("HansAps Device") }
    var sliderPosition by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(state.positionMs, state.durationMs) {
        if (state.durationMs > 0L) sliderPosition = (state.positionMs.toFloat() / state.durationMs.toFloat()).coerceIn(0f, 1f)
    }

    Column(
        modifier = modifier.fillMaxWidth().background(HansColors.Background).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(13.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("HansAps Sync", color = HansColors.Text, fontSize = 23.sp, fontWeight = FontWeight.Bold)
            Text("Sinkronkan pemutaran antarperangkat di jaringan Wi-Fi yang sama.", color = HansColors.Muted, fontSize = 12.sp, lineHeight = 17.sp)
        }
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            color = HansColors.Surface,
            border = BorderStroke(1.dp, HansColors.Border),
        ) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(11.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    Surface(shape = RoundedCornerShape(13.dp), color = HansColors.Lime.copy(alpha = .13f)) {
                        Icon(Icons.Rounded.WifiTethering, contentDescription = null, tint = HansColors.Lime, modifier = Modifier.padding(10.dp).size(21.dp))
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(if (state.hosting) "Ruang berjalan" else if (state.connected) "Terhubung" else "Buat atau gabung ruang", color = HansColors.Text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        Text(state.status, color = HansColors.Muted, fontSize = 10.sp, lineHeight = 14.sp)
                    }
                    if (state.connected) Surface(color = HansColors.Lime.copy(alpha = .14f), shape = CircleShape) {
                        Text("LIVE", modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp), color = HansColors.Lime, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                }
                OutlinedTextField(room, { room = it }, modifier = Modifier.fillMaxWidth(), singleLine = true, label = { Text("Nama ruang") }, enabled = !state.connected)
                OutlinedTextField(password, { password = it }, modifier = Modifier.fillMaxWidth(), singleLine = true, label = { Text("Password ruang") }, enabled = !state.connected)
                OutlinedTextField(deviceName, { deviceName = it }, modifier = Modifier.fillMaxWidth(), singleLine = true, label = { Text("Nama perangkat") }, enabled = !state.connected)
                OutlinedTextField(hostIp, { hostIp = it }, modifier = Modifier.fillMaxWidth(), singleLine = true, label = { Text("IP host") }, enabled = !state.connected, supportingText = { Text("Host dan perangkat lain harus memakai Wi-Fi/LAN yang sama.", fontSize = 10.sp) })
                if (!state.connected) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { SpotifySyncEngine.host(context, room, password, deviceName) },
                            modifier = Modifier.weight(1f).height(46.dp),
                            shape = RoundedCornerShape(13.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = HansColors.Lime, contentColor = Color(0xFF0B1721)),
                        ) {
                            Icon(Icons.Rounded.WifiTethering, contentDescription = null, modifier = Modifier.size(17.dp))
                            Spacer(Modifier.size(6.dp)); Text("Buat ruang", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        Button(
                            onClick = { SpotifySyncEngine.join(context, room, hostIp, password, deviceName) },
                            modifier = Modifier.weight(1f).height(46.dp),
                            shape = RoundedCornerShape(13.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = HansColors.SurfaceRaised, contentColor = HansColors.Text),
                        ) {
                            Icon(Icons.Rounded.Sync, contentDescription = null, modifier = Modifier.size(17.dp))
                            Spacer(Modifier.size(6.dp)); Text("Gabung", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
                    Text("IP host: ${state.ip.ifBlank { hostIp.ifBlank { "tidak terdeteksi" } }}  •  Port $CONTROL_PORT_LABEL", color = HansColors.Lime, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                    TextButton(onClick = { SpotifySyncEngine.disconnect() }, modifier = Modifier.align(Alignment.End)) {
                        Text("Putuskan koneksi", color = HansColors.Error, fontSize = 11.sp)
                    }
                }
                state.error?.let { Text(it, color = HansColors.Error, fontSize = 11.sp, lineHeight = 16.sp) }
            }
        }

        if (state.connected) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = HansColors.Surface,
                border = BorderStroke(1.dp, HansColors.Border),
            ) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                        Icon(Icons.Rounded.Devices, contentDescription = null, tint = HansColors.Lime, modifier = Modifier.size(19.dp))
                        Text("Anggota ruang", color = HansColors.Text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.weight(1f))
                        Text("${state.members.size}", color = HansColors.Lime, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                    if (state.members.isEmpty()) Text("Menunggu anggota…", color = HansColors.Muted, fontSize = 11.sp)
                    else state.members.forEach { member ->
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Surface(shape = CircleShape, color = HansColors.SurfaceRaised) {
                                Icon(Icons.Rounded.Devices, contentDescription = null, modifier = Modifier.padding(7.dp).size(14.dp), tint = HansColors.Muted)
                            }
                            Text(member, color = HansColors.Text, fontSize = 11.sp)
                        }
                    }
                    Spacer(Modifier.size(2.dp))
                    Text("SEDANG DIPUTAR", color = HansColors.Muted, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.3.sp)
                    Text(state.title, color = HansColors.Text, fontSize = 17.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text(state.artist.ifBlank { if (state.trackId.isBlank()) "Putar lagu di tab Dengar terlebih dahulu." else "HansAps Sync" }, color = HansColors.Muted, fontSize = 12.sp)
                    Slider(
                        value = sliderPosition,
                        onValueChange = { sliderPosition = it },
                        onValueChangeFinished = { if (state.durationMs > 0L) SpotifySyncEngine.control("seek", (sliderPosition * state.durationMs).toLong()) },
                        enabled = state.durationMs > 0L,
                    )
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(13.dp), modifier = Modifier.align(Alignment.CenterHorizontally)) {
                        androidx.compose.material3.IconButton(onClick = { SpotifySyncEngine.control("previous") }) {
                            Icon(Icons.Rounded.SkipPrevious, contentDescription = "Sebelumnya", tint = HansColors.Text, modifier = Modifier.size(27.dp))
                        }
                        Surface(onClick = { SpotifySyncEngine.control("toggle") }, shape = CircleShape, color = HansColors.Lime) {
                            Icon(if (state.playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, contentDescription = if (state.playing) "Jeda" else "Putar", tint = Color(0xFF0B1721), modifier = Modifier.padding(13.dp).size(29.dp))
                        }
                        androidx.compose.material3.IconButton(onClick = { SpotifySyncEngine.control("next") }) {
                            Icon(Icons.Rounded.SkipNext, contentDescription = "Berikutnya", tint = HansColors.Text, modifier = Modifier.size(27.dp))
                        }
                    }
                    Text("Antrean dan posisi dipimpin host. Track offline akan dikirim ke perangkat anggota melalui jaringan lokal.", color = HansColors.Muted, fontSize = 10.sp, lineHeight = 15.sp)
                }
            }
        }
    }
}

private const val CONTROL_PORT_LABEL = "38741"
