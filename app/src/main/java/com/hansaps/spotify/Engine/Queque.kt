package com.hansaps.spotify.Engine

import com.hansaps.core.LocalTrack
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Small queue model; actual audio transport is controlled by Media3's MediaController. */
class QueueEngine {
    private val mutableQueue = MutableStateFlow<List<LocalTrack>>(emptyList())
    val queue: StateFlow<List<LocalTrack>> = mutableQueue

    fun replace(items: List<LocalTrack>) { mutableQueue.value = items.toList() }
    fun enqueue(item: LocalTrack) { mutableQueue.value = mutableQueue.value + item }
    fun enqueueNext(item: LocalTrack, currentIndex: Int) {
        val next = (currentIndex + 1).coerceIn(0, mutableQueue.value.size)
        mutableQueue.value = mutableQueue.value.toMutableList().apply { add(next, item) }
    }
    fun removeAt(index: Int) {
        if (index !in mutableQueue.value.indices) return
        mutableQueue.value = mutableQueue.value.toMutableList().apply { removeAt(index) }
    }
    fun clear() { mutableQueue.value = emptyList() }
}
