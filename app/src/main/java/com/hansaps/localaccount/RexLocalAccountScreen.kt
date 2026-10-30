// RexLocalAccountScreen.kt
package com.hansaps.localaccount

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaExtractor
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Environment
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.*
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.media3.common.util.UnstableApi
import kotlinx.coroutines.CoroutineScope
import java.io.File
import java.io.InputStream
import org.json.JSONObject

/* ═════════════════════ Konstanta & Model ═════════════════════ */

internal const val DAY_MS = 24L * 60 * 60 * 1000
internal const val MAX_CLIP_MS = 4000L
internal const val BANNER_ASPECT = 16f / 9f

internal object RexPaths {
    @Volatile private var appContext: Context? = null

    /** Initialize before profile reads/writes. App-private storage works on Android 10+ without broad storage permission. */
    fun initialize(context: Context) {
        appContext = context.applicationContext
    }

    private val storageBase: File
        get() = File(
            requireNotNull(appContext) { "RexPaths harus diinisialisasi dari Application Context terlebih dahulu" }.filesDir,
            "HansAps/.config"
        )

    /** Best-effort incremental import from the former public Downloads location. Old files are never deleted. */
    fun migrateLegacyIfNeeded() {
        val target = storageBase
        val legacy = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            "HansAps/.config"
        )
        if (!legacy.isDirectory || legacy.absolutePath == target.absolutePath) return
        runCatching {
            target.mkdirs()
            legacy.listFiles()?.forEach { old ->
                val destination = File(target, old.name)
                if (!destination.exists()) old.copyRecursively(destination, overwrite = false)
            }
            // The legacy JSON stored absolute paths under public Downloads. Re-point those paths
            // to the copied private assets so the existing avatar/banner remains visible.
            val configFile = File(target, "account.json")
            if (configFile.isFile) {
                runCatching {
                    val json = JSONObject(configFile.readText())
                    val accountDir = File(target, "account")
                    listOf(
                        "profilePicture" to "profile-pictures.png",
                        "profileVideo" to "profile-video.mp4",
                        "banner" to "banner.png",
                        "bannerVideo" to "banner.mp4"
                    ).forEach { (field, assetName) ->
                        val stored = File(accountDir, assetName)
                        if (stored.isFile && json.optString(field).isNotBlank()) {
                            json.put(field, stored.absolutePath)
                        }
                    }
                    configFile.writeText(json.toString(2))
                }
            }
        }
    }

    val base get() = storageBase
    val account get() = File(base, "account")
    val video get() = File(account, "video")
    val story get() = File(base, "story")
    val config get() = File(base, "account.json")
}

internal data class RexLocalAccount(
    val name: String = "HansAps User",
    val username: String = "@hansuser",
    val bio: String = "Halo! Aku menggunakan HansAps.",
    val bannerPath: String = "",
    val bannerVideo: String = "",
    val bannerScale: Float = 1f,
    val bannerX: Float = 0f,
    val bannerY: Float = 0f,
    val picturePath: String = "",
    val pictureVideo: String = "",
    val pictureScale: Float = 1f,
    val pictureX: Float = 0f,
    val pictureY: Float = 0f
)

internal data class RexStory(
    val file: File,
    val time: Long,
    val isVideo: Boolean,
    val text: String,
    val gradient: Int
)

internal enum class CropTarget { AVATAR, BANNER }

internal class VSrc(val uri: Uri?, val file: File?) {
    fun bind(r: MediaMetadataRetriever, c: Context) {
        return this.__splitImpl_bind(r, c)
    }

    fun bind(e: MediaExtractor, c: Context) {
        return this.__splitImpl_bind(e, c)
    }

    fun open(c: Context): InputStream? = this.__splitImpl_open(c)
}

internal class CropReq(
    val target: CropTarget,
    val bitmap: Bitmap,
    val video: VSrc?,
    val durationMs: Long,
    val live: Boolean
)

