package com.hansaps.youtube

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hansaps.core.DownloadStorage
import com.hansaps.core.LocalVideo
import com.hansaps.core.NexrayApi
import com.hansaps.core.NexrayYoutubeVideo
import com.hansaps.youtube.Engine.AuthorizedVideoSource
import com.hansaps.youtube.Engine.YoutubeDownloadManager
import com.hansaps.youtube.Engine.YoutubeDownloadOption
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class YoutubeViewModel : ViewModel() {
    private val mutableVideos = MutableStateFlow<List<LocalVideo>>(emptyList())
    val localVideos: StateFlow<List<LocalVideo>> = mutableVideos
    private val mutableResults = MutableStateFlow<List<NexrayYoutubeVideo>>(emptyList())
    val searchResults: StateFlow<List<NexrayYoutubeVideo>> = mutableResults
    private val mutableSelectedVideo = MutableStateFlow<NexrayYoutubeVideo?>(null)
    val selectedVideo: StateFlow<NexrayYoutubeVideo?> = mutableSelectedVideo
    private val mutableOptions = MutableStateFlow<List<YoutubeDownloadOption>>(emptyList())
    val qualityOptions: StateFlow<List<YoutubeDownloadOption>> = mutableOptions
    private val mutableSearching = MutableStateFlow(false)
    val searching: StateFlow<Boolean> = mutableSearching
    private val mutablePreparing = MutableStateFlow(false)
    val preparing: StateFlow<Boolean> = mutablePreparing
    private val mutableMessage = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = mutableMessage

    fun refresh(context: Context) {
        viewModelScope.launch {
            mutableVideos.value = withContext(Dispatchers.IO) {
                DownloadStorage.listLocalVideos(context.applicationContext)
            }
        }
    }

    fun search(query: String) {
        val clean = query.trim()
        if (clean.isBlank()) {
            mutableMessage.value = "Masukkan judul video atau nama channel."
            return
        }
        viewModelScope.launch {
            mutableSearching.value = true
            mutableMessage.value = null
            mutableSelectedVideo.value = null
            mutableOptions.value = emptyList()
            try {
                mutableResults.value = NexrayApi.searchYoutube(clean)
                mutableMessage.value = if (mutableResults.value.isEmpty()) "Video tidak ditemukan." else "${mutableResults.value.size} video ditemukan dari Nexray."
            } catch (error: Throwable) {
                mutableResults.value = emptyList()
                mutableMessage.value = error.message ?: "Pencarian YouTube melalui Nexray gagal."
            } finally {
                mutableSearching.value = false
            }
        }
    }

    fun prepareVideo(video: NexrayYoutubeVideo) {
        viewModelScope.launch {
            mutableSelectedVideo.value = video
            mutableOptions.value = emptyList()
            mutablePreparing.value = true
            mutableMessage.value = "Meminta opsi kualitas dari Nexray…"
            try {
                val formats = NexrayApi.resolveYoutubeMedia(video.url)
                val withAudio = formats.filter { it.hasAudio }
                val selectable = if (withAudio.isNotEmpty()) withAudio else formats
                mutableOptions.value = YoutubeDownloadManager.toOptions(selectable)
                mutableMessage.value = when {
                    selectable.isEmpty() -> "Nexray tidak mengembalikan format video yang bisa diputar."
                    withAudio.isEmpty() -> "Format tersedia, tetapi tidak ada format dengan audio gabungan. Pemutaran dan download offline penuh mungkin tidak tersedia untuk video ini."
                    else -> "${withAudio.size} kualitas dengan audio tersedia."
                }
            } catch (error: Throwable) {
                mutableMessage.value = error.message ?: "Gagal mengambil opsi kualitas dari Nexray."
            } finally {
                mutablePreparing.value = false
            }
        }
    }

    fun downloadOption(context: Context, video: NexrayYoutubeVideo, option: YoutubeDownloadOption) {
        if (!option.hasAudio) {
            mutableMessage.value = "Nexray mengembalikan video tanpa audio gabungan. Pilih format bertanda audio agar file offline memiliki suara."
            return
        }
        viewModelScope.launch {
            mutableMessage.value = "Mengunduh video ${option.quality}…"
            val result = YoutubeDownloadManager.download(
                context.applicationContext,
                AuthorizedVideoSource(
                    title = video.title,
                    videoId = video.id,
                    directUrl = option.directUrl,
                    quality = option.quality,
                    thumbnailUrl = video.thumbnail.ifBlank { null },
                    sourceLabel = "Nexray API",
                ),
            )
            result.onSuccess {
                refresh(context)
                mutableMessage.value = "${it.title} disimpan ke Download/HansAps/Youtube pada kualitas ${option.quality}."
            }.onFailure { mutableMessage.value = it.message ?: "Download video gagal." }
        }
    }

    fun consumeMessage() { mutableMessage.value = null }
}
