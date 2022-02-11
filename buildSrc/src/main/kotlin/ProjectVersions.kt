import org.gradle.api.Project

object ProjectVersions {
    const val androidSdk = 31

    fun readVersion(
        project: Project
    ): String = project.providers.fileContents(
        project.rootProject.layout.projectDirectory.file("version.txt")
    ).asText.get().trim()
}
