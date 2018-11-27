package com.beepiz.bluetooth.gattcoroutines.extensions

import android.bluetooth.BluetoothGattService
import android.support.annotation.RequiresApi
import com.beepiz.bluetooth.gattcoroutines.BGC
import com.beepiz.bluetooth.gattcoroutines.BGD
import com.beepiz.bluetooth.gattcoroutines.ExperimentalBleGattCoroutinesCoroutinesApi
import com.beepiz.bluetooth.gattcoroutines.GattConnection
import java.util.*

@RequiresApi(18)
@ExperimentalBleGattCoroutinesCoroutinesApi
operator fun BluetoothGattService.get(characteristicUUID: UUID): BGC? {
    return getCharacteristic(characteristicUUID)
}

private const val ensureDiscoveryMsg = "Make sure the service discovery has been performed!"

/**
 * Returns a [BluetoothGattService] for the given [uuid], or throws a [NoSuchElementException] if
 * there's no such a service.
 */
@RequiresApi(18)
@ExperimentalBleGattCoroutinesCoroutinesApi
fun GattConnection.requireService(uuid: UUID): BluetoothGattService = getService(uuid)
    ?: throw NoSuchElementException("service($uuid) not found. $ensureDiscoveryMsg")

/**
 * Returns a [BGC] for the given [uuid], or throws a [NoSuchElementException] if there's no such a
 * characteristic.
 */
@RequiresApi(18)
@ExperimentalBleGattCoroutinesCoroutinesApi
fun BluetoothGattService.requireCharacteristic(uuid: UUID): BGC = getCharacteristic(uuid)
    ?: throw NoSuchElementException("service(${this.uuid}).characteristic($uuid)")

/**
 * Returns a [BGD] for the given [uuid], or throws a [NoSuchElementException] if there's no such a
 * descriptor.
 */
@RequiresApi(18)
@ExperimentalBleGattCoroutinesCoroutinesApi
fun BGC.requireDescriptor(uuid: UUID): BGD = getDescriptor(uuid) ?: throw NoSuchElementException(
    "service(${this.service.uuid}).characteristic(${this.uuid}).descriptor($uuid)"
)

/**
 * Returns a [BGC] for the given [serviceUuid] and [characteristicUuid], or throws a
 * [NoSuchElementException] if there's no such a characteristic.
 */
@RequiresApi(18)
@ExperimentalBleGattCoroutinesCoroutinesApi
fun GattConnection.requireCharacteristic(
    serviceUuid: UUID,
    characteristicUuid: UUID
): BGC = requireService(serviceUuid).requireCharacteristic(characteristicUuid)

/**
 * Returns a [BGD] for the given [serviceUuid], [characteristicUuid] and [descriptorUuid],
 * or throws a [NoSuchElementException] if there's no such a descriptor.
 */
@RequiresApi(18)
@ExperimentalBleGattCoroutinesCoroutinesApi
fun GattConnection.requireDescriptor(
    serviceUuid: UUID,
    characteristicUuid: UUID,
    descriptorUuid: UUID
): BGD = requireService(serviceUuid).requireCharacteristic(characteristicUuid).requireDescriptor(
    descriptorUuid
)
