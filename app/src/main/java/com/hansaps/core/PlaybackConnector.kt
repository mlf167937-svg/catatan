package com.hansaps.core

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

object PlaybackConnector {
    private val mutableController = MutableStateFlow<MediaController?>(null)
    val controller: StateFlow<MediaController?> = mutableController
    private var controllerFuture: ListenableFuture<MediaController>? = null
    @Volatile private var pendingItems: List<MediaItem>? = null
    @Volatile private var pendingStartIndex: Int = 0
    private val pendingQueue = mutableListOf<MediaItem>()

    @Synchronized
    fun connect(context: Context) {
        if (mutableController.value != null || controllerFuture != null) return
        val appContext = context.applicationContext
        val token = SessionToken(appContext, ComponentName(appContext, HansMediaService::class.java))
        val future = MediaController.Builder(appContext, token).buildAsync()
        controllerFuture = future
        future.addListener({
            runCatching {
                val built = future.get()
                mutableController.value = built
                val queued = pendingItems
                if (!queued.isNullOrEmpty()) {
                    built.setMediaItems(queued, pendingStartIndex, 0L)
                    built.prepare()
                    built.play()
                    pendingItems = null
                }
                synchronized(pendingQueue) {
                    if (pendingQueue.isNotEmpty()) {
                        if (built.mediaItemCount == 0) built.setMediaItems(pendingQueue.toList())
                        else built.addMediaItems(pendingQueue.toList())
                        if (built.playbackState == androidx.media3.common.Player.STATE_IDLE) built.prepare()
                        pendingQueue.clear()
                    }
                }
            }.onFailure { controllerFuture = null }
        }, appContext.mainExecutor)
    }

    fun play(context: Context, track: LocalTrack) = playQueue(context, listOf(track), 0)

    fun enqueue(context: Context, track: LocalTrack) {
        val item = track.toMediaItem()
        connect(context)
        val activeController = mutableController.value
        if (activeController != null) {
            if (activeController.mediaItemCount == 0) activeController.setMediaItem(item) else activeController.addMediaItem(item)
            if (activeController.playbackState == androidx.media3.common.Player.STATE_IDLE) activeController.prepare()
        } else {
            synchronized(pendingQueue) { pendingQueue.add(item) }
        }
    }

    fun playQueue(context: Context, tracks: List<LocalTrack>, startIndex: Int = 0) {
        if (tracks.isEmpty()) return
        val items = tracks.map { it.toMediaItem() }
        connect(context)
        val activeController = mutableController.value
        if (activeController != null) {
            activeController.setMediaItems(items, startIndex.coerceIn(items.indices), 0L)
            activeController.prepare()
            activeController.play()
        } else {
            pendingItems = items
            pendingStartIndex = startIndex.coerceIn(items.indices)
        }
    }

    fun playPause(context: Context) {
        connect(context)
        mutableController.value?.let { if (it.isPlaying) it.pause() else it.play() }
    }

    fun skipNext(context: Context) {
        connect(context)
        mutableController.value?.let { if (it.hasNextMediaItem()) it.seekToNextMediaItem() }
    }

    fun skipPrevious(context: Context) {
        connect(context)
        mutableController.value?.let { if (it.hasPreviousMediaItem()) it.seekToPreviousMediaItem() else it.seekTo(0L) }
    }

    fun seekTo(positionMs: Long) { mutableController.value?.seekTo(positionMs.coerceAtLeast(0L)) }

    fun currentArtworkUri(): Uri? = null

    private fun LocalTrack.toMediaItem(): MediaItem = MediaItem.Builder()
        .setMediaId(id)
        .setUri(uri)
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(title)
                .setArtist(artist)
                .setMediaType(MediaMetadata.MEDIA_TYPE_MUSIC)
                .setArtworkUri(artworkUri)
                .build()
        ).build()
}
