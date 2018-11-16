package com.beepiz.blegattcoroutines.sample.common

import android.arch.lifecycle.ViewModel
import kotlinx.coroutines.experimental.CoroutineScope
import kotlinx.coroutines.experimental.Dispatchers
import kotlinx.coroutines.experimental.SupervisorJob
import kotlin.coroutines.experimental.CoroutineContext

open class ScopedViewModel : ViewModel(), CoroutineScope {
    private val job = SupervisorJob()
    override val coroutineContext: CoroutineContext = job + Dispatchers.Main

    final override fun onCleared() {
        job.cancel()
    }
}
