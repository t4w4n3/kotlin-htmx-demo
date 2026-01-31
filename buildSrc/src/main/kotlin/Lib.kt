import org.gradle.api.Project
import org.gradle.api.artifacts.MinimalExternalModuleDependency
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.provider.Provider
import org.gradle.kotlin.dsl.getByType

fun Project.libs(name: String): Provider<MinimalExternalModuleDependency> =
    this
        .extensions
        .getByType(VersionCatalogsExtension::class)
        .named("libs")
        .findLibrary(name)
        .orElseThrow { RuntimeException("lib not found in catalog : $name") }

fun Project.version(name: String): String =
    this
        .extensions
        .getByType(VersionCatalogsExtension::class)
        .named("libs")
        .findVersion(name)
        .orElseThrow { RuntimeException("version not found in catalog : $name") }
        .toString()
