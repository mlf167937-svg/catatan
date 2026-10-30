package com.hansaps.spotify.rexmusic

import android.os.SystemClock
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlin.math.max
import kotlinx.coroutines.*
import org.json.JSONObject

internal suspend fun RexMusicSyncEngine.__splitImpl_reconcileLocalTrack(id: String, targetPosition: Long, shouldPlay: Boolean) {
        val context = appContextRef ?: return
        RexPlayerController.init(context)
        val current = RexPlayerController.state.value
        if (current.nowPlaying?.id != id) {
            val track = RexOfflineManager.state.value.entries.firstOrNull { it.track.id == id }?.track
            if (track == null) {
                _state.value = _state.value.copy(error = "Lagu belum tersedia offline di HP ini.")
                return
            }
            withContext(Dispatchers.Main) { RexPlayerController.playFromOffline(track) }
            val deadline = SystemClock.elapsedRealtime() + 10000L
            while (SystemClock.elapsedRealtime() < deadline) {
                val s = RexPlayerController.state.value
                if (s.nowPlaying?.id == id && s.durationMs > 0L) break
                delay(100L)
            }
        }
        val now = RexPlayerController.state.value
        if (now.nowPlaying?.id != id) return
        if (kotlin.math.abs(now.positionMs - targetPosition) > DRIFT_CORRECTION_MS) withContext(Dispatchers.Main) { RexPlayerController.seekToPositionImmediate(targetPosition) }
        if (shouldPlay && !now.isPlaying) withContext(Dispatchers.Main) { RexPlayerController.play() }
        if (!shouldPlay && now.isPlaying) withContext(Dispatchers.Main) { RexPlayerController.pause() }
    }

@Composable
internal fun __splitImpl_SyncOfflineTrackCard(
    track: RexTrack,
    active: Boolean,
    onQueue: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(17.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.secondaryContainer) {
                androidx.compose.material3.Icon(
                    Icons.Default.MusicNote,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.padding(10.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(track.title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                Text(track.artist, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
            }
            TextButton(enabled = active, onClick = onQueue) { Text("+ QUEUE") }
        }
    }
}

internal fun RexMusicSyncEngine.__splitImpl_togglePlayback() {
        if (!_state.value.hosting) return
        hostPlaying = !hostPlaying
        if (hostPlaying) {
            playAnchorPositionMs = RexPlayerController.state.value.positionMs
            playAnchorWallMs = SystemClock.elapsedRealtime() + START_LEAD_MS
            broadcastState()
            playbackJob?.cancel(); playbackJob = scope.launch { delay(max(0L, playAnchorWallMs-SystemClock.elapsedRealtime())); withContext(Dispatchers.Main) { RexPlayerController.seekToPositionImmediate(playAnchorPositionMs); RexPlayerController.startSyncPlayback(playAnchorPositionMs) } }
        } else {
            scope.launch { withContextMain { RexPlayerController.pause() } }
            _state.value = _state.value.copy(playing = false, positionMs = RexPlayerController.state.value.positionMs, status = "Dijeda host")
            broadcastState()
        }
    }

@Composable
internal fun __splitImpl_SyncPlaybackEmptyCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text("Playback synchronization", style = MaterialTheme.typography.titleMedium)
            Text("The shared playback panel appears when you host or join a room.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

internal suspend fun RexMusicSyncEngine.__splitImpl_distributeLocalTrack(track: RexTrack) {
    val entry = RexOfflineManager.findEntry(track) ?: return
    val peersSnapshot = peers.toList()
    var failures = 0
    for (peer in peersSnapshot) {
        val ok = sendBundle(peer.ip, entry.track)
        if (ok) peer.sentTrackId = entry.track.id else failures++
    }
    _state.value = _state.value.copy(
        cacheStatus = if (failures == 0) "Cache siap di ${peersSnapshot.size} anggota" else "Transfer gagal ke $failures anggota",
        error = if (failures == 0) null else "Cek koneksi, port $TRANSFER_PORT, serta ruang penyimpanan di perangkat penerima."
    )
}

internal fun RexMusicSyncEngine.__splitImpl_setMemberVolume(id: String, value: Float) {
        val v = value.coerceIn(0f, 1f)
        if (!_state.value.hosting) return
        peers.firstOrNull { it.id == id }?.let { p -> p.volume = v; runCatching { send(p.writer, JSONObject().put("type", "volume").put("value", v)) } }
        _state.value = _state.value.copy(volumes = _state.value.volumes + (id to v), members = _state.value.members.map { if (it.id == id) it.copy(volume = v) else it })
    }

internal fun RexMusicSyncEngine.__splitImpl_trackFromJson(o: JSONObject?): RexTrack? = o?.let { RexTrack(it.optString("id"), it.optString("title"), it.optString("artist"), it.optString("album", "Single"), it.optString("cover"), spotifyUrl = it.optString("spotifyUrl"), durationText = it.optString("durationText")) }?.takeIf { it.id.isNotBlank() && it.title.isNotBlank() }

internal fun RexMusicSyncEngine.__splitImpl_setHostVolume(value: Float) {
        val v = value.coerceIn(0f, 1f)
        _state.value = _state.value.copy(hostVolume = v)
        appContextRef?.let { context ->
            RexPlayerController.init(context)
            RexPlayerController.setSyncVolume(v)
        }
    }

internal fun RexMusicSyncEngine.__splitImpl_trackJson(t: RexTrack) = JSONObject().put("id", t.id).put("title", t.title).put("artist", t.artist).put("album", t.album).put("cover", t.cover).put("spotifyUrl", t.spotifyUrl).put("durationText", t.durationText)

internal fun RexMusicSyncEngine.__splitImpl_stopLocalPlayback() { appContextRef?.let { c -> scope.launch(Dispatchers.Main) { runCatching { RexPlayerController.init(c); RexPlayerController.pause() } } } }
