package com.beepiz.blegattcoroutines.sample.common

import android.arch.lifecycle.ViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlin.coroutines.CoroutineContext

open class ScopedViewModel : ViewModel(), CoroutineScope {
    private val job = SupervisorJob()
    override val coroutineContext: CoroutineContext = job + Dispatchers.Main

    final override fun onCleared() {
        job.cancel()
    }
}
