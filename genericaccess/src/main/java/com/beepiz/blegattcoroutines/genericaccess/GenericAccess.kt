package com.beepiz.blegattcoroutines.genericaccess

import android.os.Build.VERSION_CODES.JELLY_BEAN_MR2
import android.support.annotation.RequiresApi
import com.beepiz.bluetooth.gattcoroutines.experimental.BGC
import com.beepiz.bluetooth.gattcoroutines.experimental.GattConnection
import com.beepiz.bluetooth.gattcoroutines.experimental.extensions.get
import java.util.*
import kotlin.experimental.and

/**
 * See [official docs here](https://www.bluetooth.com/specifications/gatt/viewer?attributeXmlFile=org.bluetooth.service.generic_access.xml).
 */
@RequiresApi(JELLY_BEAN_MR2)
object GenericAccess {

    val uuid = gattUuid(0x1800)
    val deviceNameUuid = gattUuid(0x2A00) // UTF-8
    /** See constants in [Appearance]. */
    val appearanceUuid = gattUuid(0x2A01) // 16 bit

    suspend fun GattConnection.readDeviceName() = read(deviceNameUuid)
    suspend fun GattConnection.readAppearance() = read(appearanceUuid)

    val GattConnection.deviceName: String get() = get(deviceNameUuid).getStringValue(0)

    /** See constants in [Appearance]. */
    val GattConnection.appearance: Short
        get() = get(appearanceUuid).let {
            (it.value[1].toInt() shl 8 or (it.value[0] and 0xFF.toByte()).toInt()).toShort()
        }

    private fun GattConnection.get(characteristicUuid: UUID): BGC {
        val genericAccessService = getService(uuid) ?: throw IllegalStateException("Generic Access service not found. Make sure the service discovery has been performed!")
        return genericAccessService[characteristicUuid] ?: throw IllegalStateException("Characteristic with UUID $characteristicUuid not found. Make sure the service discovery has been performed!")
    }

    private suspend fun GattConnection.read(characteristicUuid: UUID) {
        readCharacteristic(get(characteristicUuid))
    }

    /**
     * Generates fully-fledged UUID from passed [shorthand] and standard Bluetooth UUID common base.
     */
    private fun gattUuid(shorthand: Short): UUID {
        val assignedNumberMask = 0x0000_FFFF_0000_0000L // Prevents negative shorthand side-effects.
        val shifted16bits = shorthand.toLong() shl 32 and assignedNumberMask
        val commonBaseMostSigBits = 0x0000_0000_0000_1000L
        val mostSigBits = commonBaseMostSigBits or shifted16bits
        val commonBaseLeastSigBits = -9223371485494954757 // unsigned notation: 0x8000_00805f9b34fb
        return UUID(mostSigBits, commonBaseLeastSigBits)
    }
}
