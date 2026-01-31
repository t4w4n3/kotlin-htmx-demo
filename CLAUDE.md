# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code)
when working with code in this repository.

## Build & Development Commands

```bash
mise install                     # Install all dev tools
./gradlew build                  # Full build (+ ktlint + tailwind + tests)
./gradlew build -x buildTailwind # Build without tailwind
./gradlew :app-server:run        # Run server (http://localhost:8080)
./gradlew test                   # Run all tests
./gradlew :app-server:test       # Run tests for a single module
./gradlew ktlintFormat           # Auto-format Kotlin code
```

## Architecture

Multi-module Kotlin/Ktor project that renders server-side
HTML with HTMX for interactivity.

**Modules:**

- `app-server` — Ktor HTTP server (Netty, port 8080).
  Defines routes and the "smart response" pattern: serves
  full HTML pages for normal requests, but only the body
  fragment when the `HX-Request` header is present (HTMX
  partial swap). Uses `addShutdownHook` for graceful
  shutdown.
- `ui-library` — Shared kotlinx-html components and
  Tailwind CSS. The `corporateShell` function renders the
  full page layout (sidebar + content area). Runs the
  Tailwind 4 CLI during `processResources` via a custom
  Gradle task that resolves the binary through
  `mise which tailwindcss`.

**Build conventions (buildSrc):**

- `buildlogic.kotlin-common-conventions` — Base plugin
  applied to all modules. Sets JDK 25 toolchain / JVM 24
  target, strict Kotlin compiler flags
  (`allWarningsAsErrors`, `-Wextra`,
  `-Xreturn-value-checker=full`), Kotest dependencies,
  and ktlint with experimental rules. The `check` task
  runs `ktlintFormat` (auto-fix) rather than
  `ktlintCheck`.
- `buildlogic.kotlin-library-conventions` — Extends
  common, adds `java-library`. Used by `ui-library`.
- `buildlogic.kotlin-application-conventions` — Extends
  common, adds `application`. Used by `app-server`.
- `Lib.kt` — Helper extensions `Project.libs(name)` and
  `Project.version(name)` for accessing the version
  catalog from buildSrc convention plugins.

**Dependency management:** Centralized in
`gradle/libs.versions.toml`. Module build files use
type-safe `libs.xxx` accessors. Convention plugins in
buildSrc use the `libs()` helper from `Lib.kt` since
type-safe accessors are not generated there.

## E2E Testing (Playwright + Kotest)

Tests use `com.microsoft.playwright:playwright` (Java)
with Kotest — no Node.js test layer. Playwright
auto-downloads Chromium on first run.

Two reusable Kotest extensions in
`app-server/src/test/kotlin/com/enterprise/app/`:

- `ServerExtension` — Starts/stops the Ktor embedded
  server per spec. Exposes `baseUrl`.
- `PlaywrightExtension` — Manages Playwright + Chromium
  lifecycle per spec, creates a fresh
  `BrowserContext` + `Page` per test for isolation.
  Exposes `page`.

Register both extensions in a test spec:

```kotlin
class MyTest : FunSpec({
    val serverExtension = ServerExtension()
    val playwrightExtension = PlaywrightExtension()
    extension(serverExtension)
    extension(playwrightExtension)

    test("example") {
        val url = serverExtension.baseUrl
        playwrightExtension.page.navigate(
            "$url/dashboard",
        )
    }
})
```

**Kotest 6 API notes:** `TestResult` is at
`io.kotest.engine.test.TestResult`
(not `io.kotest.core.test`). Test name accessed via
`testCase.name.name` (not `.testName`).

## Key Conventions

- **Kotlin 2.3.0** with strict compiler options — all
  warnings are errors
- **Testing with Kotest 6** (runner-junit6 +
  assertions-core), zero JUnit
- **KtLint** with experimental rules enabled — no
  wildcard imports, `SCREAMING_SNAKE_CASE` for
  `const val`
- **Tailwind CSS 4** compiled via CLI binary managed
  by mise
- **Gradle** configuration cache, build cache, and
  parallel execution are all enabled
  (`gradle.properties`)
- **Logging** with `io.github.oshai:kotlin-logging-jvm`
  (SLF4J facade) and Logback Classic runtime
