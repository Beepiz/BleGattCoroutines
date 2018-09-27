package com.beepiz.blegattcoroutines.sample.common.extensions

import com.beepiz.bluetooth.gattcoroutines.experimental.GattConnection
import kotlinx.coroutines.experimental.CoroutineStart
import kotlinx.coroutines.experimental.Dispatchers
import kotlinx.coroutines.experimental.GlobalScope
import kotlinx.coroutines.experimental.android.Main
import kotlinx.coroutines.experimental.channels.consumeEach
import kotlinx.coroutines.experimental.launch
import timber.log.Timber

fun GattConnection.logConnectionChanges() {
    GlobalScope.launch(Dispatchers.Main, CoroutineStart.DEFAULT) {
        stateChangeChannel.consumeEach {
            Timber.i("connection state changed: $it")
        }
    }
}
