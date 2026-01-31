# Enterprise HTMX SPA

A multi-module Kotlin/Ktor application that renders
server-side HTML with [HTMX](https://htmx.org/) for
client-side interactivity. No JavaScript framework
required -- the server is the single source of truth
and HTMX handles partial page swaps for a SPA-like
experience.

## Tech Stack

| Component    | Version       |
| ------------ | ------------- |
| Kotlin       | 2.3.0         |
| Ktor         | 3.4.0 (Netty) |
| kotlinx-html | 0.12.0        |
| HTMX         | latest        |
| Tailwind CSS | 4 (CLI)       |
| Kotest       | 6.1.2         |
| Playwright   | 1.58.0 (Java) |
| JDK          | 25            |

## Architecture

```text
enterprise-htmx-spa/
├── app-server/        # Ktor HTTP server, routes
├── ui-library/        # kotlinx-html components, CSS
├── buildSrc/          # Gradle convention plugins
└── gradle/
    └── libs.versions.toml
```

### Modules

- **`app-server`** -- Ktor server on port 8080.
  Routes serve full HTML pages on normal requests,
  but only the body fragment when the `HX-Request`
  header is present (HTMX partial swap). The
  persistent sidebar stays in place while
  `#main-area` content swaps seamlessly.
- **`ui-library`** -- Type-safe HTML components built
  with kotlinx-html. The `corporateShell` function
  renders the full page layout (sidebar + content
  area). Tailwind 4 CSS is compiled via the CLI
  binary managed by [mise](https://mise.jdx.dev/).

### Smart Response Pattern

A single route handler serves both full pages and
HTMX partials:

```kotlin
suspend fun ApplicationCall.respondSmart(
    title: String,
    block: FlowContent.() -> Unit,
) {
    if (request.headers.contains("HX-Request")) {
        // fragment only
        respondHtml { body { block() } }
    } else {
        // full page
        respondHtml { corporateShell(title, block) }
    }
}
```

## Prerequisites

- [mise](https://mise.jdx.dev/) (manages JDK, Gradle,
  Tailwind CSS, and other tools)

```bash
mise install
```

## Build & Run

```bash
# Full build (ktlint + Tailwind + tests)
./gradlew build

# Build without Tailwind compilation
./gradlew build -x buildTailwind

# Run the server (http://localhost:8080)
./gradlew :app-server:run

# Run all tests
./gradlew test

# Auto-format Kotlin code
./gradlew ktlintFormat
```

## Docker

```bash
# Build
docker build -t enterprise-htmx-spa .

# Run
docker run -p 8080:8080 enterprise-htmx-spa
```

## Testing

E2E tests use Playwright (Java binding) with Kotest
-- no Node.js test layer. Playwright auto-downloads
Chromium on first run.

Two reusable Kotest extensions handle lifecycle:

- **`ServerExtension`** -- Starts/stops the Ktor
  server per spec. Exposes `baseUrl`.
- **`PlaywrightExtension`** -- Manages Playwright +
  Chromium per spec, creates a fresh
  `BrowserContext` + `Page` per test for isolation.
  Saves screenshots to `build/test-screenshots/`.

```kotlin
class MyTest : FunSpec({
    val serverExtension = ServerExtension()
    val playwrightExtension = PlaywrightExtension()
    extension(serverExtension)
    extension(playwrightExtension)

    test("loads the dashboard") {
        val url = serverExtension.baseUrl
        playwrightExtension.page.navigate(
            "$url/dashboard",
        )
    }
})
```

## Code Quality

- All compiler warnings are errors
  (`allWarningsAsErrors`, `-Wextra`,
  `-Xreturn-value-checker=full`)
- KtLint with experimental rules enabled
- Gradle configuration cache, build cache, and
  parallel execution enabled

## License

This project is licensed under the GNU General Public
License v3.0. See [LICENSE](LICENSE) for details.
