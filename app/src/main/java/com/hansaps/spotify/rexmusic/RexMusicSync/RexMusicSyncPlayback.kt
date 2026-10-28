package com.hansaps.spotify.rexmusic

import android.os.SystemClock
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlin.math.max
import kotlinx.coroutines.*

@Composable
internal fun __splitImpl_SyncPlaybackCard(
    state: SyncUiState,
    onHostVolumeChange: (Float) -> Unit,
    onTogglePlayback: () -> Unit,
    onStop: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = scheme.primaryContainer)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            SyncSectionHeading("Playback synchronization", "Shared playback status for this room", scheme.onPrimaryContainer, scheme.onPrimaryContainer.copy(alpha = 0.8f))
            Text(
                state.trackTitle.ifBlank { "Belum ada lagu" },
                style = MaterialTheme.typography.titleLarge,
                color = scheme.onPrimaryContainer,
                maxLines = 2,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = scheme.primary.copy(alpha = 0.12f)
                ) {
                    Text(
                        if (state.playing) "PLAYING" else "PAUSED",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = scheme.onPrimaryContainer
                    )
                }
                Spacer(Modifier.weight(1f))
                Text(
                    if (state.roomElapsedMs > 0L) formatRoomTime(state.roomElapsedMs) else "Room timer ready",
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onPrimaryContainer.copy(alpha = 0.8f)
                )
            }
            if (state.hosting) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Host volume", style = MaterialTheme.typography.labelMedium, color = scheme.onPrimaryContainer, modifier = Modifier.weight(1f))
                    Text("${(state.hostVolume.coerceIn(0f, 1f) * 100).toInt()}%", style = MaterialTheme.typography.labelLarge, color = scheme.onPrimaryContainer)
                }
                Slider(
                    value = state.hostVolume.coerceIn(0f, 1f),
                    onValueChange = onHostVolumeChange,
                    valueRange = 0f..1f
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = onTogglePlayback,
                        modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                        shape = RoundedCornerShape(13.dp)
                    ) {
                        androidx.compose.material3.Icon(
                            if (state.playing) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = null
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(if (state.playing) "Pause" else "Resume")
                    }
                    OutlinedButton(
                        onClick = onStop,
                        modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                        shape = RoundedCornerShape(13.dp)
                    ) {
                        androidx.compose.material3.Icon(Icons.Default.Stop, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Stop")
                    }
                }
            } else {
                Text(
                    "Playback is controlled by the host device.",
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onPrimaryContainer.copy(alpha = 0.8f)
                )
            }
        }
    }
}

