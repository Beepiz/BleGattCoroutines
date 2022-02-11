package com.beepiz.blegattcoroutines.sample.common

import android.Manifest
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.beepiz.blegattcoroutines.genericaccess.GenericAccess
import com.beepiz.blegattcoroutines.sample.common.extensions.deviceFor
import com.beepiz.blegattcoroutines.sample.common.extensions.useBasic
import kotlinx.coroutines.*
import splitties.toast.UnreliableToastApi
import splitties.toast.toast
import timber.log.Timber

@Suppress("InlinedApi")
class MainViewModel : ViewModel() {

    private val myEddystoneUrlBeaconMacAddress = "F2:D6:43:93:70:7A"
    private val iBks12MacAddress = "F6:61:CF:AF:D0:07"
    private val iBksPlusMacAddress = "DF:A7:85:12:7D:B1"
    private val defaultDeviceMacAddress = iBksPlusMacAddress

    private var operationAttempt: Job? = null

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    @RequiresApi(18)
    @OptIn(UnreliableToastApi::class)
    fun logNameAndAppearance(
        deviceMacAddress: String = defaultDeviceMacAddress,
        connectionTimeoutInMillis: Long = 5000L
    ) {
        operationAttempt?.cancel()
        operationAttempt = viewModelScope.launch(Dispatchers.Main) {
            deviceFor(deviceMacAddress).useBasic(connectionTimeoutInMillis) { device, services ->
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
}
