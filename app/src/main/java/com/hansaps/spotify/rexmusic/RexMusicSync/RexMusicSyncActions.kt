package com.hansaps.spotify.rexmusic

import android.os.SystemClock
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import java.net.InetAddress
import java.net.ServerSocket
import kotlinx.coroutines.*

internal fun RexMusicSyncEngine.__splitImpl_createRoom(room: String, password: String, ip: String) {
        closeAll()
        roomName = room.trim().ifBlank { "RexSyncRoom1" }
        roomStartedElapsedMs = SystemClock.elapsedRealtime()
        roomPassword = password
        val hostProfile = RexLocalProfileBridge.read()
        val hostName = hostProfile.name.trim().ifBlank { "HansAps User" }
        _state.value = _state.value.copy(hosting = true, connected = true, room = roomName,
            password = password, ip = ip, name = hostName, status = "Membuka room…", error = null,
            members = listOf(SyncMember("host", hostName, username = hostProfile.username, bio = hostProfile.bio,
                picturePath = hostProfile.picturePath.ifBlank { hostProfile.pictureVideo },
                bannerPath = hostProfile.bannerPath.ifBlank { hostProfile.bannerVideo })))
        roomJob = scope.launch {
            try {
                val ss = ServerSocket(SYNC_PORT, 32, InetAddress.getByName("0.0.0.0"))
                server = ss
                _state.value = _state.value.copy(status = "Room aktif · menunggu anggota")
                launch { monitorPeers() }
                launch { while (isActive && _state.value.hosting) { broadcastTick(); delay(900L) } }
                while (!ss.isClosed) {
                    val socket = ss.accept().apply { soTimeout = 0; tcpNoDelay = true; keepAlive = true }
                    launch { acceptPeer(socket) }
                }
            } catch (e: Exception) {
                if (_state.value.hosting) _state.value = _state.value.copy(status = "Server gagal: ${e.message}", error = e.message)
            }
        }
    }
