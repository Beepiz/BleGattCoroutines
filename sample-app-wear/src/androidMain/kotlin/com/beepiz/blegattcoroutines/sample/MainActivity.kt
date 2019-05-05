package com.beepiz.blegattcoroutines.sample

import android.Manifest
import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import com.beepiz.blegattcoroutines.sample.common.BleScanHeater
import com.beepiz.blegattcoroutines.sample.common.MainViewModel
import com.beepiz.blegattcoroutines.sample.common.register.registerWhileResumed
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.launch
import splitties.arch.lifecycle.ObsoleteSplittiesLifecycleApi
import splitties.arch.lifecycle.activityScope
import splitties.lifecycle.coroutines.lifecycleScope
import splitties.permissions.PermissionRequestResult
import splitties.permissions.requestPermission
import splitties.views.onClick

class MainActivity : FragmentActivity() {

    @UseExperimental(ObsoleteSplittiesLifecycleApi::class)
    private val viewModel by activityScope<MainViewModel>()
    private val bleScanHeater = BleScanHeater()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        lifecycleScope.launch {
            val result = requestPermission(Manifest.permission.ACCESS_FINE_LOCATION)
            if (result is PermissionRequestResult.Denied) {
                finish(); return@launch
            }
            btn_log_ble_device_name.onClick { viewModel.logNameAndAppearance() }
            registerWhileResumed(bleScanHeater)
        }
    }
}

