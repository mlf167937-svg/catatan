package com.hansaps.localaccount

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Videocam
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
internal fun __splitImpl_StoryComposeDialog(onDismiss: () -> Unit, onText: (String, Int) -> Unit, onVideo: () -> Unit) {
    var text by remember { mutableStateOf("") }
    var g by remember { mutableIntStateOf(0) }
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(28.dp),
        title = {
            Text("Buat story", fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleLarge)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(Brush.linearGradient(storyGradients[g])),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text.ifBlank { "Tulis sesuatu…" },
                        color = Color.White.copy(alpha = if (text.isBlank()) .7f else 1f),
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(16.dp),
                        maxLines = 4,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                OutlinedTextField(
                    value = text,
                    onValueChange = { if (it.length <= 200) text = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Teks story") },
                    supportingText = { Text("${text.length}/200") },
                    shape = RoundedCornerShape(16.dp)
                )
                Text(
                    "Warna latar",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    storyGradients.forEachIndexed { i, cols ->
                        val selected = i == g
                        Box(
                            Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Brush.linearGradient(cols))
                                .border(if (selected) 2.dp else 0.dp, Color.White, CircleShape)
                                .clickable { g = i },
                            contentAlignment = Alignment.Center
                        ) {
                            if (selected) {
                                Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
                OutlinedButton(
                    onClick = onVideo,
                    Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Default.Videocam, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Pilih video .mp4")
                }
            }
        },
        confirmButton = {
            Button(
                enabled = text.isNotBlank(),
                onClick = { onText(text.trim(), g) },
                shape = RoundedCornerShape(14.dp)
            ) { Text("Posting teks") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun __splitImpl_EditProfileSheet(ctl: RexProfileController) {
    val acct = ctl.acct
    var n by remember { mutableStateOf(acct.name) }
    var u by remember { mutableStateOf(acct.username) }
    var b by remember { mutableStateOf(acct.bio) }

    ModalBottomSheet(onDismissRequest = { ctl.showEdit = false }) {
        Column(
            Modifier
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column {
                Text("Edit profil", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
                Text(
                    "Perbarui foto, banner, dan identitas kamu",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MediaOption(Icons.Default.AddAPhoto, "Banner", "Foto • video 4 dtk • live", Modifier.weight(1f)) {
                    ctl.showEdit = false
                    ctl.onPickBanner()
                }
                MediaOption(Icons.Default.AccountCircle, "Foto profil", "Foto • video 4 dtk • live", Modifier.weight(1f)) {
                    ctl.showEdit = false
                    ctl.onPickAvatar()
                }
            }

            OutlinedTextField(
                value = n, onValueChange = { n = it },
                label = { Text("Nama") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(16.dp)
            )
            OutlinedTextField(
                value = u, onValueChange = { u = it },
                label = { Text("Username") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(16.dp)
            )
            OutlinedTextField(
                value = b, onValueChange = { b = it },
                label = { Text("Bio") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                maxLines = 5,
                shape = RoundedCornerShape(16.dp)
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (acct.bannerPath.isNotBlank() || acct.bannerVideo.isNotBlank()) {
                    TextButton(onClick = { ctl.clearBanner() }) { Text("Hapus banner") }
                }
                if (acct.picturePath.isNotBlank()) {
                    TextButton(onClick = { ctl.clearAvatar() }) { Text("Hapus foto profil") }
                }
            }

            Button(
                onClick = {
                    ctl.saveProfile(n, u, b)
                    ctl.showEdit = false
                },
                Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(16.dp)
            ) { Text("Simpan profil") }
        }
    }
}
