package com.hansaps.core

fun formatTime(milliseconds: Long): String {
    if (milliseconds <= 0L) return "00:00"
    val totalSeconds = milliseconds / 1000L
    val minutes = totalSeconds / 60L
    val seconds = totalSeconds % 60L
    return "%02d:%02d".format(minutes, seconds)
}
