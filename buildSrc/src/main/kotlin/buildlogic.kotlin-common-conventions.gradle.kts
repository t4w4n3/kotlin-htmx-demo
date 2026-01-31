import org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jlleitschuh.gradle.ktlint.reporter.ReporterType.CHECKSTYLE
import org.jlleitschuh.gradle.ktlint.reporter.ReporterType.PLAIN

plugins {
    id("org.jetbrains.kotlin.jvm")
    id("org.jlleitschuh.gradle.ktlint")
}

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(libs("kotest-runner-junit6"))
    testImplementation(libs("kotest-assertions-core"))
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
        targetCompatibility = JavaVersion.VERSION_24
        sourceCompatibility = JavaVersion.VERSION_24
    }
}

kotlin {
    jvmToolchain(25)
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_24)
        allWarningsAsErrors.set(true)
        freeCompilerArgs.addAll(
            "-Wextra",
            "-Xreturn-value-checker=full",
            "-opt-in=kotlin.uuid.ExperimentalUuidApi",
        )
    }
}

tasks.named<Test>("test") {
    useJUnitPlatform()
    testLogging {
        events("failed")
        showStandardStreams = false
        showExceptions = true
        showCauses = true
        showStackTraces = false
        exceptionFormat = FULL
    }
}

configure<org.jlleitschuh.gradle.ktlint.KtlintExtension> {
    version.set(version("ktlint"))

    // ENABLE experimental rules if you want strict enforcement
    enableExperimentalRules.set(true)

    // OUTPUT format (checkstyle is useful for CI/CD parsers)
    reporters {
        reporter(PLAIN)
        reporter(CHECKSTYLE)
    }

    // HOOKS: Automatically install git hooks to prevent bad commits
    filter {
        exclude("**/generated/**")
        include("**/kotlin/**")
    }
}

// Make `check` task run ktlintFormat instead of ktlintCheck
// Disable all ktlint check tasks by name pattern
tasks.matching { it.name.contains("ktlint") && it.name.contains("Check", ignoreCase = true) }.configureEach {
    onlyIf { false }
}

tasks.named("check") {
    dependsOn("ktlintFormat")
}
