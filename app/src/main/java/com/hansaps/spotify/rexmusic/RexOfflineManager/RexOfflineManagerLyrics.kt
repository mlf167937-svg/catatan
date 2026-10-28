package com.hansaps.spotify.rexmusic

import java.io.File
import org.json.JSONObject

internal fun RexOfflineManager.__splitImpl_writeLyrics(dir: File, lyrics: Lyrics?, unavailable: Boolean) {
        runCatching {
            val synced = org.json.JSONArray().apply {
                lyrics?.synced.orEmpty().forEach { line ->
                    put(
                        org.json.JSONObject()
                            .put("timeMs", line.timeMs)
                            .put("timestamp", line.sourceTimestamp.orEmpty())
                            .put("text", line.text)
                    )
                }
            }
            File(dir, LYRICS_NAME).writeText(
                JSONObject()
                    .put("version", 2)
                    .put("available", lyrics != null && !lyrics.isEmpty)
                    .put("offlineOnly", true)
                    .put("unavailable", unavailable)
                    .put("duration", lyrics?.durationSeconds ?: 0)
                    .put("plainLyrics", lyrics?.plain.orEmpty())
                    .put("syncedLyrics", synced)
                    .toString(2)
            )
        }
    }

internal fun RexOfflineManager.__splitImpl_readOfflineLyrics(track: RexTrack): Lyrics? = runCatching {
        val file = lyricsFile(track) ?: return null
        val o = JSONObject(file.readText())
        val plain = o.optString("plainLyrics")
        val arr = o.optJSONArray("syncedLyrics") ?: org.json.JSONArray()
        val synced = (0 until arr.length()).mapNotNull { i ->
            val line = arr.optJSONObject(i) ?: return@mapNotNull null
            val text = line.optString("text").trim()
            if (text.isBlank()) return@mapNotNull null
            LyricLine(
                timeMs = line.optLong("timeMs").coerceAtLeast(0L),
                text = text,
                sourceTimestamp = line.optString("timestamp").ifBlank { null }
            )
        }.sortedBy { it.timeMs }
        Lyrics(
            plain = plain,
            synced = synced,
            durationSeconds = o.optInt("duration", 0).coerceAtLeast(0)
        )
    }.getOrNull()
