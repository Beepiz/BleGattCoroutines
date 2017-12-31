package com.beepiz.blegattcoroutines.sample

import android.app.Application
import com.beepiz.blegattcoroutines.BuildConfig
import timber.log.Timber

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) Timber.plant(Timber.DebugTree())
    }
}
