package com.hansaps.spotify.rexmusic

import android.net.Uri
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.util.Base64
import java.io.ByteArrayOutputStream
import android.os.Environment
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import org.json.JSONObject
import java.io.File

/** Snapshot of the same Local Account JSON file used by HansAps. */
internal data class RexLocalProfileSnapshot(
    val name: String = "HansAps User",
    val username: String = "@hansuser",
    val bio: String = "Halo! Aku menggunakan HansAps.",
    val picturePath: String = "",
    val pictureVideo: String = "",
    val bannerPath: String = "",
    val bannerVideo: String = ""
)

internal object RexLocalProfileBridge {
    private fun configFile(): File = File(
        File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            "HansAps/.config"
        ),
        "account.json"
    )

    fun read(): RexLocalProfileSnapshot {
        val downloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val candidates = listOf(
            File(File(downloads, "HansAps/.config"), "account.json"),
            // Compatibility fallback when both APKs are installed on the same Android device.
            File(File(downloads, "RexAps/.config"), "account.json")
        )
        for (file in candidates) {
            val profile = runCatching {
                if (!file.isFile) return@runCatching null
                val json = JSONObject(file.readText())
                val name = json.optString("name", "").trim()
                if (name.isBlank() && file == candidates.first()) return@runCatching null
                RexLocalProfileSnapshot(
                    name = name.ifBlank { "HansAps User" },
                    username = json.optString("username", "@hansuser").trim().ifBlank { "@hansuser" },
                    bio = json.optString("bio", "").trim(),
                    picturePath = json.optString("profilePicture", "").trim(),
                    pictureVideo = json.optString("profileVideo", "").trim(),
                    bannerPath = json.optString("banner", "").trim(),
                    bannerVideo = json.optString("bannerVideo", "").trim()
                )
            }.getOrNull()
            if (profile != null) return profile
        }
        return RexLocalProfileSnapshot()
    }
}

/**
 * Encode small, device-portable previews of the local account avatar and banner for Sync.
 * Images are downscaled before Base64 encoding so the JSON control message stays bounded.
 * If the account uses a video instead of a still image, the first video frame is used.
 */
internal fun RexLocalProfileBridge.encodeProfileAssets(profile: RexLocalProfileSnapshot): Pair<String, String> =
    encodeAsset(profile.picturePath, profile.pictureVideo, 360) to
        encodeAsset(profile.bannerPath, profile.bannerVideo, 960)

