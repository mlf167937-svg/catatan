package com.hansaps.spotify.rexmusic

import java.io.File

/** Import lagu yang diterima lewat RexMusicSync ke library RexMusic, tanpa menghapus cache sync. */
    internal fun RexOfflineManager.__splitImpl_importSyncTrack(track: RexTrack, sourceDir: File): Boolean {
        if (!appContextInitialized || !hasAccess(appContext)) return false
        return runCatching {
            val audio = File(sourceDir, "music.mp3")
            require(audio.isFile && audio.length() > 0L) { "File audio tidak lengkap" }
            val key = offlineKey(track)
            val dest = File(root, key)
            dest.mkdirs()
            audio.copyTo(File(dest, AUDIO_NAME), overwrite = true)
            listOf("meta.json", "lyrics.json", "thumbnail.jpg", "thumbnail.png", "thumbnail.jpeg", "thumbnail.webp").forEach { name ->
                val src = File(sourceDir, name)
                if (src.isFile && src.length() > 0L) src.copyTo(File(dest, name), overwrite = true)
            }
            writeMeta(dest, track)
            refresh()
            true
        }.getOrDefault(false)
    }
