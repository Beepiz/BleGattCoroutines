package com.beepiz.bluetooth.gattcoroutines.experimental

import android.bluetooth.*
import android.os.Build
import android.os.Build.VERSION_CODES.*
import android.support.annotation.RequiresApi
import kotlinx.coroutines.experimental.Deferred
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.channels.Channel
import kotlinx.coroutines.experimental.channels.ConflatedBroadcastChannel
import kotlinx.coroutines.experimental.channels.ReceiveChannel
import kotlinx.coroutines.experimental.channels.SendChannel
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.sync.Mutex
import kotlinx.coroutines.experimental.sync.withLock
import splitties.concurrency.LazyThreadSafetyPolicy.UI_THREAD
import splitties.concurrency.isUiThread
import splitties.concurrency.lazy
import splitties.init.appCtx
import splitties.init.consume
import java.util.*

typealias BG = BluetoothGatt
typealias BGC = BluetoothGattCharacteristic
typealias BGD = BluetoothGattDescriptor

@RequiresApi(JELLY_BEAN_MR2)
private const val STATUS_SUCCESS = BluetoothGatt.GATT_SUCCESS

class ConnectionClosedException(cause: Throwable? = null) : Exception("The connection has been irrevocably closed.", cause)

@RequiresApi(JELLY_BEAN_MR2)
class GattConnection(bluetoothDevice: BluetoothDevice) {

    init {
        checkUiThread()
        require(bluetoothDevice.type != BluetoothDevice.DEVICE_TYPE_CLASSIC) {
            "Can't connect GATT to Bluetooth Classic device!"
        }
    }

    private val rssiChannel = Channel<GattResponse<Int>>()
    private val servicesDiscoveryChannel = Channel<GattResponse<List<BluetoothGattService>>>()
    private val readChannel = Channel<GattResponse<BGC>>()
    private val writeChannel = Channel<GattResponse<BGC>>()
    private val reliableWriteChannel = Channel<GattResponse<Unit>>()
    private val characteristicChangedChannel = Channel<BGC>()
    private val readDescChannel = Channel<GattResponse<BGD>>()
    private val writeDescChannel = Channel<GattResponse<BGD>>()
    private val mtuChannel = Channel<GattResponse<Int>>()
    private val phyReadChannel = Channel<GattResponse<Phy>>()

    var isConnected = false
        private set(connected) {
            field = connected
            connectionGate.isLocked = !connected
        }
    private inline val isClosed get() = !isConnected && !connectionGate.isLocked
    private var closedException: ConnectionClosedException? = null
    val stateChangeChannel = ConflatedBroadcastChannel<StateChange>()
    val notifyChannel: ReceiveChannel<BGC> get() = characteristicChangedChannel

    fun connect(): Deferred<Unit> {
        checkNotClosed()
        gatt.connect().checkOperationInitiationSucceeded()
        return async(UI) { connectionGate.passThroughWhenUnlocked() }
    }

    fun disconnect(): Deferred<Unit> {
        checkNotClosed()
        gatt.disconnect()
        return async(UI) { connectionGate.awaitLock() }
    }

    /**
     * No need to disconnect first if you call this method.
     */
    fun close(notifyStateChangeChannel: Boolean = false) {
        val cause = ConnectionClosedException()
        closedException = cause
        gatt.close()
        isConnected = false
        connectionGate.isLocked = false
        if (notifyStateChangeChannel) {
            stateChangeChannel.offer(StateChange(STATUS_SUCCESS, BluetoothProfile.STATE_DISCONNECTED))
        }
        check(isClosed)
        rssiChannel.close(cause)
        servicesDiscoveryChannel.close(cause)
        readChannel.close(cause)
        writeChannel.close(cause)
        reliableWriteChannel.close(cause)
        characteristicChangedChannel.close(cause)
        readDescChannel.close(cause)
        writeDescChannel.close(cause)
        stateChangeChannel.close(cause)
    }

    suspend fun readRemoteRssi() = gattRequest(rssiChannel) {
        gatt.readRemoteRssi()
    }

    /**
     * Use this method only if needed, and do so carefully.
     * See [BluetoothGatt.requestConnectionPriority].
     *
     * Accepted values are [BluetoothGatt.CONNECTION_PRIORITY_LOW_POWER],
     * [BluetoothGatt.CONNECTION_PRIORITY_BALANCED] and
     * [BluetoothGatt.CONNECTION_PRIORITY_HIGH]. Read the documentation of the constant you use.
     *
     * Throws an [OperationInitiationFailedException] if the device is not connected.
     */
    @RequiresApi(LOLLIPOP)
    fun requestPriority(priority: Int) {
        gatt.requestConnectionPriority(priority).checkOperationInitiationSucceeded()
    }

    suspend fun discoverServices() = gattRequest(servicesDiscoveryChannel) {
        gatt.discoverServices()
    }

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
    fun getService(uuid: UUID): BluetoothGattService? = gatt.getService(uuid)

    suspend fun readCharacteristic(characteristic: BGC) = gattRequest(readChannel) {
        gatt.readCharacteristic(characteristic)
    }

