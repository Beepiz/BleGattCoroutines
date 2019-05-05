import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinAndroidTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinCommonCompilation
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinJsCompilation
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinJvmCompilation
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinOnlyTarget

inline fun KotlinMultiplatformExtension.jvmWithPublication(
    project: Project,
    crossinline configure: KotlinOnlyTarget<KotlinJvmCompilation>.() -> Unit = { }
) = jvm {
    mavenPublication { artifactId = "blegattcoroutines-${project.name}" }; configure()
}

inline fun KotlinMultiplatformExtension.androidWithPublication(
    project: Project,
    crossinline configure: KotlinAndroidTarget.() -> Unit = { }
) = android {
    publishLibraryVariants("release")
    mavenPublication { artifactId = "blegattcoroutines-${project.name}" }; configure()
}

inline fun KotlinMultiplatformExtension.jsWithPublication(
    project: Project,
    crossinline configure: KotlinOnlyTarget<KotlinJsCompilation>.() -> Unit = { }
) = js {
    mavenPublication { artifactId = "blegattcoroutines-${project.name}-js" }; configure()
}

inline fun KotlinMultiplatformExtension.metadataPublication(
    project: Project,
    crossinline configure: KotlinOnlyTarget<KotlinCommonCompilation>.() -> Unit = { }
) = metadata {
    mavenPublication { artifactId = "blegattcoroutines-${project.name}-metadata" }; configure()
}

object Publishing {
    const val gitUrl = "https://github.com/Beepiz/BleGattCoroutines.git"
    const val siteUrl = "https://github.com/Beepiz/BleGattCoroutines"
    const val libraryDesc = "Make Gatt Great Again! This library allows easy and safer usage of BluetoothGatt in Android"
}

@Suppress("UnstableApiUsage")
fun MavenPublication.setupPom() = pom {
    name.set("BleGattCoroutines")
    description.set(Publishing.libraryDesc)
    url.set(Publishing.siteUrl)
    licenses {
        license {
            name.set("The Apache Software License, Version 2.0")
            url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
        }
    }
    developers {
        developer {
            id.set("louiscad")
            name.set("Louis CAD")
            email.set("louis.cognault@gmail.com")
        }
    }
    scm {
        connection.set(Publishing.gitUrl)
        developerConnection.set(Publishing.gitUrl)
        url.set(Publishing.siteUrl)
    }
}

fun PublishingExtension.setupAllPublications(project: Project) {
    project.configurations.create("compileClasspath")
    //TODO: Remove line above when https://youtrack.jetbrains.com/issue/KT-27170 is fixed
    project.group = "com.beepiz.blegattcoroutines"
    project.version = ProjectVersions.thisLibrary
    val publications = publications.withType<MavenPublication>()
    publications.all(Action { setupPom() })
    publications.findByName("kotlinMultiplatform")?.apply {
        artifactId = "blegattcoroutines-${project.name}-mpp"
    }
}