internal fun RexMusicSyncEngine.__splitImpl_playTrack(track: RexTrack) {
    if (!_state.value.hosting) return
    // Hybrid lookup accepts a HansAps download, a RexAps/Music entry, or a title/artist match.
    val local = RexOfflineManager.findEntry(track)
    if (local == null) {
        _state.value = _state.value.copy(
            error = "Lagu belum tersedia di Download/HansAps/Spotify atau Download/RexAps/Music.",
            cacheStatus = "Unduh lagu atau beri izin akses semua file."
        )
        return
    }
    val resolvedTrack = local.track
    hostTrack = resolvedTrack
    playAnchorPositionMs = 0L
    hostPreparing = true
    hostPlaying = false
    _state.value = _state.value.copy(
        trackId = resolvedTrack.id,
        trackTitle = resolvedTrack.title,
        positionMs = 0L,
        playing = false,
        status = "Menyiapkan audio dan hybrid sync…",
        cacheStatus = "Memeriksa file lokal untuk anggota room…",
        error = null
    )
    playbackJob?.cancel()
    playbackJob = scope.launch {
        // Send the actual audio bundle before broadcasting playback state. This is the missing
        // step that previously left other devices with a track ID but no playable local MP3.
        val targets = peers.toList()
        if (targets.isNotEmpty()) {
            val results = coroutineScope {
                targets.map { peer -> async { peer to sendBundle(peer.ip, resolvedTrack) } }.awaitAll()
            }
            results.forEach { (peer, ok) ->
                if (ok) peer.sentTrackId = resolvedTrack.id
            }
            val failed = results.filterNot { it.second }.map { it.first.name }
            _state.value = _state.value.copy(
                cacheStatus = if (failed.isEmpty()) "File siap di ${targets.size} perangkat" else "Transfer gagal: ${failed.joinToString()}",
                error = if (failed.isEmpty()) null else "Sebagian perangkat tidak menerima file. Periksa izin penyimpanan dan koneksi Wi-Fi."
            )
        }
        if (!isActive || hostTrack?.id != resolvedTrack.id) return@launch

        withContext(Dispatchers.Main) { RexPlayerController.prepareFromOfflineForSync(resolvedTrack) }
        val deadline = SystemClock.elapsedRealtime() + 15000L
        while (isActive && SystemClock.elapsedRealtime() < deadline) {
            val state = RexPlayerController.state.value
            if (state.nowPlaying?.id == resolvedTrack.id && state.durationMs > 0L && state.phase == LoadPhase.Idle) break
            delay(40L)
        }
        val ready = RexPlayerController.state.value
        if (ready.nowPlaying?.id != resolvedTrack.id || ready.durationMs <= 0L) {
            hostPreparing = false
            hostPlaying = false
            _state.value = _state.value.copy(playing = false, status = "Lagu gagal disiapkan", error = "Player tidak siap sebelum batas waktu.")
            broadcastState()
            return@launch
        }
        hostPreparing = false
        hostPlaying = true
        playAnchorWallMs = SystemClock.elapsedRealtime() + START_LEAD_MS
        _state.value = _state.value.copy(playing = true, status = "Mulai serentak · hybrid room sync")
        broadcastState()
        delay(max(0L, playAnchorWallMs - SystemClock.elapsedRealtime()))
        withContext(Dispatchers.Main) { RexPlayerController.startSyncPlayback(0L) }
    }
}

internal suspend fun RexMusicSyncEngine.__splitImpl_syncLocalTrack(id: String, pos: Long, playing: Boolean, startAt: Long) {
        RexPlayerController.init(appContextRef ?: return)
        val track = RexOfflineManager.state.value.entries.firstOrNull { it.track.id == id }?.track
        if (track == null) {
            runCatching {
                clientWriter?.let { writer ->
                    send(writer, org.json.JSONObject().put("type", "request_bundle").put("trackId", id))
                }
            }
            _state.value = _state.value.copy(status = "Menunggu file dari host…", error = "Lagu '$id' belum ada di ${RexOfflineManager.DISPLAY_PATH}; meminta transfer otomatis.")
            // The host's transfer server imports the file into this app's own library. Retry once
            // after the bounded transfer window instead of abandoning playback immediately.
            delay(8_000L)
            RexOfflineManager.refreshNow()
            val retried = RexOfflineManager.state.value.entries.firstOrNull { it.track.id == id }?.track
            if (retried == null) {
                _state.value = _state.value.copy(status = "File belum diterima", error = "Pastikan host mengirim audio dan akses penyimpanan diberikan.")
                return
            }
            syncLocalTrack(id, pos, playing, startAt)
            return
        }
        withContext(Dispatchers.Main) { RexPlayerController.prepareFromOfflineForSync(track) }
        val deadline = SystemClock.elapsedRealtime() + 12000
        while (SystemClock.elapsedRealtime() < deadline) {
            val s = RexPlayerController.state.value
            if (s.nowPlaying?.id == id && s.durationMs > 0L && s.phase == LoadPhase.Idle) break
            delay(100)
        }
        val actual = RexPlayerController.state.value
        if (actual.nowPlaying?.id != id) return
        val targetHostElapsed = if (startAt > 0L) startAt else SystemClock.elapsedRealtime() + clockOffsetMs
        val targetLocalElapsed = targetHostElapsed - clockOffsetMs
        delay(max(0L, targetLocalElapsed - SystemClock.elapsedRealtime()))
        val targetPos = max(0L, pos + max(0L, SystemClock.elapsedRealtime() + clockOffsetMs - targetHostElapsed))
        withContext(Dispatchers.Main) {
            if (playing) RexPlayerController.startSyncPlayback(targetPos)
            else { RexPlayerController.seekToPositionImmediate(pos); RexPlayerController.pause() }
        }
    }
