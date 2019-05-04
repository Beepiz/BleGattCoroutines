@file:Suppress("SpellCheckingInspection")

plugins {
    id("com.android.library")
    kotlin("multiplatform")
}

android {
    setDefaults()
}

kotlin {
    android()
    sourceSets {
        all {
            languageSettings.apply {
                useExperimentalAnnotation("kotlin.Experimental")
                useExperimentalAnnotation("splitties.experimental.ExperimentalSplittiesApi")
                useExperimentalAnnotation("splitties.lifecycle.coroutines.PotentialFutureAndroidXLifecycleKtxApi")
                useExperimentalAnnotation("com.beepiz.bluetooth.gattcoroutines.ExperimentalBleGattCoroutinesCoroutinesApi")
            }
        }
        getByName("commonMain").dependencies {
            api(kotlin("stdlib-common"))
            api(project(":core"))
            api(project(":genericaccess"))
        }
        getByName("androidMain").dependencies {
            with(Libs) {
                arrayOf(
                    kotlin.stdlibJdk7,
                    androidX.coreKtx,
                    androidX.constraintLayout,
                    timber,
                    kotlinX.coroutines.android,
                    splitties.pack.androidBaseWithViewsDsl,
                    splitties.checkedlazy,
                    splitties.archLifecycle
                )
            }.forEach { api(it) }
        }
    }
}
