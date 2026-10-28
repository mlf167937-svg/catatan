package com.hansaps.localaccount

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
internal fun __splitImpl_ProfileTabs(ctl: RexProfileController, columns: Int, cellAspect: Float) {
    val colors = MaterialTheme.colorScheme
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("Koleksi", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    if (ctl.tab == 0) "Video dan foto lokal" else "Story berlaku 24 jam",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant
                )
            }
            Surface(
                shape = CircleShape,
                color = colors.surfaceVariant.copy(alpha = .48f),
                border = BorderStroke(1.dp, colors.outlineVariant.copy(alpha = .35f))
            ) {
                Text(
                    if (ctl.tab == 0) "${ctl.videos.size} item" else "${ctl.stories.size} item",
                    Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.onSurfaceVariant
                )
            }
        }

        SegTabs(listOf("Video", "Story"), ctl.tab) { ctl.tab = it }

        AnimatedContent(
            targetState = ctl.tab,
            transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(120)) },
            label = "local-content-tab"
        ) { selectedTab ->
            if (selectedTab == 0) {
                if (ctl.videos.isEmpty()) {
                    Column(
                        Modifier.fillMaxWidth().padding(vertical = 30.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            Modifier.size(54.dp).clip(RoundedCornerShape(17.dp))
                                .background(colors.primary.copy(alpha = .10f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Movie, null, tint = colors.primary, modifier = Modifier.size(25.dp))
                        }
                        Text("Belum ada video", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        Text(
                            "Tambahkan video atau foto untuk mengisi koleksi ini.",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    RexGrid(ctl.videos.size, columns, cellAspect, gap = 5.dp) { i, m ->
                        val file = ctl.videos[i]
                        val isVideo = file.extension.lowercase() == "mp4"
                        val shape = RoundedCornerShape(12.dp)
                        Box(
                            m.pressable { ctl.feedIndex = i }
                                .clip(shape)
                                .border(1.dp, Color.White.copy(alpha = .08f), shape)
                        ) {
                            MediaThumb(file, Modifier.fillMaxSize())
                            Box(
                                Modifier.fillMaxSize().background(
                                    Brush.verticalGradient(
                                        colorStops = arrayOf(
                                            0f to Color.Black.copy(alpha = .05f),
                                            .56f to Color.Transparent,
                                            1f to Color.Black.copy(alpha = .52f)
                                        )
                                    )
                                )
                            )
                            Row(
                                Modifier.align(Alignment.BottomStart).padding(horizontal = 7.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    if (isVideo) Icons.Default.Movie else Icons.Default.Image,
                                    null,
                                    tint = Color.White,
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(
                                    if (isVideo) "VIDEO" else "FOTO",
                                    color = Color.White,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Clip
                                )
                            }
                        }
                    }
                }
            } else {
                RexGrid(ctl.stories.size + 1, columns, cellAspect, gap = 5.dp) { i, m ->
                    if (i == 0) AddTile(m, "Story baru", "teks · mp4") { ctl.showStoryCompose = true }
                    else StoryTile(ctl.stories[i - 1], ctl.acct, ctl.rev, m) { ctl.storyIndex = i - 1 }
                }
            }
        }
    }
}

@Composable
internal fun __splitImpl_SegTabs(labels: List<String>, selected: Int, onSelect: (Int) -> Unit) {
    val colors = MaterialTheme.colorScheme
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        labels.forEachIndexed { index, label ->
            val isSelected = index == selected
            val foreground by animateColorAsState(
                targetValue = if (isSelected) colors.onSurface else colors.onSurfaceVariant.copy(alpha = .72f),
                animationSpec = tween(180),
                label = "local-tab-text"
            )
            Column(
                Modifier.weight(1f).clickable { onSelect(index) },
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    label,
                    modifier = Modifier.padding(vertical = 10.dp),
                    color = foreground,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                )
                val lineColor by animateColorAsState(
                    targetValue = if (isSelected) colors.primary else colors.outlineVariant.copy(alpha = .38f),
                    animationSpec = tween(180),
                    label = "local-tab-indicator"
                )
                Box(Modifier.fillMaxWidth().height(if (isSelected) 2.dp else 1.dp).background(lineColor))
            }
        }
    }
}
