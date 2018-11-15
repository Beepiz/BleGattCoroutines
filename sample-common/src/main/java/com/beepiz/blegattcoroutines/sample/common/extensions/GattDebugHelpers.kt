package com.beepiz.blegattcoroutines.sample.common.extensions

import com.beepiz.bluetooth.gattcoroutines.experimental.GattConnection
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.launch
import timber.log.Timber

fun GattConnection.logConnectionChanges() {
    GlobalScope.launch(Dispatchers.Main, CoroutineStart.DEFAULT) {
        stateChangeChannel.consumeEach {
            Timber.i("connection state changed: $it")
        }
    }
}
