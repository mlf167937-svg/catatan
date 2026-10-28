package com.hansaps.spotify.rexmusic.player

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hansaps.spotify.rexmusic.*

@Composable
internal fun __splitImpl_PlayerTopBar(
    state: RexMusicUiState,
    track: RexTrack,
    onClose: () -> Unit,
    onLyrics: () -> Unit,
    onMore: () -> Unit
) {
    val busy = state.phase.isPlayerBusy

    Row(
        Modifier
            .fillMaxWidth()
            .height(56.dp)
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RexIconButton(
            Icons.Default.KeyboardArrowDown,
            "Tutup player",
            onClose,
            size = 48.dp,
            iconSize = 32.dp,
            tint = MaterialTheme.colorScheme.onBackground
        )

        Column(
            Modifier
                .weight(1f)
                .padding(horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                when {
                    busy -> CircularProgressIndicator(
                        strokeWidth = 1.5.dp,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.size(16.dp)
                    )

                    state.isPlaying -> EqualizerBars(
                        color = MaterialTheme.colorScheme.onBackground,
                        barWidth = 3.dp
                    )

                    else -> Unit
                }

                Text(
                    statusLabel(state),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                    letterSpacing = 1.5.sp
                )
            }

            Text(
                track.album,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }

        RexIconButton(
            Icons.Default.Subtitles,
            "Tampilkan lirik",
            onLyrics,
            size = 48.dp,
            tint = MaterialTheme.colorScheme.onBackground
        )

        RexIconButton(
            Icons.Default.MoreVert,
            "Opsi lagu",
            onMore,
            size = 48.dp,
            tint = MaterialTheme.colorScheme.onBackground
        )
    }
}
