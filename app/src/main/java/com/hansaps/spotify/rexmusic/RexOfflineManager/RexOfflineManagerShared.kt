package com.hansaps.spotify.rexmusic

import android.net.Uri
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

internal fun RexOfflineManager.__splitImpl_scan(): List<OfflineEntry> {
        val dirs = root.listFiles { f -> f.isDirectory } ?: return emptyList()
        return dirs.mapNotNull { dir ->
            val mp3 = File(dir, AUDIO_NAME)
            if (!mp3.isFile || mp3.length() <= 0L) return@mapNotNull null
            val thumb = findThumbnail(dir)
            val meta = readMeta(dir)
            val track = RexTrack(
                id = meta?.optString("id").orEmpty().ifBlank { "off-${dir.name}" },
                title = meta?.optString("title").orEmpty().ifBlank { dir.name.replace('-', ' ') },
                artist = meta?.optString("artist").orEmpty().ifBlank { "Unknown" },
                album = meta?.optString("album").orEmpty().ifBlank { "Offline" },
                cover = thumb?.let { Uri.fromFile(it).toString() }.orEmpty(),
                spotifyUrl = meta?.optString("spotifyUrl").orEmpty(),
                durationText = meta?.optString("durationText").orEmpty()
            )
            OfflineEntry(
                key = dir.name,
                track = track,
                sizeBytes = dir.walkTopDown().filter { it.isFile }.sumOf { it.length() },
                savedAt = mp3.lastModified()
            )
        }.sortedByDescending { it.savedAt }
    }

/** Buka koneksi dan ikuti redirect sendiri (termasuk http -> https). */
    internal fun RexOfflineManager.__splitImpl_openConnection(url: String): HttpURLConnection {
        var current = url
        for (i in 0 until 6) {
            val conn = URL(current).openConnection() as HttpURLConnection
            conn.connectTimeout = 15_000
            conn.readTimeout = 20_000
            conn.instanceFollowRedirects = false
            conn.setRequestProperty("User-Agent", USER_AGENT)
            val code = conn.responseCode
            if (code in 300..399) {
                val location = conn.getHeaderField("Location")
                conn.disconnect()
                if (location.isNullOrBlank()) throw IOException("redirect tanpa tujuan")
                current = URL(URL(current), location).toString()
                continue
            }
            return conn
        }
        throw IOException("terlalu banyak redirect")
    }
