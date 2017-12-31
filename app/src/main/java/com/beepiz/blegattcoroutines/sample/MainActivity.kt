package com.beepiz.blegattcoroutines.sample

import android.annotation.SuppressLint
import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES.LOLLIPOP
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.Gravity
import com.beepiz.blegattcoroutines.sample.common.BleScanHeater
import com.beepiz.blegattcoroutines.sample.common.MainViewModel
import com.beepiz.blegattcoroutines.sample.common.extensions.activityScope
import com.beepiz.blegattcoroutines.sample.common.register.registerWhileResumed
import com.beepiz.blegattcoroutines.sample.common.viewdsl.*
import com.beepiz.blegattcoroutines.sample.common.views.onClick
import com.beepiz.blegattcoroutines.sample.common.views.padding

@SuppressLint("SetTextI18n") // This is just a sample, English is enough.
class MainActivity : AppCompatActivity() {

    private val viewModel by activityScope<MainViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        contentView = v(::verticalLayout) {
            padding = dip(16)
            val lp = lParams(gravity = Gravity.CENTER_HORIZONTAL)
            add(::button, lp) {
                text = "Log name and appearance of default device"
                onClick { viewModel.logNameAndAppearance() }
            }
        }
        if (SDK_INT >= LOLLIPOP) registerWhileResumed(BleScanHeater())
    }
}
