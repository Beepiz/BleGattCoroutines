package com.beepiz.blegattcoroutines.sample.common.extensions

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGattService
import android.os.Build.VERSION_CODES.JELLY_BEAN_MR2
import android.support.annotation.RequiresApi
import com.beepiz.bluetooth.gattcoroutines.experimental.GattConnection
import kotlinx.coroutines.experimental.CancellationException
import timber.log.Timber

@RequiresApi(JELLY_BEAN_MR2)
fun deviceFor(macAddress: String): BluetoothDevice = bluetoothManager.adapter.getRemoteDevice(macAddress)

/**
 * Connects to the device, discovers services, executes [block] and finally closes the connection.
 */
@RequiresApi(JELLY_BEAN_MR2)
inline suspend fun BluetoothDevice.useBasic(block: (GattConnection, List<BluetoothGattService>) -> Unit) {
    val deviceConnection = GattConnection(this)
    try {
        deviceConnection.logConnectionChanges()
        deviceConnection.connect().await()
        Timber.i("Connected!")
        val services = deviceConnection.discoverServices()
        Timber.i("Services discovered!")
        block(deviceConnection, services)
    } catch (ignored: CancellationException) {
    } catch (e: Exception) {
        Timber.e(e)
    } finally {
        deviceConnection.close()
        Timber.i("Closed!")
    }
}
