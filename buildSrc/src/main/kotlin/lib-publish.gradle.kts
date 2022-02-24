plugins {
    `maven-publish`
    signing
}

group = "com.beepiz.blegattcoroutines"
version = ProjectVersions.readVersion(project)

signing {
    useInMemoryPgpKeys(
        propertyOrEnvOrNull("GPG_key_id"),
        propertyOrEnvOrNull("GPG_private_key") ?: return@signing,
        propertyOrEnv("GPG_private_password")
    )
    sign(publishing.publications)
}

publishing {

    val mavenPublications = publications.withType<MavenPublication>()
    mavenPublications.all {
        artifact(project.tasks.emptyJavadocJar())
        setupPom()
    }
    repositories {
        mavenCentralStaging(
            project = project,
            repositoryId = System.getenv("sonatype_staging_repo_id")
        )
        sonatypeSnapshots(project = project)
    }
}
