package com.hansaps.spotify.rexmusic

import android.app.Notification
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import com.hansaps.MainActivity
import com.hansaps.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

/** Keeps Spotify/offline music downloads visible and alive while HansAps is backgrounded. */
class RexMusicDownloadService : Service() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override fun onCreate() {
        super.onCreate()
        RexOfflineManager.init(applicationContext)
        createChannel()
        // Android requires the initial foreground notification immediately after service start.
        startForegroundCompat(buildSummary(RexOfflineManager.state.value))

        serviceScope.launch {
            RexOfflineManager.state.collect { state ->
                if (state.downloads.isEmpty()) {
                    stopForegroundCompat()
                    stopSelf()
                } else if (NotificationManagerCompat.from(this@RexMusicDownloadService).areNotificationsEnabled()) {
                    runCatching {
                        NotificationManagerCompat.from(this@RexMusicDownloadService)
                            .notify(SUMMARY_NOTIFICATION_ID, buildSummary(state))
                    }.onFailure { Log.w(TAG, "Gagal memperbarui notifikasi ringkasan unduhan", it) }
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_NOT_STICKY

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createChannel() = RexDownloadNotification.init(this)

    private fun buildSummary(state: OfflineState): Notification {
        val active = state.downloads.values.toList()
        val knownProgress = active.mapNotNull { it.percent.takeIf { pct -> pct >= 0 } }
        val percent = if (knownProgress.isEmpty()) 0 else knownProgress.average().toInt().coerceIn(0, 100)
        val indeterminate = knownProgress.size != active.size
        val title = if (active.size == 1) active.first().track.title else "${active.size} lagu sedang diunduh"
        val detail = if (active.size == 1) {
            val status = active.first()
            when (status.stage) {
                DownloadStage.Queued -> "Menunggu antrean"
                DownloadStage.Resolving -> "Menyiapkan sumber audio"
                DownloadStage.Downloading -> if (status.percent >= 0) "Mengunduh · ${status.percent}%" else "Mengunduh audio…"
                DownloadStage.FetchingLyrics -> "Audio selesai · mengambil lirik"
                DownloadStage.Finalizing -> "Menyimpan ke koleksi offline"
            }
        } else {
            "Unduhan berjalan di latar belakang"
        }
        val openIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val contentIntent = android.app.PendingIntent.getActivity(
            this,
            SUMMARY_NOTIFICATION_ID,
            openIntent,
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Builder(this, RexDownloadNotification.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_download)
            .setContentTitle("Unduhan musik aktif")
            .setContentText(title)
            .setSubText("HANSAPS  ·  SPOTIFY")
            .setStyle(NotificationCompat.BigTextStyle().bigText("$title\n$detail"))
            .setContentIntent(contentIntent)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setColor(0xFF1DB954.toInt())
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setOngoing(true)
            .setGroup(RexDownloadNotification.GROUP_KEY)
            .setGroupSummary(true)
            .setGroupAlertBehavior(NotificationCompat.GROUP_ALERT_SUMMARY)
            .setProgress(100, percent, indeterminate)
            .build()
    }

    private fun startForegroundCompat(notification: Notification) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ServiceCompat.startForeground(
                this,
                SUMMARY_NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC,
            )
        } else {
            startForeground(SUMMARY_NOTIFICATION_ID, notification)
        }
    }

    private fun stopForegroundCompat() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
    }

    companion object {
        private const val TAG = "RexMusicDownloadService"
        private const val SUMMARY_NOTIFICATION_ID = 42_999

        fun start(context: Context) {
            val app = context.applicationContext
            runCatching {
                val intent = Intent(app, RexMusicDownloadService::class.java)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    androidx.core.content.ContextCompat.startForegroundService(app, intent)
                } else {
                    app.startService(intent)
                }
            }.onFailure { Log.w(TAG, "Tidak dapat memulai layanan unduhan latar belakang", it) }
        }
    }
}
