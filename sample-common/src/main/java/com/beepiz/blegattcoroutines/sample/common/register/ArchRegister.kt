@file:Suppress("NOTHING_TO_INLINE")

package com.beepiz.blegattcoroutines.sample.common.register

import android.arch.lifecycle.GenericLifecycleObserver
import android.arch.lifecycle.Lifecycle
import android.arch.lifecycle.LifecycleOwner

inline fun LifecycleOwner.registerWhileResumed(vararg registrables: Registrable) {
    registerWhile(Lifecycle.State.RESUMED, registrables)
}

inline fun LifecycleOwner.registerWhileStarted(vararg registrables: Registrable) {
    registerWhile(Lifecycle.State.STARTED, registrables)
}

inline fun LifecycleOwner.registerWhileCreated(vararg registrables: Registrable) {
    registerWhile(Lifecycle.State.CREATED, registrables)
}

@PublishedApi
internal fun LifecycleOwner.registerWhile(
    minState: Lifecycle.State,
    registrables: Array<out Registrable>
) {
    if (registrables.isEmpty()) return
    val registerNow = lifecycle.currentState.isAtLeast(minState)
    if (registerNow) registrables.register()
    lifecycle.addObserver(object : GenericLifecycleObserver {
        var registered = registerNow
        override fun onStateChanged(owner: LifecycleOwner, event: Lifecycle.Event) {
            val shouldBeRegistered = lifecycle.currentState.isAtLeast(minState)
            if (registered != shouldBeRegistered) {
                registered = shouldBeRegistered
                if (shouldBeRegistered) registrables.register() else registrables.unregister()
            }
        }
    })
}
