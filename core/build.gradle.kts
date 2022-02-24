plugins {
    lib
    `lib-publish`
}

kotlin {
    sourceSets {
        getByName("commonMain").dependencies {
            api(KotlinX.coroutines.core)
        }
        getByName("androidMain").dependencies {
            api(AndroidX.annotation)
            implementation(Splitties.appctx)
            implementation(Splitties.bitflags)
            implementation(Splitties.checkedlazy)
            implementation(Splitties.lifecycleCoroutines)
            implementation(Splitties.mainthread)
        }
        all {
            languageSettings.apply {
                optIn("kotlin.RequiresOptIn")
            }
        }
    }
}
