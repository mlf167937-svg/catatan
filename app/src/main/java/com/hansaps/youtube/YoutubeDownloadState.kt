package com.hansaps.youtube

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** In-memory download state shared by the foreground service and the Koleksi UI. */
data class YoutubeDownloadTask(
    val key: String,
    val title: String,
    val quality: String,
    val percent: Int = 0,
    val message: String = "Menyiapkan unduhan…",
    val indeterminate: Boolean = true,
    val phase: Phase = Phase.DOWNLOADING,
) {
    enum class Phase { DOWNLOADING, COMPLETED, FAILED }
}

object YoutubeDownloadState {
    private val mutableTasks = MutableStateFlow<List<YoutubeDownloadTask>>(emptyList())
    val tasks: StateFlow<List<YoutubeDownloadTask>> = mutableTasks.asStateFlow()

    @Synchronized
    fun begin(key: String, title: String, quality: String) {
        mutableTasks.value = mutableTasks.value.filterNot { it.key == key } +
            YoutubeDownloadTask(key = key, title = title, quality = quality)
    }

    @Synchronized
    fun update(key: String, percent: Int, message: String, indeterminate: Boolean) {
        mutableTasks.value = mutableTasks.value.map { task ->
            if (task.key == key && task.phase == YoutubeDownloadTask.Phase.DOWNLOADING) {
                task.copy(percent = percent.coerceIn(0, 100), message = message, indeterminate = indeterminate)
            } else task
        }
    }

    @Synchronized
    fun complete(key: String) {
        mutableTasks.value = mutableTasks.value.map { task ->
            if (task.key == key) task.copy(
                percent = 100,
                message = "Video tersimpan · ${task.quality}",
                indeterminate = false,
                phase = YoutubeDownloadTask.Phase.COMPLETED,
            ) else task
        }
    }

    @Synchronized
    fun fail(key: String, message: String) {
        mutableTasks.value = mutableTasks.value.map { task ->
            if (task.key == key) task.copy(
                message = "Gagal: ${message.take(120)}",
                indeterminate = false,
                phase = YoutubeDownloadTask.Phase.FAILED,
            ) else task
        }
    }

    @Synchronized
    fun remove(key: String) {
        mutableTasks.value = mutableTasks.value.filterNot { it.key == key }
    }

    fun hasActiveDownload(): Boolean = mutableTasks.value.any { it.phase == YoutubeDownloadTask.Phase.DOWNLOADING }
}
