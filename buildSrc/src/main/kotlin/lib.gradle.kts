@file:Suppress("UnstableApiUsage")

import org.gradle.api.JavaVersion
import org.gradle.kotlin.dsl.kotlin
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmCompile

plugins {
    kotlin("multiplatform")
    id("com.android.library")
}

val optInAnnotations = listOf(
    "kotlin.RequiresOptIn"
)

kotlin {
    android {
        publishLibraryVariants("release")
    }
}

android {
    compileSdk = 31
    defaultConfig {
        minSdk = 14
        targetSdk = 31
    }
    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }
    sourceSets.getByName("main").apply {
        manifest.srcFile("src/androidMain/AndroidManifest.xml")
        res.srcDir("src/androidMain/res") // Android Studio picks last added directory.
    }
    sourceSets.getByName("debug").apply {
        res.srcDir("src/androidDebug/res")
    }
    sourceSets.getByName("release").apply {
        res.srcDir("src/androidRelease/res")
    }
    buildFeatures.buildConfig = false
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}

tasks.withType<KotlinJvmCompile>().configureEach {
    kotlinOptions.jvmTarget = "1.8"
    kotlinOptions.freeCompilerArgs += optInAnnotations.map { "-Xopt-in=$it" }
}