internal val storyGradients = listOf(
    listOf(Color(0xFFFF512F), Color(0xFFDD2476)),
    listOf(Color(0xFF4776E6), Color(0xFF8E54E9)),
    listOf(Color(0xFF11998E), Color(0xFF38EF7D)),
    listOf(Color(0xFFF7971E), Color(0xFFFFD200)),
    listOf(Color(0xFF232526), Color(0xFF414345)),
    listOf(Color(0xFFEE0979), Color(0xFFFF6A00))
)

/* ═════════════════════ File & Media Helpers ═════════════════════ */

internal fun hasStorageAccess(ctx: Context): Boolean = __splitImpl_hasStorageAccess(ctx)

internal fun readAccount(): RexLocalAccount? = __splitImpl_readAccount()

internal fun writeAccount(a: RexLocalAccount) {
        return __splitImpl_writeAccount(a)
    }

internal fun loadStories(): List<RexStory> {
        return __splitImpl_loadStories()
    }

internal fun loadVideos(): List<File> = __splitImpl_loadVideos()

internal fun decodeSampled(ctx: Context, uri: Uri, maxDim: Int = 2400): Bitmap? = __splitImpl_decodeSampled(ctx, uri, maxDim)

internal fun decodeFileSampled(path: String, maxDim: Int): Bitmap? = __splitImpl_decodeFileSampled(path, maxDim)

internal fun applyExif(ctx: Context, uri: Uri, b: Bitmap): Bitmap {
        return __splitImpl_applyExif(ctx, uri, b)
    }

internal fun rotate90(b: Bitmap): Bitmap {
        return __splitImpl_rotate90(b)
    }

internal fun downscale(b: Bitmap, maxDim: Int): Bitmap {
        return __splitImpl_downscale(b, maxDim)
    }

internal fun videoFrameAt(ctx: Context, src: VSrc, ms: Long): Bitmap? {
        return __splitImpl_videoFrameAt(ctx, src, ms)
    }

internal fun videoDurationOf(ctx: Context, src: VSrc): Long {
        return __splitImpl_videoDurationOf(ctx, src)
    }

internal fun extractMotionVideo(ctx: Context, uri: Uri, out: File): Boolean = __splitImpl_extractMotionVideo(ctx, uri, out)

internal fun trimVideo(ctx: Context, src: VSrc, startMs: Long, out: File): Boolean {
        return __splitImpl_trimVideo(ctx, src, startMs, out)
    }

internal fun buildCropReq(ctx: Context, uri: Uri, target: CropTarget): CropReq? {
        return __splitImpl_buildCropReq(ctx, uri, target)
    }

internal fun renderCrop(src: Bitmap, aspect: Float, outW: Int, s: Float, ox: Float, oy: Float): Bitmap {
        return __splitImpl_renderCrop(src, aspect, outW, s, ox, oy)
    }

internal fun secs(ms: Long) = String.format("%.1fs", ms / 1000f)

/* ═════════════════════ UI Utilities ═════════════════════ */

internal fun Modifier.pressable(onClick: () -> Unit): Modifier = this.__splitImpl_pressable(onClick)

@Composable
internal fun GlassCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
        return __splitImpl_GlassCard(modifier, content)
    }

@Composable
internal fun Reveal(visible: Boolean, delayMs: Int, content: @Composable () -> Unit) {
        return __splitImpl_Reveal(visible, delayMs, content)
    }

@Composable
internal fun Aurora(modifier: Modifier) {
        return __splitImpl_Aurora(modifier)
    }

@Composable
internal fun GlowBadge(text: String) {
        return __splitImpl_GlowBadge(text)
    }

/* ═════════════════════ Video Player ═════════════════════ */

@OptIn(UnstableApi::class)
@Composable
internal fun RexVideo(
    path: String,
    modifier: Modifier = Modifier,
    key: Any = path,
    loop: Boolean = true,
    muted: Boolean = true,
    playing: Boolean = true,
    fit: Boolean = false
) {
        return __splitImpl_RexVideo(path, modifier, key, loop, muted, playing, fit)
    }

