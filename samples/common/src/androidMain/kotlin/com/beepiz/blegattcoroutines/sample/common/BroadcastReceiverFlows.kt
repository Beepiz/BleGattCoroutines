package com.beepiz.blegattcoroutines.sample.common

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import splitties.init.appCtx

fun broadcastReceiverFlow(
    action: String,
    priority: Int = 0,
    emitInitialEmptyIntent: Boolean = false,
    conflate: Boolean = false
): Flow<Intent> = broadcastReceiverFlow(
    filter = IntentFilter(action).also { it.priority = priority },
    emitInitialEmptyIntent = emitInitialEmptyIntent,
    conflate = conflate
)

fun broadcastReceiverFlow(
    filter: IntentFilter,
    emitInitialEmptyIntent: Boolean = false,
    conflate: Boolean = false
): Flow<Intent> = channelFlow {
    val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            trySend(intent)
        }
    }
    val ctx = appCtx
    ctx.registerReceiver(receiver, filter)
    if (emitInitialEmptyIntent) trySend(Intent())
    awaitClose {
        ctx.unregisterReceiver(receiver)
    }
}.let {
    if (conflate) it.conflate() else it
}.flowOn(Dispatchers.IO)
