package com.hansaps.spotify.rexmusic

import android.content.Context
import android.os.SystemClock
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import com.google.gson.Gson
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject
import java.io.BufferedReader
import java.io.BufferedWriter
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.CopyOnWriteArrayList

internal const val SYNC_PORT = 38741
internal const val TRANSFER_PORT = 38742
internal const val HEARTBEAT_MS = 800L
internal const val CLIENT_TIMEOUT_MS = 3500L
internal const val START_LEAD_MS = 3000L
internal const val DRIFT_CORRECTION_MS = 250L

internal data class SyncMember(
    val id: String,
    val name: String,
    val connectedAt: Long = SystemClock.elapsedRealtime(),
    val ip: String = "",
    val volume: Float = 1f,
    val deviceVolume: Float = -1f,
    val username: String = "",
    val bio: String = "",
    /** Local path to a received avatar thumbnail, not a path from another device. */
    val picturePath: String = "",
    /** Local path to a received banner thumbnail, not a path from another device. */
    val bannerPath: String = ""
)
internal data class SyncUiState(
    val hosting: Boolean = false, val connected: Boolean = false, val room: String = "RexSyncRoom1",
    val ip: String = "", val password: String = "rexsync", val name: String = "User",
    val status: String = "Belum terhubung", val members: List<SyncMember> = emptyList(),
    val trackId: String = "", val trackTitle: String = "Belum ada lagu", val positionMs: Long = 0L,
    val playing: Boolean = false, val error: String? = null, val queue: List<RexTrack> = emptyList(), val cacheStatus: String = "", val volumes: Map<String, Float> = emptyMap(), val hostVolume: Float = 1f, val roomElapsedMs: Long = 0L
)

