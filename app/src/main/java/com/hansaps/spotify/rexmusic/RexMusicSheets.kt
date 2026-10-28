package com.hansaps.spotify.rexmusic

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

//  Queue (dipakai sheet & tab Koleksi) 

fun LazyListScope.queueSection(
    state: RexMusicUiState,
    actions: RexActions,
    showNow: Boolean,
    afterPlay: () -> Unit = {}
) {
    item(key = "q-autoplay") { AutoplayCard(state, actions.toggleAutoplay) }

    val now = state.nowPlaying
    if (showNow && now != null) {
        item(key = "q-now-title") { SectionTitle("Sedang diputar") }
        item(key = "q-now") {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CoverThumb(
                    now.cover, 48.dp,
                    overlay = overlayFor(true, state.isPlaying, state.phase.isPlayerBusy)
                )
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        now.title, style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1, overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        now.artist, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1, overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }

    item(key = "q-title") {
        SectionTitle(
            if (state.userQueue.isEmpty()) "Berikutnya" else "Berikutnya  ${state.userQueue.size}",
            trailing = {
                if (state.userQueue.isNotEmpty()) {
                    TextButton(onClick = actions.clearQueue) { Text("Kosongkan") }
                }
            }
        )
    }
    if (state.userQueue.isEmpty()) {
        item(key = "q-empty") {
            Text(
                "Antrian kosong. Tambahkan lagu lewat menu  di daftar lagu." +
                    if (state.autoplay) " Setelah itu lagu dipilih otomatis oleh Autoplay pintar." else "",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )
        }
    } else {
        itemsIndexed(state.userQueue, key = { _, t -> "q-${t.id}" }) { idx, track ->
            QueueRow(
                index = idx, track = track,
                onPlay = { actions.playQueue(idx); afterPlay() },
                onMoveTop = { actions.moveQueueTop(idx) },
                onRemove = { actions.removeQueue(idx) }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QueueSheet(state: RexMusicUiState, actions: RexActions, onDismiss: () -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        LazyColumn(
            Modifier.fillMaxWidth().heightIn(max = 560.dp).navigationBarsPadding(),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            item(key = "sheet-title") {
                Text(
                    "Antrian", style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }
            queueSection(state, actions, showNow = true, afterPlay = onDismiss)
        }
    }
}

//  Repeat 

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RepeatSheet(
    total: Int,
    left: Int,
    title: String,
    onSet: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scheme = MaterialTheme.colorScheme
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = scheme.surface
    ) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 24.dp).navigationBarsPadding().padding(bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Ulangi lagu", style = MaterialTheme.typography.headlineSmall)
            Text(
                title, style = MaterialTheme.typography.bodyMedium, color = scheme.onSurfaceVariant,
                maxLines = 1, overflow = TextOverflow.Ellipsis
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                StepButton(Icons.Default.Remove, "Kurangi", total > 0) { onSet(total - 1) }
                Column(Modifier.width(132.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        if (total == 0) "Mati" else "$total",
                        style = MaterialTheme.typography.displaySmall,
                        color = if (total == 0) scheme.onSurface else scheme.primary
                    )
                    Text(
                        if (total == 0) "lagu lanjut seperti biasa" else "sisa $left kali lagi",
                        style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant
                    )
                }
                StepButton(Icons.Default.Add, "Tambah", total < 50) { onSet(total + 1) }
            }

            Slider(
                value = total.toFloat(),
                onValueChange = { onSet(it.roundToInt()) },
                valueRange = 0f..50f,
                steps = 49
            )

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(0, 2, 5, 10, 25, 50).forEach { n ->
                    PresetPill(
                        if (n == 0) "Mati" else "$n", total == n, Modifier.weight(1f)
                    ) { onSet(n) }
                }
            }

            Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                Text("Selesai")
            }
        }
    }
}

