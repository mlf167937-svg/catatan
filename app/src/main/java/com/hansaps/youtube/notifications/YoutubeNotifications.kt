package com.hansaps.youtube.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.os.Handler
import android.os.Looper
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.hansaps.MainActivity
import com.hansaps.R
import java.lang.ref.WeakReference
import java.net.URL
import java.util.concurrent.Executors

/** Playback notification for online YouTube videos, including artwork and a live play/pause action. */
object YoutubePlaybackNotification {
    const val ACTION_TOGGLE = "com.hansaps.youtube.notification.TOGGLE"
    const val ACTION_STOP = "com.hansaps.youtube.notification.STOP"
    const val CHANNEL_ID = "hansaps_youtube_playback"
    const val CHANNEL_NAME = "Nonton YT"
    const val NOTIFICATION_ID = 82_001
    private val artworkExecutor = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())
    @Volatile private var activePlayer = WeakReference<ExoPlayer>(null)
    @Volatile private var appContext: Context? = null
    @Volatile private var title: String = "Video YouTube"
    @Volatile private var quality: String = "Auto"
    @Volatile private var thumbnailUrl: String? = null
    @Volatile private var artwork: Bitmap? = null
    @Volatile private var artworkForUrl: String? = null
    @Volatile private var artworkRequestToken: Long = 0L
    private var attachedPlayer: ExoPlayer? = null
    private var playerListener: Player.Listener? = null

    @Synchronized
    fun bind(context: Context, player: ExoPlayer) {
        val app = context.applicationContext
        appContext = app
        createChannel(app)
        val current = activePlayer.get()
        if (current !== player) {
            if (current == null) {
                runCatching {
                    val serviceIntent = Intent(app, YoutubePlaybackService::class.java)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) app.startForegroundService(serviceIntent)
                    else app.startService(serviceIntent)
                }
            } else {
                detachListener(current)
            }
            activePlayer = WeakReference(player)
            attachedPlayer = player
            val listener = object : Player.Listener {
                override fun onIsPlayingChanged(isPlaying: Boolean) = refresh()
                override fun onPlaybackStateChanged(playbackState: Int) = refresh()
                override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) = refresh()
            }
            playerListener = listener
            player.addListener(listener)
        }
        refresh()
    }

    @Synchronized
    fun updateMetadata(context: Context, player: ExoPlayer, newTitle: String, newThumbnailUrl: String?, newQuality: String) {
        appContext = context.applicationContext
        title = newTitle.ifBlank { "Video YouTube" }
        quality = newQuality.ifBlank { "Auto" }
        val cleanUrl = newThumbnailUrl?.takeIf { it.startsWith("https://", true) }
        if (thumbnailUrl != cleanUrl) {
            thumbnailUrl = cleanUrl
            artwork = null
            artworkForUrl = null
            artworkRequestToken += 1
            val token = artworkRequestToken
            if (cleanUrl != null) {
                artworkExecutor.execute {
                    val bitmap = runCatching {
                        URL(cleanUrl).openConnection().apply {
                            connectTimeout = 6_000
                            readTimeout = 8_000
                            setRequestProperty("User-Agent", "HansAps/1.1 Android")
                        }.getInputStream().use { BitmapFactory.decodeStream(it) }
                    }.getOrNull()
                    synchronized(this) {
                        if (token == artworkRequestToken && thumbnailUrl == cleanUrl) {
                            artwork = bitmap
                            artworkForUrl = cleanUrl
                            mainHandler.post { refresh() }
                        }
                    }
                }
            }
        }
        if (activePlayer.get() !== player) bind(context, player) else refresh()
    }

    @Synchronized
    fun unbind(context: Context, player: ExoPlayer) {
        if (activePlayer.get() !== player) return
        detachListener(player)
        activePlayer.clear()
        activePlayer = WeakReference(null)
        val app = context.applicationContext
        appContext = app
        (app.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager)?.cancel(NOTIFICATION_ID)
        app.stopService(Intent(app, YoutubePlaybackService::class.java))
    }

    fun toggle() {
        val player = activePlayer.get() ?: return
        val shouldPause = player.playbackState != Player.STATE_ENDED && (player.isPlaying || player.playWhenReady)
        if (shouldPause) {
            player.pause()
        } else {
            if (player.playbackState == Player.STATE_ENDED) player.seekTo(0L)
            player.play()
        }
        refresh()
    }

    fun stop() {
        val player = activePlayer.get() ?: return
        player.seekTo(0L)
        player.play()
        refresh()
    }

    /** Re-publishes rich metadata after the foreground service posts its required initial notification. */
    fun refreshFromService() = refresh()

    @Synchronized
    private fun detachListener(player: ExoPlayer) {
        playerListener?.let(player::removeListener)
        playerListener = null
        if (attachedPlayer === player) attachedPlayer = null
    }

    private fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_LOW).apply {
                    description = "Kontrol pemutaran video dan thumbnail yang sedang ditonton"
                    setShowBadge(false)
                },
            )
        }
    }

    @Synchronized
    private fun refresh() {
        val context = appContext ?: return
        val player = activePlayer.get() ?: return
        createChannel(context)
        val isPlaying = player.playbackState != Player.STATE_ENDED && (player.isPlaying || player.playWhenReady)
        val bitmap = artwork?.takeIf { artworkForUrl == thumbnailUrl }
        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentIntent = PendingIntent.getActivity(
            context, 52_001, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val toggleIntent = PendingIntent.getBroadcast(
            context, 52_002, Intent(context, YoutubeMediaActionReceiver::class.java).setAction(ACTION_TOGGLE),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val stopIntent = PendingIntent.getBroadcast(
            context, 52_003, Intent(context, YoutubeMediaActionReceiver::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_media)
            .setContentTitle("Nonton YT")
            .setContentText(title)
            .setSubText("$quality  •  ${if (isPlaying) "Sedang diputar" else "Dijeda"}")
            .setContentIntent(contentIntent)
            .setCategory(NotificationCompat.CATEGORY_TRANSPORT)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setOngoing(true)
            .addAction(
                if (isPlaying) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play,
                if (isPlaying) "Jeda" else "Putar",
                toggleIntent,
            )
            .addAction(android.R.drawable.ic_media_rew, "Mulai ulang", stopIntent)
        if (bitmap != null) {
            builder.setLargeIcon(bitmap)
            builder.setStyle(NotificationCompat.BigPictureStyle().bigPicture(bitmap).bigLargeIcon(null as Bitmap?))
        }
        (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).notify(NOTIFICATION_ID, builder.build())
    }
}

/** Explicit, non-exported receiver means only actions created by this app can control its video player. */
class YoutubeMediaActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        when (intent?.action) {
            YoutubePlaybackNotification.ACTION_TOGGLE -> YoutubePlaybackNotification.toggle()
            YoutubePlaybackNotification.ACTION_STOP -> YoutubePlaybackNotification.stop()
        }
    }
}

