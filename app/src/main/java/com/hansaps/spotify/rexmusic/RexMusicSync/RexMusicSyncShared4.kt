package com.hansaps.spotify.rexmusic

import android.os.SystemClock
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import java.io.BufferedWriter
import kotlinx.coroutines.*
import org.json.JSONObject

internal fun RexMusicSyncEngine.__splitImpl_broadcastMembers() {
    val profile = RexLocalProfileBridge.read()
    val hostAssets = RexLocalProfileBridge.encodeProfileAssets(profile)
    val hostPreview = RexLocalProfileBridge.saveSyncedProfileAssets("host-local", hostAssets.first, hostAssets.second)
    val hostPicture = hostPreview.first.ifBlank { profile.picturePath.ifBlank { profile.pictureVideo } }
    val hostBanner = hostPreview.second.ifBlank { profile.bannerPath.ifBlank { profile.bannerVideo } }
    val membersJson = org.json.JSONArray().put(
        JSONObject().put("id", "host").put("name", profile.name.ifBlank { _state.value.name })
            .put("username", profile.username).put("bio", profile.bio)
            .put("picturePath", hostPicture).put("bannerPath", hostBanner)
            .put("pictureData", hostAssets.first).put("bannerData", hostAssets.second)
    )
    peers.forEach { peer ->
        membersJson.put(
            JSONObject().put("id", peer.id).put("name", peer.name)
                .put("username", peer.username).put("bio", peer.bio)
                .put("pictureData", peer.pictureData).put("bannerData", peer.bannerData)
                .put("volume", peer.volume).put("deviceVolume", peer.deviceVolume)
        )
    }
    _state.value = _state.value.copy(
        members = listOf(
            SyncMember("host", profile.name.ifBlank { _state.value.name }, username = profile.username,
                bio = profile.bio, picturePath = hostPicture, bannerPath = hostBanner)
        ) + peers.map { peer ->
            SyncMember(peer.id, peer.name, ip = peer.ip, volume = peer.volume, deviceVolume = peer.deviceVolume,
                username = peer.username, bio = peer.bio, picturePath = peer.picturePath, bannerPath = peer.bannerPath)
        },
        volumes = peers.associate { it.id to it.volume }
    )
    val msg = JSONObject().put("type", "members").put("members", membersJson)
    peers.forEach { send(it.writer, msg) }
}

internal fun RexMusicSyncEngine.__splitImpl_closeAll() {
        hostPreparing = false; hostPlaying = false; roomStartedElapsedMs = 0L; clockOffsetMs = 0L; lastClockRttMs = -1L
        heartbeat?.cancel(); playbackJob?.cancel(); roomJob?.cancel(); roomJob = null
        sharedQueue.clear()
        peers.forEach { runCatching { it.socket.close() } }; peers.clear()
        runCatching { client?.close() }; client = null; clientWriter = null; clientPeerId = ""
        runCatching { server?.close() }; server = null
        if (_state.value.hosting) stopLocalPlayback()
        _state.value = _state.value.copy(queue = emptyList())
    }

internal fun RexMusicSyncEngine.__splitImpl_stopAll(reason: String = "Host menghentikan pemutaran") {
        hostPreparing = false; hostPlaying = false; playbackJob?.cancel()
        scope.launch { withContextMain { RexPlayerController.pause() } }
        _state.value = _state.value.copy(playing = false, positionMs = RexPlayerController.state.value.positionMs, status = reason)
        peers.forEach { runCatching { send(it.writer, JSONObject().put("type", "stop").put("reason", reason)) } }
    }

internal fun RexMusicSyncEngine.__splitImpl_send(w: BufferedWriter, m: JSONObject) {
        // Socket writes may fail when a phone disconnects mid-gesture. Never let an IOException
        // escape a Compose callback (especially the volume slider) and crash the whole screen.
        runCatching { synchronized(w) { w.write(m.toString()); w.newLine(); w.flush() } }
    }

internal suspend fun RexMusicSyncEngine.__splitImpl_monitorPeers() {
        while (_state.value.hosting) {
            val now = SystemClock.elapsedRealtime()
            peers.filter { now - it.lastSeen > CLIENT_TIMEOUT_MS }.forEach { p -> runCatching { p.socket.close() } }
            delay(HEARTBEAT_MS)
        }
    }

internal fun __splitImpl_formatRoomTime(ms: Long): String {
    val total = (ms.coerceAtLeast(0L) / 1000L)
    return String.format(java.util.Locale.ROOT, "%02d:%02d:%02d", total / 3600L, (total / 60L) % 60L, total % 60L)
}

internal fun RexMusicSyncEngine.__splitImpl_disconnect() { closeAll(); _state.value = _state.value.copy(hosting = false, connected = false, playing = false, members = emptyList(), status = "Terputus", error = null) }
