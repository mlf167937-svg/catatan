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

internal fun RexProfileController.__splitImpl_deleteVideo(f: File) {
        scope.launch(Dispatchers.IO) {
            f.delete()
            val list = loadVideos()
            withContext(Dispatchers.Main) { videos = list }
        }
    }
