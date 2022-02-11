@file:OptIn(ExperimentalCoroutinesApi::class)
@file:Suppress("InlinedApi")

package com.beepiz.blegattcoroutines.sample.common

import android.Manifest
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.*
import splitties.coroutines.repeatWhileActive
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes

@RequiresApi(21)
@RequiresPermission(
    allOf = [
        Manifest.permission.BLUETOOTH_ADMIN,
        Manifest.permission.BLUETOOTH,
        Manifest.permission.BLUETOOTH_SCAN,
        Manifest.permission.ACCESS_FINE_LOCATION
    ]
)
actual fun bluetoothLeScanFlow(
    filters: List<ScanFilter>?,
    settings: ScanSettings
): Flow<ScanResult> {
    requireSafeSettings(settings)
    return isBluetoothEnabledFlow().flatMapLatest { isBluetoothEnabled ->
        if (isBluetoothEnabled) bluetoothLeScanner?.scanFlow(
            filters = filters,
            settings = settings
        ) ?: error("Bluetooth scanner not available")
        else emptyFlow()
    }
}

@RequiresApi(21)
@RequiresPermission(
    allOf = [
        Manifest.permission.BLUETOOTH_ADMIN,
        Manifest.permission.BLUETOOTH,
        Manifest.permission.BLUETOOTH_SCAN,
        Manifest.permission.ACCESS_FINE_LOCATION
    ]
)
actual fun BluetoothLeScanner.scanFlow(
    filters: List<ScanFilter>?,
    settings: ScanSettings
): Flow<ScanResult> {
    requireSafeSettings(settings)
    return flow {
        delay(500.milliseconds) // Allow cancellation for a short duration before starting the scan.
        repeatWhileActive {
            emit(Unit)
            delay(29.minutes)
            // After 30 minutes, if the scan is not exempt from scan downgrade,
            // Android will downgrade it to opportunistic.
            // To avoid this shady and undocumented behavior,
            // we restart the scan after 29 minutes.
        }
    }.flatMapLatest {
        ScanRestrictionsTracker.delayScanIfNeededThenTrack()
        unTrackedScanFlow(filters, settings)
    }
}

@RequiresApi(21)
private fun requireSafeSettings(settings: ScanSettings) {
    val callbackType = settings.callbackType
    require(callbackType == ScanSettings.CALLBACK_TYPE_ALL_MATCHES) {
        "Only CALLBACK_TYPE_ALL_MATCHES is supported, because CALLBACK_TYPE_FIRST_MATCH and" +
                "CALLBACK_TYPE_MATCH_LOST are unreliable on many devices, and their behavior" +
                "is not documented. If you need such a feature, use CALLBACK_TYPE_ALL_MATCHES" +
                "(default) and develop your own logic where you control timeouts."
    }
}

@RequiresApi(21)
@RequiresPermission(
    allOf = [
        Manifest.permission.BLUETOOTH_ADMIN,
        Manifest.permission.BLUETOOTH_SCAN,
        Manifest.permission.BLUETOOTH,
        Manifest.permission.ACCESS_FINE_LOCATION
    ]
)
@OptIn(ExperimentalCoroutinesApi::class)
private fun BluetoothLeScanner.unTrackedScanFlow(
    filters: List<ScanFilter>?,
    settings: ScanSettings
): Flow<ScanResult> = channelFlow {
    val scanCallback = ChannelScanCallback(this)
    //TODO: Restart scan in loop on faulty devices that are especially pesky with Wi-Fi enabled?
    startScan(filters, settings, scanCallback)
    awaitClose {
        try {
            stopScan(scanCallback)
        } catch (ignore: java.lang.IllegalStateException) {
            // Bluetooth or Bluetooth LE is probably disabled.
            // Android 10 would give the following error message: BT Adapter is not turned ON
        }
    }
}.flowOn(Dispatchers.IO)

private object ScanRestrictionsTracker {

    private val scope = CoroutineScope(Dispatchers.Default)

    suspend fun delayScanIfNeededThenTrack() {
        semaphore.acquire()
        scope.launch {
            delay(30_000)
            semaphore.release()
        }
    }

    private val semaphore = Semaphore(permits = 5)
}

@RequiresApi(21)
private class ChannelScanCallback(private val channel: SendChannel<ScanResult>) : ScanCallback() {
    override fun onScanResult(callbackType: Int, result: ScanResult) {
        channel.trySend(result)
    }

    override fun onBatchScanResults(results: List<ScanResult>) {
        results.forEach { channel.trySend(it) }
    }

    override fun onScanFailed(errorCode: Int) {
        channel.close(ScanFailedException(errorCode))
    }
}
