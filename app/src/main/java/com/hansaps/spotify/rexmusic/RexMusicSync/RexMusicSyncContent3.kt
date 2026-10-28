package com.hansaps.spotify.rexmusic

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.*

@Composable
internal fun __splitImpl_SyncMemberCard(
    member: SyncMember,
    state: SyncUiState,
    onVolumeChange: (Float) -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = scheme.surface)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(13.dp),
                    color = scheme.secondaryContainer
                ) {
                    androidx.compose.material3.Icon(
                        Icons.Default.MusicNote,
                        contentDescription = null,
                        modifier = Modifier.padding(10.dp),
                        tint = scheme.onSecondaryContainer
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(member.name, style = MaterialTheme.typography.titleMedium, maxLines = 1)
                    Text(
                        if (member.id == "host") "Host device" else "Connected · ${member.ip}",
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
                if (member.id == "host") {
                    Text("YOU", style = MaterialTheme.typography.labelSmall, color = scheme.primary)
                }
            }
            if (state.hosting && member.id != "host") {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("RexSync gain", style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(1f))
                    Text(
                        "${((state.volumes[member.id] ?: member.volume).coerceIn(0f, 1f) * 100).toInt()}%",
                        style = MaterialTheme.typography.labelLarge,
                        color = scheme.primary
                    )
                }
                Slider(
                    value = (state.volumes[member.id] ?: member.volume).coerceIn(0f, 1f),
                    onValueChange = onVolumeChange,
                    valueRange = 0f..1f
                )
                Text(
                    if (member.deviceVolume >= 0f) {
                        "Media volume: ${(member.deviceVolume * 100).toInt()}% · app gain is separate"
                    } else {
                        "Waiting for this device to report media volume…"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant
                )
            } else if (member.id != "host" && member.deviceVolume >= 0f) {
                Text(
                    "Media volume: ${(member.deviceVolume * 100).toInt()}%",
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
internal fun __splitImpl_SyncWaitingCard(connecting: Boolean, error: String?) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (error != null) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.Start
        ) {
            if (connecting) {
                CircularProgressIndicator(modifier = Modifier.size(28.dp), strokeWidth = 2.5.dp)
            } else {
                androidx.compose.material3.Icon(
                    Icons.Default.Groups,
                    contentDescription = null,
                    tint = if (error != null) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
            }
            Text(
                if (connecting) "Connecting to room…" else if (error != null) "Connection needs attention" else "Not connected to a room",
                style = MaterialTheme.typography.titleMedium,
                color = if (error != null) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurface
            )
            Text(
                if (connecting) "RexMusic is trying to establish the local Wi-Fi connection." else "Create a room on this device or enter a host IP to join an existing room.",
                style = MaterialTheme.typography.bodySmall,
                color = if (error != null) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
internal fun __splitImpl_SyncOnlineSearchCard(
    query: String,
    onQueryChange: (String) -> Unit,
    searching: Boolean,
    searchMessage: String,
    onSearch: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            SyncSectionHeading("Find music", "Search online tracks to download for the shared room")
            OutlinedTextField(
                value = query,
                onValueChange = onQueryChange,
                label = { Text("Judul lagu atau artis") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                enabled = !searching,
                trailingIcon = {
                    TextButton(
                        enabled = !searching && query.isNotBlank(),
                        onClick = onSearch
                    ) {
                        Text(if (searching) "…" else "CARI")
                    }
                }
            )
            if (searching) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    Text("Mencari lagu…", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if (searchMessage.isNotBlank()) {
                Text(
                    searchMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
internal fun __splitImpl_SyncOnlineResultCard(
    track: RexTrack,
    downloaded: Boolean,
    onDownload: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(17.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            androidx.compose.material3.Icon(
                Icons.Default.MusicNote,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(26.dp)
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(track.title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                Text(track.artist, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
            }
            TextButton(onClick = onDownload) {
                Text(if (downloaded) "TERSIMPAN" else "UNDUH")
            }
        }
    }
}

@Composable
internal fun __splitImpl_SyncSectionHeading(
    title: String,
    subtitle: String? = null,
    titleColor: androidx.compose.ui.graphics.Color? = null,
    subtitleColor: androidx.compose.ui.graphics.Color? = null
) {
    val scheme = MaterialTheme.colorScheme
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium, color = titleColor ?: scheme.primary)
        if (!subtitle.isNullOrBlank()) {
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = subtitleColor ?: scheme.onSurfaceVariant
            )
        }
    }
}
