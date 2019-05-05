package com.beepiz.blegattcoroutines.sample.common

import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanSettings
import androidx.annotation.RequiresApi
import com.beepiz.blegattcoroutines.sample.common.register.Registrable
import splitties.systemservices.bluetoothManager
import timber.log.Timber

/**
 * Just heats up the BluetoothGatt stack. Useful to do before connecting to any device to decrease
 * the chances of connection failures (even if you just access the device directly via
 * its MAC address).
 */
@RequiresApi(21)
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
