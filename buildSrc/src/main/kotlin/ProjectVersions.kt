object ProjectVersions {
    const val androidBuildTools = "28.0.3"
    const val androidSdk = 28
    const val thisLibrary = "0.4.0-dev-001"
}

val isDevVersion = ProjectVersions.thisLibrary.contains("-dev-")
