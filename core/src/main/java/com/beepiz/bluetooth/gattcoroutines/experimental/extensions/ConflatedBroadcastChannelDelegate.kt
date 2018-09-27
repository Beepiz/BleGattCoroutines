package com.beepiz.bluetooth.gattcoroutines.experimental.extensions

import kotlinx.coroutines.experimental.channels.ConflatedBroadcastChannel
import kotlin.reflect.KProperty

@Suppress("NOTHING_TO_INLINE")
internal inline operator fun <E> ConflatedBroadcastChannel<E>.getValue(
        thisRef: Any?,
        prop: KProperty<*>
): E = value

@Suppress("NOTHING_TO_INLINE")
internal inline operator fun <E> ConflatedBroadcastChannel<E>.setValue(
        thisRef: Any?,
        prop: KProperty<*>,
        value: E
) = offer(value).let { Unit }
