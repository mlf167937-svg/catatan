package com.hansaps.spotify.rexmusic

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.media.AudioManager
import android.media.MediaMetadata
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.os.Build
import android.os.IBinder
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import com.hansaps.spotify.rexmusic.widget.RexMusicWidget

/**
 * Foreground service: notifikasi media (prev / play-pause / next / tutup) + MediaSession.
 * Sengaja tidak memakai R.layout/R.id agar tidak bergantung pada resource custom.
 */
class RexMusicService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private lateinit var session: MediaSession
    private lateinit var nm: NotificationManager

    private var artJob: Job? = null
    private var coverUrl = ""
    private var coverBitmap: Bitmap? = null
    private var foreground = false
    private var closed = false

    private val noisyReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == AudioManager.ACTION_AUDIO_BECOMING_NOISY) {
                RexPlayerController.pause()
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        RexPlayerController.init(applicationContext)
        nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        createChannel()

        session = MediaSession(this, "HansAps Music").apply {
            setCallback(object : MediaSession.Callback() {
                override fun onPlay() = RexPlayerController.play()
                override fun onPause() = RexPlayerController.pause()
                override fun onSkipToNext() = RexPlayerController.next()
                override fun onSkipToPrevious() = RexPlayerController.prev(force = true)
                override fun onStop() = shutdown()
                override fun onSeekTo(pos: Long) {
                    val d = RexPlayerController.state.value.durationMs
                    if (d > 0) RexPlayerController.seekTo(pos.toFloat() / d)
                }
            })
            isActive = true
        }

        val filter = IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY)
        if (Build.VERSION.SDK_INT >= 33) {
            registerReceiver(noisyReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(noisyReceiver, filter)
        }

        startForegroundCompat(buildNotification(RexPlayerController.state.value))
        observeState()
        observeWidgetState()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_TOGGLE -> RexPlayerController.togglePlay()
            ACTION_NEXT -> RexPlayerController.next()
            ACTION_PREV -> RexPlayerController.prev(force = true)
            ACTION_CLOSE -> shutdown()
            else -> if (!closed) {
                runCatching { startForegroundCompat(buildNotification(RexPlayerController.state.value)) }
            }
        }
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        closed = true
        artJob?.cancel()
        scope.cancel()
        runCatching { unregisterReceiver(noisyReceiver) }
        runCatching { session.isActive = false }
        runCatching { session.release() }
        super.onDestroy()
    }

    private data class NotifKey(
        val id: String?, val title: String?, val artist: String?, val cover: String?,
        val playing: Boolean, val busy: Boolean, val duration: Long,
        val seekVersion: Int, val loadingText: String
    )

    private fun observeState() {
        scope.launch {
            RexPlayerController.state
                .map {
                    NotifKey(
                        it.nowPlaying?.id, it.nowPlaying?.title, it.nowPlaying?.artist,
                        it.nowPlaying?.cover, it.isPlaying, it.phase.isPlayerBusy,
                        it.durationMs, it.seekVersion, it.loadingText
                    )
                }
                .distinctUntilChanged()
                .collect { refresh() }
        }
    }

    /** Keep home-screen player metadata and synced lyric line aligned with playback. */
    /** Keep home-screen player metadata and synced lyric line aligned with playback. */
    private fun observeWidgetState() {
        scope.launch {
            RexPlayerController.state
                .map { s ->
                    val lyricIndex = s.lyrics.synced.indexOfLast { it.timeMs <= s.positionMs }
                    listOf(
                        s.nowPlaying?.id.orEmpty(),
                        s.nowPlaying?.title.orEmpty(),
                        s.nowPlaying?.artist.orEmpty(),
                        s.isPlaying.toString(),
                        s.nowPlayingOffline.toString(),
                        (s.positionMs / 1000L).toString(),
                        lyricIndex.toString(),
                        s.lyrics.plain.hashCode().toString(),
                        s.lyricsLoading.toString(),
                        s.lyrics.synced.size.toString(),
                        s.phase.name,
                        s.offline.enabled.toString()
                    ).joinToString("|")
                }
                .distinctUntilChanged()
                .collect {
                    RexMusicWidget.updateAll(
                        applicationContext,
                        RexPlayerController.state.value,
                        coverBitmap
                    )
                }
        }
    }

    private fun refresh() {
        if (closed) return
        val s = RexPlayerController.state.value
        loadCover(s.nowPlaying?.cover.orEmpty())
        updateSession(s)
        postNotification(s)
    }

    private fun loadCover(url: String) {
        if (url == coverUrl) return
        coverUrl = url
        coverBitmap = null
        artJob?.cancel()
        if (url.isBlank()) return
        artJob = scope.launch {
            val request = ImageRequest.Builder(applicationContext)
                .data(url).size(512).allowHardware(false).build()
            val drawable = (applicationContext.imageLoader.execute(request) as? SuccessResult)?.drawable
            val bmp = runCatching { drawable?.toBitmap() }.getOrNull()
            if (bmp != null && coverUrl == url && !closed) {
                coverBitmap = bmp
                refresh()
                RexMusicWidget.updateAll(applicationContext, RexPlayerController.state.value, coverBitmap)
            }
        }
    }

    private fun updateSession(s: RexMusicUiState) {
        val track = s.nowPlaying
        val meta = MediaMetadata.Builder()
            .putString(MediaMetadata.METADATA_KEY_TITLE, track?.title ?: "HansAps Music")
            .putString(MediaMetadata.METADATA_KEY_ARTIST, track?.artist.orEmpty())
            .putString(MediaMetadata.METADATA_KEY_ALBUM, track?.album.orEmpty())
        if (s.durationMs > 0) meta.putLong(MediaMetadata.METADATA_KEY_DURATION, s.durationMs)
        coverBitmap?.let { meta.putBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART, it) }
        session.setMetadata(meta.build())

        val code = when {
            s.phase.isPlayerBusy -> PlaybackState.STATE_BUFFERING
            s.isPlaying -> PlaybackState.STATE_PLAYING
            track != null -> PlaybackState.STATE_PAUSED
            else -> PlaybackState.STATE_STOPPED
        }
        val actions = PlaybackState.ACTION_PLAY or PlaybackState.ACTION_PAUSE or
            PlaybackState.ACTION_PLAY_PAUSE or PlaybackState.ACTION_SKIP_TO_NEXT or
            PlaybackState.ACTION_SKIP_TO_PREVIOUS or PlaybackState.ACTION_SEEK_TO or
            PlaybackState.ACTION_STOP
        session.setPlaybackState(
            PlaybackState.Builder()
                .setActions(actions)
                .setState(code, s.positionMs, if (code == PlaybackState.STATE_PLAYING) 1f else 0f)
                .build()
        )
    }

    private fun postNotification(s: RexMusicUiState) {
        val n = buildNotification(s)
        val active = s.isPlaying || s.phase.isPlayerBusy
        if (active) {
            if (!foreground) {
                runCatching { startForegroundCompat(n) }.onFailure { nm.notify(NOTIF_ID, n) }
            } else nm.notify(NOTIF_ID, n)
        } else {
            if (foreground) detachForeground()
            nm.notify(NOTIF_ID, n)
        }
    }

    private fun buildNotification(s: RexMusicUiState): Notification {
        val track = s.nowPlaying
        val busy = s.phase.isPlayerBusy
        val active = s.isPlaying || busy
        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, CHANNEL_ID)
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this).setPriority(Notification.PRIORITY_LOW)
        }

        builder
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle(track?.title ?: "HansAps Music")
            .setContentText(if (busy) s.loadingText.ifBlank { "Memuat..." } else track?.artist ?: "Siap memutar")
            .setOnlyAlertOnce(true)
            .setShowWhen(false)
            .setOngoing(active)
            .setCategory(Notification.CATEGORY_TRANSPORT)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .setDeleteIntent(servicePending(ACTION_CLOSE))
            .addAction(action(android.R.drawable.ic_media_previous, "Sebelumnya", ACTION_PREV))
            .addAction(if (s.isPlaying) action(android.R.drawable.ic_media_pause, "Jeda", ACTION_TOGGLE)
                       else action(android.R.drawable.ic_media_play, "Putar", ACTION_TOGGLE))
            .addAction(action(android.R.drawable.ic_media_next, "Berikutnya", ACTION_NEXT))
            .addAction(action(android.R.drawable.ic_menu_close_clear_cancel, "Tutup", ACTION_CLOSE))
            .setStyle(
                Notification.MediaStyle()
                    .setMediaSession(session.sessionToken)
                    .setShowActionsInCompactView(0, 1, 2)
            )

        track?.album?.let { builder.setSubText(it) }
        coverBitmap?.let { builder.setLargeIcon(it) }
        contentIntent()?.let { builder.setContentIntent(it) }
        return builder.build()
    }

    @Suppress("DEPRECATION")
    private fun action(icon: Int, title: String, action: String): Notification.Action =
        Notification.Action.Builder(icon, title, servicePending(action)).build()

    private fun servicePending(action: String): PendingIntent = PendingIntent.getService(
        this, action.hashCode(), Intent(this, RexMusicService::class.java).setAction(action),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )

    private fun contentIntent(): PendingIntent? {
        val launch = packageManager.getLaunchIntentForPackage(packageName) ?: return null
        launch.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        return PendingIntent.getActivity(
            this, 0, launch, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(CHANNEL_ID, "Pemutar musik", NotificationManager.IMPORTANCE_LOW)
        channel.description = "Kontrol pemutar RexMusic"
        channel.setShowBadge(false)
        channel.lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        nm.createNotificationChannel(channel)
    }

    private fun startForegroundCompat(n: Notification) {
        if (Build.VERSION.SDK_INT >= 29) {
            startForeground(NOTIF_ID, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
        } else startForeground(NOTIF_ID, n)
        foreground = true
    }

    private fun detachForeground() {
        if (Build.VERSION.SDK_INT >= 24) stopForeground(STOP_FOREGROUND_DETACH)
        else {
            @Suppress("DEPRECATION")
            stopForeground(false)
        }
        foreground = false
    }

    private fun shutdown() {
        closed = true
        RexPlayerController.stop()
        if (Build.VERSION.SDK_INT >= 24) stopForeground(STOP_FOREGROUND_REMOVE)
        else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
        foreground = false
        nm.cancel(NOTIF_ID)
        stopSelf()
    }

    companion object {
        private const val CHANNEL_ID = "hansaps_spotify_playback"
        private const val NOTIF_ID = 4711
        const val ACTION_START = "com.hansaps.spotify.rexmusic.action.START"
        const val ACTION_TOGGLE = "com.hansaps.spotify.rexmusic.action.TOGGLE"
        const val ACTION_NEXT = "com.hansaps.spotify.rexmusic.action.NEXT"
        const val ACTION_PREV = "com.hansaps.spotify.rexmusic.action.PREV"
        const val ACTION_CLOSE = "com.hansaps.spotify.rexmusic.action.CLOSE"

        fun start(context: Context) {
            runCatching {
                ContextCompat.startForegroundService(
                    context, Intent(context, RexMusicService::class.java).setAction(ACTION_START)
                )
            }
        }
    }
}
