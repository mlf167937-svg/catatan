package com.hansaps.spotify.rexmusic

import android.content.Context
import android.os.Environment
import android.os.SystemClock
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import kotlin.math.max
import kotlinx.coroutines.*
import org.json.JSONObject

internal fun RexMusicSyncEngine.__splitImpl_receiveBundle(socket: Socket) {
        socket.use { s ->
            try {
                s.soTimeout = 20000
                val input = DataInputStream(s.getInputStream())
                val track = trackFromJson(JSONObject(input.readUTF())) ?: return
                val size = input.readLong(); if (size <= 0L || size > 600L * 1024L * 1024L) return
                val tempZip = File.createTempFile("rexsync-in-", ".zip", appContextRef?.cacheDir)
                try { tempZip.outputStream().buffered().use { out ->
                    var remaining = size; val buffer = ByteArray(64 * 1024)
                    while (remaining > 0L) { val n = input.read(buffer, 0, minOf(buffer.size.toLong(), remaining).toInt()); if (n < 0) error("Transfer terputus"); out.write(buffer, 0, n); remaining -= n }
                }
                val base = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "HansAps/.cache/spotifySync/${offlineKey(track)}")
                base.mkdirs()
                ZipInputStream(tempZip.inputStream().buffered()).use { zip -> var e = zip.nextEntry; while (e != null) {
                    val name = File(e.name).name
                    if (name in setOf("music.mp3", "meta.json", "lyrics.json", "thumbnail.jpg", "thumbnail.png", "thumbnail.jpeg", "thumbnail.webp")) File(base, name).outputStream().use { zip.copyTo(it) }
                    zip.closeEntry(); e = zip.nextEntry
                } }
                val audio = File(base, "music.mp3"); if (!audio.isFile || audio.length() == 0L) error("Cache audio kosong")
                File(base, "meta.json").writeText(trackJson(track).toString())
                val imported = RexOfflineManager.importSyncTrack(track, base)
                _state.value = _state.value.copy(cacheStatus = if (imported) "Cache siap: ${track.title}" else "Cache tersimpan; beri izin penyimpanan agar RexMusic bisa memutarnya", error = null)
                if (_state.value.hosting) {
                    if (sharedQueue.none { it.id == track.id }) sharedQueue += track
                    publishQueue()
                    val senderIp = s.inetAddress?.hostAddress
                    peers.filter { it.ip != senderIp }.forEach { peer -> scope.launch { delay(900L); sendBundle(peer.ip, track) } }
                }
                } finally { tempZip.delete() }
            } catch (e: Exception) { _state.value = _state.value.copy(cacheStatus = "Transfer gagal: ${e.message}") }
        }
    }

internal fun RexMusicSyncEngine.__splitImpl_broadcastTick() {
        val now = SystemClock.elapsedRealtime()
        val roomElapsed = (now - roomStartedElapsedMs).coerceAtLeast(0L)
        _state.value = _state.value.copy(roomElapsedMs = roomElapsed)
        val roomMessage = JSONObject().put("type", "room_time").put("roomElapsedMs", roomElapsed)
        peers.filter { it.clockSynced }.forEach { runCatching { send(it.writer, roomMessage) } }
        if (hostPreparing) return
        val track = hostTrack ?: return
        val current = RexPlayerController.state.value
        // RexPlayerController has its own autoplay. If it advances away from the shared track,
        // hand control back to the room queue so every member gets the same next track.
        if (hostPlaying && SystemClock.elapsedRealtime() > playAnchorWallMs + 2500L &&
            current.isPlaying && current.nowPlaying?.id != null && current.nowPlaying?.id != track.id) {
            val next = sharedQueue.firstOrNull()
            if (next != null) { playQueued(next); return }
            stopAll("Lagu selesai · antrean bersama kosong")
            return
        }
        val position = if (hostPlaying) {
            if (current.nowPlaying?.id == track.id && current.positionMs > 0L) current.positionMs
            else playAnchorPositionMs + max(0L, SystemClock.elapsedRealtime() - playAnchorWallMs)
        } else current.positionMs
        val hostReference = if (hostPlaying) max(now, playAnchorWallMs) else now
        val msg = JSONObject().put("type", "sync").put("trackId", track.id).put("title", track.title)
            .put("position", position).put("playing", hostPlaying).put("hostElapsed", hostReference).put("roomElapsedMs", (now - roomStartedElapsedMs).coerceAtLeast(0L))
        peers.filter { it.clockSynced }.forEach { send(it.writer, msg) }
        _state.value = _state.value.copy(positionMs = position, playing = hostPlaying, roomElapsedMs = (SystemClock.elapsedRealtime() - roomStartedElapsedMs).coerceAtLeast(0L))
    }

