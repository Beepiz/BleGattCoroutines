plugins {
    lib
    `lib-publish`
}

kotlin {
    sourceSets {
        getByName("commonMain").dependencies {
            api(project(":blegattcoroutines-core"))
        }
        all {
            languageSettings.apply {
                optIn("kotlin.RequiresOptIn")
            }
        }
    }
}
