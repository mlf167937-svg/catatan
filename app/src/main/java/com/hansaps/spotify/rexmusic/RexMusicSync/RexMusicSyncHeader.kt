package com.hansaps.spotify.rexmusic

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import kotlinx.coroutines.*

@Composable
internal fun __splitImpl_SyncOfflineHeader(count: Int, active: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            SyncSectionHeading("Your offline library", "$count lagu tersedia di perangkat")
        }
        if (active) {
            Text("TAP + TO QUEUE", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
internal fun __splitImpl_SyncMembersHeader(count: Int) {
    SyncSectionHeading("Connected members", "$count device${if (count == 1) "" else "s"} in this room")
}

@Composable
internal fun __splitImpl_SyncQueueHeader(count: Int) {
    SyncSectionHeading("Shared queue", "$count track${if (count == 1) "" else "s"}")
}
