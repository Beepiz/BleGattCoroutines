package com.beepiz.bluetooth.gattcoroutines.experimental

import kotlinx.coroutines.experimental.CancellationException

class ConnectionClosedException internal constructor(
        cause: Throwable? = null,
        messageSuffix: String = ""
) : CancellationException("The connection has been irrevocably closed$messageSuffix.") {
    init {
        initCause(cause)
    }
}