private fun RexLocalProfileBridge.encodeAsset(stillPath: String, videoPath: String, maxDimension: Int): String = runCatching {
    val context = RexMusicSyncEngine.appContextRef ?: return@runCatching ""
    val video = stillPath.isBlank() && videoPath.isNotBlank()
    val source = if (video) videoPath else stillPath
    if (source.isBlank()) return@runCatching ""
    val bitmap = (if (video) {
        val retriever = MediaMetadataRetriever()
        try {
            val file = profileFile(source)
            if (file != null && file.isFile) retriever.setDataSource(file.absolutePath)
            else if (source.startsWith("content://")) retriever.setDataSource(context, Uri.parse(source))
            else return@runCatching ""
            retriever.getFrameAtTime(0L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
        } finally { runCatching { retriever.release() } }
    } else {
        val file = profileFile(source)
        if (file != null && file.isFile) BitmapFactory.decodeFile(file.absolutePath)
        else if (source.startsWith("content://")) context.contentResolver.openInputStream(Uri.parse(source))?.use { BitmapFactory.decodeStream(it) }
        else null
    }) ?: return@runCatching ""
    val longest = maxOf(bitmap.width, bitmap.height).coerceAtLeast(1)
    val scale = minOf(1f, maxDimension.toFloat() / longest.toFloat())
    val scaled = if (scale < 1f) Bitmap.createScaledBitmap(bitmap, (bitmap.width * scale).toInt().coerceAtLeast(1), (bitmap.height * scale).toInt().coerceAtLeast(1), true) else bitmap
    val bytes = ByteArrayOutputStream().use { output ->
        scaled.compress(Bitmap.CompressFormat.JPEG, 78, output)
        output.toByteArray()
    }
    if (scaled !== bitmap) scaled.recycle()
    bitmap.recycle()
    Base64.encodeToString(bytes, Base64.NO_WRAP)
}.getOrDefault("")

private fun profileFile(path: String): File? = runCatching {
    val uri = Uri.parse(path)
    when {
        uri.scheme == "file" -> uri.path?.let(::File)
        uri.scheme == null -> File(path)
        else -> null
    }
}.getOrNull()

/** Persist an incoming preview in app-private cache; remote absolute paths are never trusted. */
internal fun RexLocalProfileBridge.saveSyncedProfileAssets(id: String, pictureData: String, bannerData: String): Pair<String, String> {
    val context = RexMusicSyncEngine.appContextRef ?: return "" to ""
    val safeId = id.replace(Regex("[^A-Za-z0-9_-]"), "_").take(80).ifBlank { "member" }
    val folder = File(context.cacheDir, "syncProfiles/$safeId")
    return runCatching {
        folder.mkdirs()
        fun save(data: String, name: String): String {
            if (data.isBlank()) return ""
            val bytes = Base64.decode(data, Base64.DEFAULT)
            if (bytes.size > 2_500_000) return ""
            val file = File(folder, name)
            file.writeBytes(bytes)
            return file.absolutePath
        }
        save(pictureData, "avatar.jpg") to save(bannerData, "banner.jpg")
    }.getOrDefault("" to "")
}

/** One shared event per RexMusic composition; tab headers use this to open the profile sheet. */
internal val LocalRexMusicOpenProfile = staticCompositionLocalOf<() -> Unit> { { } }

@Composable
internal fun RexLocalProfilePhoto(
    profile: RexLocalProfileSnapshot,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    val model = remember(profile.picturePath) { profilePictureModel(profile.picturePath) }
    Box(
        modifier = modifier.clip(CircleShape).background(scheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        if (model != null) {
            AsyncImage(
                model = model,
                contentDescription = "Foto profil ${profile.name}",
                modifier = Modifier.fillMaxSize().clip(CircleShape),
                contentScale = ContentScale.Crop
            )
        } else {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = null,
                tint = scheme.onSurfaceVariant,
                modifier = Modifier.fillMaxSize(0.58f)
            )
        }
    }
}

private fun profilePictureModel(path: String): Any? {
    if (path.isBlank()) return null
    if (path.startsWith("content://") || path.startsWith("file://") ||
        path.startsWith("https://") || path.startsWith("http://")) return path
    val file = File(path)
    if (file.isFile) return file
    return if (path.startsWith("/")) null else runCatching { Uri.parse(path) }.getOrNull()
}

@Composable
internal fun RexMusicProfileAvatarButton(onClick: (() -> Unit)? = null) {
    val profile = rememberRexMusicProfile()
    val scheme = MaterialTheme.colorScheme
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.94f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "localProfileAvatarPress"
    )
    Box(
        Modifier
            .size(44.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(CircleShape)
            .background(scheme.surface.copy(alpha = 0.92f))
            .border(BorderStroke(1.dp, scheme.outlineVariant.copy(alpha = 0.85f)), CircleShape)
            .clickable(
                interactionSource = interaction,
                indication = null,
                role = Role.Button,
                onClick = onClick ?: LocalRexMusicOpenProfile.current
            )
            .padding(3.dp),
        contentAlignment = Alignment.Center
    ) {
        RexLocalProfilePhoto(profile, Modifier.fillMaxSize())
    }
}

@Composable
internal fun rememberRexMusicProfileSnapshotForSync(): RexLocalProfileSnapshot {
    val context = LocalContext.current
    var profile by remember(context) { mutableStateOf(RexLocalProfileBridge.read()) }
    LaunchedEffect(context) { profile = RexLocalProfileBridge.read() }
    return profile
}

@Composable
private fun rememberRexMusicProfile(): RexLocalProfileSnapshot {
    val context = LocalContext.current
    var profile by remember(context) { mutableStateOf(RexLocalProfileBridge.read()) }
    LaunchedEffect(context) { profile = RexLocalProfileBridge.read() }
    return profile
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun RexLocalProfileSheet(
    profile: RexLocalProfileSnapshot,
    onDismiss: () -> Unit,
    onViewFullProfile: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp),
        containerColor = scheme.surface,
        tonalElevation = 6.dp,
        dragHandle = {
            Surface(
                modifier = Modifier.padding(vertical = 10.dp).width(38.dp).height(4.dp),
                shape = CircleShape,
                color = scheme.onSurfaceVariant.copy(alpha = 0.30f)
            ) {}
        }
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .heightIn(min = 300.dp, max = 430.dp)
                .padding(horizontal = 24.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(15.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    Modifier
                        .size(82.dp)
                        .background(
                            Brush.linearGradient(listOf(scheme.primary.copy(alpha = 0.35f), scheme.tertiary.copy(alpha = 0.22f))),
                            CircleShape
                        )
                        .padding(3.dp)
                        .border(1.dp, scheme.outlineVariant, CircleShape)
                        .padding(3.dp)
                ) {
                    RexLocalProfilePhoto(profile, Modifier.fillMaxSize())
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text("LOCAL ACCOUNT", style = MaterialTheme.typography.labelSmall, color = scheme.primary, fontWeight = FontWeight.Bold)
                    Text(profile.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(profile.username, style = MaterialTheme.typography.bodyMedium, color = scheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            if (profile.bio.isNotBlank()) {
                Text(profile.bio, style = MaterialTheme.typography.bodyMedium, color = scheme.onSurfaceVariant, maxLines = 3, overflow = TextOverflow.Ellipsis)
            }
            Surface(
                shape = RoundedCornerShape(15.dp),
                color = scheme.surfaceVariant.copy(alpha = 0.72f),
                border = BorderStroke(1.dp, scheme.outlineVariant.copy(alpha = 0.58f))
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 11.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = scheme.primary, modifier = Modifier.size(19.dp))
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                        Text("Profil lokal", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                        Text("Dikelola dari RexLocal Account", style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
                    }
                }
            }
            Button(
                onClick = onViewFullProfile,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(17.dp)
            ) {
                Text("Lihat Full Profile", fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.width(9.dp))
                Icon(Icons.Default.ArrowForward, contentDescription = null, modifier = Modifier.size(19.dp))
            }
        }
    }
}
