package com.hansaps.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import com.hansaps.core.AccountStore
import com.hansaps.core.AppAccount
import com.hansaps.core.HansColors
import com.hansaps.core.RolePill
import com.hansaps.core.SoftCard
import com.hansaps.core.UserRole

@Composable
fun AccountManagementScreen(
    account: AppAccount,
    pageTitle: String,
    accent: Color,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var role by remember { mutableStateOf(UserRole.USER) }
    var roleMenuExpanded by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var accountVersion by remember { mutableStateOf(0) }
    val accounts = remember(accountVersion) { AccountStore.listAccounts(context) }

    Column(
        modifier = Modifier.fillMaxSize().background(HansColors.Background).windowInsetsPadding(WindowInsets.safeDrawing).padding(horizontal = 20.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(17.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack) {
                Icon(Icons.Rounded.ArrowBack, contentDescription = "Kembali", tint = HansColors.Text)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(pageTitle, color = HansColors.Text, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Text("Akses lokal • ${account.username}", color = HansColors.Muted, fontSize = 12.sp)
            }
            RolePill(account.role)
        }

        SoftCard {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                Icon(Icons.Rounded.PersonAdd, contentDescription = null, tint = accent)
                Text("Buat akun baru", color = HansColors.Text, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = username,
                onValueChange = { username = it; message = null },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("Username baru") },
                shape = RoundedCornerShape(14.dp),
            )
            Spacer(Modifier.height(9.dp))
            OutlinedTextField(
                value = password,
                onValueChange = { password = it; message = null },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                label = { Text("Password baru (min. 8 karakter)") },
                shape = RoundedCornerShape(14.dp),
            )
            Spacer(Modifier.height(9.dp))
            Text("Role akun", color = HansColors.Muted, fontSize = 12.sp)
            Surface(
                onClick = { roleMenuExpanded = true },
                shape = RoundedCornerShape(14.dp),
                color = HansColors.SurfaceRaised,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(Modifier.padding(horizontal = 14.dp, vertical = 13.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(role.label, color = HansColors.Text, fontWeight = FontWeight.Medium)
                    Text("Ubah ▾", color = accent, fontSize = 12.sp)
                    DropdownMenu(expanded = roleMenuExpanded, onDismissRequest = { roleMenuExpanded = false }) {
                        UserRole.entries.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option.label) },
                                onClick = { role = option; roleMenuExpanded = false },
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(13.dp))
            Button(
                onClick = {
                    val result = AccountStore.createAccount(context, username, password, role)
                    message = result.fold(
                        onSuccess = {
                            username = ""
                            password = ""
                            accountVersion++
                            "Akun berhasil dibuat. Password tidak ditampilkan lagi."
                        },
                        onFailure = { it.message ?: "Gagal membuat akun." },
                    )
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(15.dp),
                colors = ButtonDefaults.buttonColors(containerColor = accent, contentColor = Color(0xFF11130D)),
            ) {
                Icon(Icons.Rounded.CheckCircle, contentDescription = null)
                Spacer(Modifier.size(8.dp))
                Text("Simpan akun", fontWeight = FontWeight.Bold)
            }
            message?.let {
                Spacer(Modifier.height(9.dp))
                Text(it, color = if (it.startsWith("Akun berhasil")) HansColors.Lime else HansColors.Error, fontSize = 12.sp)
            }
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Akun terdaftar", color = HansColors.Text, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            Text("${accounts.size} akun", color = HansColors.Muted, fontSize = 12.sp)
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(9.dp), modifier = Modifier.weight(1f)) {
            items(accounts, key = { it.username.lowercase() }) { item ->
                Surface(color = HansColors.Surface, shape = RoundedCornerShape(17.dp), border = androidx.compose.foundation.BorderStroke(1.dp, HansColors.Border)) {
                    Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text(item.username, color = HansColors.Text, fontWeight = FontWeight.SemiBold)
                            Text(if (item.username.equals(account.username, true)) "Akun aktif" else "Akun lokal", color = HansColors.Muted, fontSize = 11.sp)
                        }
                        RolePill(item.role)
                    }
                }
            }
        }
        Text("V1 menyimpan akun di perangkat ini. Jangan gunakan untuk sistem akun produksi tanpa server dan kontrol akses tepercaya.", color = HansColors.Muted, fontSize = 11.sp, lineHeight = 16.sp)
    }
}
