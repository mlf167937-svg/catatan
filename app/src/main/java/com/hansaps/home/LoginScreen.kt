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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hansaps.R
import com.hansaps.core.HansColors
import com.hansaps.core.HansWordmark
import com.hansaps.core.InlineVideoBanner

@Composable
fun LoginScreen(
    errorMessage: String?,
    onLogin: (String, String) -> Unit,
    onClearError: () -> Unit,
) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier.fillMaxSize().background(HansColors.Background).windowInsetsPadding(WindowInsets.safeDrawing),
    ) {
        Box(Modifier.fillMaxWidth().height(350.dp).background(Brush.verticalGradient(listOf(Color(0xFF1B2514), HansColors.Background))))
        Column(
            modifier = Modifier.fillMaxSize().imePadding().verticalScroll(rememberScrollState()).padding(horizontal = 22.dp, vertical = 26.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            HansWordmark()
            Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                Text("Ruang pribadimu.", color = HansColors.Text, fontSize = 30.sp, lineHeight = 36.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.8).sp)
                Text("Musik, video, dan koleksi dalam satu tempat.", color = HansColors.Muted, fontSize = 14.sp)
            }
            Box(Modifier.fillMaxWidth().height(134.dp).clip(RoundedCornerShape(22.dp))) {
                InlineVideoBanner(resourceId = R.raw.intro, modifier = Modifier.fillMaxSize())
                Box(Modifier.fillMaxSize().background(Brush.horizontalGradient(listOf(Color.Black.copy(alpha = 0.65f), Color.Transparent))))
                Column(Modifier.align(Alignment.BottomStart).padding(15.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text("HANSAPS V1", color = HansColors.Lime, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.8.sp)
                    Text("Built for your own rhythm.", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                }
            }
            Surface(color = HansColors.Surface, shape = RoundedCornerShape(24.dp), border = androidx.compose.foundation.BorderStroke(1.dp, HansColors.Border)) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(15.dp)) {
                    Text("Masuk", color = HansColors.Text, fontSize = 23.sp, fontWeight = FontWeight.Bold)
                    Text("Gunakan akun yang sudah dibuat oleh admin.", color = HansColors.Muted, fontSize = 13.sp)
                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it; if (errorMessage != null) onClearError() },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        label = { Text("Username") },
                        leadingIcon = { Icon(Icons.Rounded.Person, contentDescription = null) },
                        shape = RoundedCornerShape(15.dp),
                        colors = fieldColors(),
                    )
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it; if (errorMessage != null) onClearError() },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        label = { Text("Password") },
                        leadingIcon = { Icon(Icons.Rounded.Lock, contentDescription = null) },
                        trailingIcon = {
                            TextButton(onClick = { passwordVisible = !passwordVisible }) {
                                Text(if (passwordVisible) "Sembunyi" else "Lihat", fontSize = 11.sp, color = HansColors.Muted)
                            }
                        },
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        shape = RoundedCornerShape(15.dp),
                        colors = fieldColors(),
                    )
                    if (!errorMessage.isNullOrBlank()) {
                        Text(errorMessage, color = HansColors.Error, fontSize = 12.sp)
                    }
                    Button(
                        onClick = { onLogin(username, password) },
                        modifier = Modifier.fillMaxWidth().height(53.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = HansColors.Lime, contentColor = Color(0xFF15190D)),
                    ) {
                        Text("Masuk ke HansAps", fontWeight = FontWeight.Bold)
                        Spacer(Modifier.size(8.dp))
                        Icon(Icons.Rounded.ArrowForward, contentDescription = null)
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(6.dp).clip(CircleShape).background(HansColors.Lime))
                        Spacer(Modifier.size(8.dp))
                        Text("Tidak ada pendaftaran publik", color = HansColors.Muted, fontSize = 11.sp)
                    }
                }
            }
            Text("Local-first • Offline library • Role-based access", color = HansColors.Muted, fontSize = 11.sp, modifier = Modifier.align(Alignment.CenterHorizontally))
        }
    }
}

@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = HansColors.Lime,
    unfocusedBorderColor = HansColors.Border,
    focusedLabelColor = HansColors.Lime,
    unfocusedLabelColor = HansColors.Muted,
    focusedTextColor = HansColors.Text,
    unfocusedTextColor = HansColors.Text,
    cursorColor = HansColors.Lime,
    focusedLeadingIconColor = HansColors.Lime,
    unfocusedLeadingIconColor = HansColors.Muted,
)
