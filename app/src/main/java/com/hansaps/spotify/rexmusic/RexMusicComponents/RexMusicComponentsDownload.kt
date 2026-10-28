package com.hansaps.spotify.rexmusic

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
internal fun __splitImpl_DownloadRow(status: DownloadStatus, onCancel: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val label = when (status.stage) {
        DownloadStage.Queued -> "Menunggu giliran..."
        DownloadStage.Resolving -> "Menyiapkan..."
        DownloadStage.Downloading ->
            if (status.percent >= 0) "Mengunduh ${status.percent}%" else "Mengunduh..."
        DownloadStage.FetchingLyrics -> "Mengunduh lirik..."
        DownloadStage.Finalizing -> "Menyimpan..."
    }
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(scheme.surfaceVariant)
            .padding(start = 10.dp, end = 4.dp, top = 8.dp, bottom = 10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CoverThumb(status.track.cover, 44.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    status.track.title, style = MaterialTheme.typography.titleSmall,
                    maxLines = 1, overflow = TextOverflow.Ellipsis
                )
                Text(label, style = MaterialTheme.typography.labelSmall, color = scheme.tertiary)
            }
            RexIconButton(
                Icons.Default.Close, "Batalkan unduhan", onCancel,
                size = 40.dp, iconSize = 20.dp, tint = scheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.height(8.dp))
        val bar = Modifier.fillMaxWidth().padding(end = 6.dp).clip(CircleShape)
        if (status.stage == DownloadStage.Downloading && status.percent >= 0) {
            LinearProgressIndicator(
                progress = status.percent / 100f, modifier = bar, color = scheme.tertiary
            )
        } else {
            LinearProgressIndicator(modifier = bar, color = scheme.tertiary)
        }
    }
}

@Composable
internal fun __splitImpl_DownloadRing(status: DownloadStatus, size: Dp = 22.dp, modifier: Modifier = Modifier) {
    val color = MaterialTheme.colorScheme.tertiary
    if (status.stage == DownloadStage.Downloading && status.percent >= 0) {
        CircularProgressIndicator(
            progress = status.percent / 100f,
            modifier = modifier.size(size), color = color, strokeWidth = 2.dp
        )
    } else {
        CircularProgressIndicator(modifier = modifier.size(size), color = color, strokeWidth = 2.dp)
    }
}