/** Local Wi-Fi sync protocol. Uses newline-delimited JSON control messages; no SSH or internet. */
internal object RexMusicSyncEngine {
    internal val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO + CoroutineExceptionHandler { _, error ->
        _state.value = _state.value.copy(error = "RexMusicSync: ${error.message ?: "terjadi kesalahan"}")
    })
    private val gson = Gson()
    internal val _state = MutableStateFlow(SyncUiState())
    val state = _state.asStateFlow()
    internal var server: ServerSocket? = null
    internal var transferServer: ServerSocket? = null
    internal var transferJob: Job? = null
    internal val sharedQueue = mutableListOf<RexTrack>()
    internal var roomJob: Job? = null
    internal val peers = CopyOnWriteArrayList<Peer>()
    internal var client: Socket? = null
    internal var clientWriter: BufferedWriter? = null
    @Volatile internal var clientPeerId: String = ""
    internal var heartbeat: Job? = null
    internal var playbackJob: Job? = null
    internal var roomPassword = "rexsync"
    internal var roomName = "RexSyncRoom1"
    internal var hostTrack: RexTrack? = null
    internal var playAnchorWallMs = 0L // host monotonic elapsedRealtime timestamp
    internal var roomStartedElapsedMs = 0L
    @Volatile internal var clockOffsetMs = 0L // host elapsedRealtime - client elapsedRealtime
    @Volatile internal var lastClockRttMs = -1L
    internal var playAnchorPositionMs = 0L
    internal var hostPlaying = false
    internal var hostPreparing = false
    internal var resumeWhenMemberReturns = false

    internal data class Peer(val socket: Socket, val reader: BufferedReader, val writer: BufferedWriter,
                            var id: String = "", var name: String = "", @Volatile var lastSeen: Long = SystemClock.elapsedRealtime(), val ip: String = socket.inetAddress?.hostAddress.orEmpty(), @Volatile var volume: Float = 1f, @Volatile var deviceVolume: Float = -1f, @Volatile var clockSynced: Boolean = false, @Volatile var sentTrackId: String = "", var username: String = "", var bio: String = "", var picturePath: String = "", var bannerPath: String = "", var pictureData: String = "", var bannerData: String = "")

    fun detectIp(context: Context): String = this.__splitImpl_detectIp(context)

    fun createRoom(room: String, password: String, ip: String) {
        return this.__splitImpl_createRoom(room, password, ip)
    }

    internal suspend fun acceptPeer(socket: Socket) {
        return this.__splitImpl_acceptPeer(socket)
    }

    internal suspend fun monitorPeers() {
        return this.__splitImpl_monitorPeers()
    }

    fun joinRoom(room: String, ip: String, password: String) {
        return this.__splitImpl_joinRoom(room, ip, password)
    }

    internal fun handleClientMessage(m: JSONObject) {
        return this.__splitImpl_handleClientMessage(m)
    }

    internal suspend fun syncLocalTrack(id: String, pos: Long, playing: Boolean, startAt: Long) {
        return this.__splitImpl_syncLocalTrack(id, pos, playing, startAt)
    }

    internal suspend fun reconcileLocalTrack(id: String, targetPosition: Long, shouldPlay: Boolean) {
        return this.__splitImpl_reconcileLocalTrack(id, targetPosition, shouldPlay)
    }

    internal var appContextRef: Context? = null
    fun bindContext(context: Context) {
        return this.__splitImpl_bindContext(context)
    }

    fun playTrack(track: RexTrack) {
        return this.__splitImpl_playTrack(track)
    }

    internal fun resumeRoomAtCurrentPosition() {
        return this.__splitImpl_resumeRoomAtCurrentPosition()
    }

    fun togglePlayback() {
        return this.__splitImpl_togglePlayback()
    }

    fun stopAll(reason: String = "Host menghentikan pemutaran") {
        return this.__splitImpl_stopAll(reason)
    }

    internal fun sendState(p: Peer) {
        return this.__splitImpl_sendState(p)
    }
    internal fun broadcastState() { peers.filter { it.clockSynced }.forEach { sendState(it) } }
    internal fun broadcastTick() {
        return this.__splitImpl_broadcastTick()
    }
    internal fun broadcastMembers() {
        return this.__splitImpl_broadcastMembers()
    }
    fun addToQueue(track: RexTrack) {
        return this.__splitImpl_addToQueue(track)
    }

    fun playQueued(track: RexTrack) {
        return this.__splitImpl_playQueued(track)
    }

    fun setHostVolume(value: Float) {
        return this.__splitImpl_setHostVolume(value)
    }

    fun setMemberVolume(id: String, value: Float) {
        return this.__splitImpl_setMemberVolume(id, value)
    }

    internal fun trackJson(t: RexTrack) = this.__splitImpl_trackJson(t)
    internal fun trackFromJson(o: JSONObject?): RexTrack? = this.__splitImpl_trackFromJson(o)
    internal fun sendQueue(p: Peer) {
        return this.__splitImpl_sendQueue(p)
    }
    internal fun publishQueue() {
        return this.__splitImpl_publishQueue()
    }

    internal suspend fun distributeLocalTrack(track: RexTrack) {
        return this.__splitImpl_distributeLocalTrack(track)
    }

    internal suspend fun sendBundle(host: String, track: RexTrack): Boolean = this.__splitImpl_sendBundle(host, track)

    internal suspend fun receiveBundle(socket: Socket) {
        return this.__splitImpl_receiveBundle(socket)
    }

    internal fun send(w: BufferedWriter, m: JSONObject) {
        return this.__splitImpl_send(w, m)
    }
    internal suspend fun withContextMain(block: () -> Unit) { withContext(Dispatchers.Main) { block() } }

    fun disconnect() {
        return this.__splitImpl_disconnect()
    }
    internal fun closeAll() {
        return this.__splitImpl_closeAll()
    }
    internal fun stopLocalPlayback() {
        return this.__splitImpl_stopLocalPlayback()
    }
}

internal fun formatRoomTime(ms: Long): String {
        return __splitImpl_formatRoomTime(ms)
    }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RexMusicSyncScreen(onBack: () -> Unit) {
        return __splitImpl_RexMusicSyncScreen(onBack)
    }