@Composable
private fun StepButton(icon: ImageVector, description: String, enabled: Boolean, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    RexIconButton(
        icon, description, onClick,
        size = 52.dp, iconSize = 24.dp, tint = scheme.onSurface,
        background = scheme.onSurface.copy(alpha = if (enabled) 0.10f else 0.04f),
        enabled = enabled
    )
}

@Composable
private fun PresetPill(label: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Box(
        modifier.height(40.dp).clip(CircleShape)
            .background(if (selected) scheme.primary else scheme.onSurface.copy(alpha = 0.08f))
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            label, style = MaterialTheme.typography.labelLarge, maxLines = 1,
            color = if (selected) scheme.onPrimary else scheme.onSurface,
            textAlign = TextAlign.Center
        )
    }
}

//  Track menu 

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistPickerSheet(
    track: RexTrack,
    playlists: List<RexPlaylist>,
    onPick: (String) -> Unit,
    onCreate: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState()
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            Modifier.fillMaxWidth().navigationBarsPadding().padding(bottom = 20.dp)
        ) {
            Text("Simpan ke playlist", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp))
            Text(
                track.title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 20.dp)
            )
            Spacer(Modifier.height(10.dp))
            MenuItem(Icons.Default.Add, "Buat playlist baru") { onCreate() }
            if (playlists.isEmpty()) {
                EmptyState(Icons.Default.QueueMusic, "Belum ada playlist", "Buat playlist untuk menyimpan lagu favoritmu.")
            } else {
                playlists.forEach { playlist ->
                    MenuItem(
                        Icons.Default.QueueMusic,
                        "${playlist.name} · ${playlist.tracks.size} lagu"
                    ) { onPick(playlist.name) }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackMenuSheet(
    target: MenuTarget,
    state: RexMusicUiState,
    actions: RexActions,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState()
    val scheme = MaterialTheme.colorScheme
    val track = target.track
    val key = offlineKey(track)
    val downloaded = state.offline.isDownloaded(track, key)
    val status = state.offline.downloads[key]
    val queued = state.userQueue.any { it.id == track.id }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = scheme.surface
    ) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(bottom = 16.dp)) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CoverThumb(track.cover, 56.dp)
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        track.title, style = MaterialTheme.typography.titleMedium,
                        maxLines = 1, overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        track.artist, style = MaterialTheme.typography.bodyMedium,
                        color = scheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Box(Modifier.fillMaxWidth().height(1.dp).background(scheme.onSurface.copy(alpha = 0.08f)))

            target.onPlay?.let { play ->
                MenuItem(Icons.Default.PlayArrow, "Putar sekarang") { play(); onDismiss() }
            }
            MenuItem(
                if (queued) Icons.Default.Check else Icons.Default.Add,
                if (queued) "Sudah di antrian" else "Tambah ke antrian",
                enabled = !queued
            ) { actions.addQueue(track); onDismiss() }

            MenuItem(Icons.Default.PlaylistAdd, "Simpan ke playlist") {
                actions.openPlaylistPicker(track)
                onDismiss()
            }

            when {
                downloaded -> MenuItem(
                    Icons.Default.Delete, "Hapus dari offline", tint = scheme.error
                ) { actions.delete(track); onDismiss() }
                status != null -> MenuItem(
                    Icons.Default.Close, "Batalkan unduhan"
                ) { actions.cancelDownload(track); onDismiss() }
                else -> MenuItem(
                    Icons.Default.Download, "Unduh untuk offline"
                ) { actions.download(track); onDismiss() }
            }
        }
    }
}

@Composable
private fun MenuItem(
    icon: ImageVector,
    label: String,
    enabled: Boolean = true,
    tint: Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit
) {
    val color = if (enabled) tint else tint.copy(alpha = 0.4f)
    Row(
        Modifier.fillMaxWidth()
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(24.dp))
        Spacer(Modifier.width(16.dp))
        Text(label, style = MaterialTheme.typography.bodyLarge, color = color)
    }
}
