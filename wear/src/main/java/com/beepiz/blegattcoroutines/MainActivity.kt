package com.beepiz.blegattcoroutines

import android.os.Bundle
import android.support.v4.app.FragmentActivity
import com.beepiz.blegattcoroutines.sample.common.MainViewModel
import com.beepiz.blegattcoroutines.sample.common.extensions.activityScope
import com.beepiz.blegattcoroutines.sample.common.views.onClick
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : FragmentActivity() {

    private val viewModel by activityScope<MainViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        btn_log_ble_device_name.onClick { viewModel.logNameAndAppearance() }
    }
}
