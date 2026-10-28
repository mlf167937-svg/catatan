package com.hansaps.spotify.rexmusic

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
internal fun __splitImpl_PlaylistEmptyState(onCreate: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 24.dp)
            .clip(RoundedCornerShape(24.dp)).background(scheme.surfaceVariant).padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            Modifier.size(76.dp).clip(RoundedCornerShape(24.dp)).background(RexHeroBrush),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.QueueMusic, contentDescription = null, tint = Color.White, modifier = Modifier.size(36.dp))
        }
        Text("Mulai koleksi musikmu", style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
        Text(
            "Buat playlist untuk mengelompokkan lagu sesuai mood. Sampul playlist akan terbentuk otomatis dari thumbnail lagu di dalamnya.",
            style = MaterialTheme.typography.bodyMedium,
            color = scheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Button(onClick = onCreate, shape = RoundedCornerShape(14.dp)) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(Modifier.width(6.dp))
            Text("Buat playlist pertama")
        }
    }
}

@Composable
internal fun __splitImpl_PlaylistArtworkTile(track: RexTrack?, modifier: Modifier = Modifier) {
    if (track == null) {
        Box(
            modifier.clip(RoundedCornerShape(5.dp)).background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.MusicNote, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
        }
    } else {
        CoverImage(
            track.cover,
            modifier,
            shape = RoundedCornerShape(5.dp),
            contentDescription = "Sampul ${track.title}"
        )
    }
}
