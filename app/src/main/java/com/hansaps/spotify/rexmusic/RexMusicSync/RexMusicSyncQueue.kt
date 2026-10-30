package com.hansaps.spotify.rexmusic

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.Color
import coil.compose.AsyncImage
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.*
import org.json.JSONObject

@Composable
internal fun __splitImpl_SyncQueueTrackCard(track: RexTrack, onPlay: () -> Unit) {
    Card(
        onClick = onPlay,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(17.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier.size(58.dp).clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                if (track.cover.isNotBlank()) {
                    AsyncImage(
                        model = track.cover,
                        contentDescription = "Cover ${track.title}",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    androidx.compose.material3.Icon(
                        Icons.AutoMirrored.Filled.QueueMusic,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(track.title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                Text(track.artist, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
            }
            androidx.compose.material3.Icon(Icons.Default.PlayArrow, contentDescription = "Putar dari antrean", tint = MaterialTheme.colorScheme.primary)
        }
    }
}

internal fun RexMusicSyncEngine.__splitImpl_addToQueue(track: RexTrack) {
        val local = RexOfflineManager.findEntry(track)
        if (local == null) { _state.value = _state.value.copy(error = "Lagu ini belum tersedia offline di HP kamu.", cacheStatus = "Pilih lagu yang sudah diunduh."); return }
        if (_state.value.hosting) {
            if (sharedQueue.none { it.id == local.track.id }) sharedQueue += local.track
            publishQueue()
            _state.value = _state.value.copy(cacheStatus = "Menyalin ${track.title} ke cache anggota…")
            scope.launch { distributeLocalTrack(local.track) }
        } else if (_state.value.connected) {
            runCatching { clientWriter?.let { send(it, JSONObject().put("type", "queue_add").put("track", trackJson(local.track))) } }
            scope.launch { sendBundle(_state.value.ip, local.track) }
            _state.value = _state.value.copy(cacheStatus = "Mengirim ${track.title} ke room…", error = null)
        }
    }

internal fun RexMusicSyncEngine.__splitImpl_playQueued(track: RexTrack) {
        if (_state.value.hosting) scope.launch {
            delay(500L)
            val offline = RexOfflineManager.findEntry(track)
            if (offline == null) { _state.value = _state.value.copy(error = "Transfer lagu belum selesai atau file belum tersedia di library offline."); return@launch }
            sharedQueue.removeAll { it.id == track.id }; publishQueue(); playTrack(offline.track)
        } else if (_state.value.connected) runCatching { clientWriter?.let { send(it, JSONObject().put("type", "play_request").put("trackId", track.id)) } }
    }

@Composable
internal fun __splitImpl_SyncEmptyQueueCard() {
    Text(
        "The shared queue is empty. Add a downloaded track from the offline library to get started.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 6.dp)
    )
}

internal fun RexMusicSyncEngine.__splitImpl_publishQueue() { val arr = org.json.JSONArray(); sharedQueue.forEach { arr.put(trackJson(it)) }; val msg = JSONObject().put("type", "queue").put("items", arr); peers.forEach { send(it.writer, msg) }; _state.value = _state.value.copy(queue = sharedQueue.toList()) }

internal fun RexMusicSyncEngine.__splitImpl_sendQueue(p: RexMusicSyncEngine.Peer) { val arr = org.json.JSONArray(); sharedQueue.forEach { arr.put(trackJson(it)) }; send(p.writer, JSONObject().put("type", "queue").put("items", arr)) }
