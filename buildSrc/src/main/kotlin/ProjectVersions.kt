object ProjectVersions {
    const val androidBuildTools = "28.0.3"
    const val androidSdk = 28
    const val thisLibrary = "0.4.1"
}

val isDevVersion = ProjectVersions.thisLibrary.contains("-dev-")
