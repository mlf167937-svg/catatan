package com.hansaps.spotify.rexmusic

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
internal fun __splitImpl_QueueRow(
    index: Int,
    track: RexTrack,
    onPlay: () -> Unit,
    onMoveTop: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier.fillMaxWidth().padding(horizontal = 8.dp)
            .clip(RoundedCornerShape(12.dp)).clickable(onClick = onPlay)
            .padding(start = 8.dp, end = 0.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            "${index + 1}", style = MaterialTheme.typography.labelLarge,
            color = scheme.onSurfaceVariant, textAlign = TextAlign.Center,
            modifier = Modifier.width(22.dp)
        )
        Spacer(Modifier.width(8.dp))
        CoverThumb(track.cover, 44.dp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                track.title, style = MaterialTheme.typography.titleSmall,
                maxLines = 1, overflow = TextOverflow.Ellipsis
            )
            Text(
                track.artist, style = MaterialTheme.typography.bodySmall,
                color = scheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis
            )
        }
        if (index > 0) {
            RexIconButton(
                Icons.Default.KeyboardArrowUp, "Naikkan ke paling atas", onMoveTop,
                size = 40.dp, iconSize = 22.dp, tint = scheme.onSurfaceVariant
            )
        } else {
            Spacer(Modifier.size(40.dp))
        }
        RexIconButton(
            Icons.Default.Close, "Hapus dari antrian", onRemove,
            size = 40.dp, iconSize = 20.dp, tint = scheme.onSurfaceVariant
        )
    }
}
