package com.beepiz.blegattcoroutines.sample.common.extensions

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGattService
import android.support.annotation.RequiresApi
import com.beepiz.bluetooth.gattcoroutines.experimental.GattConnection
import kotlinx.coroutines.*
import splitties.systemservices.bluetoothManager
import splitties.toast.toast
import timber.log.Timber

@RequiresApi(18)
fun deviceFor(macAddress: String): BluetoothDevice =
    bluetoothManager.adapter.getRemoteDevice(macAddress)

/**
 * Connects to the device, discovers services, executes [block] and finally closes the connection.
 */
@RequiresApi(18)
@UseExperimental(ObsoleteCoroutinesApi::class, ExperimentalCoroutinesApi::class)
suspend inline fun BluetoothDevice.useBasic(
    connectionTimeoutInMillis: Long = 5000L,
    block: (GattConnection, List<BluetoothGattService>) -> Unit
) {
    val deviceConnection = GattConnection(this)
    try {
        deviceConnection.logConnectionChanges()
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
        Timber.i("Closed!")
    }
}
