@file:Suppress("PackageDirectoryMismatch")

import org.gradle.api.Project
import org.gradle.api.artifacts.dsl.RepositoryHandler
import org.gradle.api.artifacts.repositories.MavenArtifactRepository
import java.net.URI

fun RepositoryHandler.mavenCentralStaging(
    project: Project,
    legacyRepo: Boolean = false,
    sonatypeUsername: String? = project.propertyOrEnvOrNull("sonatype_username"),
    sonatypePassword: String? = project.propertyOrEnvOrNull("sonatype_password"),
    repositoryId: String?
): MavenArtifactRepository = maven {
    name = "MavenCentralStaging"
    val subDomain = when {
        legacyRepo -> "oss"
        else -> "s01.oss"
    }
    url = when (repositoryId) {
        null -> URI("https://$subDomain.sonatype.org/service/local/staging/deploy/maven2/")
        else -> URI("https://$subDomain.sonatype.org/service/local/staging/deployByRepositoryId/$repositoryId/")
    }
    credentials {
        username = sonatypeUsername
        password = sonatypePassword
    }
}

fun RepositoryHandler.sonatypeSnapshots(
    project: Project,
    legacyRepo: Boolean = false,
    sonatypeUsername: String? = project.propertyOrEnvOrNull("sonatype_username"),
    sonatypePassword: String? = project.propertyOrEnvOrNull("sonatype_password")
): MavenArtifactRepository = maven {
    name = "SonatypeSnapshots"
    val subDomain = when {
        legacyRepo -> "oss"
        else -> "s01.oss"
    }
    url = URI("https://$subDomain.sonatype.org/content/repositories/snapshots/")
    credentials {
        username = sonatypeUsername
        password = sonatypePassword
    }
}
