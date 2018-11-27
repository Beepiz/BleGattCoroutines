package com.beepiz.bluetooth.gattcoroutines.experimental.extensions

import android.bluetooth.BluetoothGattService
import android.support.annotation.RequiresApi
import com.beepiz.bluetooth.gattcoroutines.experimental.BGC
import com.beepiz.bluetooth.gattcoroutines.experimental.BGD
import com.beepiz.bluetooth.gattcoroutines.experimental.GattConnection
import java.util.*

@RequiresApi(18)
operator fun BluetoothGattService.get(characteristicUUID: UUID): BGC? {
    return getCharacteristic(characteristicUUID)
}

/**
 * Returns a [BluetoothGattService] for the given [uuid], or throws a [NoSuchElementException] if
 * there's no such a service.
 */
@RequiresApi(18)
fun GattConnection.requireService(uuid: UUID): BluetoothGattService = getService(uuid)
    ?: throw NoSuchElementException("service($uuid)")

/**
 * Returns a [BGC] for the given [uuid], or throws a [NoSuchElementException] if there's no such a
 * characteristic.
 */
@RequiresApi(18)
fun BluetoothGattService.requireCharacteristic(uuid: UUID): BGC = getCharacteristic(uuid)
    ?: throw NoSuchElementException("service(${this.uuid}).characteristic($uuid)")

/**
 * Returns a [BGD] for the given [uuid], or throws a [NoSuchElementException] if there's no such a
 * descriptor.
 */
@RequiresApi(18)
fun BGC.requireDescriptor(uuid: UUID): BGD = getDescriptor(uuid) ?: throw NoSuchElementException(
    "service(${this.service.uuid}).characteristic(${this.uuid}).descriptor($uuid)"
)

/**
 * Returns a [BGC] for the given [serviceUuid] and [characteristicUuid], or throws a
 * [NoSuchElementException] if there's no such a characteristic.
 */
@RequiresApi(18)
fun GattConnection.requireCharacteristic(
    serviceUuid: UUID,
    characteristicUuid: UUID
): BGC = requireService(serviceUuid).requireCharacteristic(characteristicUuid)

/**
 * Returns a [BGD] for the given [serviceUuid], [characteristicUuid] and [descriptorUuid],
 * or throws a [NoSuchElementException] if there's no such a descriptor.
 */
@RequiresApi(18)
fun GattConnection.requireDescriptor(
    serviceUuid: UUID,
    characteristicUuid: UUID,
    descriptorUuid: UUID
): BGD = requireService(serviceUuid).requireCharacteristic(characteristicUuid).requireDescriptor(
    descriptorUuid
)
