package com.beepiz.blegattcoroutines.sample.common

import android.Manifest
import android.bluetooth.le.ScanSettings
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.coroutineScope
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import splitties.lifecycle.coroutines.isStartedFlow

/**
 * Just heats up the BluetoothGatt stack. Useful to do before connecting to any device to decrease
 * the chances of connection failures (even if you just access the device directly via
 * its MAC address).
 */
@Suppress("InlinedApi")
@RequiresApi(21)
object BleScanHeater {

    @OptIn(ExperimentalCoroutinesApi::class)
    @RequiresPermission(
        allOf = [
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.ACCESS_FINE_LOCATION
        ]
    )
    fun heatUpWhileStarted(
        lifecycle: Lifecycle,
        scanMode: Int = ScanSettings.SCAN_MODE_LOW_LATENCY
    ): Job {
        return lifecycle.coroutineScope.launch {
            lifecycle.isStartedFlow().collectLatest { isStarted ->
                if (isStarted) heatUp(scanMode)
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @RequiresPermission(
        allOf = [
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.ACCESS_FINE_LOCATION
        ]
    )
    suspend fun heatUp(scanMode: Int = ScanSettings.SCAN_MODE_LOW_LATENCY): Nothing {
        val scanSettings = ScanSettings.Builder().apply {
            setScanMode(scanMode)
        }.build()
        bluetoothLeScanFlow(
            filters = null,
            settings = scanSettings
        ).collect()
        awaitCancellation()
    }
}
