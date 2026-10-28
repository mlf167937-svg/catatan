package com.hansaps.spotify.rexmusic.player

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import com.hansaps.spotify.rexmusic.*

@Composable
internal fun __splitImpl_BottomActions(
    state: RexMusicUiState,
    downloaded: Boolean,
    status: DownloadStatus?,
    actions: PlayerActions
) {
    val queueCount = state.userQueue.size
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        when {
            downloaded -> ActionPill(Icons.Default.DownloadDone, "Tersimpan", true, actions.onDelete)
            status != null -> ActionPill(
                Icons.Default.Close,
                if (status.stage == DownloadStage.Downloading && status.percent >= 0)
                    "Mengunduh ${status.percent}%" else "Menyiapkan...",
                true, actions.onCancelDownload
            )
            else -> ActionPill(Icons.Default.Download, "Unduh", false, actions.onDownload)
        }
        ActionPill(
            Icons.AutoMirrored.Filled.QueueMusic,
            if (queueCount > 0) "Antrian $queueCount" else "Antrian",
            queueCount > 0, actions.onQueue
        )
    }
}

@Composable
internal fun __splitImpl_ActionPill(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    active: Boolean,
    onClick: () -> Unit
) {
    val accent = MaterialTheme.colorScheme.tertiary
    val fg = if (active) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onBackground
    Row(
        Modifier.height(40.dp).clip(CircleShape)
            .background(if (active) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.12f))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(icon, contentDescription = null, tint = fg, modifier = Modifier.size(18.dp))
        Text(label, color = fg, style = MaterialTheme.typography.labelLarge, maxLines = 1)
    }
}
