package com.beepiz.blegattcoroutines.sample

import android.Manifest
import android.annotation.SuppressLint
import android.os.Build.VERSION.SDK_INT
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.beepiz.blegattcoroutines.sample.common.BleScanHeater
import com.beepiz.blegattcoroutines.sample.common.MainViewModel
import kotlinx.coroutines.*
import splitties.dimensions.dip
import splitties.views.dsl.core.add
import splitties.views.dsl.core.button
import splitties.views.dsl.core.contentView
import splitties.views.dsl.core.lParams
import splitties.views.dsl.core.verticalLayout
import splitties.views.gravityCenterHorizontal
import splitties.views.onClick
import splitties.views.padding

@SuppressLint("SetTextI18n") // This is just a sample, English is enough.
class MainActivity : AppCompatActivity() {

    private val viewModel by viewModels<MainViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lifecycleScope.launch {

            ensureBlePermissionsOrFinishActivity()

            @Suppress("MissingPermission")
            if (SDK_INT >= 21) launch { BleScanHeater.heatUpWhileStarted(lifecycle) }

            @Suppress("MissingPermission")
            contentView = verticalLayout {
                padding = dip(16)
                val lp = lParams(gravity = gravityCenterHorizontal)
                add(button {
                    text = "Log name and appearance of default device"
                    onClick { viewModel.logNameAndAppearance() }
                }, lp)
            }
        }
    }

    private suspend fun ensureBlePermissionsOrFinishActivity() = ensureAllPermissions(
        permissionNames = when {
            SDK_INT >= 31 -> listOf(
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
