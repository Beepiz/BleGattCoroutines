package com.beepiz.bluetooth.gattcoroutines.extensions

import kotlinx.coroutines.channels.SendChannel

//TODO: Replace that when https://github.com/Kotlin/kotlinx.coroutines/issues/974 is resolved.
internal fun <E> SendChannel<E>.offerCatching(element: E): Boolean {
    return runCatching { offer(element) }.getOrDefault(false)
}
