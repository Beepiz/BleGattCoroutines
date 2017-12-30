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
    val uuid = uuid("00001800-0000-1000-8000-00805f9b34fb")
    val deviceNameUuid = uuid("00002A00-0000-1000-8000-00805f9b34fb") // UTF-8
    /** See constants in [Appearance]. */
    val appearanceUuid = uuid("00002A01-0000-1000-8000-00805f9b34fb") // 16 bit

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

    @Suppress("NOTHING_TO_INLINE")
    private inline fun uuid(uuidString: String): UUID = UUID.fromString(uuidString)
}
