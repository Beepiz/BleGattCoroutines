package com.beepiz.blegattcoroutines

import kotlinx.coroutines.experimental.CompletableDeferred

/* Message types for counterActor */
sealed class CounterMsg

object IncCounter : CounterMsg() // one-way message to increment counter
class GetCounter(val response: CompletableDeferred<Int>) : CounterMsg() // a request with reply
