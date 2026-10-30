package com.hansaps.spotify.rexmusic

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.hansaps.R

/** Download notifications are best-effort and must never interrupt the actual file download. */
object RexDownloadNotification {
    internal const val CHANNEL_ID = "hansaps_music_downloads"
    private const val CHANNEL_NAME = "Unduhan musik"

    fun init(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        runCatching {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                ?: return@runCatching
            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = "Progres dan hasil unduhan musik HansAps"
                    setShowBadge(false)
                }
            )
        }
    }

    fun notificationsEnabled(context: Context): Boolean {
        init(context.applicationContext)
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return false
        if (!manager.areNotificationsEnabled()) return false
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.getNotificationChannel(CHANNEL_ID)?.importance?.let { it != NotificationManager.IMPORTANCE_NONE } ?: false
        } else true
    }

    fun showProgress(context: Context, key: String, status: DownloadStatus) {
        val pct = status.percent.coerceIn(0, 100)
        show(context, key, status.track.title, when (status.stage) {
            DownloadStage.Queued -> "Menunggu giliran unduh"
            DownloadStage.Resolving -> "Menyiapkan sumber audio"
            DownloadStage.Downloading -> if (status.percent >= 0) "Mengunduh · $pct%" else "Mengunduh audio…"
            DownloadStage.FetchingLyrics -> "Mengambil lirik"
            DownloadStage.Finalizing -> "Menyimpan metadata"
        }, true, pct, status.percent < 0)
    }

    fun showCompleted(context: Context, key: String, status: DownloadStatus) =
        show(context, key, status.track.title, "Tersimpan ke koleksi offline", false, 100, false)

    fun showCancelled(context: Context, key: String, track: RexTrack) =
        show(context, key, track.title, "Unduhan dibatalkan", false, 0, false)

    fun showFailed(context: Context, key: String, track: RexTrack, message: String) =
        show(context, key, track.title, "Unduhan gagal: $message", false, 0, false)

    private fun show(
        context: Context,
        key: String,
        title: String,
        text: String,
        ongoing: Boolean,
        progress: Int,
        indeterminate: Boolean
    ) {
        // A ROM-specific notification failure should not cancel an otherwise valid download.
        runCatching {
            init(context)
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                ?: return@runCatching
            val launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)
            val contentIntent = launchIntent?.let {
                PendingIntent.getActivity(
                    context,
                    43_000 + key.hashCode().and(0x7fff),
                    it,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            }
            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification_download)
                .setContentTitle(title.ifBlank { "HansAps Music" })
                .setContentText(text)
                .setSubText("HansAps · Unduhan musik")
                .setStyle(NotificationCompat.BigTextStyle().bigText(text))
                .setContentIntent(contentIntent)
                .setOnlyAlertOnce(true)
                .setOngoing(ongoing)
                .setAutoCancel(!ongoing)
                .setProgress(if (ongoing) 100 else 0, if (ongoing) progress else 0, ongoing && indeterminate)
                .setCategory(NotificationCompat.CATEGORY_PROGRESS)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setSilent(true)
                .build()
            manager.notify(100_000 + key.hashCode().and(0x3fff_ffff), notification)
        }.onFailure { Log.e("HansApsNotifications", "Music download notification failed", it) }
    }
}
