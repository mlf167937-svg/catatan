package com.hansaps.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowOutward
import androidx.compose.material.icons.rounded.Logout
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hansaps.core.AppAccount
import com.hansaps.core.HansColors
import com.hansaps.R
import com.hansaps.core.HansWordmark
import com.hansaps.core.InlineVideoBanner
import com.hansaps.core.RolePill
import com.hansaps.core.SoftCard
import com.hansaps.core.UserRole

@Composable
fun HomeScreen(
    account: AppAccount,
    onOpenSpotify: () -> Unit,
    onOpenYoutube: () -> Unit,
    onOpenDeveloper: () -> Unit,
    onOpenOwner: () -> Unit,
    onLogout: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().background(HansColors.Background).windowInsetsPadding(WindowInsets.safeDrawing).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(22.dp),
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            HansWordmark()
            TextButton(onClick = onLogout) {
                Icon(Icons.Rounded.Logout, contentDescription = "Keluar", tint = HansColors.Muted, modifier = Modifier.size(18.dp))
                Spacer(Modifier.size(5.dp))
                Text("Keluar", color = HansColors.Muted, fontSize = 12.sp)
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                Text("Halo, ${account.username}", color = HansColors.Text, fontSize = 28.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.8).sp)
                RolePill(account.role)
            }
            Text("Semua media favoritmu, tertata di satu ruang.", color = HansColors.Muted, fontSize = 14.sp, lineHeight = 20.sp)
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MetricCard(label = "SALDO", value = "Rp0", accent = HansColors.Lime, modifier = Modifier.weight(1f))
            MetricCard(label = "KREDIT", value = "0", accent = HansColors.Orange, modifier = Modifier.weight(1f))
            MetricCard(label = "BREACHED", value = "0", accent = HansColors.Coral, modifier = Modifier.weight(1f))
        }
        Text("Ringkasan lokal belum terhubung ke backend akun.", color = HansColors.Muted, fontSize = 10.sp)

        Box(Modifier.fillMaxWidth().height(190.dp).clip(RoundedCornerShape(24.dp))) {
            InlineVideoBanner(resourceId = R.raw.banner_vid, modifier = Modifier.fillMaxSize())
            Box(Modifier.fillMaxSize().background(Brush.horizontalGradient(listOf(Color(0xDD090B0E), Color(0x22202020), Color.Transparent))))
            Column(Modifier.align(Alignment.BottomStart).padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("HANSAPS / PERSONAL MEDIA", color = HansColors.Lime, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
                Text("Everything you\nwant to play.", color = Color.White, fontSize = 24.sp, lineHeight = 27.sp, fontWeight = FontWeight.Bold)
                Text("Music and video, in one space.", color = Color.White.copy(alpha = .76f), fontSize = 12.sp)
            }
            Surface(Modifier.align(Alignment.TopEnd).padding(14.dp), color = Color.Black.copy(alpha = 0.36f), shape = CircleShape) {
                Text("V1.0", modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Jelajahi", color = HansColors.Text, fontSize = 19.sp, fontWeight = FontWeight.SemiBold)
            FeatureCard(
                title = "HansAps Spotify",
                subtitle = "Playlist, pemutar, antrean, dan musik offline.",
                label = "MUSIC SPACE",
                tint = HansColors.Lime,
                icon = { Icon(Icons.Rounded.MusicNote, contentDescription = null, tint = HansColors.Lime, modifier = Modifier.size(23.dp)) },
                onClick = onOpenSpotify,
            )
            FeatureCard(
                title = "HansAps YouTube",
                subtitle = "Koleksi video lokal dan opsi kualitas provider.",
                label = "VIDEO SPACE",
                tint = HansColors.Coral,
                icon = { Icon(Icons.Rounded.Movie, contentDescription = null, tint = HansColors.Coral, modifier = Modifier.size(23.dp)) },
                onClick = onOpenYoutube,
            )
        }

        if (account.role != UserRole.USER) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Kontrol akses", color = HansColors.Text, fontSize = 19.sp, fontWeight = FontWeight.SemiBold)
                SoftCard {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(13.dp)) {
                        Box(Modifier.size(46.dp).clip(RoundedCornerShape(15.dp)).background(HansColors.SurfaceRaised), contentAlignment = Alignment.Center) {
                            Icon(Icons.Rounded.Shield, contentDescription = null, tint = if (account.role == UserRole.OWNER) HansColors.Coral else HansColors.Lime)
                        }
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(if (account.role == UserRole.OWNER) "Owner Console" else "Developer Console", color = HansColors.Text, fontWeight = FontWeight.SemiBold)
                            Text("Buat akun dan kelola role lokal.", color = HansColors.Muted, fontSize = 12.sp)
                        }
                        Button(
                            onClick = if (account.role == UserRole.OWNER) onOpenOwner else onOpenDeveloper,
                            shape = RoundedCornerShape(13.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = HansColors.SurfaceRaised, contentColor = HansColors.Text),
                        ) { Icon(Icons.Rounded.ArrowOutward, contentDescription = "Buka kontrol") }
                    }
                }
            }
        }

        Text("LOCAL-FIRST • YOUR LIBRARY • YOUR SPACE", color = HansColors.Muted, fontSize = 9.sp, letterSpacing = 1.4.sp, modifier = Modifier.align(Alignment.CenterHorizontally).padding(vertical = 4.dp))
    }
}

@Composable
private fun MetricCard(label: String, value: String, accent: Color, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        color = HansColors.Surface,
        shape = RoundedCornerShape(17.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, HansColors.Border),
    ) {
        Column(Modifier.padding(horizontal = 11.dp, vertical = 13.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(label, color = accent, fontSize = 8.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp)
            Text(value, color = HansColors.Text, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun FeatureCard(
    title: String,
    subtitle: String,
    label: String,
    tint: Color,
    icon: @Composable () -> Unit,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        color = HansColors.Surface,
        shape = RoundedCornerShape(22.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, HansColors.Border),
    ) {
        Row(Modifier.padding(17.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Box(Modifier.size(54.dp).clip(RoundedCornerShape(17.dp)).background(tint.copy(alpha = 0.10f)), contentAlignment = Alignment.Center) { icon() }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(label, color = tint, fontSize = 9.sp, letterSpacing = 1.25.sp, fontWeight = FontWeight.Bold)
                Text(title, color = HansColors.Text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                Text(subtitle, color = HansColors.Muted, fontSize = 12.sp, lineHeight = 17.sp)
            }
            Icon(Icons.Rounded.ArrowOutward, contentDescription = "Buka $title", tint = HansColors.Muted, modifier = Modifier.size(18.dp))
        }
    }
}
