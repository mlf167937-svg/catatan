package com.hansaps.spotify.rexmusic

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.hansaps.MainActivity
import com.hansaps.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/** Keeps Spotify audio downloads alive and visible in the Android 10+ notification shade. */
class RexDownloadForegroundService : Service() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override fun onCreate() {
        super.onCreate()
        RexOfflineManager.init(applicationContext)
        RexDownloadNotification.init(this)
        startForegroundCompat(buildNotification(RexOfflineManager.state.value.downloads.values.toList()))

        serviceScope.launch {
            RexOfflineManager.state
                .map { state -> state.downloads.values.toList() }
                .distinctUntilChanged()
                .collect { downloads ->
                    if (downloads.isEmpty()) {
                        stopForeground(STOP_FOREGROUND_REMOVE)
                        stopSelf()
                    } else {
                        startForegroundCompat(buildNotification(downloads))
                    }
                }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_NOT_STICKY

    override fun onBind(intent: Intent?): IBinder? = null

    // Android 15 applies a six-hour daily limit to dataSync foreground services.
    override fun onTimeout(startId: Int, fgsType: Int) {
        RexOfflineManager.jobs.values.forEach { it.cancel() }
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf(startId)
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun buildNotification(downloads: List<DownloadStatus>): Notification {
        val current = downloads.firstOrNull()
        val title = when {
            downloads.size > 1 -> "${downloads.size} lagu sedang diunduh"
            current != null -> current.track.title
            else -> "Menyiapkan unduhan musik"
        }
        val body = current?.let { statusText(it) } ?: "Download musik berjalan di HansAps"
        val launchIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentIntent = PendingIntent.getActivity(
            this, FOREGROUND_NOTIFICATION_ID, launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val pct = current?.percent?.coerceIn(0, 100) ?: 0
        val indeterminate = current == null || current.percent < 0
        return NotificationCompat.Builder(this, RexDownloadNotification.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_download)
            .setContentTitle(title)
            .setContentText(body)
            .setSubText("HansAps · Spotify offline")
            .setContentIntent(contentIntent)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setOngoing(true)
            .setProgress(100, pct, indeterminate)
            .build()
    }

    private fun statusText(status: DownloadStatus): String = when (status.stage) {
        DownloadStage.Queued -> "Menunggu giliran unduh"
        DownloadStage.Resolving -> "Menyiapkan sumber audio"
        DownloadStage.Downloading -> if (status.percent >= 0) "Mengunduh · ${status.percent}%" else "Mengunduh audio…"
        DownloadStage.FetchingLyrics -> "Mengambil lirik"
        DownloadStage.Finalizing -> "Menyimpan metadata"
    }

    private fun startForegroundCompat(notification: Notification) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(
                    FOREGROUND_NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC,
                )
            } else {
                startForeground(FOREGROUND_NOTIFICATION_ID, notification)
            }
        } catch (error: Throwable) {
            Log.e("HansApsDownload", "Unable to enter download foreground service", error)
            throw error
        }
    }

    companion object {
        private const val FOREGROUND_NOTIFICATION_ID = 82_003

        fun start(context: Context): Boolean {
            val app = context.applicationContext
            val intent = Intent(app, RexDownloadForegroundService::class.java)
            return runCatching {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) app.startForegroundService(intent)
                else app.startService(intent)
                true
            }.onFailure {
                Log.e("HansApsDownload", "Unable to start music download service", it)
            }.getOrDefault(false)
        }
    }
}
