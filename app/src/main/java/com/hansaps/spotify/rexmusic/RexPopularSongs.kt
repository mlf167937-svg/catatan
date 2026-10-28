package com.hansaps.spotify.rexmusic

/**
 * DAFTAR LAGU POPULER BUATANMU SENDIRI.
 *
 * Cara isi: ganti song("", "") dengan song("Judul", "Artis"). Contoh:
 *     song("Jatuh Suka", "Tulus"),
 *
 * - Sudah tersedia 30 lagu.
 * - Baris yang judul ATAU artisnya kosong otomatis diabaikan.
 * - Saat menekan Next (autoplay pintar), lagu diacak dari daftar ini.
 * - Hanya judul + artis yang disimpan, tidak ada mp3. Audio dicari lewat API saat diputar.
 */
object RexPopularSongs {
    val songs = listOf(
        song("iqro'", "Raim Laode"),
        song("Menari-nari", "Raim Laode"),
        song("Dunia Yang Nanti", "Raim Laode"),
        song("Komang", "Raim Laode"),
        song("Lesung Pipi", "Raim Laode"),
        song("Babak Terakhir", "Raim Laode"),
        song("Suasana Rumah", "Raim Laode"),
        song("Salah", "Raim Laode"),
        song("Cemburu", "Raim Laode"),
        song("Su Terlalu Lama", "Raim Laode"),

        song("Penjaga Hati", "Nadhif Basalamah"),
        song("bergema sampai selamanya", "Nadhif Basalamah"),
        song("kota ini tak sama tanpamu", "Nadhif Basalamah"),

        song("Pamit", "Tulus"),
        song("Monokrom", "Tulus"),
        song("Jatuh Suka", "Tulus"),
        song("Hati-Hati di Jalan", "Tulus"),
        song("Teh Hijau", "Tulus"),

        song("Evakuasi", "Hindia"),
        song("Secukupnya", "Hindia"),
        song("Rumah Ke Rumah", "Hindia"),
        song("Evaluasi", "Hindia"),
        song("Cincin", "Hindia"),
        song("everything u are", "Hindia"),

        song("o,Tuan", ".Feast"),
        song("Nina", ".Feast"),
        song("Tarot", ".Feast"),

        song("Surat Cinta Untuk Starla", "Virgoun"),
        song("Bukti", "Virgoun"),
        song("Selamat (Selamat Tinggal)", "Virgoun")
    )

    private fun song(title: String, artist: String) = title to artist

    /** Hanya entri yang terisi lengkap, tanpa duplikat. */
    val tracks: List<RexTrack> by lazy {
        songs
            .filter { it.first.isNotBlank() && it.second.isNotBlank() }
            .distinctBy {
                "${it.first.trim().lowercase()}|${it.second.trim().lowercase()}"
            }
            .map { (title, artist) ->
                RexTrack(
                    id = "pop-" + slug("$title $artist"),
                    title = title.trim(),
                    artist = artist.trim(),
                    album = "Pilihan HansAps",
                    cover = RexMusicRegistry.defaultTracks.firstOrNull {
                        it.artist.equals(artist.trim(), ignoreCase = true) && it.cover.isNotBlank()
                    }?.cover.orEmpty()
                )
            }
    }

    private fun slug(text: String): String =
        text.lowercase()
            .replace(Regex("[^a-z0-9]+"), "-")
            .trim('-')
}
