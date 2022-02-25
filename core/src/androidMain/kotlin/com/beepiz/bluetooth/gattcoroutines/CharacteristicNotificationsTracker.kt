package com.beepiz.bluetooth.gattcoroutines

import androidx.annotation.RequiresApi
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.*
import java.util.UUID

@RequiresApi(18)
internal class CharacteristicNotificationsTracker(
    private val switch: suspend (characteristic: BGC, enable: Boolean) -> Unit
) {

    suspend fun keepNotificationsEnabled(characteristic: BGC) {
        try {
            countAndSwitchIfNeeded(characteristic, Operation.Add)
            awaitCancellation()
        } finally {
            countAndSwitchIfNeeded(characteristic, Operation.Remove)
        }
    }

    private enum class Operation { Add, Remove; }

    private suspend fun countAndSwitchIfNeeded(
        characteristic: BGC,
        operation: Operation
    ) = withContext(NonCancellable) {
        mutex.withLock {
            val uuid = characteristic.uuid
            val previousCount = counts[uuid] ?: 0
            counts[uuid] = when (operation) {
                Operation.Add -> {
                    if (previousCount == 0) switch(characteristic, true)
                    previousCount + 1
                }
                Operation.Remove -> {
                    if (previousCount == 1) switch(characteristic, false)
                    previousCount - 1
                }
            }
        }
    }

    private val mutex = Mutex()

    private val counts = mutableMapOf<UUID, Int>()
}