@Composable
internal fun SyncOverviewCard(
    state: SyncUiState,
    active: Boolean,
    connecting: Boolean
) {
        return __splitImpl_SyncOverviewCard(state, active, connecting)
    }

@Composable
internal fun SyncConnectionCard(
    room: String,
    onRoomChange: (String) -> Unit,
    ip: String,
    onIpChange: (String) -> Unit,
    password: String,
    onPasswordChange: (String) -> Unit,
    active: Boolean,
    onAutoIp: () -> Unit,
    onCreateRoom: () -> Unit,
    onJoinRoom: () -> Unit
) {
        return __splitImpl_SyncConnectionCard(room, onRoomChange, ip, onIpChange, password, onPasswordChange, active, onAutoIp, onCreateRoom, onJoinRoom)
    }

@Composable
internal fun SyncSectionHeading(
    title: String,
    subtitle: String? = null,
    titleColor: androidx.compose.ui.graphics.Color? = null,
    subtitleColor: androidx.compose.ui.graphics.Color? = null
) {
        return __splitImpl_SyncSectionHeading(title, subtitle, titleColor, subtitleColor)
    }

@Composable
internal fun SyncMembersHeader(count: Int) {
        return __splitImpl_SyncMembersHeader(count)
    }

@Composable
internal fun SyncMemberCard(
    member: SyncMember,
    state: SyncUiState,
    onVolumeChange: (Float) -> Unit
) {
        return __splitImpl_SyncMemberCard(member, state, onVolumeChange)
    }

@Composable
internal fun SyncPlaybackCard(
    state: SyncUiState,
    onHostVolumeChange: (Float) -> Unit,
    onTogglePlayback: () -> Unit,
    onStop: () -> Unit
) {
        return __splitImpl_SyncPlaybackCard(state, onHostVolumeChange, onTogglePlayback, onStop)
    }

@Composable
internal fun SyncQueueHeader(count: Int) {
        return __splitImpl_SyncQueueHeader(count)
    }

@Composable
internal fun SyncQueueTrackCard(track: RexTrack, onPlay: () -> Unit) {
        return __splitImpl_SyncQueueTrackCard(track, onPlay)
    }

@Composable
internal fun SyncOnlineSearchCard(
    query: String,
    onQueryChange: (String) -> Unit,
    searching: Boolean,
    searchMessage: String,
    onSearch: () -> Unit
) {
        return __splitImpl_SyncOnlineSearchCard(query, onQueryChange, searching, searchMessage, onSearch)
    }

@Composable
internal fun SyncOnlineResultCard(
    track: RexTrack,
    downloaded: Boolean,
    onDownload: () -> Unit
) {
        return __splitImpl_SyncOnlineResultCard(track, downloaded, onDownload)
    }

@Composable
internal fun SyncOfflineHeader(count: Int, active: Boolean) {
        return __splitImpl_SyncOfflineHeader(count, active)
    }

@Composable
internal fun SyncOfflineTrackCard(
    track: RexTrack,
    active: Boolean,
    onQueue: () -> Unit
) {
        return __splitImpl_SyncOfflineTrackCard(track, active, onQueue)
    }

@Composable
internal fun SyncEmptyQueueCard() {
        return __splitImpl_SyncEmptyQueueCard()
    }

@Composable
internal fun SyncEmptyOfflineCard() {
        return __splitImpl_SyncEmptyOfflineCard()
    }

@Composable
internal fun SyncWaitingCard(connecting: Boolean, error: String?) {
        return __splitImpl_SyncWaitingCard(connecting, error)
    }

@Composable
internal fun SyncMembersEmptyCard() {
        return __splitImpl_SyncMembersEmptyCard()
    }

@Composable
internal fun SyncPlaybackEmptyCard() {
        return __splitImpl_SyncPlaybackEmptyCard()
    }
