plugins {
    id("buildlogic.kotlin-library-conventions")
}

dependencies {
    api(libs.kotlinx.html)
}

// Tailwind 4 CLI Task (resolved via mise, falls back to PATH, skipped when unavailable)
val tailwindBinary: String =
    providers
        .exec {
            commandLine("sh", "-c", "mise which tailwindcss 2>/dev/null || which tailwindcss 2>/dev/null || true")
            isIgnoreExitValue = true
        }.standardOutput.asText
        .get()
        .trim()

if (tailwindBinary.isNotEmpty()) {
    val buildTailwind =
        tasks.register<Exec>("buildTailwind") {
            group = "build"
            inputs.files("src/main/resources/css/input.css")
            inputs.files(fileTree("src/main/kotlin") { include("**/*.kt") })
            outputs.file("build/resources/main/static/style.css")
            commandLine(
                tailwindBinary,
                "-i",
                "src/main/resources/css/input.css",
                "-o",
                "build/resources/main/static/style.css",
                "--minify",
            )
        }

    tasks.named("processResources") { finalizedBy(buildTailwind) }
    tasks.named("jar") { dependsOn(buildTailwind) }
}
