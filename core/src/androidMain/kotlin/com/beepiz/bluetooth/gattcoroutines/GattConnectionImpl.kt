package com.beepiz.bluetooth.gattcoroutines

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothProfile
import android.os.Build.VERSION.SDK_INT
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import com.beepiz.bluetooth.gattcoroutines.GattConnection.Companion.clientCharacteristicConfiguration
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import splitties.bitflags.hasFlag
import splitties.init.appCtx
import java.util.UUID
import kotlin.coroutines.CoroutineContext
import android.Manifest.permission.BLUETOOTH_CONNECT as BluetoothConnectPermission

@RequiresApi(18)
private const val STATUS_SUCCESS = BluetoothGatt.GATT_SUCCESS

@RequiresApi(18)
@Suppress("InlinedApi")
@OptIn(ExperimentalBleGattCoroutinesCoroutinesApi::class)
internal class GattConnectionImpl
@RequiresPermission(BluetoothConnectPermission)
constructor(
    override val bluetoothDevice: BluetoothDevice,
    private val connectionSettings: GattConnection.ConnectionSettings
) : GattConnection, CoroutineScope {
    private val job = Job()
    override val coroutineContext: CoroutineContext = Dispatchers.Main + job

    init {
        require(bluetoothDevice.type != BluetoothDevice.DEVICE_TYPE_CLASSIC) {
            "Can't connect GATT to Bluetooth Classic device!"
        }
    }

    private val rssiChannel = Channel<GattResponse<Int>>()
    private val servicesDiscoveryChannel = Channel<GattResponse<List<BluetoothGattService>>>()
    private val readChannel = Channel<GattResponse<BGC>>()
    private val writeChannel = Channel<GattResponse<BGC>>()
    private val reliableWriteChannel = Channel<GattResponse<Unit>>()
    private val characteristicChangedFlow = MutableSharedFlow<BGC>(extraBufferCapacity = 1)
    private val readDescChannel = Channel<GattResponse<BGD>>()
    private val writeDescChannel = Channel<GattResponse<BGD>>()
    private val mtuChannel = Channel<GattResponse<Int>>()
    private val phyReadChannel = Channel<GattResponse<GattConnection.Phy>>()

    private val isConnectedMutableFlow = MutableStateFlow(false)

    private val isConnectedFlow = isConnectedMutableFlow.asStateFlow()

    override var isConnected: Boolean
        get() = isClosed.not() && isConnectedMutableFlow.value
        private set(value) {
            isConnectedMutableFlow.value = value
        }

    private var isClosed = false

    private var closedException: ConnectionClosedException? = null

    private val stateChangesMutableFlow = MutableSharedFlow<GattConnection.StateChange>(replay = 1)

    override val stateChanges = stateChangesMutableFlow.asSharedFlow()

    @Suppress("OverridingDeprecatedMember")
    @OptIn(ExperimentalCoroutinesApi::class)
    override val stateChangeChannel: ReceiveChannel<GattConnection.StateChange>
        get() = produce { stateChangesMutableFlow.collect { send(it) } }

    override val notifications: Flow<BGC>
        get() = characteristicChangedFlow

    @Suppress("OverridingDeprecatedMember")
    @OptIn(ExperimentalCoroutinesApi::class)
    override val notifyChannel: ReceiveChannel<BGC>
        get() = produce { notifications.collect { send(it) } }

    private var bluetoothGatt: BG? = null
    private fun requireGatt(): BG = bluetoothGatt ?: error("Call connect() first!")

    @RequiresPermission(BluetoothConnectPermission)
    override suspend fun connect() {
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
        isConnectedFlow.first { connected -> connected }
    }

    @RequiresPermission(BluetoothConnectPermission)
    override suspend fun disconnect() {
        require(connectionSettings.allowAutoConnect) {
            "Disconnect is not supported when auto connect is not allowed. Use close() instead."
        }
        checkNotClosed()
        requireGatt().disconnect()
        isConnectedFlow.first { connected -> !connected }
    }

    @RequiresPermission(BluetoothConnectPermission)
    override fun close(notifyStateChangeChannel: Boolean) {
        closeInternal(notifyStateChangeChannel, ConnectionClosedException())
    }

    @RequiresPermission(BluetoothConnectPermission)
    private fun closeInternal(notifyStateChangeChannel: Boolean, cause: ConnectionClosedException) {
        closedException = cause
        if (connectionSettings.disconnectOnClose) bluetoothGatt?.disconnect()
        bluetoothGatt?.close()
        isClosed = true
        isConnected = false
        if (notifyStateChangeChannel) stateChangesMutableFlow.tryEmit(
            value = GattConnection.StateChange(
                status = STATUS_SUCCESS,
                newState = BluetoothProfile.STATE_DISCONNECTED
            )
        )
        rssiChannel.close(cause)
        servicesDiscoveryChannel.close(cause)
        readChannel.close(cause)
        writeChannel.close(cause)
        reliableWriteChannel.close(cause)
        readDescChannel.close(cause)
        writeDescChannel.close(cause)
        job.cancel()
    }

    @RequiresPermission(BluetoothConnectPermission)
    override suspend fun readRemoteRssi() = gattRequest(rssiChannel) {
        readRemoteRssi()
    }

    @RequiresApi(21)
    @RequiresPermission(BluetoothConnectPermission)
    override fun requestPriority(priority: Int) {
        requireGatt().requestConnectionPriority(priority).checkOperationInitiationSucceeded()
    }

    @RequiresPermission(BluetoothConnectPermission)
    override suspend fun discoverServices(): List<BluetoothGattService> =
        gattRequest(servicesDiscoveryChannel) {
            discoverServices()
        }

    @RequiresPermission(BluetoothConnectPermission)
    override fun setCharacteristicNotificationsEnabled(characteristic: BGC, enable: Boolean) {
        requireGatt().setCharacteristicNotification(characteristic, enable)
            .checkOperationInitiationSucceeded()
    }

    @RequiresPermission(BluetoothConnectPermission)
    override suspend fun setCharacteristicNotificationsEnabledOnRemoteDevice(
        characteristic: BGC,
        enable: Boolean
    ) {
        require(characteristic.properties.hasFlag(BGC.PROPERTY_NOTIFY)) {
            "This characteristic doesn't support notification or doesn't come from discoverServices()."
        }
        val descriptor: BGD? = characteristic.getDescriptor(clientCharacteristicConfiguration)
        requireNotNull(descriptor) {
            "This characteristic misses the client characteristic configuration descriptor."
        }
        descriptor.value = if (enable) {
            BGD.ENABLE_NOTIFICATION_VALUE
        } else BGD.DISABLE_NOTIFICATION_VALUE
        writeDescriptor(descriptor)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @RequiresPermission(BluetoothConnectPermission)
    override fun openNotificationSubscription(
        characteristic: BGC,
        disableNotificationsOnChannelClose: Boolean
    ): ReceiveChannel<BGC> {
        require(characteristic.properties.hasFlag(BGC.PROPERTY_NOTIFY)) {
            "This characteristic doesn't support notification or doesn't come from discoverServices()."
        }
        return produce {
            setCharacteristicNotificationsEnabled(characteristic, enable = true)
            try {
                characteristicChangedFlow.collect {
                    if (it.uuid == characteristic.uuid) send(it)
                }
            } finally {
                if (disableNotificationsOnChannelClose) {
                    setCharacteristicNotificationsEnabled(characteristic, enable = false)
                }
            }
        }
    }

    override fun getService(uuid: UUID): BluetoothGattService? = requireGatt().getService(uuid)

    @RequiresPermission(BluetoothConnectPermission)
    override suspend fun readCharacteristic(characteristic: BGC) = gattRequest(readChannel) {
        readCharacteristic(characteristic)
    }

    @RequiresPermission(BluetoothConnectPermission)
    override suspend fun writeCharacteristic(characteristic: BGC) = gattRequest(writeChannel) {
        writeCharacteristic(characteristic)
    }

    @RequiresApi(19)
    @RequiresPermission(BluetoothConnectPermission)
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

    @RequiresPermission(BluetoothConnectPermission)
    override suspend fun readDescriptor(desc: BGD) = gattRequest(readDescChannel) {
        readDescriptor(desc)
    }

    @RequiresPermission(BluetoothConnectPermission)
    override suspend fun writeDescriptor(desc: BGD) = gattRequest(writeDescChannel) {
        writeDescriptor(desc)
    }

    @RequiresApi(26)
    @RequiresPermission(BluetoothConnectPermission)
    override suspend fun readPhy() = gattRequest(phyReadChannel) {
        readPhy().let { true }
    }

    @RequiresApi(21)
    @RequiresPermission(BluetoothConnectPermission)
    override suspend fun requestMtu(mtu: Int) = gattRequest(mtuChannel) {
        requestMtu(mtu)
    }

    private val callback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BG, status: Int, newState: Int) {
            when (status) {
                STATUS_SUCCESS -> isConnected = newState == BluetoothProfile.STATE_CONNECTED
            }
            stateChangesMutableFlow.tryEmit(
                GattConnection.StateChange(status = status, newState = newState)
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
            launch { characteristicChangedFlow.emit(characteristic) }
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
    private inline fun checkNotClosed() {
        if (isClosed) throw ConnectionClosedException(closedException)
    }

    private class GattResponse<out E>(val e: E, val status: Int) {
        inline val isSuccess get() = status == STATUS_SUCCESS
    }

    init {
        val closeOnDisconnect = connectionSettings.allowAutoConnect.not()
        if (closeOnDisconnect) launch {
            stateChanges.collect { stateChange ->
                if (stateChange.newState == BluetoothProfile.STATE_DISCONNECTED) {
                    val cause =
                        ConnectionClosedException(messageSuffix = " because of disconnection")
                    closeInternal(notifyStateChangeChannel = false, cause = cause)
                }
            }
        }
    }
}
