package com.hansaps.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.ArrowOutward
import androidx.compose.material.icons.rounded.Logout
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.text.style.TextOverflow
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
    onOpenLocalAccount: () -> Unit,
    onOpenDeveloper: () -> Unit,
    onOpenOwner: () -> Unit,
    onLogout: () -> Unit,
) {
    Scaffold(
        containerColor = HansColors.Background,
        bottomBar = {
            HansMainBottomNavigation(
                selected = MainNavTab.HOME,
                onHome = {},
                onSpotify = onOpenSpotify,
                onYoutube = onOpenYoutube,
                onAccount = onOpenLocalAccount,
            )
        },
    ) { scaffoldPadding ->
        BoxWithConstraints(
            modifier = Modifier.fillMaxSize().padding(scaffoldPadding),
        ) {
            val wideLayout = maxWidth >= 820.dp
            Column(
                modifier = Modifier
                    .widthIn(max = 1180.dp)
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = if (wideLayout) 30.dp else 18.dp, vertical = 18.dp),
                verticalArrangement = Arrangement.spacedBy(if (wideLayout) 26.dp else 21.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    HansWordmark()
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                        Surface(
                            color = HansColors.SurfaceRaised,
                            shape = CircleShape,
                            border = androidx.compose.foundation.BorderStroke(1.dp, HansColors.Border),
                        ) {
                            Text(
                                account.username.trim().firstOrNull()?.uppercase() ?: "H",
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                color = HansColors.Lime,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                            )
                        }
                        TextButton(onClick = onLogout) {
                            Icon(Icons.Rounded.Logout, contentDescription = "Keluar", tint = HansColors.Muted, modifier = Modifier.size(17.dp))
                            Spacer(Modifier.size(5.dp))
                            Text("Keluar", color = HansColors.Muted, fontSize = 12.sp)
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(18.dp),
                    verticalAlignment = Alignment.Bottom,
                ) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Surface(
                            color = HansColors.Lime.copy(alpha = .10f),
                            shape = RoundedCornerShape(50),
                            border = androidx.compose.foundation.BorderStroke(1.dp, HansColors.Lime.copy(alpha = .24f)),
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(7.dp),
                            ) {
                                Box(Modifier.size(6.dp).clip(CircleShape).background(HansColors.Lime))
                                Text("PERSONAL MEDIA SPACE", color = HansColors.Lime, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.1.sp)
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                "Halo, ${account.username}",
                                color = HansColors.Text,
                                fontSize = if (wideLayout) 31.sp else 27.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = (-.9).sp,
                                modifier = Modifier.weight(1f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            RolePill(account.role)
                        }
                        Text(
                            "Musik, video, dan koleksi offline dalam satu ruang yang personal.",
                            color = HansColors.Muted,
                            fontSize = 13.sp,
                            lineHeight = 19.sp,
                        )
                    }
                    if (wideLayout) {
                        Surface(
                            color = HansColors.Surface,
                            shape = RoundedCornerShape(17.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, HansColors.Border),
                        ) {
                            Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("HANSAPS", color = HansColors.Lime, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
                                Text("Your space. Your sound.", color = HansColors.Text, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }

                // Existing local metric values are retained, now presented as a deliberate dashboard row.
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    MetricCard(label = "SALDO", value = "Rp0", accent = HansColors.Lime, modifier = Modifier.weight(1f))
                    MetricCard(label = "KREDIT", value = "0", accent = HansColors.Orange, modifier = Modifier.weight(1f))
                    MetricCard(label = "BREACHED", value = "0", accent = HansColors.Coral, modifier = Modifier.weight(1f))
                }
                Text("Ringkasan akun bersifat lokal dan belum terhubung ke backend finansial.", color = HansColors.Muted, fontSize = 10.sp)

                Surface(
                    onClick = onOpenYoutube,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(26.dp),
                    color = HansColors.Surface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, HansColors.Border),
                ) {
                    Box(Modifier.fillMaxWidth().height(if (wideLayout) 250.dp else 220.dp).clip(RoundedCornerShape(26.dp))) {
                        InlineVideoBanner(resourceId = R.raw.banner_vid, modifier = Modifier.fillMaxSize())
                        Box(
                            Modifier.fillMaxSize().background(
                                Brush.horizontalGradient(
                                    listOf(Color(0xF0080A0E), Color(0xA8080A0E), Color(0x18080A0E)),
                                    startX = 0f,
                                    endX = if (wideLayout) 1450f else 850f,
                                ),
                            ),
                        )
                        Box(
                            Modifier.align(Alignment.TopEnd).padding(15.dp).clip(CircleShape)
                                .background(Color.Black.copy(alpha = .32f))
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                        ) {
                            Text("HANSAPS  /  01", color = Color.White.copy(alpha = .90f), fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                        }
                        Column(
                            Modifier.align(Alignment.BottomStart).padding(if (wideLayout) 25.dp else 19.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text("ONE HOME FOR YOUR MEDIA", color = HansColors.Lime, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.6.sp)
                            Text(
                                if (wideLayout) "Press play.\nStay in your flow." else "Press play. Stay\nin your flow.",
                                color = Color.White,
                                fontSize = if (wideLayout) 30.sp else 25.sp,
                                lineHeight = if (wideLayout) 34.sp else 29.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = (-.65).sp,
                            )
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Surface(color = HansColors.Lime, shape = RoundedCornerShape(50)) {
                                    Row(
                                        Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    ) {
                                        Icon(Icons.Rounded.PlayArrow, contentDescription = null, tint = HansColors.Background, modifier = Modifier.size(15.dp))
                                        Text("Jelajahi YouTube", color = HansColors.Background, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                                Text("Video & musik", color = Color.White.copy(alpha = .78f), fontSize = 11.sp)
                            }
                        }
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Ruang favorit", color = HansColors.Text, fontSize = 20.sp, fontWeight = FontWeight.Bold, letterSpacing = (-.35).sp)
                            Text("Pilih pengalaman yang ingin kamu buka.", color = HansColors.Muted, fontSize = 12.sp)
                        }
                        Text("EXPLORE", color = HansColors.Lime, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.3.sp)
                    }
                    if (wideLayout) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            FeatureCard(
                                title = "Local Account",
                                subtitle = "Foto profil, banner, bio, dan profil lokal untuk Sync.",
                                label = "YOUR PROFILE",
                                tint = HansColors.Lime,
                                icon = { Icon(Icons.Rounded.AccountCircle, contentDescription = null, tint = HansColors.Lime, modifier = Modifier.size(23.dp)) },
                                onClick = onOpenLocalAccount,
                                modifier = Modifier.weight(1f),
                            )
                            FeatureCard(
                                title = "HansAps Spotify",
                                subtitle = "Playlist, pemutar, antrean, dan musik offline.",
                                label = "MUSIC SPACE",
                                tint = HansColors.Lime,
                                icon = { Icon(Icons.Rounded.MusicNote, contentDescription = null, tint = HansColors.Lime, modifier = Modifier.size(23.dp)) },
                                onClick = onOpenSpotify,
                                modifier = Modifier.weight(1f),
                            )
                            FeatureCard(
                                title = "HansAps YouTube",
                                subtitle = "Video online, pilihan resolusi, dan koleksi offline.",
                                label = "VIDEO SPACE",
                                tint = HansColors.Coral,
                                icon = { Icon(Icons.Rounded.Movie, contentDescription = null, tint = HansColors.Coral, modifier = Modifier.size(23.dp)) },
                                onClick = onOpenYoutube,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    } else {
                        FeatureCard(
                            title = "Local Account",
                            subtitle = "Foto profil, banner, bio, dan profil lokal untuk Sync.",
                            label = "YOUR PROFILE",
                            tint = HansColors.Lime,
                            icon = { Icon(Icons.Rounded.AccountCircle, contentDescription = null, tint = HansColors.Lime, modifier = Modifier.size(23.dp)) },
                            onClick = onOpenLocalAccount,
                        )
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
                            subtitle = "Video online, pilihan resolusi, dan koleksi offline.",
                            label = "VIDEO SPACE",
                            tint = HansColors.Coral,
                            icon = { Icon(Icons.Rounded.Movie, contentDescription = null, tint = HansColors.Coral, modifier = Modifier.size(23.dp)) },
                            onClick = onOpenYoutube,
                        )
                    }
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

                Text(
                    "LOCAL-FIRST  •  YOUR LIBRARY  •  YOUR SPACE",
                    color = HansColors.Muted,
                    fontSize = 9.sp,
                    letterSpacing = 1.35.sp,
                    modifier = Modifier.align(Alignment.CenterHorizontally).padding(vertical = 4.dp),
                )
            }
        }
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
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
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
