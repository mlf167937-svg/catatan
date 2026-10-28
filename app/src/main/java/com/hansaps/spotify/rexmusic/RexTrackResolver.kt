package com.hansaps.spotify.rexmusic

/**
 * Mengubah RexTrack (metadata saja) jadi RexTrack dengan audioUrl yang bisa diputar / diunduh.
 * Dipakai bersama oleh pemutar dan pengunduh supaya logikanya sama.
 *
 * 1) spotifyUrl ada -> langsung resolve audio.
 * 2) Gagal / kosong -> search "artist + title", ambil hasil pertama, resolve.
 * id/title/artist/album/cover asli dipertahankan.
 */
internal object RexTrackResolver {

    suspend fun resolve(track: RexTrack): Result<RexTrack> {
        if (track.spotifyUrl.isNotBlank()) {
            val direct = RexMusicApi.resolveAudio(track)
            if (direct.isSuccess) return direct
        }

        val list = RexMusicApi.search("${track.artist} ${track.title}".trim())
            .getOrElse { return Result.failure(it) }
        val first = list.firstOrNull()
            ?: return Result.failure(Exception("lagu \"${track.title}\" tidak ditemukan"))

        return RexMusicApi.resolveAudio(
            first.copy(
                id = track.id, title = track.title, artist = track.artist,
                album = track.album, cover = track.cover.ifBlank { first.cover }
            )
        )
    }
}
