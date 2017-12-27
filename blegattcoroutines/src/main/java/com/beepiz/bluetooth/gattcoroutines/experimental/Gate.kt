package com.beepiz.bluetooth.gattcoroutines.experimental

import kotlinx.coroutines.experimental.channels.ConflatedBroadcastChannel
import kotlinx.coroutines.experimental.channels.consumeEach

internal class Gate(locked: Boolean = false) {

    /** Internal value is true when the gate is locked */
    private val lockStateChannel = ConflatedBroadcastChannel<Boolean>(locked)

    var isLocked = locked
        set(value) {
            field = value
            lockStateChannel.offer(value)
        }

    suspend fun passThroughWhenUnlocked() = lockStateChannel.consumeEach { locked ->
        if (!locked) return
    }

    suspend fun awaitLock() = lockStateChannel.consumeEach { locked ->
        if (locked) return
    }
}
