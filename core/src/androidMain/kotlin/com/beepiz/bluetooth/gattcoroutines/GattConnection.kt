package com.beepiz.bluetooth.gattcoroutines

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothGattService
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import com.beepiz.bluetooth.gattcoroutines.GattConnection.Companion.invoke
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*
import kotlinx.coroutines.flow.*
import java.util.UUID

/**
 * The entry point of BluetoothGatt with coroutines.
 *
 * Create an instance by passing a Low Energy compatible [BluetoothDevice], then
 * call [connect] when you need to perform operations, perform your operations, and when you're
 * done, call [close] or [disconnect].
 *
 * All the functions of this interface can throw one of the following exceptions:
 * - [ConnectionClosedException]
 * - [GattException] (either [OperationFailedException] or [OperationInitiationFailedException])
 *
 * You should catch them and react appropriately (fixing your code, fixing the device if you can,
 * closing the connection and retrying (with linear/exponential backoff), and/or inform the user,
 * allowing manual retry if appropriate and/or cancel.
 *
 * Note that [discoverServices] is usually the first call you want to make after calling [connect].
 *
 * The default implementation of this interface can be instantiated with the [invoke] operator
 * function.
 *
 * **For production apps, see [stateChangeChannel].**
 */
@ExperimentalBleGattCoroutinesCoroutinesApi
@Suppress("InlinedApi")
interface GattConnection {
    companion object {

        @RequiresApi(18)
        @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
        operator fun invoke(
            bluetoothDevice: BluetoothDevice,
            connectionSettings: ConnectionSettings = ConnectionSettings()
        ): GattConnection = GattConnectionImpl(bluetoothDevice, connectionSettings)

        /**
         * The characteristic used to enable notifications on the remote device in the
         * [setCharacteristicNotificationsEnabledOnRemoteDevice] function.
         *
         * Sources:
         * - [Android sample](https://github.com/googlesamples/android-BluetoothLeGatt/blob/74c0006b9e87112d09bc6ed2cb3fcb51b07af4a4/Application/src/main/java/com/example/android/bluetoothlegatt/SampleGattAttributes.java#L27)
         * - [Official Bluetooth website](https://www.bluetooth.com/specifications/gatt/descriptors/)
         * (look for `0x2902` or "Client Characteristic Configuration")
         */
        @JvmField
        val clientCharacteristicConfiguration: UUID =
            UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
    }

    @SuppressLint("InlinedApi")
    class ConnectionSettings(
        val autoConnect: Boolean = false,
        val allowAutoConnect: Boolean = autoConnect,
        val disconnectOnClose: Boolean = true,
        val transport: Int = BluetoothDevice.TRANSPORT_AUTO,
        val phy: Int = BluetoothDevice.PHY_LE_1M_MASK
    ) {
        init {
            if (autoConnect) require(allowAutoConnect)
        }
    }

    val bluetoothDevice: BluetoothDevice

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
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    suspend fun connect()

    /**
     * Suspends until the target device is disconnected, or throw if an error happens.
     *
     * Useful if you want to disconnect from the device for a relatively short time and connect
     * back later using this same instance.
     */
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    suspend fun disconnect()

    /**
     * No need to disconnect first if you call this method.
     *
     * This [GattConnection] instance is no longer usable after this has been called, and all
     * coroutines suspended on calls to this instance will receive a cancellation signal.
     */
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    fun close(notifyStateChangeChannel: Boolean = false)

    /** Reads the RSSI for a connected remote device */
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
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
    @RequiresApi(21)
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    fun requestPriority(priority: Int)

    /**
     * This is usually the first call you make after calling [connect].
     *
     * Discovers services offered by the remote device as well as its characteristics and
     * its descriptors.
     */
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    suspend fun discoverServices(): List<BluetoothGattService>

    /**
     * **Note:** You can use [openNotificationSubscription] that automatically calls this function
     * for you.
     *
     * Enable or disable notifications/indications for the passed [characteristic].
     * Once notifications are enabled for a characteristic, the [notifyChannel] will receive updated
     * characteristic if the remote device indicates that is has changed.
     *
     * Note that **there is a concurrent active notifications limit**, which, according to
     * [this video](https://youtu.be/qx55Sa8UZAQ?t=28m30s) is **4 on Android 4.3**,
     * 7 on Android 4.4, and 15 on Android 5.0+.
     */
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    fun setCharacteristicNotificationsEnabled(characteristic: BGC, enable: Boolean)

