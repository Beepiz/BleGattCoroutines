package com.beepiz.bluetooth.gattcoroutines

import android.bluetooth.*
import android.os.Build.VERSION.SDK_INT
import androidx.annotation.RequiresApi
import com.beepiz.bluetooth.gattcoroutines.extensions.offerCatching
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import splitties.init.appCtx
import splitties.mainthread.isMainThread
import java.util.*
import kotlin.coroutines.CoroutineContext

@RequiresApi(18)
private const val STATUS_SUCCESS = BluetoothGatt.GATT_SUCCESS

@RequiresApi(18)
@ObsoleteCoroutinesApi
@ExperimentalCoroutinesApi
@ExperimentalBleGattCoroutinesCoroutinesApi
internal class GattConnectionImpl(
    override val bluetoothDevice: BluetoothDevice,
    private val connectionSettings: GattConnection.ConnectionSettings
) : GattConnection, CoroutineScope {
    private val job = Job()
    override val coroutineContext: CoroutineContext = Dispatchers.Main + job

    init {
        checkMainThread()
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
    override var isConnected: Boolean
        get() = runCatching { !isClosed && isConnectedBroadcastChannel.value }.getOrDefault(false)
        private set(value) = isConnectedBroadcastChannel.offerCatching(value).let { Unit }
    private var isClosed = false
    private var closedException: ConnectionClosedException? = null
    private val stateChangeBroadcastChannel =
        ConflatedBroadcastChannel<GattConnection.StateChange>()

    override val stateChangeChannel get() = stateChangeBroadcastChannel.openSubscription()

    override val notifyChannel: ReceiveChannel<BGC> get() = characteristicChangedChannel

    private var bluetoothGatt: BG? = null
    private fun requireGatt(): BG = bluetoothGatt ?: error("Call connect() first!")

    override suspend fun connect() {
        checkMainThread()
        checkNotClosed()
        val gatt = bluetoothGatt
        if (gatt == null) {
            val device = bluetoothDevice
            bluetoothGatt = with(connectionSettings) {
                when {
                    SDK_INT >= 26 -> device.connectGatt(
                        appCtx,
                        autoConnect,
                        callback,
                        transport,
                        phy
                    )
                    SDK_INT >= 23 -> device.connectGatt(appCtx, autoConnect, callback, transport)
                    else -> device.connectGatt(appCtx, autoConnect, callback)
                }
            } ?: error("No BluetoothGatt instance returned. Is Bluetooth supported and enabled?")
        } else {
            require(connectionSettings.allowAutoConnect) {
                "Connecting more than once would implicitly enable auto connect, which is not" +
                        "allowed with current connection settings."
            }
            gatt.connect().checkOperationInitiationSucceeded()
        }
        isConnectedChannel.first { connected -> connected }
    }

    override suspend fun disconnect() {
        require(connectionSettings.allowAutoConnect) {
            "Disconnect is not supported when auto connect is not allowed. Use close() instead."
        }
        checkMainThread()
        checkNotClosed()
        requireGatt().disconnect()
        isConnectedChannel.first { connected -> !connected }
    }

    override fun close(notifyStateChangeChannel: Boolean) {
        closeInternal(notifyStateChangeChannel, ConnectionClosedException())
    }

    private fun closeInternal(notifyStateChangeChannel: Boolean, cause: ConnectionClosedException) {
        closedException = cause
        bluetoothGatt?.close()
        isClosed = true
        isConnected = false
        if (notifyStateChangeChannel) stateChangeBroadcastChannel.offerCatching(
            element = GattConnection.StateChange(
                status = STATUS_SUCCESS,
                newState = BluetoothProfile.STATE_DISCONNECTED
            )
        )
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
        readRemoteRssi()
    }

    @RequiresApi(21)
    override fun requestPriority(priority: Int) {
        requireGatt().requestConnectionPriority(priority).checkOperationInitiationSucceeded()
    }

    override suspend fun discoverServices(): List<BluetoothGattService> =
        gattRequest(servicesDiscoveryChannel) {
            discoverServices()
        }

    override fun setCharacteristicNotificationsEnabled(characteristic: BGC, enable: Boolean) {
        requireGatt().setCharacteristicNotification(characteristic, enable)
            .checkOperationInitiationSucceeded()
    }

    override fun getService(uuid: UUID): BluetoothGattService? = requireGatt().getService(uuid)

    override suspend fun readCharacteristic(characteristic: BGC) = gattRequest(readChannel) {
        readCharacteristic(characteristic)
    }

    override suspend fun writeCharacteristic(characteristic: BGC) = gattRequest(writeChannel) {
        writeCharacteristic(characteristic)
    }

    @RequiresApi(19)
    override suspend fun reliableWrite(writeOperations: suspend GattConnection.() -> Unit) =
        gattRequest(reliableWriteChannel) {
            try {
                reliableWriteOngoing = true
                requireGatt().beginReliableWrite().checkOperationInitiationSucceeded()
                writeOperations()
                requireGatt().executeReliableWrite()
            } catch (e: Throwable) {
                requireGatt().abortReliableWrite()
                throw e
            } finally {
                reliableWriteOngoing = false
            }
        }

    override suspend fun readDescriptor(desc: BGD) = gattRequest(readDescChannel) {
        readDescriptor(desc)
    }

    override suspend fun writeDescriptor(desc: BGD) = gattRequest(writeDescChannel) {
        writeDescriptor(desc)
    }

    @RequiresApi(26)
    override suspend fun readPhy() = gattRequest(phyReadChannel) {
        readPhy().let { true }
    }

    @RequiresApi(21)
    override suspend fun requestMtu(mtu: Int) = gattRequest(mtuChannel) {
        requestMtu(mtu)
    }

    private val callback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BG, status: Int, newState: Int) {
            when (status) {
                STATUS_SUCCESS -> isConnected = newState == BluetoothProfile.STATE_CONNECTED
            }
            stateChangeBroadcastChannel.offerCatching(
                GattConnection.StateChange(
                    status,
                    newState
                )
            )
        }

        override fun onReadRemoteRssi(gatt: BG, rssi: Int, status: Int) {
            rssiChannel.launchAndSendResponse(rssi, status)
        }

        override fun onServicesDiscovered(gatt: BG, status: Int) {
            servicesDiscoveryChannel.launchAndSendResponse(gatt.services, status)
        }

        override fun onCharacteristicRead(gatt: BG, characteristic: BGC, status: Int) {
            readChannel.launchAndSendResponse(characteristic, status)
        }

        override fun onCharacteristicWrite(gatt: BG, characteristic: BGC, status: Int) {
            writeChannel.launchAndSendResponse(characteristic, status)
        }

        override fun onReliableWriteCompleted(gatt: BG, status: Int) {
            reliableWriteChannel.launchAndSendResponse(Unit, status)
        }

        override fun onCharacteristicChanged(gatt: BG, characteristic: BGC) {
            launch { characteristicChangedChannel.send(characteristic) }
        }

        override fun onDescriptorRead(gatt: BG, descriptor: BGD, status: Int) {
            readDescChannel.launchAndSendResponse(descriptor, status)
        }

        override fun onDescriptorWrite(gatt: BG, descriptor: BGD, status: Int) {
            writeDescChannel.launchAndSendResponse(descriptor, status)
        }

        override fun onMtuChanged(gatt: BG, mtu: Int, status: Int) {
            mtuChannel.launchAndSendResponse(mtu, status)
        }

        override fun onPhyRead(gatt: BG, txPhy: Int, rxPhy: Int, status: Int) {
            phyReadChannel.launchAndSendResponse(
                GattConnection.Phy(
                    tx = txPhy,
                    rx = rxPhy
                ), status
            )
        }
    }

    private fun Boolean.checkOperationInitiationSucceeded() {
        if (!this) throw OperationInitiationFailedException()
    }

    /** @see gattRequest */
    private val bleOperationMutex = Mutex()
    private val reliableWritesMutex = Mutex()
    private var reliableWriteOngoing = false

    /**
     * We need to wait for one operation to fully complete before making another one to avoid
     * Bluetooth Gatt errors.
     */
    private suspend inline fun <E> gattRequest(
        ch: ReceiveChannel<GattResponse<E>>,
        operation: BluetoothGatt.() -> Boolean
    ): E {
        checkMainThread()
        checkNotClosed()
        val mutex = when {
            writeChannel === ch && reliableWriteOngoing -> reliableWritesMutex
            else -> bleOperationMutex
        }
        return mutex.withLock {
            checkNotClosed()
            requireGatt().operation().checkOperationInitiationSucceeded()
            val response = ch.receive()
            if (response.isSuccess) response.e else throw OperationFailedException(
                response.status
            )
        }
    }

    /**
     * This code is currently not fault tolerant. The channel is irrevocably closed if the GATT
     * status is not success.
     */
    private fun <E> SendChannel<GattResponse<E>>.launchAndSendResponse(e: E, status: Int) {
        launch {
            send(GattResponse(e, status))
        }
    }

    @Suppress("NOTHING_TO_INLINE")
    private inline fun checkMainThread() = check(isMainThread) {
        "Only UI Thread is supported at the moment"
    }

    @Suppress("NOTHING_TO_INLINE")
    private inline fun checkNotClosed() {
        if (isClosed) throw ConnectionClosedException(closedException)
    }

    private class GattResponse<out E>(val e: E, val status: Int) {
        inline val isSuccess get() = status == STATUS_SUCCESS
    }

    init {
        val closeOnDisconnect = connectionSettings.allowAutoConnect.not()
        if (closeOnDisconnect) launch {
            stateChangeChannel.consumeEach { stateChange ->
                if (stateChange.newState == BluetoothProfile.STATE_DISCONNECTED) {
                    val cause =
                        ConnectionClosedException(messageSuffix = " because of disconnection")
                    closeInternal(notifyStateChangeChannel = false, cause = cause)
                }
            }
        }
    }
}
