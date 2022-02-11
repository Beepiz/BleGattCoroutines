plugins {
    lib
}

kotlin {
    sourceSets {
        all {
            languageSettings.apply {
                optIn("kotlin.RequiresOptIn")
                optIn("splitties.experimental.ExperimentalSplittiesApi")
                optIn("com.beepiz.bluetooth.gattcoroutines.ExperimentalBleGattCoroutinesCoroutinesApi")
            }
        }
        getByName("commonMain").dependencies {
            api(project(":blegattcoroutines-core"))
            api(project(":blegattcoroutines-genericaccess"))
            api(KotlinX.coroutines.core)
        }
        getByName("androidMain").dependencies {
            api(AndroidX.core.ktx)
            api(AndroidX.constraintLayout)
            api(JakeWharton.timber)
            api(Splitties.pack.androidBaseWithViewsDsl)
            api(Splitties.checkedlazy)
        }
    }
}