/* ═════════════════════ Crop Dialog ═════════════════════ */

@Composable
internal fun CropDialog(
    req: CropReq,
    aspect: Float,
    circle: Boolean,
    title: String,
    onCancel: () -> Unit,
    onConfirm: (Bitmap, Float, Float, Float, Long) -> Unit
) {
        return __splitImpl_CropDialog(req, aspect, circle, title, onCancel, onConfirm)
    }

/* ═════════════════════ Profile Controller ═════════════════════ */

internal class RexProfileController(
    internal val context: Context,
    internal val scope: CoroutineScope
) {
    var acct by mutableStateOf(RexLocalAccount())
    var rev by mutableIntStateOf(0)
    var stories by mutableStateOf<List<RexStory>>(emptyList())
    var videos by mutableStateOf<List<File>>(emptyList())
    var tab by mutableIntStateOf(0)
    var visible by mutableStateOf(false)
    var showEdit by mutableStateOf(false)
    var showStoryCompose by mutableStateOf(false)
    var storyIndex by mutableStateOf<Int?>(null)
    var feedIndex by mutableStateOf<Int?>(null)
    var crop by mutableStateOf<CropReq?>(null)
    var busy by mutableStateOf<String?>(null)

    var onPickBanner: () -> Unit = {}
    var onPickAvatar: () -> Unit = {}
    var onPickStoryVideo: () -> Unit = {}
    var onPickVideo: () -> Unit = {}

    internal fun toast(m: String) = this.__splitImpl_toast(m)

    suspend fun load() {
        return this.__splitImpl_load()
    }

    fun handlePick(uri: Uri?, target: CropTarget) {
        return this.__splitImpl_handlePick(uri, target)
    }

    fun addStoryVideo(uri: Uri?) {
        return this.__splitImpl_addStoryVideo(uri)
    }

    fun addVideo(uri: Uri?) {
        return this.__splitImpl_addVideo(uri)
    }

    fun applyCrop(req: CropReq, bmp: Bitmap, s: Float, ox: Float, oy: Float, startMs: Long) {
        return this.__splitImpl_applyCrop(req, bmp, s, ox, oy, startMs)
    }

    fun clearBanner() {
        return this.__splitImpl_clearBanner()
    }

    fun clearAvatar() {
        return this.__splitImpl_clearAvatar()
    }

    fun saveProfile(n: String, u: String, b: String) {
        return this.__splitImpl_saveProfile(n, u, b)
    }

    fun postStory(text: String, gradient: Int) {
        return this.__splitImpl_postStory(text, gradient)
    }

    fun deleteStory(s: RexStory) {
        return this.__splitImpl_deleteStory(s)
    }

    fun deleteVideo(f: File) {
        return this.__splitImpl_deleteVideo(f)
    }
}

@Composable
internal fun rememberRexProfileController(): RexProfileController {
        return __splitImpl_rememberRexProfileController()
    }

/* ═════════════════════ Entry Point & Permission ═════════════════════ */

@Composable
fun RexLocalAccountScreen(padding: PaddingValues) {
        return __splitImpl_RexLocalAccountScreen(padding)
    }

@Composable
internal fun RexProfileRoot(padding: PaddingValues) {
        return __splitImpl_RexProfileRoot(padding)
    }

@Composable
internal fun RexProfileOverlays(ctl: RexProfileController) {
        return __splitImpl_RexProfileOverlays(ctl)
    }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun EditProfileSheet(ctl: RexProfileController) {
        return __splitImpl_EditProfileSheet(ctl)
    }

@Composable
internal fun PermissionGate(padding: PaddingValues, onAllow: () -> Unit) {
        return __splitImpl_PermissionGate(padding, onAllow)
    }

@Composable
internal fun PermissionStep(number: Int, text: String) {
        return __splitImpl_PermissionStep(number, text)
    }

