package com.beepiz.blegattcoroutines.sample.common

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.os.Build.VERSION.SDK_INT
import androidx.annotation.RequiresPermission
import kotlinx.coroutines.flow.*

enum class BluetoothState {
    On, Off, TurningOn, TurningOff;
}

@RequiresPermission(Manifest.permission.BLUETOOTH)
expect fun isBluetoothEnabledFlow(): Flow<Boolean>

@RequiresPermission(Manifest.permission.BLUETOOTH)
expect fun bluetoothStateFlow(): Flow<BluetoothState>
