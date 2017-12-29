package com.beepiz.blegattcoroutines.sample.extensions

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGattService
import com.beepiz.bluetooth.gattcoroutines.experimental.GattConnection
import timber.log.Timber

fun deviceFor(macAddress: String): BluetoothDevice = bluetoothManager.adapter.getRemoteDevice(macAddress)

/**
 * Connects to the device, discovers services, executes [block] and finally closes the connection.
 */
inline suspend fun BluetoothDevice.useBasic(block: (GattConnection, List<BluetoothGattService>) -> Unit) {
    val deviceConnection = GattConnection(this)
    try {
        deviceConnection.logConnectionChanges()
        deviceConnection.connect().await()
        Timber.i("Connected!")
        val services = deviceConnection.discoverServices()
        Timber.i("Services discovered!")
        block(deviceConnection, services)
        deviceConnection.disconnect().await()
        Timber.i("Disconnected!")
    } catch (e: Exception) {
        Timber.e(e)
    } finally {
        deviceConnection.close()
        Timber.i("Closed!")
    }
}
