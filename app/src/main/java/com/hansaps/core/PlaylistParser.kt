package com.hansaps.core

object PlaylistParser {
    fun parse(text: String): List<MusicPlaylist> {
        val result = mutableListOf<MusicPlaylist>()
        var currentName: String? = null
        var currentTracks = mutableListOf<PlaylistEntry>()

        fun flush() {
            val name = currentName?.trim().orEmpty()
            if (name.isNotEmpty()) result += MusicPlaylist(name, currentTracks.toList())
            currentTracks = mutableListOf()
        }

        text.lineSequence().forEach { raw ->
            val line = raw.trim()
            if (line.isEmpty() || line.startsWith(";") || line.startsWith("#")) return@forEach
            if (line.startsWith("[") && line.endsWith("]") && line.length > 2) {
                flush()
                currentName = line.substring(1, line.length - 1).trim().removePrefix("Playlist ").ifBlank { "Playlist" }
            } else if (currentName != null) {
                val separator = line.indexOf(" - ")
                val artist = if (separator > 0) line.substring(0, separator).trim() else "Unknown artist"
                val title = if (separator > 0) line.substring(separator + 3).trim() else line
                currentTracks += PlaylistEntry(artist, title, line)
            }
        }
        flush()
        return result
    }
}
