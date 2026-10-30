package com.hansaps.spotify.rexmusic

import java.io.File
import java.io.IOException
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.withPermit

internal suspend fun RexOfflineManager.__splitImpl_downloadFile(
        url: String,
        dest: File,
        onProgress: (RexOfflineManager.DownloadProgress) -> Unit
    ) {
        val conn = openConnection(url)
        try {
            val code = conn.responseCode
            if (code !in 200..299) throw IOException("server membalas kode $code")
            val type = conn.contentType.orEmpty().lowercase()
            if (type.startsWith("text/") || type.contains("json")) {
                throw IOException("server tidak mengirim file audio")
            }
            val total = conn.contentLengthLong.coerceAtLeast(0L)
            onProgress(RexOfflineManager.DownloadProgress(
                percent = if (total > 0L) 0 else -1,
                downloadedBytes = 0L,
                totalBytes = total,
                speedBytesPerSecond = 0L
            ))

            val startAt = System.currentTimeMillis()
            conn.inputStream.use { input ->
                dest.outputStream().use { out ->
                    val buffer = ByteArray(32 * 1024)
                    var done = 0L
                    var lastTick = 0L
                    var lastBytes = 0L
                    var speed = 0L
                    while (true) {
                        currentCoroutineContext().ensureActive()
                        val n = input.read(buffer)
                        if (n < 0) break
                        out.write(buffer, 0, n)
                        done += n

                        val now = System.currentTimeMillis()
                        if (done == n.toLong() || now - lastTick >= 250L || (total > 0L && done >= total)) {
                            val dt = now - lastTick
                            if (lastTick > 0L && dt > 0L) {
                                val instant = ((done - lastBytes) * 1000L / dt).coerceAtLeast(0L)
                                speed = if (speed == 0L) instant else ((speed * 0.72f) + (instant * 0.28f)).toLong()
                            }
                            lastTick = now
                            lastBytes = done
                            val pct = if (total > 0L) {
                                (done * 100L / total).toInt().coerceIn(0, 100)
                            } else {
                                -1
                            }
                            onProgress(RexOfflineManager.DownloadProgress(pct, done, total, speed))
                        }
                    }
                    if (total > 0L && done < total) throw IOException("unduhan terputus")
                    if (done > 0L) {
                        val elapsed = (System.currentTimeMillis() - startAt).coerceAtLeast(1L)
                        val finalSpeed = if (speed > 0L) speed else (done * 1000L / elapsed)
                        onProgress(RexOfflineManager.DownloadProgress(
                            percent = if (total > 0L) 100 else -1,
                            downloadedBytes = done,
                            totalBytes = total,
                            speedBytesPerSecond = finalSpeed
                        ))
                    }
                }
            }
            if (dest.length() <= 0L) throw IOException("file kosong")
        } finally {
            conn.disconnect()
        }
    }

