package com.beepiz.blegattcoroutines.sample

import android.arch.lifecycle.ViewModel
import com.beepiz.blegattcoroutines.genericaccess.GenericAccess
import com.beepiz.blegattcoroutines.sample.extensions.deviceFor
import com.beepiz.blegattcoroutines.sample.extensions.useBasic
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch
import timber.log.Timber

class MainViewModel : ViewModel() {

    private val myEddystoneUrlBeaconMacAddress = "F2:D6:43:93:70:7A"
    private val defaultDeviceMacAddress = myEddystoneUrlBeaconMacAddress

    fun logNameAndAppearance(deviceMacAddress: String = defaultDeviceMacAddress) = launch(UI) {
        deviceFor(deviceMacAddress).useBasic { device, services ->
            services.forEach { Timber.d("Service found with UUID: ${it.uuid}") }
            with(GenericAccess) {
                device.readAppearance()
                Timber.d("Device appearance: ${device.appearance}")
                device.readDeviceName()
                Timber.d("Device name: ${device.deviceName}")
            }
        }
    }
}
