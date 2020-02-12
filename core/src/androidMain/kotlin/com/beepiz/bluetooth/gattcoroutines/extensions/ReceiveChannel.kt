package com.beepiz.bluetooth.gattcoroutines.extensions

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.channels.produce
import kotlin.coroutines.CoroutineContext

@ObsoleteCoroutinesApi
@ExperimentalCoroutinesApi
internal fun <E> ReceiveChannel<E>.withCloseHandler(
    context: CoroutineContext = Dispatchers.Unconfined,
    handler: (Throwable?) -> Unit
): ReceiveChannel<E> = GlobalScope.produce(context) {
    invokeOnClose(handler)
    consumeEach { send(it) }
}
