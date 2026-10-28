package com.hansaps.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaylistParserTest {
    @Test
    fun parsesPlaylistSectionsAndArtistTitlePairs() {
        val playlists = PlaylistParser.parse(
            """
            [Playlist Populer]
            Raim Laode - Dunia Yang Nanti
            Tulus - Jatuh Suka

            [Playlist Santai]
            Nadifh Basalamah - Bergema Sampai Selamanya
            """.trimIndent()
        )

        assertEquals(2, playlists.size)
        assertEquals("Populer", playlists[0].name)
        assertEquals(2, playlists[0].tracks.size)
        assertEquals("Raim Laode", playlists[0].tracks[0].artist)
        assertEquals("Dunia Yang Nanti", playlists[0].tracks[0].title)
        assertEquals("Santai", playlists[1].name)
        assertTrue(playlists[1].tracks.single().title.contains("Bergema"))
    }
}
