package com.beepiz.blegattcoroutines.sample.common

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeScanner
import android.os.Build.VERSION.SDK_INT
import androidx.annotation.RequiresApi
import splitties.systemservices.bluetoothManager

val defaultBluetoothAdapter: BluetoothAdapter?
    get() = bluetoothManager.defaultAdapter

val BluetoothManager.defaultAdapter: BluetoothAdapter?
    inline get() = when {
        SDK_INT >= 18 -> adapter
        else -> @Suppress("DEPRECATION") BluetoothAdapter.getDefaultAdapter()
    }

inline val bluetoothLeScanner: BluetoothLeScanner?
    @RequiresApi(21)
    get() = defaultBluetoothAdapter?.bluetoothLeScanner