internal suspend fun RexOfflineManager.__splitImpl_doDownload(track: RexTrack, key: String) {
        setStatus(key, DownloadStatus(track, DownloadStage.Resolving))
        val fresh = RexTrackResolver.resolve(track).getOrThrow()
        if (fresh.audioUrl.isBlank()) throw IOException("URL audio kosong")

        withContext(Dispatchers.IO) {
            val dir = File(root, key)
            if (!dir.isDirectory && !dir.mkdirs()) {
                throw IOException("tidak bisa membuat folder $DISPLAY_PATH/$key")
            }
            val part = File(dir, "$AUDIO_NAME.part")
            downloadFile(fresh.audioUrl, part) { progress ->
                setStatus(
                    key,
                    DownloadStatus(
                        track = track,
                        stage = DownloadStage.Downloading,
                        percent = progress.percent,
                        downloadedBytes = progress.downloadedBytes,
                        totalBytes = progress.totalBytes,
                        speedBytesPerSecond = progress.speedBytesPerSecond
                    )
                )
            }
            val target = File(dir, AUDIO_NAME)
            if (target.exists()) target.delete()
            if (!part.renameTo(target)) throw IOException("gagal menyimpan file")

            ensureActive()
            downloadThumbnail(fresh.cover.ifBlank { track.cover }, dir)
            writeMeta(dir, fresh)

            val audioStatus = _state.value.downloads[key]
                ?: DownloadStatus(track = track, stage = DownloadStage.Downloading, percent = 100, downloadedBytes = target.length(), totalBytes = target.length())
            setStatus(
                key,
                audioStatus.copy(
                    track = track,
                    stage = DownloadStage.FetchingLyrics,
                    percent = 100,
                    speedBytesPerSecond = 0L
                )
            )
            val lyrics = RexMusicApi.fetchLyrics("${fresh.artist} ${fresh.title}".trim()).getOrNull()
            writeLyrics(dir, lyrics, lyrics == null)

            setStatus(
                key,
                _state.value.downloads[key]?.copy(
                    stage = DownloadStage.Finalizing,
                    percent = 100,
                    speedBytesPerSecond = 0L
                ) ?: audioStatus.copy(stage = DownloadStage.Finalizing, percent = 100, speedBytesPerSecond = 0L)
            )
        }
    }

internal fun RexOfflineManager.__splitImpl_download(track: RexTrack) {
        if (!hasAccess(appContext)) {
            _messages.tryEmit("Penyimpanan privat aplikasi tidak dapat ditulis. Periksa ruang penyimpanan perangkat.")
            return
        }
        val key = offlineKey(track)
        if (_state.value.isDownloaded(track, key)) {
            _messages.tryEmit("Sudah ada di offline")
            return
        }
        if (jobs[key]?.isActive == true) return

        setStatus(key, DownloadStatus(track, DownloadStage.Queued))
        val job = scope.launch(start = CoroutineStart.LAZY) {
            try {
                gate.withPermit { doDownload(track, key) }
                refreshNow()
                val finalStatus = _state.value.downloads[key]
                    ?: DownloadStatus(track, DownloadStage.Finalizing, 100)
                RexDownloadNotification.showCompleted(appContext, key, finalStatus)
                _messages.tryEmit("Tersimpan offline: ${track.title}")
            } catch (e: CancellationException) {
                cleanup(key)
                RexDownloadNotification.showCancelled(appContext, key, track)
                throw e
            } catch (e: Exception) {
                cleanup(key)
                RexDownloadNotification.showFailed(appContext, key, track, e.message ?: "kesalahan jaringan")
                _messages.tryEmit("Gagal mengunduh ${track.title}: ${e.message ?: "kesalahan jaringan"}")
            } finally {
                clearStatus(key)
                jobs.remove(key)
            }
        }
        jobs[key] = job
        job.start()
    }

/** Thumbnail bersifat opsional: kalau gagal, lagu tetap tersimpan tanpa cover. */
    internal fun RexOfflineManager.__splitImpl_downloadThumbnail(url: String, dir: File) {
        if (!url.startsWith("http")) return
        val tmp = File(dir, "thumbnail.tmp")
        try {
            val conn = openConnection(url)
            try {
                if (conn.responseCode !in 200..299) return
                conn.inputStream.use { input -> tmp.outputStream().use { input.copyTo(it) } }
            } finally {
                conn.disconnect()
            }
            if (tmp.length() <= 0L) return
            val isPng = tmp.inputStream().use { s ->
                val h = ByteArray(4)
                s.read(h) == 4 && h[0] == 0x89.toByte() && h[1] == 0x50.toByte() &&
                    h[2] == 0x4E.toByte() && h[3] == 0x47.toByte()
            }
            File(dir, "thumbnail.jpg").delete()
            File(dir, "thumbnail.png").delete()
            tmp.renameTo(File(dir, if (isPng) "thumbnail.png" else "thumbnail.jpg"))
        } catch (_: Exception) {
            // abaikan
        } finally {
            tmp.delete()
        }
    }
