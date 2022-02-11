package com.beepiz.blegattcoroutines.sample.common.extensions

import android.Manifest
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGattService
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import com.beepiz.bluetooth.gattcoroutines.GattConnection
import kotlinx.coroutines.*
import splitties.systemservices.bluetoothManager
import splitties.toast.UnreliableToastApi
import splitties.toast.toast
import timber.log.Timber

@RequiresApi(18)
fun deviceFor(macAddress: String): BluetoothDevice =
    bluetoothManager.adapter.getRemoteDevice(macAddress)

/**
 * Connects to the device, discovers services, executes [block] and finally closes the connection.
 */
@RequiresApi(18)
@RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
@OptIn(UnreliableToastApi::class)
@Suppress("InlinedApi")
suspend inline fun BluetoothDevice.useBasic(
    connectionTimeoutInMillis: Long = 5000L,
    block: (GattConnection, List<BluetoothGattService>) -> Unit
) {
    val deviceConnection = GattConnection(this)
    val loggingJob = MainScope().launch(Dispatchers.Main) {
        deviceConnection.stateChanges.collect {
            Timber.i("connection state changed: $it")
        }
    }
    try {
        withTimeout(connectionTimeoutInMillis) {
            deviceConnection.connect()
        }
        Timber.i("Connected!")
        val services = deviceConnection.discoverServices()
        Timber.i("Services discovered!")
        block(deviceConnection, services)
    } catch (e: TimeoutCancellationException) {
        Timber.e("Connection timed out after $connectionTimeoutInMillis milliseconds!".also {
            toast(it)
        })
        throw e
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Timber.e(e)
    } finally {
        deviceConnection.close()
        loggingJob.cancel()
        Timber.i("Closed!")
    }
}
