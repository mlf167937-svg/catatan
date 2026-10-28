package com.hansaps.core

import android.net.Uri

enum class UserRole(val label: String) {
    DEVELOPER("Developer"), OWNER("Owner"), USER("User");

    companion object {
        fun fromStored(value: String?): UserRole = entries.firstOrNull { it.name == value } ?: USER
    }
}

data class AppAccount(
    val username: String,
    val passwordHash: String,
    val role: UserRole,
    val passwordSalt: String = "",
)

data class LocalTrack(
    val id: String,
    val title: String,
    val artist: String,
    val uri: Uri,
    val durationMs: Long = 0L,
    val displayName: String = "",
    val artworkUri: Uri? = null,
)

data class LocalVideo(
    val id: String,
    val title: String,
    val uri: Uri,
    val durationMs: Long = 0L,
    val displayName: String = "",
)

data class PlaylistEntry(
    val artist: String,
    val title: String,
    val originalLine: String,
)

data class MusicPlaylist(
    val name: String,
    val tracks: List<PlaylistEntry>,
)
