package com.beepiz.blegattcoroutines.sample.common

import android.arch.lifecycle.ViewModel
import android.os.Build.VERSION_CODES.JELLY_BEAN_MR2
import android.support.annotation.RequiresApi
import com.beepiz.blegattcoroutines.experimental.genericaccess.GenericAccess
import com.beepiz.blegattcoroutines.sample.common.extensions.deviceFor
import com.beepiz.blegattcoroutines.sample.common.extensions.toast
import com.beepiz.blegattcoroutines.sample.common.extensions.useBasic
import kotlinx.coroutines.experimental.Job
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch
import timber.log.Timber

class MainViewModel : ViewModel() {

    private val myEddystoneUrlBeaconMacAddress = "F2:D6:43:93:70:7A"
    private val iBks12MacAddress = "F6:61:CF:AF:D0:07"
    private val defaultDeviceMacAddress = iBks12MacAddress

    private var operationAttempt: Job? = null

    @RequiresApi(JELLY_BEAN_MR2)
    fun logNameAndAppearance(deviceMacAddress: String = defaultDeviceMacAddress) {
        operationAttempt?.cancel()
        operationAttempt = launch(UI) {
            deviceFor(deviceMacAddress).useBasic { device, services ->
                services.forEach { Timber.d("Service found with UUID: ${it.uuid}") }
                with(GenericAccess) {
                    device.readAppearance()
                    Timber.d("Device appearance: ${device.appearance}")
                    device.readDeviceName()
                    Timber.d("Device name: ${device.deviceName}".also { toast(it) })
                }
            }
            operationAttempt = null
        }
    }

    override fun onCleared() {
        operationAttempt?.cancel()
    }
}
