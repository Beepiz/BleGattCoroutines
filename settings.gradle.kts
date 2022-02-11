pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
    }
}

plugins {
    // See https://jmfayard.github.io/refreshVersions
    id("de.fayard.refreshVersions") version "0.40.1"
}

rootProject.name = "BleGattCoroutines"

@Suppress("UnstableApiUsage")
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenCentral()

        google().ensureGroups(
            "com.google.gms",
            "com.google.test.platform",
            "com.google.firebase",
            "org.chromium.net",
            "com.google.mlkit",
            "com.google.testing.platform",
            "com.google.devtools.ksp"
        ).ensureGroupsStartingWith(
            "androidx.",
            "com.android",
            "com.google.android.",
            "com.google.ar",
            "android.arch"
        )
    }
}

include {
    "core".withPrefix("blegattcoroutines-")
    "genericaccess".withPrefix("blegattcoroutines-")
    "samples" {
        "app-mobile"()
        "app-wear"()
        "common"()
    }
}

//region include DSL
class ModuleParentScope(
    private val name: String,
    private val parent: ModuleParentScope? = null
) {

    fun String.withPrefix(
        prefix: String,
        block: (ModuleParentScope.() -> Unit)? = null
    ) {
        invoke(gradleName = "$prefix$this", block)
    }

    operator fun String.invoke(
        gradleName: String = this,
        block: (ModuleParentScope.() -> Unit)? = null
    ) {
        check(startsWith(':').not())
        val moduleName = ":$this"
        val projectName = "$parentalPath$moduleName"
        include(projectName)
        if (gradleName != this) {
            project(moduleName).name = gradleName
        }
        block?.let { buildNode ->
            ModuleParentScope(
                name = moduleName,
                parent = this@ModuleParentScope
            ).buildNode()
        }
    }

    private val parentalPath: String =
        generateSequence(this) { it.parent }
            .map { it.name }.toList().reversed().joinToString("")

}

inline fun include(block: ModuleParentScope.() -> Unit) {
    ModuleParentScope("").block()
}
//endregion

//region MavenArtifactRepository extensions

fun MavenArtifactRepository.ensureGroups(vararg group: String): MavenArtifactRepository = apply {
    content { group.forEach { includeGroup(it) } }
}

fun MavenArtifactRepository.ensureGroupsByRegexp(vararg regexp: String): MavenArtifactRepository =
    apply {
        content { regexp.forEach { includeGroupByRegex(it) } }
    }

fun MavenArtifactRepository.ensureGroupsStartingWith(vararg regexp: String): MavenArtifactRepository =
    apply {
        content { regexp.forEach { includeGroupByRegex(it.replace(".", "\\.") + ".*") } }
    }

fun MavenArtifactRepository.ensureModules(vararg modules: String): MavenArtifactRepository = apply {
    content { modules.forEach { includeModule(it.substringBefore(':'), it.substringAfter(':')) } }
}

fun MavenArtifactRepository.ensureModulesByRegexp(vararg regexp: String): MavenArtifactRepository =
    apply {
        content {
            regexp.forEach { includeModuleByRegex(it.substringBefore(':'), it.substringAfter(':')) }
        }
    }

fun MavenArtifactRepository.ensureModulesStartingWith(vararg regexp: String): MavenArtifactRepository =
    apply {
        content {
            regexp.forEach {
                val groupRegex = it.substringBefore(':').replace(".", "\\.")
                val moduleNameRegex = it.substringAfter(':').replace(".", "\\.") + ".*"
                includeModuleByRegex(groupRegex, moduleNameRegex)
            }
        }
    }

//endregion
