package com.hansaps.spotify.rexmusic

import android.content.Context
import android.media.AudioManager
import android.os.SystemClock
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.Socket
import kotlinx.coroutines.*
import org.json.JSONObject

internal suspend fun RexMusicSyncEngine.__splitImpl_acceptPeer(socket: Socket) {
        val p = runCatching { RexMusicSyncEngine.Peer(socket, BufferedReader(InputStreamReader(socket.getInputStream(), Charsets.UTF_8)),
            BufferedWriter(OutputStreamWriter(socket.getOutputStream(), Charsets.UTF_8))) }.getOrNull() ?: return
        try {
            val first = withContext(Dispatchers.IO) { p.reader.readLine() } ?: return
            val join = JSONObject(first)
            if (join.optString("type") != "join" || join.optString("room") != roomName || join.optString("password") != roomPassword) {
                send(p.writer, JSONObject().put("type", "reject").put("reason", "Nama room atau password salah")); return
            }
            p.id = join.optString("id").ifBlank { java.util.UUID.randomUUID().toString() }
            p.name = join.optString("name").trim().ifBlank { "User" }
            p.username = join.optString("username").trim()
            p.bio = join.optString("bio").trim()
            val savedAssets = RexLocalProfileBridge.saveSyncedProfileAssets(
                p.id, join.optString("pictureData"), join.optString("bannerData")
            )
            p.picturePath = savedAssets.first
            p.bannerPath = savedAssets.second
            p.pictureData = join.optString("pictureData")
            p.bannerData = join.optString("bannerData")
            p.lastSeen = SystemClock.elapsedRealtime()
            peers.add(p)
            val localProfile = RexLocalProfileBridge.read()
            val localAssets = RexLocalProfileBridge.encodeProfileAssets(localProfile)
            send(p.writer, JSONObject().put("type", "welcome").put("id", p.id).put("room", roomName)
                .put("name", localProfile.name).put("username", localProfile.username).put("bio", localProfile.bio)
                .put("pictureData", localAssets.first).put("bannerData", localAssets.second))
            sendQueue(p)
            broadcastMembers()
            while (true) {
                val line = withContext(Dispatchers.IO) { p.reader.readLine() } ?: break
                val msg = JSONObject(line)
                when (msg.optString("type")) {
                    "ping" -> {
                        p.lastSeen = SystemClock.elapsedRealtime()
                        p.deviceVolume = msg.optDouble("deviceVolume", -1.0).toFloat().coerceIn(-1f, 1f)
                        _state.value = _state.value.copy(members = _state.value.members.map { if (it.id == p.id) it.copy(deviceVolume = p.deviceVolume) else it })
                        val hostSend = SystemClock.elapsedRealtime()
                        val activeTrack = hostTrack
                        val needsBundle = activeTrack != null && p.sentTrackId != activeTrack.id
                        p.clockSynced = !needsBundle
                        send(p.writer, JSONObject().put("type", "pong")
                            .put("clientElapsed", msg.optLong("clientElapsed", 0L))
                            .put("hostReceiveElapsed", hostSend)
                            .put("hostSendElapsed", SystemClock.elapsedRealtime()))
                        if (needsBundle && activeTrack != null) {
                            scope.launch {
                                val sent = sendBundle(p.ip, activeTrack)
                                if (sent) p.sentTrackId = activeTrack.id
                                else _state.value = _state.value.copy(cacheStatus = "Gagal mengirim ${activeTrack.title} ke ${p.name}")
                                p.clockSynced = true
                                sendState(p)
                            }
                        } else if (activeTrack != null) sendState(p)
                    }
                    "ready" -> { p.lastSeen = SystemClock.elapsedRealtime() }
                    "request_track" -> { p.lastSeen = SystemClock.elapsedRealtime(); if (hostTrack != null) sendState(p) }
                    "request_bundle" -> {
                        p.lastSeen = SystemClock.elapsedRealtime()
                        val id = msg.optString("trackId")
                        val entry = RexOfflineManager.state.value.entries.firstOrNull { it.track.id == id }
                        if (entry != null) scope.launch {
                            val sent = sendBundle(p.ip, entry.track)
                            if (sent) p.sentTrackId = entry.track.id
                            else _state.value = _state.value.copy(cacheStatus = "Gagal mengirim ${entry.track.title} ke ${p.name}")
                        }
                    }
                    "queue_add" -> {
                        p.lastSeen = SystemClock.elapsedRealtime()
                        val t = trackFromJson(msg.optJSONObject("track"))
                        if (t != null && sharedQueue.none { it.id == t.id }) { sharedQueue += t; publishQueue() }
                        _state.value = _state.value.copy(cacheStatus = "Menerima ${t?.title ?: "lagu"} dari ${p.name}…")
                    }
                    "queue_request" -> { p.lastSeen = SystemClock.elapsedRealtime(); sendQueue(p) }
                    "volume" -> { p.lastSeen = SystemClock.elapsedRealtime(); p.volume = msg.optDouble("value", 1.0).toFloat().coerceIn(0f, 1f) }
                    "play_request" -> { p.lastSeen = SystemClock.elapsedRealtime(); val id = msg.optString("trackId"); sharedQueue.firstOrNull { it.id == id }?.let { queued -> val local = RexOfflineManager.state.value.entries.firstOrNull { it.track.id == id }; if (local != null) { sharedQueue.removeAll { it.id == id }; publishQueue(); playTrack(local.track) } } }
                }
            }
        } catch (_: Exception) {
        } finally {
            peers.remove(p); runCatching { p.socket.close() }
            if (p.id.isNotBlank() && _state.value.hosting) {
                // Keep playback running for remaining members. A returning member receives the latest state.
                playAnchorPositionMs = RexPlayerController.state.value.positionMs.coerceAtLeast(playAnchorPositionMs)
                resumeWhenMemberReturns = hostTrack != null && hostPlaying
                broadcastMembers()
            }
        }
    }

