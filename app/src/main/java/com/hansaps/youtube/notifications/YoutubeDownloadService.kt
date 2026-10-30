package com.hansaps.youtube.notifications

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.Handler
import android.os.Looper
import androidx.core.app.NotificationCompat
import com.hansaps.MainActivity
import com.hansaps.R
import com.hansaps.youtube.Engine.AuthorizedVideoSource
import com.hansaps.youtube.Engine.YoutubeDownloadManager
import com.hansaps.youtube.Engine.YoutubeDownloadProgress
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** State shared by the download service and the Compose Koleksi screen. */
data class YoutubeDownloadUiState(
    val key: String,
    val title: String,
    val quality: String,
    val percent: Int = 0,
    val message: String = "Menyiapkan unduhan",
    val indeterminate: Boolean = true,
    val finished: Boolean = false,
    val failed: Boolean = false,
)

object YoutubeDownloadProgressStore {
    private val lock = Any()
    private val handler = Handler(Looper.getMainLooper())
    private val mutableDownloads = MutableStateFlow<List<YoutubeDownloadUiState>>(emptyList())
    val downloads: StateFlow<List<YoutubeDownloadUiState>> = mutableDownloads.asStateFlow()

    fun update(item: YoutubeDownloadUiState) {
        synchronized(lock) {
            mutableDownloads.value = mutableDownloads.value.filterNot { it.key == item.key } + item
        }
        if (item.finished) {
            handler.postDelayed({
                synchronized(lock) {
                    val current = mutableDownloads.value.firstOrNull { it.key == item.key }
                    if (current?.finished == true) {
                        mutableDownloads.value = mutableDownloads.value.filterNot { it.key == item.key }
                    }
                }
            }, if (item.failed) 6_000L else 3_500L)
        }
    }

    fun remove(key: String) = synchronized(lock) {
        mutableDownloads.value = mutableDownloads.value.filterNot { it.key == key }
    }
}

