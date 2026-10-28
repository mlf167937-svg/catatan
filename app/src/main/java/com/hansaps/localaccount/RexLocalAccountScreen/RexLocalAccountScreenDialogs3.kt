package com.hansaps.localaccount

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import java.io.File

@Composable
internal fun __splitImpl_RexProfileOverlays(ctl: RexProfileController) {
    if (ctl.showEdit) EditProfileSheet(ctl)

    if (ctl.showStoryCompose) {
        StoryComposeDialog(
            onDismiss = { ctl.showStoryCompose = false },
            onText = { text, g ->
                ctl.showStoryCompose = false
                ctl.postStory(text, g)
            },
            onVideo = {
                ctl.showStoryCompose = false
                ctl.onPickStoryVideo()
            }
        )
    }

    ctl.storyIndex?.let { idx ->
        StoryViewer(
            acct = ctl.acct,
            rev = ctl.rev,
            stories = ctl.stories,
            start = idx,
            onDelete = { ctl.deleteStory(it) },
            onClose = { ctl.storyIndex = null }
        )
    }

    ctl.feedIndex?.let { idx ->
        VideoFeed(
            acct = ctl.acct,
            rev = ctl.rev,
            files = ctl.videos,
            start = idx,
            onDelete = { ctl.deleteVideo(it) },
            onClose = { ctl.feedIndex = null }
        )
    }

    ctl.crop?.let { req ->
        CropDialog(
            req = req,
            aspect = if (req.target == CropTarget.AVATAR) 1f else BANNER_ASPECT,
            circle = req.target == CropTarget.AVATAR,
            title = if (req.target == CropTarget.AVATAR) "Crop foto profil" else "Crop banner",
            onCancel = {
                ctl.crop = null
                req.video?.file?.delete()
            },
            onConfirm = { bmp, s, ox, oy, st -> ctl.applyCrop(req, bmp, s, ox, oy, st) }
        )
    }

    ctl.busy?.let { msg ->
        Dialog(
            onDismissRequest = {},
            properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
        ) {
            Surface(shape = RoundedCornerShape(20.dp), tonalElevation = 6.dp) {
                Row(
                    Modifier.padding(22.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    CircularProgressIndicator(Modifier.size(28.dp), strokeWidth = 3.dp)
                    Text(msg)
                }
            }
        }
    }
}

@Composable
internal fun __splitImpl_VideoFeed(
    acct: RexLocalAccount,
    rev: Int,
    files: List<File>,
    start: Int,
    onDelete: (File) -> Unit,
    onClose: () -> Unit
) {
    if (files.isEmpty()) {
        LaunchedEffect(Unit) { onClose() }
        return
    }
    val pager = rememberPagerState(initialPage = start.coerceIn(0, files.lastIndex)) { files.size }

    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(Modifier.fillMaxSize().background(Color.Black)) {
            VerticalPager(pager, Modifier.fillMaxSize()) { page ->
                FeedItem(files[page], acct, rev, active = pager.currentPage == page)
            }
            Box(Modifier.fillMaxWidth().height(120.dp).background(Brush.verticalGradient(listOf(Color.Black.copy(.5f), Color.Transparent))))
            Row(
                Modifier.align(Alignment.TopCenter).statusBarsPadding().padding(horizontal = 14.dp, vertical = 12.dp).fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        "MEDIA LOKAL",
                        color = Color.White.copy(alpha = .72f),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "${pager.currentPage + 1} dari ${files.size}",
                        color = Color.White,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Surface(shape = CircleShape, color = Color.White.copy(alpha = .12f)) {
                    IconButton(onClick = { onDelete(files[pager.currentPage]) }) {
                        Icon(Icons.Default.Delete, null, tint = Color.White)
                    }
                }
                Spacer(Modifier.width(8.dp))
                Surface(shape = CircleShape, color = Color.White.copy(alpha = .12f)) {
                    IconButton(onClick = onClose) { Icon(Icons.Default.Close, null, tint = Color.White) }
                }
            }
        }
    }
}
