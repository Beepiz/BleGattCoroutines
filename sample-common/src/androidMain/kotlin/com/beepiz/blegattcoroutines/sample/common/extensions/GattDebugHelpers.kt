package com.beepiz.blegattcoroutines.sample.common.extensions

import com.beepiz.bluetooth.gattcoroutines.GattConnection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.launch
import timber.log.Timber

@ObsoleteCoroutinesApi
fun GattConnection.logConnectionChanges() {
    GlobalScope.launch(Dispatchers.Main) {
        stateChangeChannel.consumeEach {
            Timber.i("connection state changed: $it")
        }
    }
}