    suspend fun writeCharacteristic(characteristic: BGC) = gattRequest(writeChannel) {
        gatt.writeCharacteristic(characteristic)
    }

    @RequiresApi(Build.VERSION_CODES.KITKAT)
    suspend fun reliableWrite(writeOperations: suspend GattConnection.() -> Unit) = gattRequest(reliableWriteChannel) {
        try {
            reliableWriteOngoing = true
            gatt.beginReliableWrite().checkOperationInitiationSucceeded()
            writeOperations()
            gatt.executeReliableWrite()
        } catch (e: Throwable) {
            gatt.abortReliableWrite()
            throw e
        } finally {
            reliableWriteOngoing = false
        }
    }

    suspend fun readDescriptor(desc: BGD) = gattRequest(readDescChannel) {
        gatt.readDescriptor(desc)
    }

    suspend fun writeDescriptor(desc: BGD) = gattRequest(writeDescChannel) {
        gatt.writeDescriptor(desc)
    }

    @RequiresApi(O)
    suspend fun readPhy() = gattRequest(phyReadChannel) {
        consume { gatt.readPhy() }
    }

    private val callback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BG, status: Int, newState: Int) {
            when (status) {
                STATUS_SUCCESS -> isConnected = newState == BluetoothProfile.STATE_CONNECTED
            }
            stateChangeChannel.offer(StateChange(status, newState))
        }

        override fun onReadRemoteRssi(gatt: BG, rssi: Int, status: Int) {
            rssiChannel.send(rssi, status)
        }

        override fun onServicesDiscovered(gatt: BG, status: Int) {
            servicesDiscoveryChannel.send(gatt.services, status)
        }

        override fun onCharacteristicRead(gatt: BG, characteristic: BGC, status: Int) {
            readChannel.send(characteristic, status)
        }

        override fun onCharacteristicWrite(gatt: BG, characteristic: BGC, status: Int) {
            writeChannel.send(characteristic, status)
        }

        override fun onReliableWriteCompleted(gatt: BG, status: Int) {
            reliableWriteChannel.send(Unit, status)
        }

        override fun onCharacteristicChanged(gatt: BG, characteristic: BGC) {
            launch(UI) { characteristicChangedChannel.send(characteristic) }
        }

        override fun onDescriptorRead(gatt: BG, descriptor: BGD, status: Int) {
            readDescChannel.send(descriptor, status)
        }

        override fun onDescriptorWrite(gatt: BG, descriptor: BGD, status: Int) {
            writeDescChannel.send(descriptor, status)
        }

        override fun onMtuChanged(gatt: BG, mtu: Int, status: Int) {
            mtuChannel.send(mtu, status)
        }

        override fun onPhyRead(gatt: BG, txPhy: Int, rxPhy: Int, status: Int) {
            phyReadChannel.send(Phy(txPhy, rxPhy), status)
        }
    }

    private fun Boolean.checkOperationInitiationSucceeded() {
        if (!this) throw OperationInitiationFailedException()
    }

    private val connectionGate = Gate(locked = !isConnected)
    /** @see gattRequest */
    private val bleOperationMutex = Mutex()
    private val reliableWritesMutex = Mutex()
    private var reliableWriteOngoing = false

    private val gatt: BG by lazy(mode = UI_THREAD) {
        bluetoothDevice.connectGatt(appCtx, false, callback)
    }

    /**
     * We need to wait for one operation to fully complete before making another one to avoid
     * Bluetooth Gatt errors.
     */
    private inline suspend fun <E> gattRequest(ch: ReceiveChannel<GattResponse<E>>, operation: () -> Boolean): E {
        checkUiThread()
        checkNotClosed()
        val mutex = when {
            writeChannel === ch && reliableWriteOngoing -> reliableWritesMutex
            else -> bleOperationMutex
        }
        return mutex.withLock {
            connectionGate.passThroughWhenUnlocked()
            checkNotClosed()
            operation().checkOperationInitiationSucceeded()
            val response = ch.receive()
            if (response.isSuccess) response.e else throw OperationFailedException(response.status)
        }
    }

    /**
     * This code is currently not fault tolerant. The channel is irrevocably closed if the GATT
     * status is not success.
     */
    private fun <E> SendChannel<GattResponse<E>>.send(e: E, status: Int) {
        val response = GattResponse(e, status)
        launch(UI) {
            send(response)
        }
    }

    @Suppress("NOTHING_TO_INLINE")
    private inline fun checkUiThread() = check(isUiThread) {
        "Only UI Thread is supported at the moment"
    }

    @Suppress("NOTHING_TO_INLINE")
    private inline fun checkNotClosed() {
        if (isClosed) throw ConnectionClosedException(closedException)
    }

    private class GattResponse<out E>(val e: E, val status: Int) {
        val isSuccess = status == STATUS_SUCCESS
    }

    data class StateChange internal constructor(val status: Int, val newState: Int)
    data class Phy(val tx: Int, val rx: Int)
}
