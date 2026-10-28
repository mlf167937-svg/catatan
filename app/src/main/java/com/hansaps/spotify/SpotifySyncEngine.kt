package com.hansaps.spotify

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.OpenableColumns
import androidx.media3.common.MediaItem
import com.hansaps.core.LocalTrack
import com.hansaps.core.PlaybackConnector
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.Inet4Address
import java.net.NetworkInterface
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketException
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/** LAN playback sync modeled after RexMusic's room-based sync, adapted to HansAps Media3. */
data class SpotifySyncState(
    val hosting: Boolean = false,
    val connected: Boolean = false,
    val room: String = "HansRoom",
    val ip: String = "",
    val deviceName: String = "HansAps Device",
    val status: String = "Belum tersambung",
    val error: String? = null,
    val members: List<String> = emptyList(),
    val trackId: String = "",
    val title: String = "Belum ada lagu",
    val artist: String = "",
    val playing: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
)

object SpotifySyncEngine {
    private const val CONTROL_PORT = 38741
    private const val TRANSFER_PORT = 38742
    private const val TICK_MS = 750L
    private const val DRIFT_LIMIT_MS = 1_300L

    private data class Peer(
        val socket: Socket,
        val reader: BufferedReader,
        val writer: BufferedWriter,
        val name: String,
        @Volatile var alive: Boolean = true,
    ) {
        @Synchronized fun send(message: JSONObject) {
            writer.write(message.toString())
            writer.newLine()
            writer.flush()
        }
        fun close() { alive = false; runCatching { socket.close() } }
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mutableState = MutableStateFlow(SpotifySyncState())
    val state: StateFlow<SpotifySyncState> = mutableState.asStateFlow()

    private val peers = ConcurrentHashMap<String, Peer>()
    @Volatile private var appContext: Context? = null
    @Volatile private var roomPassword: String = "hansaps"
    @Volatile private var controlServer: ServerSocket? = null
    @Volatile private var transferServer: ServerSocket? = null
    @Volatile private var clientSocket: Socket? = null
    @Volatile private var clientWriter: BufferedWriter? = null
    @Volatile private var lastAppliedTrackId: String = ""
    private var hostJob: Job? = null
    private var transferJob: Job? = null
    private var broadcastJob: Job? = null
    private var clientJob: Job? = null

    fun detectIp(): String = runCatching {
        val interfaces = Collections.list(NetworkInterface.getNetworkInterfaces())
        interfaces.asSequence().filter { it.isUp && !it.isLoopback }.flatMap { Collections.list(it.inetAddresses).asSequence() }
            .filterIsInstance<Inet4Address>()
            .map { it.hostAddress.orEmpty() }
            .firstOrNull { it.startsWith("192.168.") || it.startsWith("10.") || it.startsWith("172.") }
            ?: ""
    }.getOrDefault("")

    fun host(context: Context, room: String, password: String, deviceName: String) {
        closeResources()
        val cleanRoom = room.trim().ifBlank { "HansRoom" }
        val cleanPassword = password
        val cleanName = deviceName.trim().ifBlank { "HansAps Host" }
        appContext = context.applicationContext
        roomPassword = cleanPassword
        val ip = detectIp()
        mutableState.value = SpotifySyncState(
            hosting = true,
            connected = true,
            room = cleanRoom,
            ip = ip,
            deviceName = cleanName,
            status = if (ip.isBlank()) "Host aktif; IP Wi-Fi tidak terdeteksi" else "Menyiapkan ruang lokal…",
            members = listOf(cleanName),
        )
        hostJob = scope.launch {
            try {
                val control = ServerSocket(CONTROL_PORT)
                control.reuseAddress = true
                controlServer = control
                val transfer = runCatching { ServerSocket(TRANSFER_PORT).apply { reuseAddress = true } }.getOrNull()
                transferServer = transfer
                if (transfer == null) mutableState.value = mutableState.value.copy(error = "Port transfer lokal tidak tersedia; track offline dari perangkat host mungkin tidak tersinkron.")
                mutableState.value = mutableState.value.copy(status = "Ruang ${cleanRoom} aktif pada ${ip.ifBlank { "IP tidak diketahui" }}")

                if (transfer != null) {
                    transferJob = scope.launch { acceptTransferRequests(transfer) }
                }
                broadcastJob = scope.launch { broadcastLoop() }
                while (true) {
                    val socket = control.accept()
                    socket.tcpNoDelay = true
                    scope.launch { handleIncomingPeer(socket, cleanRoom, cleanPassword) }
                }
            } catch (cancel: CancellationException) {
                throw cancel
            } catch (error: Exception) {
                mutableState.value = mutableState.value.copy(error = error.message ?: "Gagal membuka ruang sync.", status = "Ruang sync tidak aktif")
            }
        }
    }

    fun join(context: Context, room: String, hostIp: String, password: String, deviceName: String) {
        closeResources()
        val ip = hostIp.trim()
        if (ip.isBlank()) {
            mutableState.value = SpotifySyncState(room = room.trim(), deviceName = deviceName, error = "Masukkan IP host.", status = "IP host belum diisi")
            return
        }
        appContext = context.applicationContext
        PlaybackConnector.connect(context.applicationContext)
        mutableState.value = SpotifySyncState(
            connected = false, room = room.trim(), ip = ip,
            deviceName = deviceName.trim().ifBlank { "HansAps Device" }, status = "Menghubungkan ke $ip…",
        )
        clientJob = scope.launch {
            try {
                val socket = Socket()
                socket.connect(java.net.InetSocketAddress(ip, CONTROL_PORT), 5_000)
                socket.tcpNoDelay = true
                val reader = BufferedReader(InputStreamReader(socket.getInputStream(), Charsets.UTF_8))
                val writer = BufferedWriter(OutputStreamWriter(socket.getOutputStream(), Charsets.UTF_8))
                writer.write(JSONObject().put("type", "hello").put("room", room.trim()).put("password", password)
                    .put("name", deviceName.trim().ifBlank { "HansAps Device" }).toString())
                writer.newLine(); writer.flush()
                val welcomeLine = reader.readLine() ?: throw IOException("Host menutup koneksi.")
                val welcome = JSONObject(welcomeLine)
                if (!welcome.optBoolean("accepted", false)) throw IOException(welcome.optString("message").ifBlank { "Room atau password tidak cocok." })
                clientSocket = socket
                clientWriter = writer
                mutableState.value = mutableState.value.copy(
                    connected = true,
                    status = "Tersambung ke ruang ${room.trim()}",
                    members = jsonStringArray(welcome.optJSONArray("members")),
                    error = null,
                )
                while (true) {
                    val line = reader.readLine() ?: throw IOException("Koneksi host terputus.")
                    val message = JSONObject(line)
                    when (message.optString("type")) {
                        "state" -> applyRemoteState(message)
                        "members" -> mutableState.value = mutableState.value.copy(members = jsonStringArray(message.optJSONArray("items")))
                        "notice" -> mutableState.value = mutableState.value.copy(status = message.optString("message", "Tersambung"))
                    }
                }
            } catch (cancel: CancellationException) {
                throw cancel
            } catch (error: Exception) {
                if (mutableState.value.connected || mutableState.value.status.startsWith("Menghubungkan")) {
                    mutableState.value = mutableState.value.copy(connected = false, status = "Tidak tersambung", error = error.message ?: "Gagal tersambung ke host.")
                }
            }
        }
    }

    fun control(action: String, value: Long = 0L) {
        val s = mutableState.value
        if (!s.connected) return
        if (s.hosting) {
            val controller = PlaybackConnector.controller.value
            when (action) {
                "play" -> controller?.play()
                "pause" -> controller?.pause()
                "toggle" -> if (controller?.isPlaying == true) controller.pause() else controller?.play()
                "seek" -> controller?.seekTo(value.coerceAtLeast(0L))
                "next" -> if (controller?.hasNextMediaItem() == true) controller.seekToNextMediaItem()
                "previous" -> if (controller?.hasPreviousMediaItem() == true) controller.seekToPreviousMediaItem() else controller?.seekTo(0L)
            }
        } else {
            runCatching {
                synchronized(this) {
                    val writer = clientWriter ?: return
                    writer.write(JSONObject().put("type", "control").put("action", action).put("value", value).toString())
                    writer.newLine(); writer.flush()
                }
            }.onFailure { mutableState.value = mutableState.value.copy(error = it.message ?: "Gagal mengirim kontrol ke host.") }
        }
    }

    fun disconnect() {
        closeResources()
        mutableState.value = SpotifySyncState(status = "Terputus")
        lastAppliedTrackId = ""
    }

    private suspend fun handleIncomingPeer(socket: Socket, expectedRoom: String, expectedPassword: String) {
        val reader = runCatching { BufferedReader(InputStreamReader(socket.getInputStream(), Charsets.UTF_8)) }.getOrElse { socket.close(); return }
        val writer = runCatching { BufferedWriter(OutputStreamWriter(socket.getOutputStream(), Charsets.UTF_8)) }.getOrElse { socket.close(); return }
        val peer: Peer
        try {
            val hello = reader.readLine()?.let(::JSONObject) ?: throw IOException("Handshake kosong.")
            if (hello.optString("type") != "hello" || hello.optString("room") != expectedRoom || hello.optString("password") != expectedPassword) {
                val response = JSONObject().put("accepted", false).put("message", "Nama ruang atau password salah.")
                writer.write(response.toString()); writer.newLine(); writer.flush(); socket.close(); return
            }
            val name = hello.optString("name").ifBlank { "HansAps Device" }.take(32)
            peer = Peer(socket, reader, writer, name)
            writer.write(JSONObject().put("accepted", true).put("room", expectedRoom).put("members", JSONArray((listOf(mutableState.value.deviceName) + peers.values.map { it.name } + name).distinct())).toString())
            writer.newLine(); writer.flush()
            peers[socket.inetAddress.hostAddress.orEmpty() + ":" + socket.port] = peer
            updateMembers()
            broadcastStateNow()
            while (peer.alive) {
                val line = reader.readLine() ?: break
                val message = JSONObject(line)
                if (message.optString("type") == "control") applyHostControl(message)
            }
        } catch (_: Exception) {
            // A peer disconnect should not stop the other participants.
        } finally {
            peers.entries.removeIf { it.value.socket === socket }
            runCatching { socket.close() }
            updateMembers()
        }
    }

    private suspend fun applyHostControl(message: JSONObject) {
        val controller = PlaybackConnector.controller.value
        val action = message.optString("action")
        val value = message.optLong("value", 0L).coerceAtLeast(0L)
        withContext(Dispatchers.Main) {
            when (action) {
                "play" -> controller?.play()
                "pause" -> controller?.pause()
                "toggle" -> if (controller?.isPlaying == true) controller.pause() else controller?.play()
                "seek" -> controller?.seekTo(value)
                "next" -> if (controller?.hasNextMediaItem() == true) controller.seekToNextMediaItem()
                "previous" -> if (controller?.hasPreviousMediaItem() == true) controller.seekToPreviousMediaItem() else controller?.seekTo(0L)
            }
        }
    }

    private suspend fun broadcastLoop() {
        while (mutableState.value.hosting) {
            broadcastStateNow()
            delay(TICK_MS)
        }
    }

    private suspend fun broadcastStateNow() {
        val context = appContext ?: return
        // Media3 Player must be read from its application Looper (the main thread).
        val message = withContext(Dispatchers.Main) {
            val controller = PlaybackConnector.controller.value
            val item = controller?.currentMediaItem
            val uri = item?.localConfiguration?.uri
            val direct = uri?.scheme.equals("https", true) || uri?.scheme.equals("http", true)
            val mime = if (!direct && uri != null) runCatching { context.contentResolver.getType(uri).orEmpty() }.getOrDefault("") else ""
            val displayName = if (!direct && uri != null) resolveDisplayName(context, uri) else ""
            val position = controller?.currentPosition?.coerceAtLeast(0L) ?: 0L
            val duration = controller?.duration?.takeIf { it > 0L } ?: 0L
            mutableState.value = mutableState.value.copy(
                trackId = item?.mediaId.orEmpty(),
                title = item?.mediaMetadata?.title?.toString() ?: "Belum ada lagu",
                artist = item?.mediaMetadata?.artist?.toString().orEmpty(),
                playing = controller?.isPlaying == true,
                positionMs = position,
                durationMs = duration,
            )
            val queue = JSONArray()
            if (controller != null) {
                for (index in 0 until controller.mediaItemCount) {
                    val queued = controller.getMediaItemAt(index)
                    queue.put(JSONObject().put("id", queued.mediaId).put("title", queued.mediaMetadata.title?.toString().orEmpty()).put("artist", queued.mediaMetadata.artist?.toString().orEmpty()))
                }
            }
            JSONObject()
                .put("type", "state")
                .put("room", mutableState.value.room)
                .put("trackId", item?.mediaId.orEmpty())
                .put("title", item?.mediaMetadata?.title?.toString().orEmpty())
                .put("artist", item?.mediaMetadata?.artist?.toString().orEmpty())
                .put("uri", if (direct) uri.toString() else "")
                .put("local", uri != null && !direct)
                .put("mimeType", mime)
                .put("displayName", displayName)
                .put("positionMs", position)
                .put("durationMs", duration)
                .put("playing", controller?.isPlaying == true)
                .put("queue", queue)
        }
        peers.values.forEach { peer -> runCatching { peer.send(message) }.onFailure { peer.close() } }
    }

    private suspend fun applyRemoteState(message: JSONObject) {
        val context = appContext ?: return
        val trackId = message.optString("trackId")
        val title = message.optString("title").ifBlank { "HansAps Sync" }
        val artist = message.optString("artist")
        val uri = message.optString("uri")
        val isLocal = message.optBoolean("local", false)
        val hostIp = mutableState.value.ip
        try {
            if (trackId.isNotBlank() && trackId != lastAppliedTrackId) {
                val playbackUri = if (isLocal) {
                    val file = fetchSharedTrack(context, hostIp, trackId, message.optString("displayName"), message.optString("mimeType"))
                    Uri.fromFile(file)
                } else {
                    if (!uri.startsWith("http://", true) && !uri.startsWith("https://", true)) throw IOException("Host belum memutar URL stream yang bisa dibagikan.")
                    Uri.parse(uri)
                }
                val track = LocalTrack(
                    id = "hans-sync:$trackId", title = title, artist = artist,
                    uri = playbackUri, displayName = message.optString("displayName"),
                )
                withContext(Dispatchers.Main) { PlaybackConnector.playQueue(context, listOf(track)) }
                lastAppliedTrackId = trackId
                delay(300L)
            }
            val target = message.optLong("positionMs", 0L).coerceAtLeast(0L)
            val wantsPlaying = message.optBoolean("playing", false)
            withContext(Dispatchers.Main) {
                val controller = PlaybackConnector.controller.value
                if (controller != null && kotlin.math.abs(controller.currentPosition - target) > DRIFT_LIMIT_MS) controller.seekTo(target)
                if (controller != null) {
                    if (wantsPlaying && !controller.isPlaying) controller.play()
                    if (!wantsPlaying && controller.isPlaying) controller.pause()
                }
            }
            mutableState.value = mutableState.value.copy(
                trackId = trackId, title = title, artist = artist, playing = wantsPlaying,
                positionMs = target, durationMs = message.optLong("durationMs", 0L).coerceAtLeast(0L),
                status = "Sinkron dengan host", error = null,
            )
        } catch (cancel: CancellationException) {
            throw cancel
        } catch (error: Exception) {
            mutableState.value = mutableState.value.copy(error = error.message ?: "Gagal menyinkronkan pemutaran.")
        }
    }

    private suspend fun fetchSharedTrack(context: Context, hostIp: String, trackId: String, name: String, mime: String): File = withContext(Dispatchers.IO) {
        if (hostIp.isBlank()) throw IOException("IP host tidak tersedia untuk transfer audio lokal.")
        val dir = File(context.cacheDir, "hansaps-sync").apply { mkdirs() }
        val ext = when {
            mime.contains("mpeg", true) -> "mp3"
            mime.contains("mp4", true) -> "m4a"
            mime.contains("aac", true) -> "aac"
            mime.contains("wav", true) -> "wav"
            else -> name.substringAfterLast('.', "mp3").takeIf { it.matches(Regex("[a-zA-Z0-9]{2,5}")) } ?: "mp3"
        }
        val destination = File(dir, "${safeFilePart(trackId)}.$ext")
        if (destination.exists() && destination.length() > 0L) return@withContext destination
        val socket = Socket()
        try {
            socket.connect(java.net.InetSocketAddress(hostIp, TRANSFER_PORT), 6_000)
            val writer = BufferedWriter(OutputStreamWriter(socket.getOutputStream(), Charsets.UTF_8))
            writer.write(JSONObject().put("id", trackId).toString()); writer.newLine(); writer.flush()
            val input = DataInputStream(BufferedInputStream(socket.getInputStream()))
            val expected = input.readLong()
            destination.outputStream().buffered().use { output ->
                val buffer = ByteArray(128 * 1024)
                if (expected >= 0L) {
                    var remaining = expected
                    while (remaining > 0) {
                        val count = input.read(buffer, 0, minOf(buffer.size.toLong(), remaining).toInt())
                        if (count < 0) throw IOException("Transfer track dari host terputus.")
                        output.write(buffer, 0, count); remaining -= count
                    }
                } else input.copyTo(output)
            }
        } catch (error: Exception) {
            destination.delete()
            throw error
        } finally { runCatching { socket.close() } }
        if (!destination.exists() || destination.length() <= 0L) throw IOException("File sync yang diterima kosong.")
        destination
    }

    private suspend fun acceptTransferRequests(server: ServerSocket) {
        while (mutableState.value.hosting) {
            try {
                val socket = server.accept()
                scope.launch { serveTrackTransfer(socket) }
            } catch (cancel: CancellationException) { throw cancel }
            catch (_: SocketException) { if (!mutableState.value.hosting) break }
            catch (_: Exception) { /* Keep other transfers available. */ }
        }
    }

    private suspend fun serveTrackTransfer(socket: Socket) {
        try {
            val reader = BufferedReader(InputStreamReader(socket.getInputStream(), Charsets.UTF_8))
            val request = reader.readLine()?.let(::JSONObject) ?: throw IOException("Permintaan transfer kosong.")
            val id = request.optString("id")
            val context = appContext ?: throw IOException("Context host tidak tersedia.")
            val uri = withContext(Dispatchers.Main) {
                val controller = PlaybackConnector.controller.value ?: throw IOException("Player host belum aktif.")
                val item = (0 until controller.mediaItemCount).asSequence()
                    .map { controller.getMediaItemAt(it) }
                    .firstOrNull { it.mediaId == id }
                    ?: throw IOException("Track tidak lagi berada di antrean host.")
                item.localConfiguration?.uri ?: throw IOException("Sumber track tidak tersedia.")
            }
            if (uri.scheme.equals("http", true) || uri.scheme.equals("https", true)) throw IOException("Track ini berupa stream, tidak perlu transfer file.")
            val input = openLocalInput(context, uri) ?: throw IOException("File audio host tidak dapat dibuka.")
            input.use { audioInput ->
                val length = fileLength(context, uri)
                DataOutputStream(BufferedOutputStream(socket.getOutputStream())).use { output ->
                    output.writeLong(length)
                    audioInput.copyTo(output, 128 * 1024)
                    output.flush()
                }
            }
        } catch (_: Exception) {
            runCatching { DataOutputStream(socket.getOutputStream()).use { it.writeLong(0L); it.flush() } }
        } finally { runCatching { socket.close() } }
    }

    private fun openLocalInput(context: Context, uri: Uri): InputStream? = when (uri.scheme?.lowercase()) {
        "content" -> context.contentResolver.openInputStream(uri)
        "file" -> uri.path?.let(::File)?.inputStream()
        else -> null
    }

    private fun fileLength(context: Context, uri: Uri): Long {
        if (uri.scheme.equals("file", true)) return uri.path?.let(::File)?.length() ?: -1L
        var cursor: Cursor? = null
        return try {
            cursor = context.contentResolver.query(uri, arrayOf(OpenableColumns.SIZE), null, null, null)
            if (cursor != null && cursor.moveToFirst()) cursor.getLong(cursor.getColumnIndexOrThrow(OpenableColumns.SIZE)).takeIf { it >= 0L } ?: -1L
            else context.contentResolver.openAssetFileDescriptor(uri, "r")?.use { it.length }.takeIf { (it ?: -1L) >= 0L } ?: -1L
        } catch (_: Exception) { -1L } finally { cursor?.close() }
    }

    private fun resolveDisplayName(context: Context, uri: Uri): String {
        if (uri.scheme.equals("file", true)) return uri.lastPathSegment.orEmpty()
        return runCatching {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) cursor.getString(cursor.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME)).orEmpty() else ""
            }.orEmpty()
        }.getOrDefault("")
    }

    private fun updateMembers() {
        val current = mutableState.value
        if (current.hosting) mutableState.value = current.copy(members = (listOf(current.deviceName) + peers.values.map { it.name }).distinct())
        broadcastMembers()
    }

    private fun broadcastMembers() {
        val names = JSONArray(mutableState.value.members)
        val message = JSONObject().put("type", "members").put("items", names)
        peers.values.forEach { peer -> runCatching { peer.send(message) }.onFailure { peer.close() } }
    }

    private fun safeFilePart(value: String): String = value.replace(Regex("[^a-zA-Z0-9_-]"), "_").take(80).ifBlank { "track" }
    private fun jsonStringArray(array: JSONArray?): List<String> = buildList {
        if (array != null) for (i in 0 until array.length()) array.optString(i).takeIf { it.isNotBlank() }?.let(::add)
    }

    private fun closeResources() {
        listOf(hostJob, transferJob, broadcastJob, clientJob).forEach { it?.cancel() }
        hostJob = null; transferJob = null; broadcastJob = null; clientJob = null
        runCatching { controlServer?.close() }; controlServer = null
        runCatching { transferServer?.close() }; transferServer = null
        peers.values.forEach { it.close() }; peers.clear()
        runCatching { clientWriter?.close() }; clientWriter = null
        runCatching { clientSocket?.close() }; clientSocket = null
    }
}
