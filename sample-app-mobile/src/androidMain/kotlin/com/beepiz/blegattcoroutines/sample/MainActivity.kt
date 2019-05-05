package com.beepiz.blegattcoroutines.sample

import android.Manifest
import android.annotation.SuppressLint
import android.os.Build.VERSION.SDK_INT
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import com.beepiz.blegattcoroutines.sample.common.BleScanHeater
import com.beepiz.blegattcoroutines.sample.common.MainViewModel
import com.beepiz.blegattcoroutines.sample.common.register.registerWhileResumed
import com.beepiz.blegattcoroutines.sample.common.register.registerWhileStarted
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import splitties.arch.lifecycle.activityScope
import splitties.dimensions.dip
import splitties.lifecycle.coroutines.createScope
import splitties.lifecycle.coroutines.lifecycleScope
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

    private val viewModel by activityScope<MainViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lifecycleScope.launch {
            ensureLocationPermissionOrFinishActivity()
            if (SDK_INT >= 21) registerWhileStarted(BleScanHeater())
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

    private suspend fun ensureLocationPermissionOrFinishActivity() = ensurePermission(
        permission = Manifest.permission.ACCESS_FINE_LOCATION,
        askDialogTitle = "Location permission required",
        askDialogMessage = "Bluetooth Low Energy can be used for location, " +
                "so the permission is required."
    ) { finish(); throw CancellationException() }
}
