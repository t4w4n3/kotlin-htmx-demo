plugins {
    alias(libs.plugins.ktor)
    id("buildlogic.kotlin-application-conventions")
}

dependencies {
    implementation(project(":ui-library"))
    implementation(libs.ktor.server.netty)
    implementation(libs.ktor.server.html.builder)
    testImplementation(libs.playwright)
    testImplementation(libs.kotlin.logging)
    testRuntimeOnly(libs.logback.classic)
}

application { mainClass = "com.enterprise.app.ApplicationKt" }
