@file:Suppress("NOTHING_TO_INLINE")

package com.beepiz.blegattcoroutines.sample.common.extensions

import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattCharacteristic.*
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothGattService.SERVICE_TYPE_PRIMARY
import android.bluetooth.BluetoothGattService.SERVICE_TYPE_SECONDARY

fun BluetoothGattService.print(printCharacteristics: Boolean = true): String {
    return if (printCharacteristics) printWithCharacteristics() else printWithoutCharacteristics()
}

fun BluetoothGattService.printWithoutCharacteristics(): String = """UUID: $uuid
instance ID: $instanceId
type: $typeString
characteristics count: ${characteristics.count()}
included services count: ${includedServices?.count()}
"""

fun BluetoothGattService.printWithCharacteristics(): String = """UUID: $uuid
instance ID: $instanceId
type: $typeString
characteristics: {
${characteristics.joinToString { it.print() }.prependIndent()}
}
included services count: ${includedServices?.count()}
"""

fun BluetoothGattCharacteristic.print(): String = """UUID: $uuid
instance ID: $instanceId
permissions: $permissionsString
writeType: $writeTypeString
properties: $propertiesString
value: $value
stringValue: ${getStringValue(0)}
"""

fun BluetoothGattDescriptor.print(): String = """UUID: $uuid
permissions: $permissions
value: $value
characteristic: ${characteristic?.print()}
"""

private val BluetoothGattService.typeString: String
    get() = when (type) {
        SERVICE_TYPE_PRIMARY -> "PRIMARY"
        SERVICE_TYPE_SECONDARY -> "SECONDARY"
        else -> "UNKNOWN"
    }

private val BluetoothGattCharacteristic.writeTypeString: String
    get() = when (writeType) {
        WRITE_TYPE_DEFAULT -> "DEFAULT"
        WRITE_TYPE_NO_RESPONSE -> "NO_RESPONSE"
        WRITE_TYPE_SIGNED -> "SIGNED"
        else -> "UNKNOWN"
    }

private val BluetoothGattCharacteristic.propertiesString: String get() = propertiesString(properties)
private val BluetoothGattCharacteristic.permissionsString: String get() {
    return "$permissions"
    //return permissionsString(permissions)
}
@Suppress("DEPRECATION")
@Deprecated("Doesn't seem to work")
private val BluetoothGattDescriptor.permissionsString: String get() = permissionsString(permissions)

@Deprecated("Doesn't seem to work")
private fun permissionsString(permissions: Int): String = StringBuilder().apply {
    if (permissions.hasFlag(PERMISSION_READ)) append("READ, ")
    if (permissions.hasFlag(PERMISSION_READ_ENCRYPTED)) append("READ_ENCRYPTED, ")
    if (permissions.hasFlag(PERMISSION_READ_ENCRYPTED_MITM)) append("READ_ENCRYPTED_MITM, ")
    if (permissions.hasFlag(PERMISSION_WRITE)) append("WRITE, ")
    if (permissions.hasFlag(PERMISSION_WRITE_ENCRYPTED)) append("WRITE_ENCRYPTED, ")
    if (permissions.hasFlag(PERMISSION_WRITE_ENCRYPTED_MITM)) append("WRITE_ENCRYPTED_MITM, ")
    if (permissions.hasFlag(PERMISSION_WRITE_SIGNED)) append("WRITE_SIGNED, ")
    if (permissions.hasFlag(PERMISSION_WRITE_SIGNED_MITM)) append("WRITE_SIGNED_MITM, ")
}.toString()

private fun propertiesString(properties: Int): String = StringBuilder().apply {
    if (properties.hasFlag(PROPERTY_READ)) append("READ, ")
    if (properties.hasFlag(PROPERTY_WRITE)) append("WRITE, ")
    if (properties.hasFlag(PROPERTY_WRITE_NO_RESPONSE)) append("WRITE_NO_RESPONSE, ")
    if (properties.hasFlag(PROPERTY_SIGNED_WRITE)) append("SIGNED_WRITE, ")
    if (properties.hasFlag(PROPERTY_INDICATE)) append("INDICATE, ")
    if (properties.hasFlag(PROPERTY_NOTIFY)) append("NOTIFY, ")
    if (properties.hasFlag(PROPERTY_BROADCAST)) append("BROADCAST, ")
    if (properties.hasFlag(PROPERTY_EXTENDED_PROPS)) append("EXTENDED_PROPS, ")
}.toString()
