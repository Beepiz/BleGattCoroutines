package com.beepiz.blegattcoroutines.sample

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.viewModels
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.beepiz.blegattcoroutines.sample.common.BleScanHeater
import com.beepiz.blegattcoroutines.sample.common.MainViewModel
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.*
import splitties.permissions.PermissionRequestResult
import splitties.permissions.requestPermission
import splitties.views.onClick

class MainActivity : FragmentActivity() {

    private val viewModel by viewModels<MainViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        lifecycleScope.launch {
            ensureBlePermissionOrFinishActivity()
            val result = requestPermission(Manifest.permission.ACCESS_FINE_LOCATION)
            if (result is PermissionRequestResult.Denied) {
                finish(); return@launch
            }

            @Suppress("MissingPermission")
            btn_log_ble_device_name.onClick { viewModel.logNameAndAppearance() }

            @Suppress("MissingPermission")
            launch { BleScanHeater.heatUpWhileStarted(lifecycle) }
        }
    }

    private suspend fun ensureBlePermissionOrFinishActivity() = ensureAllPermissions(
        permissionNames = when {
            Build.VERSION.SDK_INT >= 31 -> listOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
            else -> listOf(Manifest.permission.ACCESS_FINE_LOCATION)
        },
        askDialogTitle = "Location permission required",
        askDialogMessage = "Bluetooth Low Energy can be used for location, " +
                "so the permission is required."
    ) { finish(); awaitCancellation() }
}
