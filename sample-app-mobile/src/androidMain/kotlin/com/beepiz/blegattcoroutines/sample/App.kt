package com.beepiz.blegattcoroutines.sample

import android.app.Application
import timber.log.Timber

@Suppress("unused") // Referenced from AndroidManifest.xml, but there's a bug in IDE or lint.
class App : Application() {
    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) Timber.plant(Timber.DebugTree())
    }
}
