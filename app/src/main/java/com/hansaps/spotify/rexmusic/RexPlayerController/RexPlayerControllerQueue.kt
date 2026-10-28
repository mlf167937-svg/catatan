package com.hansaps.spotify.rexmusic

import kotlinx.coroutines.flow.update

internal fun RexPlayerController.__splitImpl_playPlaylist(name: String, startTrackId: String? = null) {
        val playlist = playlistStore.load(name) ?: return
        val tracks = if (offlineMode()) playlist.tracks.filter { RexOfflineManager.isDownloaded(it) } else playlist.tracks
        if (tracks.isEmpty()) {
            postNotice(if (offlineMode()) "Playlist ini belum punya lagu offline" else "Playlist masih kosong")
            return
        }
        // When a track is tapped inside playlist details, start at that track while
        // keeping the playlist as the active playback context for Next / Previous.
        val startIndex = if (startTrackId == null) 0 else tracks.indexOfFirst { it.id == startTrackId }
        if (startIndex < 0) {
            postNotice(if (offlineMode()) "Lagu ini belum tersedia offline" else "Lagu tidak ditemukan di playlist")
            return
        }
        startQueue(tracks, startIndex, follow = true)
    }

internal fun RexPlayerController.__splitImpl_publishQueueInfo() {
        val now = _state.value.nowPlaying
        val hist = history.asReversed().filter { now == null || !sameSong(it, now) }
        val contextNext = !autoplay || followContext
        val up = userQueue.firstOrNull()
            ?: if (contextNext && queue.size > 1) queue[(queueIndex + 1) % queue.size] else null
        _state.update {
            it.copy(
                history = hist,
                upNext = up,
                previous = previousTarget(now),
                userQueue = userQueue.toList(),
                autoplay = autoplay
            )
        }
    }

internal fun RexPlayerController.__splitImpl_addToQueue(track: RexTrack) {
        val entry = track.copy(audioUrl = "")
        when {
            userQueue.any { sameSong(it, entry) } -> postNotice("Sudah ada di antrian")
            userQueue.size >= MAX_QUEUE -> postNotice("Antrian penuh (maks $MAX_QUEUE lagu)")
            else -> {
                userQueue.add(entry)
                persistQueue()
                publishQueueInfo()
                postNotice("Ditambahkan ke antrian")
            }
        }
    }

/** Pastikan target ada di antrian konteks & queueIndex menunjuk ke sana. */
    internal fun RexPlayerController.__splitImpl_syncQueueTo(target: RexTrack) {
        val idx = queue.indexOfFirst { sameSong(it, target) }
        if (idx >= 0) {
            queueIndex = idx
            return
        }
        val at = queueIndex.coerceIn(0, queue.size)
        queue = queue.toMutableList().apply { add(at, target) }
        queueIndex = at
    }

internal fun RexPlayerController.__splitImpl_createPlaylist(name: String) {
        if (playlistStore.create(name) == null) {
            postNotice("Nama playlist tidak valid atau sudah ada")
            return
        }
        refreshPlaylists()
        postNotice("Playlist dibuat: ${name.trim()}")
    }

internal fun RexPlayerController.__splitImpl_addToPlaylist(name: String, track: RexTrack) {
        if (playlistStore.addTrack(name, track)) {
            refreshPlaylists()
            postNotice("Ditambahkan ke $name")
        } else {
            postNotice("Playlist tidak bisa diperbarui")
        }
    }

internal fun RexPlayerController.__splitImpl_moveQueueToTop(index: Int) {
        if (index !in 1 until userQueue.size) return
        val track = userQueue.removeAt(index)
        userQueue.add(0, track)
        persistQueue()
        publishQueueInfo()
    }

/** Putar lagu dari antrian sekarang juga (lagunya keluar dari antrian). */
    internal fun RexPlayerController.__splitImpl_playFromQueue(index: Int) {
        if (index !in userQueue.indices) return
        val track = userQueue.removeAt(index)
        persistQueue()
        cancelPending()
        beginTrack(track)
    }

internal fun RexPlayerController.__splitImpl_clearQueue() {
        if (userQueue.isEmpty()) return
        userQueue.clear()
        persistQueue()
        publishQueueInfo()
        postNotice("Antrian dikosongkan")
    }

internal fun RexPlayerController.__splitImpl_removeFromPlaylist(name: String, track: RexTrack) {
        if (playlistStore.removeTrack(name, track.id)) {
            refreshPlaylists()
            postNotice("Dihapus dari $name")
        }
    }

internal fun RexPlayerController.__splitImpl_startQueue(list: List<RexTrack>, index: Int, follow: Boolean = false) {
        cancelPending()
        queue = list
        queueIndex = index
        followContext = follow
        beginTrack(list[index])
    }

internal fun RexPlayerController.__splitImpl_deletePlaylist(name: String) {
        if (playlistStore.delete(name)) {
            refreshPlaylists()
            postNotice("Playlist dihapus: $name")
        }
    }

internal fun RexPlayerController.__splitImpl_removeFromQueue(index: Int) {
        if (index !in userQueue.indices) return
        userQueue.removeAt(index)
        persistQueue()
        publishQueueInfo()
    }

internal fun RexPlayerController.__splitImpl_refreshPlaylists() {
        if (!initialized) return
        _state.update { it.copy(playlists = playlistStore.list()) }
    }
