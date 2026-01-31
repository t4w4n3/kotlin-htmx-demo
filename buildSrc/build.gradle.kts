import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    `kotlin-dsl`
}

repositories {
    gradlePluginPortal()
}

dependencies {
    implementation(libs.kotlin.gradle.plugin)
    implementation(libs.ktlint.gradle.plugin)
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
    }
}
