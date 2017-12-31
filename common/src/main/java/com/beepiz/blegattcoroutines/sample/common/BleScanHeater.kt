package com.beepiz.blegattcoroutines.sample.common

import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanSettings
import android.os.Build.VERSION_CODES.LOLLIPOP
import android.support.annotation.RequiresApi
import com.beepiz.blegattcoroutines.sample.common.extensions.bluetoothManager
import com.beepiz.blegattcoroutines.sample.common.register.Registrable
import timber.log.Timber

/**
 * Just heats up the BluetoothGatt stack. Useful to do before connecting to any device.
 */
@RequiresApi(LOLLIPOP)
class BleScanHeater(private val scanMode: Int = ScanSettings.SCAN_MODE_LOW_LATENCY) : Registrable {

    private val scanCallback = object : ScanCallback() {
        override fun onScanFailed(errorCode: Int) {
            Timber.e("Scan failed. ErrorCode: $errorCode")
        }
    }

    override fun register() {
        val scanSettings = ScanSettings.Builder().apply {
            setScanMode(scanMode)
        }.build()
        bluetoothManager.adapter.bluetoothLeScanner?.startScan(null, scanSettings, scanCallback)
    }

    override fun unregister() {
        bluetoothManager.adapter.bluetoothLeScanner?.stopScan(scanCallback)
    }
}
