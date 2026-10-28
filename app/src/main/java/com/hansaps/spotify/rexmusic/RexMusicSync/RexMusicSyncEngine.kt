package com.hansaps.spotify.rexmusic

import android.os.SystemClock
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlin.math.max
import kotlinx.coroutines.*
import org.json.JSONObject

internal fun RexMusicSyncEngine.__splitImpl_sendState(p: RexMusicSyncEngine.Peer) {
        val track = hostTrack
        val now = SystemClock.elapsedRealtime()
        val startAt = if (hostPlaying) max(now + START_LEAD_MS, playAnchorWallMs) else 0L
        val currentPosition = if (hostPlaying) playAnchorPositionMs + max(0L, now - playAnchorWallMs) else RexPlayerController.state.value.positionMs
        // If playback is already underway, project to the future start; if the anchor is still ahead, keep its start position.
        val pos = if (hostPlaying) {
            if (now < playAnchorWallMs) playAnchorPositionMs else currentPosition + max(0L, startAt - now)
        } else currentPosition
        send(p.writer, JSONObject().put("type", "state").put("track", track?.let { JSONObject().put("id", it.id).put("title", it.title).put("artist", it.artist) })
            .put("position", pos).put("playing", hostPlaying).put("hostElapsed", now).put("roomElapsedMs", (now - roomStartedElapsedMs).coerceAtLeast(0L)).put("startAt", startAt))
    }
