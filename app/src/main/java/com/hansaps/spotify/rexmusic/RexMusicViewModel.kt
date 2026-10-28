package com.hansaps.spotify.rexmusic

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.StateFlow

/**
 * ViewModel tipis. Semua logika player ada di [RexPlayerController] dan unduhan di
 * [RexOfflineManager] (hidup di level aplikasi) supaya musik + notifikasi tetap jalan walau layar ditutup.
 */
class RexMusicViewModel(app: Application) : AndroidViewModel(app) {

    init {
        RexPlayerController.init(app)
    }

    val state: StateFlow<RexMusicUiState> = RexPlayerController.state

    fun search(query: String) = RexPlayerController.search(query)
    fun clearSearch() = RexPlayerController.clearSearch()
    fun dismissError() = RexPlayerController.dismissError()
    fun consumeNotice() = RexPlayerController.consumeNotice()
    fun clearHistory() = RexPlayerController.clearHistory()

    fun refreshPlaylists() = RexPlayerController.refreshPlaylists()
    fun createPlaylist(name: String) = RexPlayerController.createPlaylist(name)
    fun addToPlaylist(name: String, track: RexTrack) = RexPlayerController.addToPlaylist(name, track)
    fun removeFromPlaylist(name: String, track: RexTrack) = RexPlayerController.removeFromPlaylist(name, track)
    fun deletePlaylist(name: String) = RexPlayerController.deletePlaylist(name)
    fun playPlaylist(name: String, startTrackId: String? = null) = RexPlayerController.playPlaylist(name, startTrackId)

    fun playFromMain(index: Int) = RexPlayerController.playFromMain(index)
    fun playPopular(index: Int) = RexPlayerController.playPopular(index)
    fun playFromSearch(index: Int) = RexPlayerController.playFromSearch(index)
    fun playFromHistory(index: Int) = RexPlayerController.playFromHistory(index)
    fun playMix() = RexPlayerController.playMix()

    fun addToQueue(track: RexTrack) = RexPlayerController.addToQueue(track)
    fun removeFromQueue(index: Int) = RexPlayerController.removeFromQueue(index)
    fun moveQueueToTop(index: Int) = RexPlayerController.moveQueueToTop(index)
    fun clearQueue() = RexPlayerController.clearQueue()
    fun playFromQueue(index: Int) = RexPlayerController.playFromQueue(index)

    fun setRepeat(times: Int) = RexPlayerController.setRepeat(times)
    fun toggleAutoplay() = RexPlayerController.setAutoplay(!state.value.autoplay)

    // offline
    fun setOfflineMode(on: Boolean) = RexPlayerController.setOfflineMode(on)
    fun playFromOffline(track: RexTrack) = RexPlayerController.playFromOffline(track)
    fun playOfflineAll(shuffle: Boolean) = RexPlayerController.playOfflineAll(shuffle)
    fun refreshOffline() = RexOfflineManager.refresh()
    fun download(track: RexTrack) = RexOfflineManager.download(track)
    fun cancelDownload(track: RexTrack) = RexOfflineManager.cancel(track)
    fun deleteDownload(track: RexTrack) = RexOfflineManager.delete(track)
    fun deleteAllDownloads() = RexOfflineManager.deleteAll()

    fun next() = RexPlayerController.next()
    fun prev(force: Boolean = false) = RexPlayerController.prev(force)
    fun togglePlay() = RexPlayerController.togglePlay()
    fun seekTo(ratio: Float) = RexPlayerController.seekTo(ratio)
    fun seekToPosition(positionMs: Long) = RexPlayerController.seekToPosition(positionMs)

    fun currentTrack(): RexTrack? = RexPlayerController.currentTrack()

    fun toggleLyrics() = RexPlayerController.toggleLyrics()
    fun closeLyrics() = RexPlayerController.closeLyrics()
}