    /**
     * Enables notifications for that [characteristic] client-side (you still need to enable it
     * on the remote device) and returns a [ReceiveChannel] that will get the notifications for that
     * characteristic only.
     *
     * By default, [disableNotificationsOnChannelClose] is true, and will cause the notifications
     * to be disabled client-side when the channel is closed/consumed.
     *
     * **IMPORTANT**: On most BLE devices, you'll need to enable notifications on the remote device
     * too. You can do so with the [setCharacteristicNotificationsEnabledOnRemoteDevice] function.
     *
     * You can enable notifications on the remote device before or after calling this function, both
     * ways, notifications will be able to arrive once enabling on remote device completes.
     */
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    fun openNotificationSubscription(
        characteristic: BGC,
        disableNotificationsOnChannelClose: Boolean = true
    ): ReceiveChannel<BGC>

    /**
     * Checks that the passed [characteristic] supports notifications (it should come from
     * [discoverServices]), and writes the [clientCharacteristicConfiguration] with
     * [BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE] if [enable] is true, or
     * [BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE] otherwise.
     *
     * If successful, notifications for that characteristic should be enabled on the remote device
     * and you can now receive them from [openNotificationSubscription] (or from [notifyChannel] if
     * you called [setCharacteristicNotificationsEnabled] beforehand).
     */
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    suspend fun setCharacteristicNotificationsEnabledOnRemoteDevice(
        characteristic: BGC,
        enable: Boolean
    )

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

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    suspend fun readCharacteristic(characteristic: BGC): BGC

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    suspend fun writeCharacteristic(characteristic: BGC): BGC

    @RequiresApi(19)
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    suspend fun reliableWrite(writeOperations: suspend GattConnection.() -> Unit)

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    suspend fun readDescriptor(desc: BGD): BGD

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    suspend fun writeDescriptor(desc: BGD): BGD

    @RequiresApi(26)
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    suspend fun readPhy(): Phy

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    suspend fun requestMtu(mtu: Int): Int

    /**
     * **You should totally use this channel and implement the appropriate behavior for a production
     * app.**
     *
     * Dispatches fine grained connection changes, including errors.
     * You can consume this channel in a separate coroutine (without awaiting completion,
     * unless you want to await until [close] is called) and perform the logic you want according
     * to connection state changes. For example, in case of a 133 status, you could retry connection
     * a few times by calling [connect] again, or call [close] and alert the user if needed after
     * multiple errors.
     * This channel will also receive disconnections that can happen if the device
     * gets out of range for example. It's then up to you to decide to retry [connect] a few times,
     * periodically, alert the user or call [close].
     */
    val stateChanges: SharedFlow<StateChange>

    @Deprecated("Use stateChanges which returns a Flow", ReplaceWith("stateChanges"))
    val stateChangeChannel: ReceiveChannel<StateChange>

    /**
     * Receives all characteristic update notifications.
     *
     * If you need to get the notifications of only a specific characteristic, you may want to use
     * the [openNotificationSubscription] function instead.
     *
     * Since version 0.4.0, in the default implementation, this channel is backed by a
     * [BroadcastChannel], which means you can have multiple consumers as each time you get this
     * property, a new subscription is opened.
     *
     * For characteristic notifications to come in this channel, you need to enable it on
     * client-side (using [setCharacteristicNotificationsEnabled]), and they also need to be enabled
     * on the remote device (you can enable it with the
     * [setCharacteristicNotificationsEnabledOnRemoteDevice] function).
     */
    val notifications: Flow<BGC>

    @Deprecated("Use notifications which returns a Flow", ReplaceWith("notifications"))
    val notifyChannel: ReceiveChannel<BGC>

    data class StateChange internal constructor(val status: Int, val newState: Int)
    data class Phy(val tx: Int, val rx: Int)
}