/** Progress notifications for YouTube video downloads, using the same Android 10-safe behavior as music downloads. */
object YoutubeDownloadNotification {
    private const val TAG = "YoutubeDownloadNotif"
    private const val CHANNEL_ID = "hansaps_youtube_downloads"
    private const val CHANNEL_NAME = "Download Video YT"

    fun showProgress(context: Context, key: String, title: String, percent: Int, message: String, indeterminate: Boolean = false) {
        show(context, key, title, message, true, percent.coerceIn(0, 100), indeterminate)
    }

    fun showCompleted(context: Context, key: String, title: String, quality: String) {
        show(context, key, title, "Video tersimpan offline · $quality", false, 0, false)
    }

    fun showFailed(context: Context, key: String, title: String, message: String) {
        show(context, key, title, "Download gagal: ${message.take(140)}", false, 0, false)
    }

    private fun show(
        context: Context,
        key: String,
        title: String,
        message: String,
        ongoing: Boolean,
        percent: Int,
        indeterminate: Boolean,
    ) {
        val app = context.applicationContext
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            runCatching {
                val manager = app.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                if (manager.getNotificationChannel(CHANNEL_ID) == null) {
                    manager.createNotificationChannel(
                        NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_LOW).apply {
                            description = "Progres dan hasil penyimpanan video untuk ditonton offline"
                            setShowBadge(false)
                            setSound(null, null)
                            enableVibration(false)
                            enableLights(false)
                        },
                    )
                }
            }.onFailure { Log.w(TAG, "Tidak dapat menyiapkan kanal notifikasi", it) }
        }

        val notifications = NotificationManagerCompat.from(app)
        if (!notifications.areNotificationsEnabled()) {
            Log.w(TAG, "Notifikasi aplikasi dinonaktifkan oleh pengguna; unduhan video tetap berjalan")
            return
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = (app.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager)
                ?.getNotificationChannel(CHANNEL_ID)
            if (channel?.importance == NotificationManager.IMPORTANCE_NONE) {
                Log.w(TAG, "Kanal unduhan video dinonaktifkan oleh pengguna")
                return
            }
        }

        val id = 100_000 + (key.hashCode() and 0x3fff)
        runCatching {
            val openIntent = Intent(app, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val contentIntent = PendingIntent.getActivity(
                app, id, openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            val builder = NotificationCompat.Builder(app, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification_download)
                .setContentTitle(title.ifBlank { "Download Video YT" })
                .setContentText(message)
                .setSubText("HANSAPS  ·  YOUTUBE")
                .setStyle(NotificationCompat.BigTextStyle().bigText(message))
                .setContentIntent(contentIntent)
                .setCategory(NotificationCompat.CATEGORY_PROGRESS)
                .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setColor(0xFFFF3030.toInt())
                .setOnlyAlertOnce(true)
                .setSilent(true)
                .setDefaults(0)
                .setOngoing(ongoing)
                .setAutoCancel(!ongoing)
                .setProgress(if (ongoing) 100 else 0, if (ongoing) percent else 0, ongoing && indeterminate)
            notifications.notify(id, builder.build())
        }.onFailure { Log.w(TAG, "Gagal menampilkan notifikasi video untuk key=$key", it) }
    }
}

/** Keeps the media notification alive while the activity is backgrounded. */
class YoutubePlaybackService : Service() {
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(
                NotificationChannel(
                    YoutubePlaybackNotification.CHANNEL_ID,
                    YoutubePlaybackNotification.CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_LOW,
                ).apply {
                    description = "Kontrol pemutaran video YouTube"
                    setShowBadge(false)
                },
            )
        }
        val placeholder = NotificationCompat.Builder(this, YoutubePlaybackNotification.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_media)
            .setContentTitle("Nonton YT")
            .setContentText("Menyiapkan pemutar video…")
            .setCategory(NotificationCompat.CATEGORY_TRANSPORT)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .build()
        startForeground(YoutubePlaybackNotification.NOTIFICATION_ID, placeholder)
        YoutubePlaybackNotification.refreshFromService()
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
