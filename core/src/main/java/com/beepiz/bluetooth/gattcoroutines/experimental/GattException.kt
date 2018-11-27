@file:Suppress("MemberVisibilityCanPrivate")

package com.beepiz.bluetooth.gattcoroutines.experimental

import android.bluetooth.BluetoothGatt

sealed class GattException(message: String? = null) : Exception(message) {

    companion object {
        /**
         * See all codes here: [https://android.googlesource.com/platform/external/bluetooth/bluedroid/+/android-5.1.0_r1/stack/include/gatt_api.h]
         */
        fun humanReadableStatusCode(statusCode: Int) = when (statusCode) {
            BluetoothGatt.GATT_SUCCESS -> "GATT_SUCCESS"
            BluetoothGatt.GATT_READ_NOT_PERMITTED -> "GATT_READ_NOT_PERMITTED"
            BluetoothGatt.GATT_WRITE_NOT_PERMITTED -> "GATT_WRITE_NOT_PERMITTED"
            BluetoothGatt.GATT_INSUFFICIENT_AUTHENTICATION -> "GATT_INSUFFICIENT_AUTHENTICATION"
            BluetoothGatt.GATT_REQUEST_NOT_SUPPORTED -> "GATT_REQUEST_NOT_SUPPORTED"
            BluetoothGatt.GATT_INSUFFICIENT_ENCRYPTION -> "GATT_INSUFFICIENT_ENCRYPTION"
            BluetoothGatt.GATT_INVALID_OFFSET -> "GATT_INVALID_OFFSET"
            BluetoothGatt.GATT_INVALID_ATTRIBUTE_LENGTH -> "GATT_INVALID_ATTRIBUTE_LENGTH"
            BluetoothGatt.GATT_CONNECTION_CONGESTED -> "GATT_CONNECTION_CONGESTED"
            BluetoothGatt.GATT_FAILURE -> "GATT_FAILURE"
            else -> "$statusCode"
        }
    }
}

class OperationInitiationFailedException : GattException()
/** @see BluetoothGatt */
class OperationFailedException(
    val statusCode: Int
) : GattException("status: ${humanReadableStatusCode(statusCode)}")
