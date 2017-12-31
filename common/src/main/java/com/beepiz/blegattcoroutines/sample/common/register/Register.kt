@file:Suppress("NOTHING_TO_INLINE")

package com.beepiz.blegattcoroutines.sample.common.register

inline fun Array<out Registrable>.register() = forEach { it.register() }
inline fun Array<out Registrable>.unregister() = forEach { it.unregister() }
inline fun Array<out Registrable>.registerIf(condition: Boolean) = forEach {
    if (condition) it.register() else it.unregister()
}
