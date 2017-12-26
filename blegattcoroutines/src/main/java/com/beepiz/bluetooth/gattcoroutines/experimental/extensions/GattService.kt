package com.beepiz.bluetooth.gattcoroutines.experimental.extensions

import android.bluetooth.BluetoothGattService
import com.beepiz.bluetooth.gattcoroutines.experimental.BGC
import java.util.*

operator fun BluetoothGattService.get(characteristicUUID: UUID): BGC? {
    return getCharacteristic(characteristicUUID)
}