internal fun RexMusicSyncEngine.__splitImpl_joinRoom(room: String, ip: String, password: String) {
        closeAll()
        val profileName = RexLocalProfileBridge.read().name.trim().ifBlank { "HansAps User" }
        _state.value = _state.value.copy(hosting = false, connected = false, room = room, name = profileName,
            ip = ip, password = password, status = "Menghubungkan…", error = null)
        scope.launch {
            try {
                val s = Socket()
                s.connect(java.net.InetSocketAddress(ip.trim(), SYNC_PORT), 3500)
                s.tcpNoDelay = true; s.keepAlive = true
                val reader = BufferedReader(InputStreamReader(s.getInputStream(), Charsets.UTF_8))
                val writer = BufferedWriter(OutputStreamWriter(s.getOutputStream(), Charsets.UTF_8))
                client = s; clientWriter = writer
                val id = java.util.UUID.randomUUID().toString()
                clientPeerId = id
                val profile = RexLocalProfileBridge.read()
                val profileAssets = RexLocalProfileBridge.encodeProfileAssets(profile)
                send(writer, JSONObject().put("type", "join").put("id", id).put("room", room.trim())
                    .put("name", profileName).put("username", profile.username).put("bio", profile.bio)
                    .put("picturePath", profile.picturePath).put("bannerPath", profile.bannerPath)
                    .put("pictureData", profileAssets.first).put("bannerData", profileAssets.second)
                    .put("password", password))
                val first = reader.readLine() ?: error("Host menutup koneksi")
                val welcome = JSONObject(first)
                if (welcome.optString("type") != "welcome") error(welcome.optString("reason", "Ditolak host"))
                RexLocalProfileBridge.saveSyncedProfileAssets(
                    "host", welcome.optString("pictureData"), welcome.optString("bannerData")
                )
                _state.value = _state.value.copy(connected = true, status = "Terhubung ke ${room.trim()}", error = null)
                heartbeat?.cancel()
                heartbeat = launch { while (isActive && client?.isClosed == false) { runCatching {
                        val am = appContextRef?.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
                        val maxVolume = am?.getStreamMaxVolume(AudioManager.STREAM_MUSIC) ?: 0
                        val currentVolume = am?.getStreamVolume(AudioManager.STREAM_MUSIC) ?: 0
                        val actual = if (maxVolume > 0) currentVolume.toFloat() / maxVolume else -1f
                        send(writer, JSONObject().put("type", "ping").put("clientElapsed", SystemClock.elapsedRealtime()).put("deviceVolume", actual))
                    }; delay(HEARTBEAT_MS) } }
                while (true) {
                    val line = reader.readLine() ?: break
                    handleClientMessage(JSONObject(line))
                }
                if (_state.value.connected) {
                    _state.value = _state.value.copy(connected = false, playing = false, status = "Koneksi putus", error = "Koneksi ke host terputus")
                    stopLocalPlayback()
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(connected = false, status = "Gagal terhubung", error = e.message ?: "Periksa IP dan password")
            }
        }
    }
