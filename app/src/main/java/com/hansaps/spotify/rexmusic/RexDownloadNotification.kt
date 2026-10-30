package com.hansaps.spotify.rexmusic

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.SystemClock
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.hansaps.MainActivity
import com.hansaps.R
import java.util.concurrent.ConcurrentHashMap

/** Reliable, quiet progress notifications for the offline music downloader. */
object RexDownloadNotification {
    private const val TAG = "RexDownloadNotification"
    private const val CHANNEL_ID = "hansaps_music_downloads"
    private const val CHANNEL_NAME = "Unduhan musik"
    private const val NOTIFICATION_BASE_ID = 43_000
    internal const val GROUP_KEY = "com.hansaps.DOWNLOADS.MUSIC"
    const val ACTION_CANCEL = "com.hansaps.spotify.notification.CANCEL_DOWNLOAD"
    private val lastProgressPost = ConcurrentHashMap<String, Long>()

    fun init(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        runCatching {
            val manager = context.applicationContext
                .getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                ?: return@runCatching
            if (manager.getNotificationChannel(CHANNEL_ID) == null) {
                manager.createNotificationChannel(
                    NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_LOW).apply {
                        description = "Progres, pembatalan, dan hasil unduhan musik HansAps"
                        setShowBadge(false)
                        setSound(null, null)
                        enableVibration(false)
                        enableLights(false)
                    }
                )
            }
        }.onFailure { Log.w(TAG, "Tidak dapat menyiapkan kanal notifikasi", it) }
    }

    fun showProgress(context: Context, key: String, status: DownloadStatus) {
        // Limit notification churn while still reflecting visible progress changes smoothly.
        val now = SystemClock.elapsedRealtime()
        val previous = lastProgressPost[key] ?: 0L
        val stageChanged = status.stage != DownloadStage.Downloading
        if (!stageChanged && now - previous < 700L && status.percent < 100) return
        lastProgressPost[key] = now

        val percent = status.percent.coerceIn(0, 100)
        val message = when (status.stage) {
            DownloadStage.Queued -> "Menunggu antrean unduhan"
            DownloadStage.Resolving -> "Menyiapkan sumber audio"
            DownloadStage.Downloading -> if (status.percent >= 0) "Mengunduh audio · $percent%" else "Mengunduh audio…"
            DownloadStage.FetchingLyrics -> "Audio selesai · mengambil lirik"
            DownloadStage.Finalizing -> "Menyimpan ke koleksi offline"
        }
        val detail = buildList {
            if (status.downloadedBytes > 0L) add(formatBytes(status.downloadedBytes))
            if (status.totalBytes > 0L) add("dari ${formatBytes(status.totalBytes)}")
            if (status.speedBytesPerSecond > 0L) add("${formatBytes(status.speedBytesPerSecond)}/dtk")
        }.joinToString("  ·  ")

        show(context, key, status.track.title, message, detail, true, percent, status.percent < 0)
    }

    fun showCompleted(context: Context, key: String, status: DownloadStatus) {
        lastProgressPost.remove(key)
        show(context, key, status.track.title, "Unduhan selesai", "Tersimpan ke koleksi offline HansAps", false, 0, false)
    }

    fun showCancelled(context: Context, key: String, track: RexTrack) {
        lastProgressPost.remove(key)
        show(context, key, track.title, "Unduhan dibatalkan", "File sementara sudah dibersihkan", false, 0, false)
    }

    fun showFailed(context: Context, key: String, track: RexTrack, message: String) {
        lastProgressPost.remove(key)
        show(context, key, track.title, "Unduhan gagal", message.take(180).ifBlank { "Periksa koneksi lalu coba lagi" }, false, 0, false)
    }

    private fun show(
        context: Context,
        key: String,
        title: String,
        message: String,
        detail: String,
        ongoing: Boolean,
        progress: Int,
        indeterminate: Boolean,
    ) {
        val app = context.applicationContext
        init(app)
        val id = notificationId(key)
        val notifications = NotificationManagerCompat.from(app)
        if (!notifications.areNotificationsEnabled()) {
            Log.w(TAG, "Notifikasi aplikasi dinonaktifkan oleh pengguna; unduhan tetap berjalan")
            return
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = (app.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager)
                ?.getNotificationChannel(CHANNEL_ID)
            if (channel?.importance == NotificationManager.IMPORTANCE_NONE) {
                Log.w(TAG, "Kanal unduhan musik dinonaktifkan oleh pengguna")
                return
            }
        }

        runCatching {
            val launchIntent = app.packageManager.getLaunchIntentForPackage(app.packageName)?.apply {
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            }
            val contentIntent = launchIntent?.let {
                PendingIntent.getActivity(
                    app,
                    id,
                    it,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                )
            }
            val builder = NotificationCompat.Builder(app, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification_download)
                .setContentTitle(title.ifBlank { "Unduhan Spotify" })
                .setContentText(message)
                .setSubText("HANSAPS  ·  SPOTIFY")
                .setStyle(NotificationCompat.BigTextStyle().bigText(if (detail.isBlank()) message else "$message\n$detail"))
                .setContentIntent(contentIntent)
                .setCategory(NotificationCompat.CATEGORY_PROGRESS)
                .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setColor(0xFF1DB954.toInt())
                .setOnlyAlertOnce(true)
                .setSilent(true)
                .setDefaults(0)
                .setOngoing(ongoing)
                .setAutoCancel(!ongoing)
                .setGroup(GROUP_KEY)
                .setProgress(if (ongoing) 100 else 0, if (ongoing) progress else 0, ongoing && indeterminate)
            if (ongoing) {
                val cancelIntent = PendingIntent.getBroadcast(
                    app,
                    id + 1,
                    Intent(app, RexDownloadActionReceiver::class.java)
                        .setAction(ACTION_CANCEL)
                        .putExtra(RexDownloadActionReceiver.EXTRA_KEY, key),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                )
                builder.addAction(android.R.drawable.ic_menu_close_clear_cancel, "Batalkan", cancelIntent)
            }
            notifications.notify(id, builder.build())
        }.onFailure { Log.w(TAG, "Gagal menampilkan notifikasi unduhan untuk key=$key", it) }
    }

    private fun notificationId(key: String): Int = NOTIFICATION_BASE_ID + (key.hashCode() and 0x7fff)

    private fun formatBytes(value: Long): String {
        if (value < 1024L) return "$value B"
        val units = arrayOf("KB", "MB", "GB", "TB")
        var amount = value.toDouble()
        var index = -1
        do {
            amount /= 1024.0
            index++
        } while (amount >= 1024.0 && index < units.lastIndex)
        return String.format(java.util.Locale.US, "%.1f %s", amount, units[index])
    }
}

/** Explicit, non-exported action receiver for the cancel button shown in active music downloads. */
class RexDownloadActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != RexDownloadNotification.ACTION_CANCEL) return
        val key = intent.getStringExtra(EXTRA_KEY)?.takeIf { it.isNotBlank() } ?: return
        RexOfflineManager.init(context.applicationContext)
        RexOfflineManager.cancelByKey(key)
    }

    companion object {
        const val EXTRA_KEY = "download_key"
    }
}
