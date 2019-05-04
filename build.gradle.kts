@file:Suppress("SpellCheckingInspection")

// Top-level build file where you can add configuration options common to all sub-projects/modules.

allprojects {
    repositories {
        setupForProject()
    }
}

task<Delete>("clean") {
    delete(rootProject.buildDir)
}
