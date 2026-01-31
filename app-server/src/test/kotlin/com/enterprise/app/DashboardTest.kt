package com.enterprise.app

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain

class DashboardTest :
    FunSpec({

        val serverExtension = ServerExtension()
        val playwrightExtension = PlaywrightExtension()

        extension(serverExtension)
        extension(playwrightExtension)

        fun page() = playwrightExtension.page

        context("layout") {
            test("full page renders with sidebar, main area, and title") {
                page().navigate("${serverExtension.baseUrl}/dashboard")

                page().locator("aside").first().shouldNotBeNull()
                page().locator("#main-area").first().shouldNotBeNull()
                page().title() shouldBe "Dashboard"
            }

            test("sidebar shows Enterprise OS branding") {
                page().navigate("${serverExtension.baseUrl}/dashboard")

                page().locator("aside").textContent() shouldContain "Enterprise OS"
            }

            test("nav contains Dashboard and Reports links") {
                page().navigate("${serverExtension.baseUrl}/dashboard")

                val nav = page().locator("nav")
                nav.locator("a[href='/dashboard']").textContent().trim() shouldBe "Dashboard"
                nav.locator("a[href='/reports']").textContent().trim() shouldBe "Reports"
            }

            test("nav has hx-boost and hx-target attributes") {
                page().navigate("${serverExtension.baseUrl}/dashboard")

                val nav = page().locator("nav")
                nav.getAttribute("hx-boost") shouldBe "true"
                nav.getAttribute("hx-target") shouldBe "#main-area"
            }
        }

        context("dashboard content") {
            test("Performance Metrics heading is visible") {
                page().navigate("${serverExtension.baseUrl}/dashboard")

                page().locator("h1").textContent().trim() shouldBe "Performance Metrics"
            }

            test("Refresh Data button is present with hx-post attribute") {
                page().navigate("${serverExtension.baseUrl}/dashboard")

                val button = page().locator("button:has-text('Refresh Data')")
                button.getAttribute("hx-post") shouldBe "/api/refresh"
            }
        }

        context("static resources") {
            test("/static/style.css returns HTTP 200") {
                val response = page().request().get("${serverExtension.baseUrl}/static/style.css")

                response.status() shouldBe 200
            }

            test("HTMX script tag is present in the page") {
                page().navigate("${serverExtension.baseUrl}/dashboard")

                val htmxScript = page().locator("script[src*='htmx']")
                htmxScript.count() shouldBe 1
            }
        }

        context("refresh endpoint") {
            test("clicking Refresh Data populates metrics area") {
                page().navigate("${serverExtension.baseUrl}/dashboard")

                page().locator("button:has-text('Refresh Data')").click()

                val metrics = page().locator("#metrics")
                metrics.locator("text=Last updated:").first().waitFor()
                metrics.textContent() shouldContain "Last updated:"
            }
        }

        context("reports page") {
            test("/reports returns HTTP 200 with Reports heading") {
                page().navigate("${serverExtension.baseUrl}/reports")

                page().title() shouldBe "Reports"
                page().locator("h1").textContent().trim() shouldBe "Reports"
            }

            test("reports page shows tab bar with Weekly, Monthly, Quarterly") {
                page().navigate("${serverExtension.baseUrl}/reports")

                val tabs = page().locator("#main-area button[hx-get^='/reports/']")
                tabs.count() shouldBe 3
                tabs.nth(0).textContent().trim() shouldBe "Weekly"
                tabs.nth(1).textContent().trim() shouldBe "Monthly"
                tabs.nth(2).textContent().trim() shouldBe "Quarterly"
            }

            test("weekly tab is loaded by default with a data table") {
                page().navigate("${serverExtension.baseUrl}/reports")

                val table = page().locator("#reports-content table")
                table.count() shouldBe 1
                page().locator("#reports-content tbody tr").count() shouldBe 4
            }
        }

        context("reports - tab switching") {
            test("clicking Monthly tab loads search panel via HTMX") {
                page().navigate("${serverExtension.baseUrl}/reports")

                page().locator("button:has-text('Monthly')").click()
                page().locator("#reports-content input[name='q']").waitFor()

                page().locator("#reports-content h2").textContent().trim() shouldBe "Monthly Reports"
            }

            test("clicking Quarterly tab loads form via HTMX") {
                page().navigate("${serverExtension.baseUrl}/reports")

                page().locator("button:has-text('Quarterly')").click()
                page().locator("#reports-content form").waitFor()

                page().locator("#reports-content h2").textContent().trim() shouldBe "Generate Report"
            }

            test("clicking Weekly tab after switching restores the table") {
                page().navigate("${serverExtension.baseUrl}/reports")

                page().locator("button:has-text('Monthly')").click()
                page().locator("#reports-content input[name='q']").waitFor()

                page().locator("button:has-text('Weekly')").click()
                page().locator("#reports-content table").waitFor()

                page().locator("#reports-content tbody tr").count() shouldBe 4
            }
        }

        context("reports - weekly delete") {
            test("clicking Delete removes a table row") {
                page().navigate("${serverExtension.baseUrl}/reports")

                val rowsBefore = page().locator("#reports-content tbody tr").count()
                page()
                    .locator("#reports-content tbody tr")
                    .first()
                    .locator("button:has-text('Delete')")
                    .click()

                page().waitForCondition { page().locator("#reports-content tbody tr").count() < rowsBefore }
                page().locator("#reports-content tbody tr").count() shouldBe rowsBefore - 1
            }
        }

        context("reports - monthly search") {
            test("typing in search filters the report list") {
                page().navigate("${serverExtension.baseUrl}/reports")

                page().locator("button:has-text('Monthly')").click()
                page().locator("#reports-content input[name='q']").waitFor()

                page().locator("input[name='q']").pressSequentially("Revenue")

                page().waitForCondition {
                    page().locator("#monthly-results div.p-3").count() == 1
                }
                page().locator("#monthly-results").textContent() shouldContain "Revenue"
            }

            test("clearing search shows all monthly reports") {
                page().navigate("${serverExtension.baseUrl}/reports")

                page().locator("button:has-text('Monthly')").click()
                page().locator("#reports-content input[name='q']").waitFor()

                val allCount = page().locator("#monthly-results div.p-3").count()

                page().locator("input[name='q']").pressSequentially("Revenue")
                page().waitForCondition { page().locator("#monthly-results div.p-3").count() == 1 }

                page().locator("input[name='q']").press("ControlOrMeta+a")
                page().locator("input[name='q']").press("Backspace")
                page().waitForCondition { page().locator("#monthly-results div.p-3").count() == allCount }
                page().locator("#monthly-results div.p-3").count() shouldBe allCount
            }
        }

        context("reports - quarterly form") {
            test("submitting the form shows success message") {
                page().navigate("${serverExtension.baseUrl}/reports")

                page().locator("button:has-text('Quarterly')").click()
                page().locator("#reports-content form").waitFor()

                page().locator("select[name='quarter']").selectOption("Q2")
                page().locator("input[name='name']").fill("Q2 Financial Summary")
                page().locator("form button:has-text('Generate')").click()

                page().locator("#form-result").waitFor()
                page().locator("#form-result").textContent() shouldContain "Report generated successfully"
                page().locator("#form-result").textContent() shouldContain "Q2 Financial Summary"
                page().locator("#form-result").textContent() shouldContain "Q2"
            }
        }

        context("HTMX navigation") {
            test("clicking Dashboard nav link swaps content without full page reload") {
                page().navigate("${serverExtension.baseUrl}/dashboard")

                // Set a canary attribute on aside to detect full page reload
                page().locator("aside").first().evaluate("el => el.setAttribute('data-canary', 'alive')")

                // Click the Dashboard link (HTMX should do a partial swap)
                page().locator("nav a[href='/dashboard']").click()

                // Wait for HTMX to finish swapping
                page().waitForLoadState()

                // Canary should still be present (no full page reload)
                page().locator("aside[data-canary='alive']").count() shouldBe 1

                // Content should still be the dashboard
                page().locator("#main-area h1").textContent().trim() shouldBe "Performance Metrics"
            }

            test("clicking Reports nav link swaps content without full page reload") {
                page().navigate("${serverExtension.baseUrl}/dashboard")

                page().locator("aside").first().evaluate("el => el.setAttribute('data-canary', 'alive')")

                page().locator("nav a[href='/reports']").click()

                // Wait for HTMX to swap in the Reports heading
                page().locator("#main-area h1:has-text('Reports')").waitFor()

                // Canary present means no full page reload (HTMX partial swap)
                page().locator("aside[data-canary='alive']").count() shouldBe 1

                page().locator("#main-area h1").textContent().trim() shouldBe "Reports"
            }
        }
    })