internal suspend fun RexMusicSyncEngine.__splitImpl_sendBundle(host: String, track: RexTrack): Boolean = withContext(Dispatchers.IO) {
        try {
            val entry = RexOfflineManager.state.value.entries.firstOrNull { it.track.id == track.id } ?: return@withContext false
            val root = RexOfflineManager.root
            val dir = File(root, entry.key)
            val files = listOf("music.mp3", "meta.json", "lyrics.json", "thumbnail.jpg", "thumbnail.png", "thumbnail.jpeg", "thumbnail.webp").map { File(dir, it) }.filter { it.isFile }
            if (files.none { it.name == "music.mp3" }) return@withContext false
            val temp = File.createTempFile("rexsync-", ".zip", appContextRef?.cacheDir)
            try {
                ZipOutputStream(temp.outputStream().buffered()).use { zip -> files.forEach { f -> zip.putNextEntry(ZipEntry(f.name)); f.inputStream().buffered().use { it.copyTo(zip) }; zip.closeEntry() } }
                Socket().use { socket -> socket.connect(java.net.InetSocketAddress(host, TRANSFER_PORT), 4000); socket.soTimeout = 30000
                    val out = DataOutputStream(socket.getOutputStream().buffered())
                    out.writeUTF(trackJson(track).toString()); out.writeLong(temp.length()); temp.inputStream().buffered().use { it.copyTo(out) }; out.flush()
                }
            } finally { temp.delete() }
            true
        } catch (_: Exception) { false }
    }

internal fun RexMusicSyncEngine.__splitImpl_resumeRoomAtCurrentPosition() {
        val track = hostTrack ?: return
        playAnchorPositionMs = RexPlayerController.state.value.positionMs.coerceAtLeast(playAnchorPositionMs)
        playAnchorWallMs = SystemClock.elapsedRealtime() + START_LEAD_MS
        hostPlaying = true
        _state.value = _state.value.copy(playing = true, positionMs = playAnchorPositionMs, status = "Anggota kembali · sinkronisasi ulang", error = null)
        broadcastState()
        playbackJob?.cancel()
        playbackJob = scope.launch {
            delay(max(0L, playAnchorWallMs - SystemClock.elapsedRealtime()))
            withContext(Dispatchers.Main) {
                if (RexPlayerController.state.value.nowPlaying?.id != track.id) RexPlayerController.playFromOffline(track)
                delay(150L)
                RexPlayerController.seekToPositionImmediate(playAnchorPositionMs)
                RexPlayerController.startSyncPlayback(playAnchorPositionMs)
            }
        }
    }

internal fun RexMusicSyncEngine.__splitImpl_detectIp(context: Context): String = runCatching {
        // Works for Wi-Fi client and many Android hotspot interfaces; avoid VPN/tunnel interfaces.
        val interfaces = java.net.NetworkInterface.getNetworkInterfaces()?.toList().orEmpty()
        val candidates = interfaces.filter { ni ->
            ni.isUp && !ni.isLoopback && !ni.name.lowercase().matches(Regex(".*(tun|tap|wg|warp|vpn|dummy).*"))
        }.flatMap { ni -> ni.inetAddresses.toList().filterIsInstance<java.net.Inet4Address>()
            .filter { !it.isLoopbackAddress && !it.isLinkLocalAddress }
            .map { ni.name.lowercase() to it.hostAddress.orEmpty() } }
        candidates.firstOrNull { (name, ip) -> name.matches(Regex(".*(wlan|wifi|ap|swlan|p2p|eth).*")) && ip.isNotBlank() }?.second
            ?: candidates.firstOrNull { it.second.isNotBlank() }?.second.orEmpty()
    }.getOrDefault("")

internal fun RexMusicSyncEngine.__splitImpl_bindContext(context: Context) {
        appContextRef = context.applicationContext
        RexMusicService.start(context.applicationContext); RexOfflineManager.init(context.applicationContext); RexPlayerController.init(context.applicationContext)
        if (transferJob?.isActive != true) transferJob = scope.launch {
            try {
                val ss = ServerSocket(TRANSFER_PORT, 16, InetAddress.getByName("0.0.0.0")); transferServer = ss
                while (isActive && !ss.isClosed) {
                    val socket = ss.accept()
                    launch(Dispatchers.IO) { receiveBundle(socket) }
                }
            } catch (e: Exception) { if (transferServer != null) _state.value = _state.value.copy(cacheStatus = "Transfer: ${e.message}") }
        }
    }
