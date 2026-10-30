package com.hansaps.spotify.rexmusic

import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.hansaps.spotify.rexmusic.player.MusicPlayerCard
import com.hansaps.spotify.rexmusic.player.PlayerActions
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map

@Composable
internal fun __splitImpl_RexMusicContent(
    onBack: () -> Unit,
    vm: RexMusicViewModel,
    onOpenLocalProfile: () -> Unit
) {
    val context = LocalContext.current

    val state by remember(vm) {
        vm.state.map { it.copy(positionMs = 0L) }.distinctUntilChanged()
    }.collectAsState(initial = vm.state.value.copy(positionMs = 0L))

    val positionState = remember(vm) {
        vm.state.map { it.positionMs }.distinctUntilChanged()
    }.collectAsState(initial = vm.state.value.positionMs)

    var tab by rememberSaveable { mutableStateOf(TAB_HOME) }
    var libTab by rememberSaveable { mutableStateOf(0) }
    var query by rememberSaveable { mutableStateOf("") }
    var typing by remember { mutableStateOf(false) }
    var showPlayer by rememberSaveable { mutableStateOf(false) }
    var selectedPlaylistName by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingDeletePlaylistName by rememberSaveable { mutableStateOf<String?>(null) }
    var showQueue by rememberSaveable { mutableStateOf(false) }
    var showRepeat by rememberSaveable { mutableStateOf(false) }
    // Permission/track-target dialogs are transient because their selected RexTrack is not saveable.
    var showStorageDialog by remember { mutableStateOf(false) }
    var showDeleteAll by rememberSaveable { mutableStateOf(false) }
    var pendingDownload by remember { mutableStateOf<RexTrack?>(null) }
    var pendingNotificationDownload by remember { mutableStateOf<RexTrack?>(null) }
    var deleteTarget by remember { mutableStateOf<RexTrack?>(null) }
    var menuTarget by remember { mutableStateOf<MenuTarget?>(null) }
    var playlistTarget by remember { mutableStateOf<RexTrack?>(null) }
    var showCreatePlaylist by remember { mutableStateOf(false) }
    var showLocalProfile by rememberSaveable { mutableStateOf(false) }
    var localProfile by remember { mutableStateOf(RexLocalProfileBridge.read()) }

    // Preserve liked IDs across configuration changes without needing to serialize RexTrack objects.
    val likedIds = rememberSaveable(
        saver = listSaver(
            save = { ids -> ids.toList() },
            restore = { saved -> mutableStateListOf<String>().apply { addAll(saved) } }
        )
    ) { mutableStateListOf<String>() }
    val snackbar = remember { SnackbarHostState() }

    val homeList = rememberLazyListState()
    val searchList = rememberLazyListState()
    val playlistList = rememberLazyListState()
    val libraryList = rememberLazyListState()

    LaunchedEffect(vm) {
        vm.refreshOffline()
    }

    LaunchedEffect(vm) {
        vm.state
            .map { it.notice }
            .distinctUntilChanged()
            .filterNotNull()
            .collectLatest { message ->
                vm.consumeNotice()
                snackbar.showSnackbar(message)
            }
    }

    LaunchedEffect(query) {
        typing = query.isNotBlank()

        if (typing) {
            delay(450)
            typing = false
        }
    }

    val startDownload: (RexTrack) -> Unit = { track ->
        if (RexOfflineManager.hasAccess(context)) {
            vm.download(track)
        } else {
            pendingDownload = track
            showStorageDialog = true
        }
    }

    // On Android 13+, ask for notification permission before creating the first
    // progress notification. The download still proceeds if the user denies it.
    val notifLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ ->
        val track = pendingNotificationDownload
        pendingNotificationDownload = null
        if (track != null) startDownload(track)
    }

    val requestNotifications: () -> Unit = {
        if (
            Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(
                context,
                NOTIF_PERMISSION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            notifLauncher.launch(NOTIF_PERMISSION)
        }
    }

    LaunchedEffect(state.offline.hasAccess) {
        val pending = pendingDownload

        if (state.offline.hasAccess && pending != null) {
            pendingDownload = null
            showStorageDialog = false
            vm.download(pending)
        }
    }

    val downloadTrack: (RexTrack) -> Unit = { track ->
        val needsNotificationPermission = Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(
                context,
                NOTIF_PERMISSION
            ) != PackageManager.PERMISSION_GRANTED

        if (needsNotificationPermission) {
            pendingNotificationDownload = track
            notifLauncher.launch(NOTIF_PERMISSION)
        } else {
            startDownload(track)
        }
    }

    BackHandler(enabled = showPlayer) {
        showPlayer = false
    }

    BackHandler(enabled = !showPlayer && tab == TAB_PLAYLIST && selectedPlaylistName != null) {
        selectedPlaylistName = null
    }

    BackHandler(enabled = !showPlayer && tab != TAB_HOME && !(tab == TAB_PLAYLIST && selectedPlaylistName != null)) {
        tab = TAB_HOME
        selectedPlaylistName = null
    }

    val actions = RexActions(
        playSearch = {
            requestNotifications()
            vm.playFromSearch(it)
            showPlayer = true
        },

        playHistory = {
            requestNotifications()
            vm.playFromHistory(it)
            showPlayer = true
        },

        playMain = {
            requestNotifications()
            vm.playFromMain(it)
            showPlayer = true
        },

        playPopular = {
            requestNotifications()
            vm.playPopular(it)
            showPlayer = true
        },

        playOffline = {
            requestNotifications()
            vm.playFromOffline(it)
            showPlayer = true
        },

        playOfflineAll = { shuffle ->
            requestNotifications()
            vm.playOfflineAll(shuffle)
            showPlayer = true
        },

        playMix = {
            requestNotifications()
            vm.playMix()
            showPlayer = true
        },

        playQueue = {
            requestNotifications()
            vm.playFromQueue(it)
            showPlayer = true
        },

        addQueue = {
            vm.addToQueue(it)
        },

        removeQueue = {
            vm.removeFromQueue(it)
        },

        moveQueueTop = {
            vm.moveQueueToTop(it)
        },

        clearQueue = {
            vm.clearQueue()
        },

        download = downloadTrack,

        cancelDownload = {
            vm.cancelDownload(it)
        },

        delete = {
            deleteTarget = it
        },

        deleteAll = {
            showDeleteAll = true
        },

        clearHistory = {
            vm.clearHistory()
        },

        requestAccess = {
            pendingDownload = null
            showStorageDialog = true
        },

        setOffline = { on ->
            if (on != state.offline.enabled) {
                query = ""
                vm.setOfflineMode(on)
            }
        },

        toggleAutoplay = {
            vm.toggleAutoplay()
        },

        searchArtist = { name ->
            tab = TAB_SEARCH
            query = name

            if (!state.offline.enabled) {
                vm.search(name)
            }
        },

        goLibrary = {
            tab = TAB_LIBRARY
            libTab = 0
        },

        goPlaylists = {
            tab = TAB_PLAYLIST
        },

        openMenu = {
            menuTarget = it
        },

        openPlaylistPicker = {
            playlistTarget = it
        },

        playPlaylist = {
            requestNotifications()
            vm.playPlaylist(it)
            showPlayer = true
        },

        playPlaylistTrack = { playlistName, trackId ->
            requestNotifications()
            vm.playPlaylist(playlistName, trackId)
            showPlayer = true
        },

        createPlaylist = { name ->
            vm.createPlaylist(name)
        },

        deletePlaylist = {
            vm.deletePlaylist(it)
        },

        dismissError = {
            vm.dismissError()
        }
    )

    CompositionLocalProvider(
        LocalRexMusicOpenProfile provides {
            localProfile = RexLocalProfileBridge.read()
            showLocalProfile = true
        }
    ) {
    Box(Modifier.fillMaxSize()) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            // Base the layout on this window's available bounds, not device configuration.
            // This also responds when the app is resized or placed in split-screen.
            val useLandscapeLayout = maxWidth > maxHeight

            val screenContent: @Composable (Modifier) -> Unit = { contentModifier ->
                AnimatedContent(
                    targetState = tab,
                    modifier = contentModifier,
                    transitionSpec = {
                        (fadeIn(tween(230, easing = FastOutSlowInEasing)) +
                            slideInHorizontally(tween(280, easing = FastOutSlowInEasing)) { it / 22 }) togetherWith
                            (fadeOut(tween(150)) +
                                slideOutHorizontally(tween(190, easing = FastOutSlowInEasing)) { -it / 30 })
                    },
                    label = "RexMusicTabTransition"
                ) { visibleTab ->
                    Box(Modifier.fillMaxSize()) {
                        when (visibleTab) {
                            TAB_HOME -> {
                                HomeTab(
                                    state,
                                    actions,
                                    homeList,
                                    onBack
                                )
                            }

                            TAB_SYNC -> {
                                RexMusicSyncScreen(onBack = { tab = TAB_HOME })
                            }

                            TAB_SEARCH -> {
                                SearchTab(
                                    state = state,
                                    query = query,
                                    typing = typing && !state.offline.enabled,
                                    onQueryChange = {
                                        query = it
                                        if (!state.offline.enabled) {
                                            if (it.isBlank()) {
                                                vm.clearSearch()
                                            } else {
                                                vm.search(it)
                                            }
                                        }
                                    },
                                    onClear = {
                                        query = ""
                                        vm.clearSearch()
                                    },
                                    actions = actions,
                                    listState = searchList
                                )
                            }

                            TAB_PLAYLIST -> {
                                PlaylistTab(
                                    state = state,
                                    actions = actions,
                                    listState = playlistList,
                                    selectedPlaylistName = selectedPlaylistName,
                                    onOpenPlaylist = { selectedPlaylistName = it },
                                    onBackFromDetail = { selectedPlaylistName = null },
                                    onRequestDeletePlaylist = { pendingDeletePlaylistName = it }
                                )
                            }

                            else -> {
                                LibraryTab(
                                    state,
                                    libTab,
                                    { libTab = it },
                                    actions,
                                    libraryList
                                )
                            }
                        }

                        ErrorCard(
                            error = state.error,
                            onDismiss = actions.dismissError,
                            modifier = Modifier.align(Alignment.TopCenter)
                        )
                    }
                }
            }

            val miniPlayerContent: @Composable () -> Unit = {
                val now = state.nowPlaying
                AnimatedVisibility(
                    visible = now != null,
                    enter = fadeIn(tween(200, easing = FastOutSlowInEasing)) +
                        slideInVertically(tween(250, easing = FastOutSlowInEasing)) { it / 3 },
                    exit = fadeOut(tween(130)) +
                        slideOutVertically(tween(180, easing = FastOutSlowInEasing)) { it / 4 }
                ) {
                    if (now != null) {
                        MiniPlayer(
                            track = now,
                            state = state,
                            positionState = positionState,
                            onToggle = { vm.togglePlay() },
                            onNext = { vm.next() },
                            onOpen = { showPlayer = true }
                        )
                    }
                }
            }

            val snackbarContent: @Composable () -> Unit = {
                SnackbarHost(snackbar)
            }

            if (useLandscapeLayout) {
                RexMusicScreenLandscape(
                    selectedTab = tab,
                    onTabSelected = { nextTab ->
                        tab = nextTab
                        if (nextTab != TAB_PLAYLIST) selectedPlaylistName = null
                    },
                    miniPlayer = miniPlayerContent,
                    snackbarHost = snackbarContent,
                    content = screenContent
                )
            } else {
                RexMusicScreenPortrait(
                    selectedTab = tab,
                    onTabSelected = { nextTab ->
                        tab = nextTab
                        if (nextTab != TAB_PLAYLIST) selectedPlaylistName = null
                    },
                    miniPlayer = miniPlayerContent,
                    snackbarHost = snackbarContent,
                    content = screenContent
                )
            }
        }

        AnimatedVisibility(
            visible = showPlayer,
            enter = slideInVertically(tween(300, easing = FastOutSlowInEasing)) { it / 12 } +
                scaleIn(
                    initialScale = 0.965f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioNoBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    )
                ) + fadeIn(tween(240, easing = FastOutSlowInEasing)),
            exit = slideOutVertically(tween(220, easing = FastOutSlowInEasing)) { it / 10 } +
                scaleOut(targetScale = 0.985f, animationSpec = tween(200)) +
                fadeOut(tween(160))
        ) {
            val track = state.nowPlaying

            if (track != null) {
                var accent by remember(track.cover) {
                    mutableStateOf<Color?>(null)
                }

                val dlKey = remember(track.title, track.artist) {
                    offlineKey(track)
                }

                MusicPlayerCard(
                    track = track,
                    state = state,
                    positionState = positionState,
                    isLiked = track.id in likedIds,
                    downloaded = state.offline.isDownloaded(
                        track,
                        dlKey
                    ),
                    downloadStatus = state.offline.downloads[dlKey],
                    accent = accent,
                    onAccentFound = {
                        accent = it
                    },

                    actions = PlayerActions(
                        onClose = {
                            showPlayer = false
                        },

                        onLyrics = {
                            vm.toggleLyrics()
                        },

                        onMore = {
                            menuTarget = MenuTarget(track, null)
                        },

                        onPlayPause = {
                            vm.togglePlay()
                        },

                        onNext = {
                            vm.next()
                        },

                        onPrev = {
                            vm.prev()
                        },

                        onOpenPrevious = {
                            vm.prev(force = true)
                        },

                        onSeek = {
                            vm.seekTo(it)
                        },

                        onToggleLike = {
                            if (track.id in likedIds) likedIds.remove(track.id)
                            else likedIds.add(track.id)
                        },

                        onRepeat = {
                            showRepeat = true
                        },

                        onAutoplay = {
                            vm.toggleAutoplay()
                        },

                        onQueue = {
                            showQueue = true
                        },

                        onDownload = {
                            downloadTrack(track)
                        },

                        onCancelDownload = {
                            vm.cancelDownload(track)
                        },

                        onDelete = {
                            deleteTarget = track
                        }
                    )
                )
            } else {
                PlayerPlaceholder(
                    state.loadingText,
                    state.error
                ) {
                    showPlayer = false
                }
            }
        }

        AnimatedVisibility(
            visible = state.lyricsVisible,
            enter = fadeIn(tween(240)),
            exit = fadeOut(tween(180))
        ) {
            RexLyricsOverlay(
                track = state.nowPlaying
                    ?: RexTrack(
                        id = "",
                        title = "",
                        artist = ""
                    ),
                lyrics = state.lyrics,
                loading = state.lyricsLoading,
                error = state.lyricsError,
                accent = null,
                positionState = positionState,
                durationMs = state.durationMs,
                isPlaying = state.isPlaying,
                onClose = {
                    vm.closeLyrics()
                },
                onSeekTo = { vm.seekToPosition(it) }
            )
        }
    }
    }

    if (showQueue) {
        QueueSheet(
            state,
            actions
        ) {
            showQueue = false
        }
    }

    if (showRepeat) {
        RepeatSheet(
            total = state.repeatTotal,
            left = state.repeatLeft,
            title = state.nowPlaying?.title.orEmpty(),
            onSet = {
                vm.setRepeat(it)
            },
            onDismiss = {
                showRepeat = false
            }
        )
    }

    menuTarget?.let { target ->
        TrackMenuSheet(
            target,
            state,
            actions
        ) {
            menuTarget = null
        }
    }

    playlistTarget?.let { target ->
        PlaylistPickerSheet(
            track = target,
            playlists = state.playlists,
            onPick = { name ->
                vm.addToPlaylist(name, target)
                playlistTarget = null
            },
            onCreate = {
                playlistTarget = target
                showCreatePlaylist = true
            },
            onDismiss = { playlistTarget = null }
        )
    }

    if (showCreatePlaylist) {
        var name by rememberSaveable { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showCreatePlaylist = false },
            title = { Text("Buat playlist") },
            text = {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    singleLine = true,
                    label = { Text("Nama playlist") },
                    placeholder = { Text("Favorit, Santai, Workout...") }
                )
            },
            confirmButton = {
                TextButton(
                    enabled = name.trim().isNotEmpty(),
                    onClick = {
                        vm.createPlaylist(name)
                        playlistTarget?.let { vm.addToPlaylist(name.trim(), it) }
                        playlistTarget = null
                        showCreatePlaylist = false
                    }
                ) { Text("Buat & simpan") }
            },
            dismissButton = { TextButton(onClick = { showCreatePlaylist = false }) { Text("Batal") } }
        )
    }

    pendingDeletePlaylistName?.let { playlistName ->
        AlertDialog(
            onDismissRequest = { pendingDeletePlaylistName = null },
            icon = { Icon(Icons.Default.DeleteOutline, contentDescription = null) },
            title = { Text("Hapus playlist?") },
            text = { Text("Playlist \"$playlistName\" akan dihapus. Lagu di dalamnya tidak akan dihapus dari koleksi atau unduhan.") },
            confirmButton = {
                TextButton(onClick = {
                    actions.deletePlaylist(playlistName)
                    if (selectedPlaylistName == playlistName) selectedPlaylistName = null
                    pendingDeletePlaylistName = null
                }) {
                    Text("Hapus", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeletePlaylistName = null }) { Text("Batal") }
            }
        )
    }

    if (showStorageDialog) {
        AlertDialog(
            onDismissRequest = {
                showStorageDialog = false
                pendingDownload = null
            },

            icon = {
                Icon(
                    Icons.Default.Folder,
                    contentDescription = null
                )
            },

            title = {
                Text("Penyimpanan aplikasi tidak tersedia")
            },

            text = {
                Text(
                    "Unduhan disimpan di ruang privat HansAps (${RexOfflineManager.DISPLAY_PATH}) dan tidak memerlukan izin storage. " +
                        "Periksa ruang penyimpanan internal perangkat, lalu coba lagi."
                )
            },

            confirmButton = {
                TextButton(
                    onClick = {
                        showStorageDialog = false
                        vm.refreshOffline()
                    }
                ) {
                    Text("Coba lagi")
                }
            },

            dismissButton = {
                TextButton(
                    onClick = {
                        showStorageDialog = false
                        pendingDownload = null
                    }
                ) {
                    Text("Nanti")
                }
            }
        )
    }

    deleteTarget?.let { target ->
        AlertDialog(
            onDismissRequest = {
                deleteTarget = null
            },

            icon = {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = null
                )
            },

            title = {
                Text("Hapus dari offline?")
            },

            text = {
                Text(
                    "\"${target.title}\" akan dihapus dari " +
                        "${RexOfflineManager.DISPLAY_PATH}/. " +
                        "Kamu tetap bisa memutarnya lagi saat online."
                )
            },

            confirmButton = {
                TextButton(
                    onClick = {
                        vm.deleteDownload(target)
                        deleteTarget = null
                    }
                ) {
                    Text(
                        "Hapus",
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },

            dismissButton = {
                TextButton(
                    onClick = {
                        deleteTarget = null
                    }
                ) {
                    Text("Batal")
                }
            }
        )
    }

    if (showDeleteAll) {
        AlertDialog(
            onDismissRequest = {
                showDeleteAll = false
            },

            icon = {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = null
                )
            },

            title = {
                Text("Hapus semua lagu offline?")
            },

            text = {
                Text(
                    "${state.offline.entries.size} lagu " +
                        "(${formatBytes(state.offline.totalBytes)}) " +
                        "akan dihapus dari perangkat."
                )
            },

            confirmButton = {
                TextButton(
                    onClick = {
                        vm.deleteAllDownloads()
                        showDeleteAll = false
                    }
                ) {
                    Text(
                        "Hapus semua",
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },

            dismissButton = {
                TextButton(
                    onClick = {
                        showDeleteAll = false
                    }
                ) {
                    Text("Batal")
                }
            }
        )
    }

    if (showLocalProfile) {
        RexLocalProfileSheet(
            profile = localProfile,
            onDismiss = { showLocalProfile = false },
            onViewFullProfile = {
                showLocalProfile = false
                onOpenLocalProfile()
            }
        )
    }
}