/** Owns YouTube downloads so they keep a visible foreground notification on Android 10+. */
class YoutubeDownloadService : Service() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val jobs = LinkedHashMap<String, Job>()
    private val active = LinkedHashMap<String, YoutubeDownloadUiState>()
    private var latestStartId = 0

    override fun onCreate() {
        super.onCreate()
        YoutubeDownloadNotification.init(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        latestStartId = startId
        // Promote the service immediately; Android requires this within five seconds.
        startForegroundCompat(buildSummaryNotification())

        val key = intent?.getStringExtra(EXTRA_KEY).orEmpty()
        val title = intent?.getStringExtra(EXTRA_TITLE).orEmpty()
        val videoId = intent?.getStringExtra(EXTRA_VIDEO_ID).orEmpty()
        val directUrl = intent?.getStringExtra(EXTRA_DIRECT_URL).orEmpty()
        val quality = intent?.getStringExtra(EXTRA_QUALITY).orEmpty()
        if (key.isBlank() || videoId.isBlank() || !directUrl.startsWith("https://", true)) {
            stopWhenIdle(startId)
            return START_NOT_STICKY
        }
        if (jobs.containsKey(key)) return START_NOT_STICKY

        val source = AuthorizedVideoSource(
            title = title.ifBlank { "Video YouTube" },
            videoId = videoId,
            directUrl = directUrl,
            quality = quality,
            thumbnailUrl = intent?.getStringExtra(EXTRA_THUMBNAIL).orEmpty().ifBlank { null },
            audioUrl = intent?.getStringExtra(EXTRA_AUDIO_URL).orEmpty().ifBlank { null },
            sourceLabel = "Nexray API",
        )
        val initial = YoutubeDownloadUiState(key, source.title, quality)
        active[key] = initial
        YoutubeDownloadProgressStore.update(initial)
        updateForeground()

        val job = serviceScope.launch {
            try {
                val result = YoutubeDownloadManager.download(applicationContext, source) { progress ->
                    val item = progress.toUiState(key, source.title, quality)
                    YoutubeDownloadProgressStore.update(item)
                    YoutubeDownloadNotification.showProgress(
                        applicationContext, key, source.title,
                        progress.percent, progress.message, progress.indeterminate,
                    )
                    serviceScope.launch {
                        active[key] = item
                        updateForeground()
                    }
                }
                result.onSuccess {
                    YoutubeDownloadNotification.showCompleted(applicationContext, key, it.title, quality)
                    YoutubeDownloadProgressStore.update(
                        YoutubeDownloadUiState(key, source.title, quality, 100, "Selesai · tersimpan offline", finished = true),
                    )
                }.onFailure { error ->
                    val errorText = error.message ?: "Kesalahan jaringan"
                    YoutubeDownloadNotification.showFailed(applicationContext, key, source.title, errorText)
                    YoutubeDownloadProgressStore.update(
                        YoutubeDownloadUiState(key, source.title, quality, 0, "Gagal · $errorText", finished = true, failed = true),
                    )
                }
            } finally {
                jobs.remove(key)
                active.remove(key)
                if (active.isEmpty()) {
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelfResult(latestStartId)
                } else {
                    updateForeground()
                }
            }
        }
        jobs[key] = job
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    // Android 15 limits dataSync foreground services to six hours per 24-hour window.
    override fun onTimeout(startId: Int, fgsType: Int) {
        jobs.values.toList().forEach { it.cancel() }
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf(startId)
    }

    override fun onDestroy() {
        serviceScope.cancel()
        jobs.values.forEach { it.cancel() }
        jobs.clear()
        super.onDestroy()
    }

    private fun YoutubeDownloadProgress.toUiState(
        key: String,
        title: String,
        quality: String,
    ) = YoutubeDownloadUiState(key, title, quality, percent, message, indeterminate)

    private fun updateForeground() {
        if (active.isNotEmpty()) startForegroundCompat(buildSummaryNotification())
    }

    private fun stopWhenIdle(startId: Int) {
        if (active.isEmpty()) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelfResult(startId)
        }
    }

    private fun buildSummaryNotification(): Notification {
        val item = active.values.firstOrNull()
        val count = active.size
        val title = when {
            count > 1 -> "$count video sedang diunduh"
            item != null -> item.title
            else -> "Menyiapkan unduhan YouTube"
        }
        val body = item?.let { "${it.message} · ${it.quality}" } ?: "Download video berjalan di HansAps"
        val launchIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pending = PendingIntent.getActivity(
            this, FOREGROUND_NOTIFICATION_ID, launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Builder(this, YoutubeDownloadNotification.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_download)
            .setContentTitle(title)
            .setContentText(body)
            .setSubText("HansAps · Download YouTube")
            .setContentIntent(pending)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setOngoing(true)
            .setProgress(100, item?.percent?.coerceIn(0, 100) ?: 0, item?.indeterminate ?: true)
            .build()
    }

    private fun startForegroundCompat(notification: Notification) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                FOREGROUND_NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC,
            )
        } else {
            startForeground(FOREGROUND_NOTIFICATION_ID, notification)
        }
    }

    companion object {
        private const val FOREGROUND_NOTIFICATION_ID = YoutubeDownloadNotification.FOREGROUND_NOTIFICATION_ID
        private const val EXTRA_KEY = "download_key"
        private const val EXTRA_TITLE = "download_title"
        private const val EXTRA_VIDEO_ID = "download_video_id"
        private const val EXTRA_DIRECT_URL = "download_direct_url"
        private const val EXTRA_QUALITY = "download_quality"
        private const val EXTRA_THUMBNAIL = "download_thumbnail"
        private const val EXTRA_AUDIO_URL = "download_audio_url"

        fun start(context: Context, source: AuthorizedVideoSource, key: String) {
            val app = context.applicationContext
            val intent = Intent(app, YoutubeDownloadService::class.java).apply {
                putExtra(EXTRA_KEY, key)
                putExtra(EXTRA_TITLE, source.title)
                putExtra(EXTRA_VIDEO_ID, source.videoId)
                putExtra(EXTRA_DIRECT_URL, source.directUrl)
                putExtra(EXTRA_QUALITY, source.quality)
                putExtra(EXTRA_THUMBNAIL, source.thumbnailUrl)
                putExtra(EXTRA_AUDIO_URL, source.audioUrl)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) app.startForegroundService(intent)
            else app.startService(intent)
        }
    }
}
