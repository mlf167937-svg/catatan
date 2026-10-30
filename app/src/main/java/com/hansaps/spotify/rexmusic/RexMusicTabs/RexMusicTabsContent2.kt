package com.hansaps.spotify.rexmusic

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
internal fun __splitImpl_SearchField(
    query: String,
    onChange: (String) -> Unit,
    onClear: () -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    val focus = LocalFocusManager.current
    TextField(
        value = query,
        onValueChange = onChange,
        singleLine = true,
        placeholder = { Text(placeholder) },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Cari") },
        trailingIcon = {
            if (query.isNotEmpty()) {
                RexIconButton(
                    Icons.Default.Clear, "Hapus pencarian", onClear,
                    size = 40.dp, iconSize = 20.dp, tint = scheme.onSurfaceVariant
                )
            }
        },
        shape = RoundedCornerShape(14.dp),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = scheme.surfaceVariant,
            unfocusedContainerColor = scheme.surfaceVariant,
            disabledContainerColor = scheme.surfaceVariant,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            disabledIndicatorColor = Color.Transparent,
            cursorColor = scheme.primary
        ),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { focus.clearFocus() }),
        modifier = modifier.fillMaxWidth()
    )
}

@Composable
internal fun __splitImpl_PermissionCard(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
            .background(scheme.errorContainer.copy(alpha = 0.7f))
            .clickable(onClick = onClick).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.Folder, contentDescription = null, tint = scheme.onErrorContainer)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                "Penyimpanan aplikasi tidak tersedia", style = MaterialTheme.typography.titleSmall,
                color = scheme.onErrorContainer
            )
            Text(
                "Unduhan offline disimpan di ${RexOfflineManager.DISPLAY_PATH}. Periksa ruang penyimpanan internal.",
                style = MaterialTheme.typography.bodySmall, color = scheme.onErrorContainer
            )
        }
        Spacer(Modifier.width(8.dp))
        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = scheme.onErrorContainer)
    }
}

@Composable
internal fun __splitImpl_ExploreTile(name: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val c = rexTileColor(name)
    Box(
        modifier.height(88.dp).clip(RoundedCornerShape(12.dp))
            .background(Brush.linearGradient(listOf(c, lerp(c, Color.Black, 0.35f))))
            .clickable(onClick = onClick)
    ) {
        Text(
            name, style = MaterialTheme.typography.titleMedium, color = Color.White,
            maxLines = 2, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.align(Alignment.TopStart).padding(start = 14.dp, top = 12.dp, end = 48.dp)
        )
        Icon(
            Icons.Default.MusicNote, contentDescription = null,
            tint = Color.White.copy(alpha = 0.35f),
            modifier = Modifier.align(Alignment.BottomEnd).offset(x = 8.dp, y = 8.dp)
                .size(56.dp).rotate(20f)
        )
    }
}

internal fun LazyListScope.__splitImpl_resultsSection(state: RexMusicUiState, actions: RexActions, typing: Boolean) {
    val results = state.searchResults
    when {
        results.isEmpty() && (state.loading || typing) ->
            item(key = "sr-skeleton") { SkeletonList() }
        results.isEmpty() && state.error == null ->
            item(key = "sr-empty") {
                EmptyState(
                    Icons.Default.Search, "Lagu tidak ditemukan",
                    "Coba kata kunci lain, misalnya judul lagu atau nama artis."
                )
            }
        results.isEmpty() -> Unit
        else -> {
            item(key = "sr-title") { SectionTitle("Hasil pencarian") }
            itemsIndexed(results, key = { _, t -> "sr-${t.id}" }) { idx, track ->
                TrackRowFor(track, state, actions) { actions.playSearch(idx) }
            }
        }
    }
}

@Composable
internal fun __splitImpl_HeroButton(
    label: String,
    icon: ImageVector,
    filled: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bg = if (filled) Color.White else Color.White.copy(alpha = 0.2f)
    val fg = if (filled) RexPlayerBase else Color.White
    Row(
        modifier.height(44.dp)
            .graphicsLayer { alpha = if (enabled) 1f else 0.5f }
            .clip(CircleShape).background(bg)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = fg, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(6.dp))
        Text(label, color = fg, style = MaterialTheme.typography.labelLarge, maxLines = 1)
    }
}

@Composable
internal fun __splitImpl_LibraryChip(
    label: String,
    count: Int,
    selected: Boolean,
    onClick: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme

    Box(
        Modifier
            .height(36.dp)
            .clip(CircleShape)
            .background(
                if (selected) scheme.primary
                else scheme.surfaceVariant
            )
            .clickable(
                role = Role.Tab,
                onClick = onClick
            )
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = if (count > 0) "$label $count" else label,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) scheme.onPrimary
            else scheme.onSurface,
            maxLines = 1
        )
    }
}

internal fun LazyListScope.__splitImpl_exploreSection(state: RexMusicUiState, actions: RexActions) {
    val artists = artistPool(state, 20)
    item(key = "explore-title") { SectionTitle("Jelajahi artis") }
    items(artists.chunked(2), key = { it.first() }) { pair ->
        Row(
            Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            pair.forEach { name ->
                ExploreTile(name, { actions.searchArtist(name) }, Modifier.weight(1f))
            }
            if (pair.size == 1) Spacer(Modifier.weight(1f))
        }
    }
}
