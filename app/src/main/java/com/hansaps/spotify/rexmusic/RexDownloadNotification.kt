package com.hansaps.spotify.rexmusic

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import com.hansaps.R

/** Compact download notifications, without RexMusic's legacy RemoteViews resource dependency. */
object RexDownloadNotification {
    private const val CHANNEL_ID = "hansaps_music_downloads"
    private const val CHANNEL_NAME = "Unduhan musik"

    fun init(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_LOW))
        }
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

    private fun show(context: Context, key: String, title: String, text: String, ongoing: Boolean, progress: Int, indeterminate: Boolean) {
        init(context)
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_hansaps)
            .setContentTitle(title)
            .setContentText(text)
            .setOnlyAlertOnce(true)
            .setOngoing(ongoing)
            .setProgress(100, progress, indeterminate)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .build()
        (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
            .notify(43_000 + key.hashCode().and(0x7fff), notification)
    }
}
