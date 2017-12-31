package com.beepiz.blegattcoroutines.sample.common.extensions

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGattService
import android.os.Build.VERSION_CODES.JELLY_BEAN_MR2
import android.support.annotation.RequiresApi
import com.beepiz.bluetooth.gattcoroutines.experimental.GattConnection
import kotlinx.coroutines.experimental.CancellationException
import kotlinx.coroutines.experimental.TimeoutCancellationException
import kotlinx.coroutines.experimental.withTimeout
import timber.log.Timber

@RequiresApi(JELLY_BEAN_MR2)
fun deviceFor(macAddress: String): BluetoothDevice = bluetoothManager.adapter.getRemoteDevice(macAddress)

typealias GattBasicUsage = (GattConnection, List<BluetoothGattService>) -> Unit

/**
 * Connects to the device, discovers services, executes [block] and finally closes the connection.
 */
@RequiresApi(JELLY_BEAN_MR2)
inline suspend fun BluetoothDevice.useBasic(connectionTimeoutInMillis: Long = 5000L,
                                            block: GattBasicUsage) {
    val deviceConnection = GattConnection(this)
    try {
        deviceConnection.logConnectionChanges()
        withTimeout(connectionTimeoutInMillis) {
            deviceConnection.connect().await()
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