@Composable
internal fun StatItem(value: Int, label: String, modifier: Modifier) {
        return __splitImpl_StatItem(value, label, modifier)
    }

@Composable
internal fun SegTabs(labels: List<String>, selected: Int, onSelect: (Int) -> Unit) {
        return __splitImpl_SegTabs(labels, selected, onSelect)
    }

@Composable
internal fun RexGrid(
    count: Int,
    columns: Int,
    cellAspect: Float,
    gap: Dp = 8.dp,
    cell: @Composable (Int, Modifier) -> Unit
) {
        return __splitImpl_RexGrid(count, columns, cellAspect, gap, cell)
    }

@Composable
internal fun AddTile(modifier: Modifier, title: String, sub: String, onClick: () -> Unit) {
        return __splitImpl_AddTile(modifier, title, sub, onClick)
    }

@Composable
internal fun MediaOption(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    sub: String,
    modifier: Modifier,
    onClick: () -> Unit
) {
        return __splitImpl_MediaOption(icon, title, sub, modifier, onClick)
    }

@Composable
internal fun BannerView(acct: RexLocalAccount, rev: Int, modifier: Modifier, parallax: () -> Float) {
        return __splitImpl_BannerView(acct, rev, modifier, parallax)
    }

@Composable
internal fun AvatarContent(acct: RexLocalAccount, rev: Int, animate: Boolean, modifier: Modifier = Modifier) {
        return __splitImpl_AvatarContent(acct, rev, animate, modifier)
    }

@Composable
internal fun StoryRingAvatar(
    acct: RexLocalAccount,
    rev: Int,
    storyCount: Int,
    dia: Dp,
    modifier: Modifier,
    animate: Boolean = true,
    onClick: () -> Unit,
    onAdd: (() -> Unit)? = null
) {
        return __splitImpl_StoryRingAvatar(acct, rev, storyCount, dia, modifier, animate, onClick, onAdd)
    }

@Composable
internal fun MediaThumb(file: File, modifier: Modifier) {
        return __splitImpl_MediaThumb(file, modifier)
    }

@Composable
internal fun ProfileTabs(ctl: RexProfileController, columns: Int, cellAspect: Float) {
        return __splitImpl_ProfileTabs(ctl, columns, cellAspect)
    }

@Composable
internal fun ProfileIdentity(acct: RexLocalAccount, centered: Boolean) {
        return __splitImpl_ProfileIdentity(acct, centered)
    }

@Composable
internal fun ProfileStats(ctl: RexProfileController) {
        return __splitImpl_ProfileStats(ctl)
    }

@Composable
internal fun ProfileActions(ctl: RexProfileController) {
        return __splitImpl_ProfileActions(ctl)
    }

@Composable
internal fun StoryTile(st: RexStory, acct: RexLocalAccount, rev: Int, modifier: Modifier, onClick: () -> Unit) {
        return __splitImpl_StoryTile(st, acct, rev, modifier, onClick)
    }

@Composable
internal fun StoryComposeDialog(onDismiss: () -> Unit, onText: (String, Int) -> Unit, onVideo: () -> Unit) {
        return __splitImpl_StoryComposeDialog(onDismiss, onText, onVideo)
    }

@Composable
internal fun StoryViewer(
    acct: RexLocalAccount,
    rev: Int,
    stories: List<RexStory>,
    start: Int,
    onDelete: (RexStory) -> Unit,
    onClose: () -> Unit
) {
        return __splitImpl_StoryViewer(acct, rev, stories, start, onDelete, onClose)
    }

@Composable
internal fun VideoFeed(
    acct: RexLocalAccount,
    rev: Int,
    files: List<File>,
    start: Int,
    onDelete: (File) -> Unit,
    onClose: () -> Unit
) {
        return __splitImpl_VideoFeed(acct, rev, files, start, onDelete, onClose)
    }

@Composable
internal fun FeedItem(file: File, acct: RexLocalAccount, rev: Int, active: Boolean) {
        return __splitImpl_FeedItem(file, acct, rev, active)
    }
