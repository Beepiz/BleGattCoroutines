package com.beepiz.bluetooth.gattcoroutines.experimental

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothGattService
import android.os.Build
import android.support.annotation.RequiresApi
import kotlinx.coroutines.experimental.CancellationException
import kotlinx.coroutines.experimental.channels.ReceiveChannel
import kotlinx.coroutines.experimental.withTimeout
import java.util.*

typealias BG = BluetoothGatt
typealias BGC = BluetoothGattCharacteristic
typealias BGD = BluetoothGattDescriptor

class ConnectionClosedException internal constructor(
        cause: Throwable? = null,
        messageSuffix: String = ""
) : CancellationException("The connection has been irrevocably closed$messageSuffix.") {
    init {
        initCause(cause)
    }
}

/**
 * The entry point of BluetoothGatt with coroutines.
 *
 * Create an instance by passing a Low Energy compatible [BluetoothDevice], then
 * call [connect] when you need to perform operations, perform your operations, and when you're
 * done, call [close] or [disconnect].
 *
 * Note that [discoverServices] is usually the first call you want to make after calling [connect].
 *
 * **For production apps, see [stateChangeChannel].**
 */
interface GattConnection {
    companion object {
        @RequiresApi(18) operator fun invoke(
                bluetoothDevice: BluetoothDevice,
                closeOnDisconnect: Boolean = true
        ): GattConnection = GattConnectionImpl(bluetoothDevice, closeOnDisconnect)
    }

    val isConnected: Boolean
    /**
     * Suspends until a connection is established with the target device, or throws if an error
     * happens.
     *
     * It is **strongly recommended** to wrap this call with [withTimeout] as this method may never
     * resume if the target device is not in range. A timeout of about 5 seconds is recommended.
     *
     * Note that after any timeout, it is **your responsibility to close this [GattConnection]
     * instance**. However, this may change in a future version of the library.
     *
     * **There's a max concurrent GATT connections** which is 4 on Android 4.3 and
     * 7 on Android 4.4+. Keep in mind the user may already have a few connected devices such
     * as a SmartWatch, that top notch Bluetooth headset, or whatever which count as GATT
     * connections too. Call [close] when you don't need the connection anymore, or [disconnect]
     * if you don't need the connection to stay active for some amount of time.
     */
    suspend fun connect()

    /**
     * Suspends until the target device is disconnected, or throw if an error happens.
     *
     * Useful if you want to disconnect from the device for a relatively short time and connect
     * back later using this same instance.
     */
    suspend fun disconnect()

    /**
     * No need to disconnect first if you call this method.
     *
     * This [GattConnection] instance is no longer usable after this has been called, and all
     * coroutines suspended on calls to this instance will receive a cancellation signal.
     */
    fun close(notifyStateChangeChannel: Boolean = false)

    /** Reads the RSSI for a connected remote device */
    suspend fun readRemoteRssi(): Int

    /**
     * Use this method only if needed, and do so carefully.
     * See [BluetoothGatt.requestConnectionPriority].
     *
     * Accepted values are [BluetoothGatt.CONNECTION_PRIORITY_LOW_POWER],
     * [BluetoothGatt.CONNECTION_PRIORITY_BALANCED] (default) and
     * [BluetoothGatt.CONNECTION_PRIORITY_HIGH]. Read the documentation of the constant you use.
     *
     * Throws an [OperationInitiationFailedException] if the device is not connected.
     */
    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    fun requestPriority(priority: Int)

    /**
     * This is usually the first call you make after calling [connect].
     *
     * Discovers services offered by the remote device as well as its characteristics and
     * its descriptors.
     */
    suspend fun discoverServices(): List<BluetoothGattService>

    /**
     * Enable or disable notifications/indications for the passed [characteristic].
     * Once notifications are enabled for a characteristic, the [notifyChannel] will receive updated
     * characteristic if the remote device indicates that is has changed.
     *
     * Note that **there is a concurrent active notifications limit**, which, according to
     * [this video](https://youtu.be/qx55Sa8UZAQ?t=28m30s) is **4 on Android 4.3**,
     * 7 on Android 4.4, and 15 on Android 5.0+.
     */
    fun setCharacteristicNotificationsEnabled(characteristic: BGC, enable: Boolean)

    /**
     * This function requires that service discovery has been completed
     * for the given device.
     *
     * If multiple instances of the same service (as identified by UUID)
     * exist, the first instance of the service is returned.
     *
     * @param uuid UUID of the requested service
     * @return The service of the requested UUID if supported by the remote device, or null
     */
    fun getService(uuid: UUID): BluetoothGattService?

    suspend fun readCharacteristic(characteristic: BGC): BGC
    suspend fun writeCharacteristic(characteristic: BGC): BGC
    @RequiresApi(Build.VERSION_CODES.KITKAT)
    suspend fun reliableWrite(writeOperations: suspend GattConnection.() -> Unit)

    suspend fun readDescriptor(desc: BGD): BGD
    suspend fun writeDescriptor(desc: BGD): BGD
    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun readPhy(): Phy

    /**
     * **You should totally use this channel and implement the appropriate behavior for a production
     * app.**
     *
     * Dispatches fine grained connection changes, including errors.
     * You can consume this channel in an `async(UI) { … }` block (without awaiting completion,
     * unless you want to await until [close] is called) and perform the logic you want according
     * to connection state changes. For example, in case of a 133 status, you could retry connection
     * a few times by calling [connect] again, or call [close] and alert the user if needed after
     * multiple errors.
     * This channel will also receive disconnections that can happen if the device
     * gets out of range for example. It's then up to you to decide to retry [connect] a few times,
     * periodically, alert the user or call [close].
     */
    val stateChangeChannel: ReceiveChannel<StateChange>

    /**
     * Receives all characteristic update notifications.
     * See [setCharacteristicNotificationsEnabled].
     */
    val notifyChannel: ReceiveChannel<BGC>

    data class StateChange internal constructor(val status: Int, val newState: Int)
    data class Phy(val tx: Int, val rx: Int)
}
