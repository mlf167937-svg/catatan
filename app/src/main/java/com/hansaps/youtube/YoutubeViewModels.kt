package com.hansaps.youtube

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hansaps.core.DownloadStorage
import com.hansaps.core.LocalVideo
import com.hansaps.youtube.Engine.YoutubeDownloadManager
import com.hansaps.youtube.Engine.YoutubeDownloadOption
import com.hansaps.youtube.Engine.AuthorizedVideoSource
import org.json.JSONObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class ParsedYoutubeApiResponse(val title: String, val videoId: String, val thumbnailUrl: String?)

class YoutubeViewModel : ViewModel() {
    private val mutableVideos = MutableStateFlow<List<LocalVideo>>(emptyList())
    val localVideos: StateFlow<List<LocalVideo>> = mutableVideos
    private val mutableOptions = MutableStateFlow<List<YoutubeDownloadOption>>(emptyList())
    val qualityOptions: StateFlow<List<YoutubeDownloadOption>> = mutableOptions
    private val mutableMessage = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = mutableMessage

    fun refresh(context: Context) {
        viewModelScope.launch {
            mutableVideos.value = withContext(Dispatchers.IO) { DownloadStorage.listLocalVideos(context.applicationContext) }
        }
    }

    fun importVideo(context: Context, uri: Uri, title: String? = null) {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) { DownloadStorage.importVideo(context.applicationContext, uri, title) }
            result.onSuccess {
                refresh(context)
                mutableMessage.value = "Video disimpan ke Download/HansAps/Youtube"
            }.onFailure { mutableMessage.value = it.message ?: "Import video gagal." }
        }
    }

    /** The UI can feed the raw JSON returned by a trusted provider here. */
    fun acceptProviderResponse(json: String): ParsedYoutubeApiResponse? {
        return runCatching {
            val root = JSONObject(json)
            val options = YoutubeDownloadManager.parseOptionsFromApiResponse(json)
            mutableOptions.value = options
            mutableMessage.value = if (options.isEmpty()) "API tidak mengembalikan opsi unduh yang diizinkan." else "${options.size} kualitas tersedia."
            ParsedYoutubeApiResponse(
                title = root.optString("title", "").ifBlank { root.optString("name", "") },
                videoId = root.optString("videoId", root.optString("id", "")),
                thumbnailUrl = root.optString("thumbnailUrl", "").ifBlank { root.optString("thumbnail", "") }.ifBlank { null },
            )
        }.getOrElse {
            mutableOptions.value = emptyList()
            mutableMessage.value = "Respons API tidak valid: ${it.message}"
            null
        }
    }

    fun downloadOption(context: Context, title: String, videoId: String, thumbnailUrl: String?, option: YoutubeDownloadOption) {
        viewModelScope.launch {
            val result = YoutubeDownloadManager.download(
                context.applicationContext,
                AuthorizedVideoSource(
                    title = title.ifBlank { "HansAps Video" },
                    videoId = videoId,
                    directUrl = option.directUrl,
                    quality = option.quality,
                    thumbnailUrl = thumbnailUrl,
                ),
            )
            result.onSuccess {
                refresh(context)
                mutableMessage.value = "Video disimpan (${option.quality}) ke Download/HansAps/Youtube"
            }.onFailure { mutableMessage.value = it.message ?: "Download gagal." }
        }
    }

    fun consumeMessage() { mutableMessage.value = null }
}
