package com.hansaps.spotify.rexmusic

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
internal fun __splitImpl_MixHeroCard(
    favorites: List<String>,
    popularCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val subtitle = when {
        favorites.isNotEmpty() -> "Berdasarkan ${favorites.joinToString(", ")}"
        popularCount > 0 -> "Acak dari $popularCount lagu populer"
        else -> "Putar beberapa lagu, Mix akan belajar seleramu"
    }
    val source = remember { MutableInteractionSource() }
    val scale = rememberPressScale(source, 0.98f)
    Box(
        modifier.fillMaxWidth()
            .graphicsLayer { scaleX = scale.value; scaleY = scale.value }
            .clip(RoundedCornerShape(20.dp))
            .background(RexHeroBrush)
            .clickable(interactionSource = source, indication = LocalIndication.current, onClick = onClick)
    ) {
        Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        Icons.Default.AutoAwesome, contentDescription = null,
                        tint = Color.White, modifier = Modifier.size(16.dp)
                    )
                    Text(
                        "MIX UNTUKMU", style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.85f), letterSpacing = 1.5.sp
                    )
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    "Putar Mix Pintar", style = MaterialTheme.typography.headlineSmall,
                    color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    subtitle, style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.88f),
                    maxLines = 2, overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.width(16.dp))
            Box(
                Modifier.size(56.dp).clip(CircleShape).background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.PlayArrow, contentDescription = "Putar Mix",
                    tint = RexPlayerBase, modifier = Modifier.size(32.dp)
                )
            }
        }
    }
}

internal fun LazyListScope.__splitImpl_offlineSection(
    state: RexMusicUiState,
    actions: RexActions,
    query: String,
    full: Boolean
) {
    val offline = state.offline
    val q = query.trim()
    val shown = if (q.isEmpty()) offline.entries else offline.entries.filter {
        it.track.title.contains(q, ignoreCase = true) || it.track.artist.contains(q, ignoreCase = true)
    }

    if (full) {
        item(key = "off-hero") {
            OfflineHeroCard(
                count = offline.entries.size,
                bytes = offline.totalBytes,
                enabled = offline.entries.isNotEmpty(),
                onPlayAll = { actions.playOfflineAll(false) },
                onShuffle = { actions.playOfflineAll(true) },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }
        if (!offline.hasAccess) {
            item(key = "off-permission") {
                PermissionCard(actions.requestAccess, Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
            }
        }
    }

    downloadsSection(offline.downloads, actions.cancelDownload)

    if (shown.isEmpty()) {
        item(key = "off-empty") {
            EmptyState(
                Icons.Default.CloudOff,
                if (q.isNotEmpty()) "Tidak ada lagu yang cocok" else "Belum ada lagu offline",
                if (q.isNotEmpty()) "Coba kata kunci lain."
                else "Pindah ke mode Online, lalu buka menu  pada lagu dan pilih Unduh."
            )
        }
    } else {
        item(key = "off-title") {
            SectionTitle(if (full) "Lagu offline  ${shown.size}" else "Hasil offline  ${shown.size}")
        }
        items(shown, key = { "o-${it.key}" }) { entry ->
            TrackRowFor(
                entry.track, state, actions,
                subtitle = "${entry.track.artist}  ${formatBytes(entry.sizeBytes)}"
            ) { actions.playOffline(entry.track) }
        }
        if (full) {
            item(key = "off-delete-all") {
                TextButton(
                    onClick = actions.deleteAll,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Hapus semua unduhan")
                }
            }
        }
    }
}

@Composable
internal fun __splitImpl_OfflineHeroCard(
    count: Int,
    bytes: Long,
    enabled: Boolean,
    onPlayAll: () -> Unit,
    onShuffle: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp))
            .background(RexOfflineBrush).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    Icons.Default.OfflinePin, contentDescription = null,
                    tint = Color.White, modifier = Modifier.size(16.dp)
                )
                Text(
                    "MODE OFFLINE", style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.85f), letterSpacing = 1.5.sp
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                if (count == 0) "Belum ada unduhan" else "$count lagu tersimpan",
                style = MaterialTheme.typography.headlineSmall, color = Color.White,
                maxLines = 1, overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(2.dp))
            Text(
                "${formatBytes(bytes)}  ${RexOfflineManager.DISPLAY_PATH}",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.88f),
                maxLines = 1, overflow = TextOverflow.Ellipsis
            )
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            HeroButton("Putar semua", Icons.Default.PlayArrow, true, enabled, onPlayAll, Modifier.weight(1f))
            HeroButton("Acak", Icons.Default.Shuffle, false, enabled, onShuffle, Modifier.weight(1f))
        }
    }
}
