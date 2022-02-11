package com.beepiz.blegattcoroutines.genericaccess

import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import com.beepiz.bluetooth.gattcoroutines.BGC
import com.beepiz.bluetooth.gattcoroutines.ExperimentalBleGattCoroutinesCoroutinesApi
import com.beepiz.bluetooth.gattcoroutines.GattConnection
import com.beepiz.bluetooth.gattcoroutines.extensions.requireCharacteristic
import java.util.UUID
import kotlin.experimental.and
import android.Manifest.permission.BLUETOOTH_CONNECT as BluetoothConnectPermission

/**
 * See [official docs here](https://www.bluetooth.com/specifications/gatt/viewer?attributeXmlFile=org.bluetooth.service.generic_access.xml).
 */
@RequiresApi(18)
@Suppress("InlinedApi")
@ExperimentalBleGattCoroutinesCoroutinesApi
object GenericAccess {

    val uuid: UUID = gattUuid(0x1800U)
    val deviceNameUuid: UUID = gattUuid(0x2A00U) // UTF-8
    /** See constants in [Appearance]. */
    val appearanceUuid: UUID = gattUuid(0x2A01U) // 16 bit


    @RequiresPermission(BluetoothConnectPermission)
    suspend fun GattConnection.readDeviceName(): Unit = read(deviceNameUuid)

    @RequiresPermission(BluetoothConnectPermission)
    suspend fun GattConnection.readAppearance() = read(appearanceUuid)

    val GattConnection.deviceName: String get() = get(deviceNameUuid).getStringValue(0)

    /** See constants in [Appearance]. */
    val GattConnection.appearance: Short
        get() = get(appearanceUuid).let {
            (it.value[1].toInt() shl 8 or (it.value[0] and 0xFF.toByte()).toInt()).toShort()
        }

    private fun GattConnection.get(characteristicUuid: UUID): BGC {
        return requireCharacteristic(uuid, characteristicUuid)
    }

    @RequiresPermission(BluetoothConnectPermission)
    private suspend fun GattConnection.read(characteristicUuid: UUID) {
        readCharacteristic(get(characteristicUuid))
    }

    /**
     * Generates fully-fledged UUID from passed [shorthand] and standard Bluetooth UUID common base.
     */
    private fun gattUuid(shorthand: UShort): UUID {
        val shifted16bits = shorthand.toULong() shl 32
        val mostSigBits = commonBaseMostSigBits or shifted16bits
        return UUID(mostSigBits.toLong(), commonBaseLeastSigBits.toLong())
    }

    /**
     * Most significant (left) part of the common base UUID: `00000000-0000-1000-8000-00805f9b34fb`.
     */
    private const val commonBaseMostSigBits: ULong = 0x0000_0000_0000_1000U

    /**
     * Least significant (right) part of the common base UUID: `00000000-0000-1000-8000-00805f9b34fb`.
     */
    private const val commonBaseLeastSigBits: ULong = 0x8000_00805f9b34fbU
}
