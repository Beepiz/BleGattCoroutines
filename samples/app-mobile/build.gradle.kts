@file:Suppress("SpellCheckingInspection")

plugins {
    id("com.android.application")
    kotlin("multiplatform")
}

android {
    compileSdk = ProjectVersions.androidSdk
    defaultConfig {
        applicationId = "com.beepiz.blegattcoroutines.sample"
        minSdk = 18
        targetSdk = ProjectVersions.androidSdk
        versionCode = 1
        versionName = ProjectVersions.readVersion(project)
        resourceConfigurations.add("en")
        proguardFile(getDefaultProguardFile("proguard-android-optimize.txt"))
        proguardFile("proguard-rules.pro")
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    signingConfigs {
        getByName("debug") {
            storeFile = file("${System.getProperty("user.home")}/.android/debug.keystore")
            keyAlias = "androiddebugkey"
            keyPassword = "android"
            storePassword = "android"
        }
    }
    buildTypes {
        getByName("debug") {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-DEBUG"
        }
        getByName("release") {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.getByName("debug")
        }
    }
    sourceSets.getByName("main") {
        manifest.srcFile("src/androidMain/AndroidManifest.xml")
        res.srcDir("src/androidMain/res")
    }
}

kotlin {
    android()
    sourceSets {
        all {
            languageSettings.apply {
                optIn("kotlin.RequiresOptIn")
                optIn("splitties.experimental.ExperimentalSplittiesApi")
                optIn("splitties.lifecycle.coroutines.PotentialFutureAndroidXLifecycleKtxApi")
                optIn("com.beepiz.bluetooth.gattcoroutines.ExperimentalBleGattCoroutinesCoroutinesApi")
            }
        }
        getByName("commonMain").dependencies {
            implementation(project(":samples:common"))
        }
        getByName("androidMain").dependencies {
            implementation(Splitties.pack.androidMdcWithViewsDsl)
        }
    }
}
