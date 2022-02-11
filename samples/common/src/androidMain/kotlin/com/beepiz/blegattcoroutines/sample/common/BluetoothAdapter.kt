package com.beepiz.blegattcoroutines.sample.common

import android.Manifest
import android.bluetooth.BluetoothAdapter
import androidx.annotation.RequiresPermission
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

@RequiresPermission(Manifest.permission.BLUETOOTH)
actual fun isBluetoothEnabledFlow(): Flow<Boolean> = bluetoothStateFlow().map { state ->
    state == BluetoothState.On
}.distinctUntilChanged()

@RequiresPermission(Manifest.permission.BLUETOOTH)
actual fun bluetoothStateFlow(): Flow<BluetoothState> = broadcastReceiverFlow(
    action = BluetoothAdapter.ACTION_STATE_CHANGED,
    priority = 999,
    emitInitialEmptyIntent = true,
    conflate = true
).transform { intent ->
    when (intent.action) {
        null -> {
            emit(bluetoothState(intState = defaultBluetoothAdapter!!.state))
        }
        BluetoothAdapter.ACTION_STATE_CHANGED -> {
            val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.STATE_OFF)
            emit(bluetoothState(intState = state))
        }
    }
}.distinctUntilChanged().flowOn(Dispatchers.IO)

@RequiresPermission(Manifest.permission.BLUETOOTH)
private fun bluetoothState(intState: Int): BluetoothState = when (intState) {
    BluetoothAdapter.STATE_ON -> BluetoothState.On
    BluetoothAdapter.STATE_OFF -> BluetoothState.Off
    BluetoothAdapter.STATE_TURNING_ON -> BluetoothState.TurningOn
    BluetoothAdapter.STATE_TURNING_OFF -> BluetoothState.TurningOff
    else -> error("Unexpected bluetooth state value: $intState")
}
