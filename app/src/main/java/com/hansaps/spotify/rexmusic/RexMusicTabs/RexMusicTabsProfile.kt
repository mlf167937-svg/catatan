package com.hansaps.spotify.rexmusic

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

internal fun LazyListScope.__splitImpl_historySection(state: RexMusicUiState, actions: RexActions) {
    if (state.history.isEmpty()) {
        item(key = "hist-empty") {
            EmptyState(
                Icons.Default.History, "Belum ada riwayat",
                "Lagu yang kamu putar akan muncul di sini."
            )
        }
        return
    }
    item(key = "hist-title") {
        SectionTitle(
            "Terakhir diputar",
            trailing = { TextButton(onClick = actions.clearHistory) { Text("Hapus") } }
        )
    }
    itemsIndexed(state.history, key = { _, t -> "hist-${t.id}" }) { idx, track ->
        TrackRowFor(track, state, actions) { actions.playHistory(idx) }
    }
}
