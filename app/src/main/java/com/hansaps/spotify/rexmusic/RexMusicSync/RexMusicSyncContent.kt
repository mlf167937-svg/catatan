package com.hansaps.spotify.rexmusic

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import kotlinx.coroutines.launch

private enum class SyncEntryMode { HOST, JOIN }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun __splitImpl_RexMusicSyncScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val state by RexMusicSyncEngine.state.collectAsState()
    val offlineState by RexOfflineManager.state.collectAsState()

    var mode by rememberSaveable { mutableStateOf<SyncEntryMode?>(null) }
    var room by rememberSaveable { mutableStateOf("RexSyncRoom1") }
    var ip by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("rexsync") }
    val localProfile = rememberRexMusicProfileSnapshotForSync()
    var query by rememberSaveable { mutableStateOf("") }
    var onlineResults by remember { mutableStateOf<List<RexTrack>>(emptyList()) }
    var searching by remember { mutableStateOf(false) }
    var searchMessage by rememberSaveable { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val active = state.hosting || state.connected
    val connecting = !active && state.error == null && state.status.lowercase().let {
        "menghubungkan" in it || "connecting" in it || "mencari host" in it || "membuka room" in it
    }

    LaunchedEffect(Unit) {
        RexMusicSyncEngine.bindContext(context)
        if (ip.isBlank()) ip = RexMusicSyncEngine.detectIp(context)
    }

    BackHandler(enabled = !active && mode != null) { mode = null }

    fun searchOnline() {
        if (searching || query.isBlank()) return
        searching = true
        searchMessage = "Mencari lagu…"
        scope.launch {
            val result = RexMusicApi.search(query.trim())
            onlineResults = result.getOrElse { emptyList() }
            searchMessage = if (result.isSuccess) {
                "${onlineResults.size} hasil · unduh agar tersedia untuk room"
            } else {
                result.exceptionOrNull()?.message ?: "Pencarian gagal"
            }
            searching = false
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                        Text("HansAps Music", fontWeight = FontWeight.Bold)
                        Text(
                            if (active) "LISTENING ROOM" else "LISTEN TOGETHER",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        when {
                            active -> onBack()
                            mode != null -> mode = null
                            else -> onBack()
                        }
                    }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Kembali")
                    }
                },
                actions = {
                    if (active) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.padding(end = 16.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(Modifier.size(6.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary))
                                Text("LIVE", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { padding ->
        AnimatedContent(
            targetState = when {
                active -> "room"
                mode != null -> "setup"
                else -> "entry"
            },
            modifier = Modifier.fillMaxSize().padding(padding),
            transitionSpec = {
                (fadeIn() + slideInHorizontally { it / 16 }) togetherWith
                    (fadeOut() + slideOutHorizontally { -it / 24 })
            },
            label = "syncDestination"
        ) { destination ->
            when (destination) {
                "entry" -> SyncEntryLanding(
                    onHost = { mode = SyncEntryMode.HOST },
                    onJoin = { mode = SyncEntryMode.JOIN },
                    error = state.error
                )
                "setup" -> SyncSetupScreen(
                    mode = mode ?: SyncEntryMode.HOST,
                    room = room,
                    onRoomChange = { room = it },
                    ip = ip,
                    onIpChange = { ip = it },
                    password = password,
                    onPasswordChange = { password = it },
                    profile = localProfile,
                    connecting = connecting,
                    error = state.error,
                    onAutoIp = { ip = RexMusicSyncEngine.detectIp(context) },
                    onBack = { mode = null },
                    onStart = {
                        if (mode == SyncEntryMode.HOST) {
                            RexMusicSyncEngine.createRoom(room, password, ip)
                        } else {
                            RexMusicSyncEngine.joinRoom(room.trim(), ip.trim(), password)
                        }
                    }
                )
                else -> SyncRoomDashboard(
                    state = state,
                    offlineTracks = offlineState.entries.distinctBy { it.track.id }.filter { it.track.id.isNotBlank() }.map { it.track },
                    query = query,
                    onQueryChange = { query = it },
                    searching = searching,
                    searchMessage = searchMessage,
                    results = onlineResults.distinctBy { it.id }.filter { it.id.isNotBlank() },
                    isDownloaded = { id -> offlineState.entries.any { it.track.id == id } },
                    onSearch = ::searchOnline,
                    onDownload = { RexOfflineManager.download(it) },
                    onQueue = { RexMusicSyncEngine.addToQueue(it) },
                    onPlayQueued = { RexMusicSyncEngine.playQueued(it) },
                    onMemberVolume = { member, value -> RexMusicSyncEngine.setMemberVolume(member.id, value) },
                    onHostVolume = { RexMusicSyncEngine.setHostVolume(it) },
                    onTogglePlayback = { RexMusicSyncEngine.togglePlayback() },
                    onStop = { RexMusicSyncEngine.stopAll() },
                    onDisconnect = {
                        RexMusicSyncEngine.disconnect()
                        mode = null
                    }
                )
            }
        }
    }
}

@Composable
private fun SyncEntryLanding(onHost: () -> Unit, onJoin: () -> Unit, error: String?) {
    val scheme = MaterialTheme.colorScheme
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Box(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(30.dp))
                    .background(Brush.linearGradient(listOf(scheme.primary.copy(alpha = 0.20f), scheme.tertiary.copy(alpha = 0.10f), scheme.surface)))
                    .border(1.dp, scheme.outlineVariant.copy(alpha = 0.65f), RoundedCornerShape(30.dp))
                    .padding(22.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Surface(shape = RoundedCornerShape(18.dp), color = scheme.primaryContainer) {
                        Icon(Icons.Default.Headphones, null, modifier = Modifier.padding(13.dp).size(30.dp), tint = scheme.onPrimaryContainer)
                    }
                    Text("Musik lebih seru\nbareng-bareng.", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.ExtraBold, color = scheme.onBackground)
                    Text(
                        "Buat ruang dengar di Wi-Fi lokal. Sinkronkan lagu dan nikmati pemutaran bersama, tanpa akun tambahan.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = scheme.onSurfaceVariant
                    )
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        Icon(Icons.Default.Wifi, null, modifier = Modifier.size(16.dp), tint = scheme.primary)
                        Text("LOCAL WI-FI · LOW LATENCY", style = MaterialTheme.typography.labelSmall, color = scheme.primary, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        item {
            Text("MULAI SESI", style = MaterialTheme.typography.labelMedium, color = scheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
        }
        item {
            SyncChoiceCard(
                icon = Icons.Default.Wifi,
                eyebrow = "KAMU YANG MENGATUR",
                title = "Buat Host",
                subtitle = "Buat room baru dan bagikan detail koneksi ke teman.",
                actionLabel = "BUAT ROOM",
                accent = scheme.primary,
                onClick = onHost
            )
        }
        item {
            SyncChoiceCard(
                icon = Icons.Default.Groups,
                eyebrow = "SUDAH ADA ROOM?",
                title = "Gabung Room",
                subtitle = "Masukkan IP host untuk bergabung ke sesi musik yang aktif.",
                actionLabel = "GABUNG",
                accent = scheme.tertiary,
                onClick = onJoin
            )
        }
        if (!error.isNullOrBlank()) {
            item {
                Surface(shape = RoundedCornerShape(16.dp), color = scheme.errorContainer) {
                    Text(error, modifier = Modifier.fillMaxWidth().padding(14.dp), color = scheme.onErrorContainer, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
private fun SyncChoiceCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    eyebrow: String,
    title: String,
    subtitle: String,
    actionLabel: String,
    accent: Color,
    onClick: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        color = scheme.surface,
        tonalElevation = 1.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, scheme.outlineVariant.copy(alpha = 0.72f))
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(15.dp)
        ) {
            Surface(shape = RoundedCornerShape(18.dp), color = accent.copy(alpha = 0.13f)) {
                Icon(icon, null, modifier = Modifier.padding(15.dp).size(28.dp), tint = accent)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(eyebrow, style = MaterialTheme.typography.labelSmall, color = accent, fontWeight = FontWeight.Bold)
                Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
                Spacer(Modifier.height(3.dp))
                Text(actionLabel + "  ↗", style = MaterialTheme.typography.labelMedium, color = accent, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun SyncSetupScreen(
    mode: SyncEntryMode,
    room: String,
    onRoomChange: (String) -> Unit,
    ip: String,
    onIpChange: (String) -> Unit,
    password: String,
    onPasswordChange: (String) -> Unit,
    profile: RexLocalProfileSnapshot,
    connecting: Boolean,
    error: String?,
    onAutoIp: () -> Unit,
    onBack: () -> Unit,
    onStart: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(if (mode == SyncEntryMode.HOST) "Siapkan room" else "Masuk ke room", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text(
                    if (mode == SyncEntryMode.HOST) "Perangkat ini akan menjadi pusat sesi. Pastikan teman menggunakan jaringan Wi-Fi yang sama."
                    else "Gunakan IP perangkat host. Kedua perangkat harus berada di jaringan Wi-Fi yang sama.",
                    style = MaterialTheme.typography.bodyMedium, color = scheme.onSurfaceVariant
                )
            }
        }
        item {
            Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = scheme.surface)) {
                Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    OutlinedTextField(value = room, onValueChange = onRoomChange, label = { Text("Nama room") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    OutlinedTextField(
                        value = ip, onValueChange = onIpChange, label = { Text(if (mode == SyncEntryMode.HOST) "IP lokal host" else "IP host tujuan") },
                        modifier = Modifier.fillMaxWidth(), singleLine = true,
                        trailingIcon = { TextButton(onClick = onAutoIp) { Text("AUTO") } }
                    )
                    OutlinedTextField(value = password, onValueChange = onPasswordChange, label = { Text("Password room") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    Row(
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(17.dp))
                            .background(scheme.surfaceVariant.copy(alpha = 0.70f))
                            .border(1.dp, scheme.outlineVariant.copy(alpha = 0.70f), RoundedCornerShape(17.dp))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        RexLocalProfilePhoto(profile, Modifier.size(42.dp))
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(profile.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text("Nama mengikuti RexLocal Account", style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
                        }
                        Icon(Icons.Default.CheckCircle, contentDescription = "Identitas dari Local Account", tint = scheme.primary, modifier = Modifier.size(19.dp))
                    }
                    if (mode == SyncEntryMode.HOST) {
                        Text("Setelah room aktif, bagikan IP dan password kepada teman.", style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
                    }
                    Button(
                        onClick = onStart,
                        enabled = !connecting && ip.isNotBlank() && room.isNotBlank() && password.isNotBlank() && (mode == SyncEntryMode.HOST || profile.name.isNotBlank()),
                        modifier = Modifier.fillMaxWidth().height(54.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        if (connecting) CircularProgressIndicator(Modifier.size(19.dp), strokeWidth = 2.dp, color = scheme.onPrimary)
                        else Icon(if (mode == SyncEntryMode.HOST) Icons.Default.Wifi else Icons.Default.Groups, null)
                        Spacer(Modifier.width(9.dp))
                        Text(if (connecting) "Menghubungkan…" else if (mode == SyncEntryMode.HOST) "Buat Host" else "Gabung Room", fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth().height(48.dp), shape = RoundedCornerShape(16.dp)) {
                        Icon(Icons.Default.ArrowBack, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Kembali ke pilihan")
                    }
                    if (!error.isNullOrBlank()) {
                        Surface(shape = RoundedCornerShape(14.dp), color = scheme.errorContainer) {
                            Text(error, modifier = Modifier.fillMaxWidth().padding(12.dp), color = scheme.onErrorContainer, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                Icon(Icons.Default.CheckCircle, null, tint = scheme.tertiary, modifier = Modifier.size(18.dp))
                Text("Tidak perlu akun. Audio diputar dari library lokal masing-masing perangkat.", style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun SyncRoomDashboard(
    state: SyncUiState,
    offlineTracks: List<RexTrack>,
    query: String,
    onQueryChange: (String) -> Unit,
    searching: Boolean,
    searchMessage: String,
    results: List<RexTrack>,
    isDownloaded: (String) -> Boolean,
    onSearch: () -> Unit,
    onDownload: (RexTrack) -> Unit,
    onQueue: (RexTrack) -> Unit,
    onPlayQueued: (RexTrack) -> Unit,
    onMemberVolume: (SyncMember, Float) -> Unit,
    onHostVolume: (Float) -> Unit,
    onTogglePlayback: () -> Unit,
    onStop: () -> Unit,
    onDisconnect: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 8.dp, bottom = 34.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Card(shape = RoundedCornerShape(26.dp), colors = CardDefaults.cardColors(containerColor = scheme.surfaceVariant)) {
                Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text(if (state.hosting) "Room kamu aktif" else "Terhubung ke room", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                            Text(state.status, style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
                        }
                        Surface(shape = CircleShape, color = scheme.tertiaryContainer) {
                            Icon(Icons.Default.CheckCircle, null, modifier = Modifier.padding(10.dp).size(23.dp), tint = scheme.onTertiaryContainer)
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                        SyncMetricPill("${state.members.distinctBy { it.id }.size}", "ANGGOTA", Modifier.weight(1f))
                        SyncMetricPill(state.room, "ROOM", Modifier.weight(1f))
                    }
                    if (state.hosting) {
                        Text("IP host: ${state.ip}", style = MaterialTheme.typography.bodyMedium, color = scheme.primary, fontWeight = FontWeight.SemiBold)
                        Text("Bagikan IP dan password kepada teman yang ingin bergabung.", style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
                    }
                    if (state.error != null) Text(state.error, color = scheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        item {
            SyncSectionHeading("Sedang diputar", state.trackTitle.ifBlank { "Tambahkan lagu dari library di bawah" })
            SyncPlaybackCard(state, onHostVolumeChange = onHostVolume, onTogglePlayback = onTogglePlayback, onStop = onStop)
        }
        item { SyncMembersHeader(state.members.distinctBy { it.id }.size) }
        items(state.members.distinctBy { it.id }, key = { "member-${it.id}" }) { member ->
            SyncMemberCard(member, state, onVolumeChange = { onMemberVolume(member, it) })
        }
        item { SyncQueueHeader(state.queue.distinctBy { it.id }.size) }
        if (state.queue.distinctBy { it.id }.isEmpty()) item { SyncEmptyQueueCard() }
        items(state.queue.distinctBy { it.id }, key = { "queue-${it.id}" }) { track ->
            SyncQueueTrackCard(track, onPlay = { onPlayQueued(track) })
        }
        item {
            SyncSectionHeading("Library offline", "${offlineTracks.size} lagu tersedia di perangkat")
        }
        if (offlineTracks.isEmpty()) item { SyncEmptyOfflineCard() }
        items(offlineTracks, key = { "offline-${it.id}" }) { track ->
            SyncArtworkTrackCard(track = track, action = "TAMBAH", onClick = { onQueue(track) })
        }
        item {
            SyncSectionHeading("Temukan lagu", "Cari musik untuk disimpan sebelum dibagikan ke room")
        }
        item {
            OutlinedTextField(
                value = query, onValueChange = onQueryChange, modifier = Modifier.fillMaxWidth(),
                label = { Text("Judul lagu atau artis") }, singleLine = true,
                shape = RoundedCornerShape(17.dp),
                trailingIcon = {
                    IconButton(onClick = onSearch, enabled = !searching && query.isNotBlank()) {
                        if (searching) CircularProgressIndicator(Modifier.size(19.dp), strokeWidth = 2.dp)
                        else Icon(Icons.Default.Search, contentDescription = "Cari")
                    }
                }
            )
        }
        if (searchMessage.isNotBlank()) item { Text(searchMessage, style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant) }
        items(results, key = { "search-${it.id}" }) { track ->
            SyncArtworkTrackCard(
                track = track,
                action = if (isDownloaded(track.id)) "TERSIMPAN" else "UNDUH",
                enabled = !isDownloaded(track.id),
                onClick = { if (!isDownloaded(track.id)) onDownload(track) }
            )
        }
        item {
            OutlinedButton(onClick = onDisconnect, modifier = Modifier.fillMaxWidth().height(50.dp), shape = RoundedCornerShape(16.dp)) {
                Text(if (state.hosting) "Tutup room" else "Keluar dari room")
            }
        }
    }
}

@Composable
private fun SyncMetricPill(value: String, label: String, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    Surface(modifier, shape = RoundedCornerShape(16.dp), color = scheme.surface) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 11.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(label, style = MaterialTheme.typography.labelSmall, color = scheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun SyncArtworkTrackCard(track: RexTrack, action: String, enabled: Boolean = true, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(enabled = enabled, onClick = onClick),
        shape = RoundedCornerShape(19.dp),
        color = scheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, scheme.outlineVariant.copy(alpha = 0.55f))
    ) {
        Row(Modifier.padding(11.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.size(62.dp).clip(RoundedCornerShape(13.dp)).background(scheme.surfaceVariant), contentAlignment = Alignment.Center) {
                if (track.cover.isNotBlank()) {
                    AsyncImage(model = track.cover, contentDescription = "Cover ${track.title}", modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                } else {
                    Icon(Icons.Default.MusicNote, null, tint = scheme.primary, modifier = Modifier.size(28.dp))
                }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(track.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(track.artist, style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (track.durationText.isNotBlank()) Text(track.durationText, style = MaterialTheme.typography.labelSmall, color = scheme.onSurfaceVariant)
            }
            Text(action, style = MaterialTheme.typography.labelSmall, color = if (enabled) scheme.primary else scheme.tertiary, fontWeight = FontWeight.Bold)
        }
    }
}
