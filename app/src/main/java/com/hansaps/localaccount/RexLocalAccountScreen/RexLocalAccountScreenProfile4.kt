package com.hansaps.localaccount

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.*
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

internal fun RexProfileController.__splitImpl_saveProfile(n: String, u: String, b: String) {
        val a = acct.copy(
            name = n.trim().ifBlank { "HansAps User" },
            username = u.trim().ifBlank { "@hansuser" },
            bio = b.trim()
        )
        acct = a
        scope.launch(Dispatchers.IO) {
            try {
                writeAccount(a)
                withContext(Dispatchers.Main) { toast("Profil tersimpan") }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { toast("Gagal: ${e.message}") }
            }
        }
    }

internal fun RexProfileController.__splitImpl_postStory(text: String, gradient: Int) {
        scope.launch(Dispatchers.IO) {
            RexPaths.story.mkdirs()
            File(RexPaths.story, "${System.currentTimeMillis()}.json")
                .writeText(JSONObject().put("text", text).put("gradient", gradient).toString())
            val list = loadStories()
            withContext(Dispatchers.Main) { stories = list }
        }
    }

internal fun RexProfileController.__splitImpl_clearAvatar() {
        val a = acct.copy(picturePath = "", pictureVideo = "")
        acct = a
        rev++
        scope.launch(Dispatchers.IO) {
            File(RexPaths.account, "profile-pictures.png").delete()
            File(RexPaths.account, "profile-video.mp4").delete()
            writeAccount(a)
        }
    }

internal fun RexProfileController.__splitImpl_deleteStory(s: RexStory) {
        scope.launch(Dispatchers.IO) {
            s.file.delete()
            val list = loadStories()
            withContext(Dispatchers.Main) { stories = list }
        }
    }
