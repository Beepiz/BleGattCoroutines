package com.beepiz.bluetooth.gattcoroutines.experimental.extensions

import android.bluetooth.BluetoothGattService
import android.os.Build.VERSION_CODES.JELLY_BEAN_MR2
import android.support.annotation.RequiresApi
import com.beepiz.bluetooth.gattcoroutines.experimental.BGC
import java.util.*

@RequiresApi(JELLY_BEAN_MR2)
operator fun BluetoothGattService.get(characteristicUUID: UUID): BGC? {
    return getCharacteristic(characteristicUUID)
}
