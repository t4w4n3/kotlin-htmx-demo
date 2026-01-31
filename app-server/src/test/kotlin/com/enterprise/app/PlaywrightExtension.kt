package com.enterprise.app

import com.microsoft.playwright.Browser
import com.microsoft.playwright.BrowserContext
import com.microsoft.playwright.Page
import com.microsoft.playwright.Page.ScreenshotOptions
import com.microsoft.playwright.Playwright
import io.github.oshai.kotlinlogging.KotlinLogging
import io.kotest.core.listeners.AfterSpecListener
import io.kotest.core.listeners.AfterTestListener
import io.kotest.core.listeners.BeforeSpecListener
import io.kotest.core.listeners.BeforeTestListener
import io.kotest.core.spec.Spec
import io.kotest.core.test.TestCase
import io.kotest.core.test.TestType
import io.kotest.engine.test.TestResult
import java.nio.file.Path

private val logger = KotlinLogging.logger {}

private val SCREENSHOT_DIR: Path = Path.of("build/test-screenshots")

class PlaywrightExtension :
    BeforeSpecListener,
    AfterSpecListener,
    BeforeTestListener,
    AfterTestListener {
    private lateinit var playwright: Playwright
    private lateinit var browser: Browser
    private lateinit var context: BrowserContext

    lateinit var page: Page
        private set

    override suspend fun beforeSpec(spec: Spec) {
        logger.info { "Creating Playwright instance and launching Chromium" }
        SCREENSHOT_DIR.toFile().mkdirs()
        playwright = Playwright.create()
        browser = playwright.chromium().launch()
        logger.info { "Chromium browser launched" }
    }

    override suspend fun beforeTest(testCase: TestCase) {
        if (testCase.type != TestType.Test) return
        logger.debug { "Creating new browser context for test: ${testCase.name.name}" }
        context = browser.newContext()
        page = context.newPage()
    }

    override suspend fun afterTest(
        testCase: TestCase,
        result: TestResult,
    ) {
        if (testCase.type != TestType.Test) return
        val safeName = testCase.name.name.replace(Regex("[^a-zA-Z0-9_-]"), "_")
        val screenshotPath = SCREENSHOT_DIR.resolve("$safeName.png")
        page.screenshot(ScreenshotOptions().setPath(screenshotPath).setFullPage(true))
        logger.info { "Screenshot saved: $screenshotPath" }
        logger.debug { "Closing browser context for test: ${testCase.name.name}" }
        context.close()
    }

    override suspend fun afterSpec(spec: Spec) {
        logger.info { "Closing Chromium browser and Playwright" }
        browser.close()
        playwright.close()
        logger.info { "Playwright resources released" }
    }
}
