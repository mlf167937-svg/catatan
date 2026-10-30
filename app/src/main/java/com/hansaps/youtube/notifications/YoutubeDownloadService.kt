package com.hansaps.youtube.notifications

import android.app.Notification
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.ServiceCompat
import com.hansaps.youtube.Engine.AuthorizedVideoSource
import com.hansaps.youtube.Engine.YoutubeDownloadManager
import com.hansaps.youtube.YoutubeDownloadState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Runs a YouTube transfer as a visible data-sync foreground service (Android 10+). */
class YoutubeDownloadService : Service() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var downloadJob: Job? = null
    private var activeKey: String? = null

    override fun onCreate() {
        super.onCreate()
        YoutubeDownloadNotification.createChannel(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action != ACTION_START) {
            if (downloadJob?.isActive != true) stopSelf(startId)
            return START_NOT_STICKY
        }

        val source = intent.toSource()
        val key = intent.getStringExtra(EXTRA_KEY).orEmpty()
        if (source == null || key.isBlank()) {
            stopSelf(startId)
            return START_NOT_STICKY
        }
        if (downloadJob?.isActive == true) {
            YoutubeDownloadState.fail(key, "Unduhan lain masih berjalan. Tunggu hingga selesai.")
            return START_NOT_STICKY
        }

        activeKey = key
        YoutubeDownloadState.begin(key, source.title, source.quality)
        // Android requires startForeground promptly after startForegroundService().
        startForegroundCompat(YoutubeDownloadNotification.buildForeground(this, source.title, source.quality, 0, "Menyiapkan unduhan…", true))

        downloadJob = serviceScope.launch {
            val result = YoutubeDownloadManager.download(applicationContext, source) { progress ->
                YoutubeDownloadState.update(key, progress.percent, progress.message, progress.indeterminate)
                val notification = YoutubeDownloadNotification.buildForeground(
                    this@YoutubeDownloadService,
                    source.title,
                    source.quality,
                    progress.percent,
                    progress.message,
                    progress.indeterminate,
                )
                (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                    .notify(YoutubeDownloadNotification.FOREGROUND_ID, notification)
            }

            result.onSuccess {
                YoutubeDownloadState.complete(key)
                stopForegroundCompat(removeNotification = true)
                YoutubeDownloadNotification.showCompleted(this@YoutubeDownloadService, key, source.title, source.quality)
            }.onFailure { error ->
                val message = error.message ?: "Kesalahan jaringan"
                YoutubeDownloadState.fail(key, message)
                stopForegroundCompat(removeNotification = true)
                YoutubeDownloadNotification.showFailed(this@YoutubeDownloadService, key, source.title, message)
            }

            // The transfer is done: permit the next download while the final Koleksi state fades out.
            activeKey = null
            downloadJob = null
            delay(8_000L)
            YoutubeDownloadState.remove(key)
            stopSelf(startId)
        }
        return START_NOT_STICKY
    }

    private fun startForegroundCompat(notification: Notification) {
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
        } else 0
        ServiceCompat.startForeground(this, YoutubeDownloadNotification.FOREGROUND_ID, notification, type)
    }

    private fun stopForegroundCompat(removeNotification: Boolean) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(if (removeNotification) STOP_FOREGROUND_REMOVE else STOP_FOREGROUND_DETACH)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(removeNotification)
        }
    }

    override fun onDestroy() {
        downloadJob?.cancel()
        serviceScope.cancel()
        activeKey?.let(YoutubeDownloadState::remove)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun Intent.toSource(): AuthorizedVideoSource? {
        val title = getStringExtra(EXTRA_TITLE) ?: return null
        val videoId = getStringExtra(EXTRA_VIDEO_ID) ?: return null
        val directUrl = getStringExtra(EXTRA_DIRECT_URL) ?: return null
        val quality = getStringExtra(EXTRA_QUALITY) ?: return null
        return AuthorizedVideoSource(
            title = title,
            videoId = videoId,
            directUrl = directUrl,
            quality = quality,
            thumbnailUrl = getStringExtra(EXTRA_THUMBNAIL),
            audioUrl = getStringExtra(EXTRA_AUDIO_URL),
            sourceLabel = "Nexray API",
        )
    }

    companion object {
        const val ACTION_START = "com.hansaps.youtube.download.START"
        private const val EXTRA_KEY = "download_key"
        private const val EXTRA_TITLE = "download_title"
        private const val EXTRA_VIDEO_ID = "download_video_id"
        private const val EXTRA_DIRECT_URL = "download_direct_url"
        private const val EXTRA_QUALITY = "download_quality"
        private const val EXTRA_THUMBNAIL = "download_thumbnail"
        private const val EXTRA_AUDIO_URL = "download_audio_url"

        fun start(context: Context, key: String, source: AuthorizedVideoSource) {
            val intent = Intent(context, YoutubeDownloadService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_KEY, key)
                putExtra(EXTRA_TITLE, source.title)
                putExtra(EXTRA_VIDEO_ID, source.videoId)
                putExtra(EXTRA_DIRECT_URL, source.directUrl)
                putExtra(EXTRA_QUALITY, source.quality)
                putExtra(EXTRA_THUMBNAIL, source.thumbnailUrl)
                putExtra(EXTRA_AUDIO_URL, source.audioUrl)
            }
            androidx.core.content.ContextCompat.startForegroundService(context, intent)
        }
    }
}
