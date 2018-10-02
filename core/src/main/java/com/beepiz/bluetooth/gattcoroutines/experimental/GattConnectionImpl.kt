package com.beepiz.bluetooth.gattcoroutines.experimental

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothProfile
import android.os.Build
import android.os.Build.VERSION_CODES.JELLY_BEAN_MR2
import android.os.Build.VERSION_CODES.LOLLIPOP
import android.os.Build.VERSION_CODES.O
import android.support.annotation.RequiresApi
import com.beepiz.bluetooth.gattcoroutines.experimental.extensions.getValue
import com.beepiz.bluetooth.gattcoroutines.experimental.extensions.setValue
import kotlinx.coroutines.experimental.CoroutineScope
import kotlinx.coroutines.experimental.Dispatchers
import kotlinx.coroutines.experimental.Job
import kotlinx.coroutines.experimental.android.Main
import kotlinx.coroutines.experimental.channels.Channel
import kotlinx.coroutines.experimental.channels.ConflatedBroadcastChannel
import kotlinx.coroutines.experimental.channels.ReceiveChannel
import kotlinx.coroutines.experimental.channels.SendChannel
import kotlinx.coroutines.experimental.channels.consumeEach
import kotlinx.coroutines.experimental.channels.first
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.sync.Mutex
import kotlinx.coroutines.experimental.sync.withLock
import splitties.checkedlazy.uiLazy
import splitties.init.appCtx
import splitties.uithread.isUiThread
import java.util.*
import kotlin.coroutines.experimental.CoroutineContext

@RequiresApi(JELLY_BEAN_MR2)
private const val STATUS_SUCCESS = BluetoothGatt.GATT_SUCCESS

@RequiresApi(18)
internal class GattConnectionImpl(
        bluetoothDevice: BluetoothDevice,
        closeOnDisconnect: Boolean
) : GattConnection, CoroutineScope {
    private val job = Job()
    override val coroutineContext: CoroutineContext = Dispatchers.Main + job

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
    private val phyReadChannel = Channel<GattResponse<GattConnection.Phy>>()

    private val isConnectedBroadcastChannel = ConflatedBroadcastChannel(false)
    private val isConnectedChannel get() = isConnectedBroadcastChannel.openSubscription()
    override var isConnected by isConnectedBroadcastChannel; private set
    private var isClosed = false
    private var closedException: ConnectionClosedException? = null
    private val stateChangeBroadcastChannel = ConflatedBroadcastChannel<GattConnection.StateChange>()

    override val stateChangeChannel get() = stateChangeBroadcastChannel.openSubscription()

    override val notifyChannel: ReceiveChannel<BGC> get() = characteristicChangedChannel

    override suspend fun connect() {
        checkNotClosed()
        gatt.connect().checkOperationInitiationSucceeded()
        isConnectedChannel.first { connected -> connected }
    }

    override suspend fun disconnect() {
        checkNotClosed()
        gatt.disconnect()
        isConnectedChannel.first { connected -> !connected }
    }

    override fun close(notifyStateChangeChannel: Boolean) {
        closeInternal(notifyStateChangeChannel, ConnectionClosedException())
    }

    private fun closeInternal(notifyStateChangeChannel: Boolean, cause: ConnectionClosedException) {
        closedException = cause
        gatt.close()
        isClosed = true
        isConnected = false
        if (notifyStateChangeChannel) {
            stateChangeBroadcastChannel.offer(GattConnection.StateChange(STATUS_SUCCESS, BluetoothProfile.STATE_DISCONNECTED))
        }
        isConnectedBroadcastChannel.close(cause)
        rssiChannel.close(cause)
        servicesDiscoveryChannel.close(cause)
        readChannel.close(cause)
        writeChannel.close(cause)
        reliableWriteChannel.close(cause)
        characteristicChangedChannel.close(cause)
        readDescChannel.close(cause)
        writeDescChannel.close(cause)
        stateChangeBroadcastChannel.close(cause)
        job.cancel()
    }

    override suspend fun readRemoteRssi() = gattRequest(rssiChannel) {
        gatt.readRemoteRssi()
    }

    @RequiresApi(LOLLIPOP)
    override fun requestPriority(priority: Int) {
        gatt.requestConnectionPriority(priority).checkOperationInitiationSucceeded()
    }

    override suspend fun discoverServices(): List<BluetoothGattService> = gattRequest(servicesDiscoveryChannel) {
        gatt.discoverServices()
    }

    override fun setCharacteristicNotificationsEnabled(characteristic: BGC, enable: Boolean) {
        gatt.setCharacteristicNotification(characteristic, enable).checkOperationInitiationSucceeded()
    }

    override fun getService(uuid: UUID): BluetoothGattService? = gatt.getService(uuid)

    override suspend fun readCharacteristic(characteristic: BGC) = gattRequest(readChannel) {
        gatt.readCharacteristic(characteristic)
    }

    override suspend fun writeCharacteristic(characteristic: BGC) = gattRequest(writeChannel) {
        gatt.writeCharacteristic(characteristic)
    }

    @RequiresApi(Build.VERSION_CODES.KITKAT)
    override suspend fun reliableWrite(writeOperations: suspend GattConnection.() -> Unit) = gattRequest(reliableWriteChannel) {
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

    override suspend fun readDescriptor(desc: BGD) = gattRequest(readDescChannel) {
        gatt.readDescriptor(desc)
    }

    override suspend fun writeDescriptor(desc: BGD) = gattRequest(writeDescChannel) {
        gatt.writeDescriptor(desc)
    }

    @RequiresApi(O)
    override suspend fun readPhy() = gattRequest(phyReadChannel) {
        gatt.readPhy().let { true }
    }

    private val callback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BG, status: Int, newState: Int) {
            when (status) {
                STATUS_SUCCESS -> isConnected = newState == BluetoothProfile.STATE_CONNECTED
            }
            stateChangeBroadcastChannel.offer(GattConnection.StateChange(status, newState))
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
            launch { characteristicChangedChannel.send(characteristic) }
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
            phyReadChannel.send(GattConnection.Phy(txPhy, rxPhy), status)
        }
    }

    private fun Boolean.checkOperationInitiationSucceeded() {
        if (!this) throw OperationInitiationFailedException()
    }

    /** @see gattRequest */
    private val bleOperationMutex = Mutex()
    private val reliableWritesMutex = Mutex()
    private var reliableWriteOngoing = false

    private val gatt: BG by uiLazy {
        bluetoothDevice.connectGatt(appCtx, false, callback)
    }

    /**
     * We need to wait for one operation to fully complete before making another one to avoid
     * Bluetooth Gatt errors.
     */
    private suspend inline fun <E> gattRequest(ch: ReceiveChannel<GattResponse<E>>, operation: () -> Boolean): E {
        checkUiThread()
        checkNotClosed()
        val mutex = when {
            writeChannel === ch && reliableWriteOngoing -> reliableWritesMutex
            else -> bleOperationMutex
        }
        return mutex.withLock {
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
        launch {
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

    init {
        if (closeOnDisconnect) launch {
            stateChangeChannel.consumeEach { stateChange ->
                if (stateChange.newState == BluetoothProfile.STATE_DISCONNECTED) {
                    val cause = ConnectionClosedException(messageSuffix = " because of disconnection")
                    closeInternal(notifyStateChangeChannel = false, cause = cause)
                }
            }
        }
    }
}
