plugins {
    `kotlin-dsl`
}

repositories {
    google()
    mavenCentral()
    gradlePluginPortal()
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(11))
}

dependencies {
    compileOnly(gradleApi())
    api(Android.tools.build.gradlePlugin)
    api("org.jetbrains.kotlin:kotlin-gradle-plugin:_")
}
