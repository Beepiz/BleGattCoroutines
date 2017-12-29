package com.beepiz.blegattcoroutines.sample.extensions

import com.beepiz.bluetooth.gattcoroutines.experimental.GattConnection
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.channels.consumeEach
import timber.log.Timber

fun GattConnection.logConnectionChanges() {
    async(UI) {
        stateChangeChannel.consumeEach {
            Timber.i("connection state changed: $it")
        }
    }
}
