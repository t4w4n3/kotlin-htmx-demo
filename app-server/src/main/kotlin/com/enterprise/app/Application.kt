package com.enterprise.app

import com.enterprise.ui.UI
import com.enterprise.ui.corporateShell
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.engine.addShutdownHook
import io.ktor.server.engine.embeddedServer
import io.ktor.server.html.respondHtml
import io.ktor.server.http.content.staticResources
import io.ktor.server.netty.Netty
import io.ktor.server.request.receiveParameters
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import kotlinx.html.FlowContent
import kotlinx.html.FormMethod
import kotlinx.html.body
import kotlinx.html.button
import kotlinx.html.div
import kotlinx.html.form
import kotlinx.html.h1
import kotlinx.html.h2
import kotlinx.html.id
import kotlinx.html.input
import kotlinx.html.label
import kotlinx.html.option
import kotlinx.html.p
import kotlinx.html.select
import kotlinx.html.span
import kotlinx.html.table
import kotlinx.html.tbody
import kotlinx.html.td
import kotlinx.html.th
import kotlinx.html.thead
import kotlinx.html.tr
import java.time.Instant

fun main() {
    embeddedServer(Netty, port = 8080) {
        module()
    }.apply {
        addShutdownHook { stop(gracePeriodMillis = 3000, timeoutMillis = 5000) }
        start(wait = true)
    }
}

fun Application.module() {
    routing {
        staticResources("/static", "static")

        get("/dashboard") {
            call.respondSmart("Dashboard") {
                h1(classes = "text-3xl font-bold") { +"Performance Metrics" }
                div {
                    attributes["id"] = "metrics"
                }
                button(classes = UI.BTN_PRIMARY + " mt-4") {
                    attributes["hx-post"] = "/api/refresh"
                    attributes["hx-target"] = "#metrics"
                    attributes["hx-swap"] = "innerHTML"
                    +"Refresh Data"
                }
            }
        }

        get("/reports") {
            call.respondSmart("Reports") {
                h1(classes = "text-3xl font-bold") { +"Reports" }
                reportsTabBar("weekly")
                div {
                    id = "reports-content"
                    weeklyTable(WEEKLY_REPORTS)
                }
            }
        }

        get("/reports/weekly") {
            call.respondHtml {
                body { weeklyTable(WEEKLY_REPORTS) }
            }
        }

        delete("/api/reports/{id}") {
            val id = call.pathParameters["id"]
            WEEKLY_REPORTS.removeIf { it.id == id }
            call.respondHtml { body {} }
        }

        get("/reports/monthly") {
            call.respondHtml {
                body { monthlyPanel("") }
            }
        }

        get("/api/reports/monthly/search") {
            val query = call.queryParameters["q"].orEmpty()
            val filtered =
                if (query.isBlank()) {
                    MONTHLY_ITEMS
                } else {
                    MONTHLY_ITEMS.filter {
                        it.contains(query, ignoreCase = true)
                    }
                }
            call.respondHtml {
                body { monthlyResultList(filtered) }
            }
        }

        get("/reports/quarterly") {
            call.respondHtml {
                body { quarterlyForm() }
            }
        }

        post("/api/reports/generate") {
            val params = call.receiveParameters()
            val quarter = params["quarter"].orEmpty()
            val name = params["name"].orEmpty()
            call.respondHtml {
                body {
                    div(classes = "bg-green-50 border border-green-200 rounded-lg p-4") {
                        p(classes = "font-semibold text-green-800") {
                            +"Report generated successfully"
                        }
                        p(classes = "text-sm text-green-700 mt-1") {
                            +"\"$name\" for $quarter created at "
                            span(classes = "font-mono") {
                                +Instant.now().toString()
                            }
                        }
                    }
                }
            }
        }

        post("/api/refresh") {
            call.respondHtml {
                body {
                    p(classes = "text-slate-600 mt-2") {
                        +"Last updated: "
                        span(classes = "font-semibold") {
                            +Instant.now().toString()
                        }
                    }
                }
            }
        }
    }
}

// --- Data ---

data class WeeklyReport(
    val id: String,
    val date: String,
    val title: String,
    val status: String,
)

val WEEKLY_REPORTS: MutableList<WeeklyReport> =
    mutableListOf(
        WeeklyReport("w1", "2025-01-27", "Sprint 12 Summary", "Complete"),
        WeeklyReport("w2", "2025-01-20", "Sprint 11 Summary", "Complete"),
        WeeklyReport("w3", "2025-01-13", "Sprint 10 Summary", "In Review"),
        WeeklyReport("w4", "2025-01-06", "Sprint 9 Summary", "Draft"),
    )

private val MONTHLY_ITEMS =
    listOf(
        "January 2025 - Revenue Analysis",
        "February 2025 - Customer Acquisition",
        "March 2025 - Infrastructure Costs",
        "April 2025 - Team Performance",
        "May 2025 - Product Roadmap Review",
        "June 2025 - Mid-Year Summary",
    )

// --- Shared HTML fragments ---

private fun FlowContent.reportsTabBar(active: String) {
    div(classes = "flex gap-1 border-b border-slate-200 mt-6 mb-6") {
        for ((tab, label) in listOf(
            "weekly" to "Weekly",
            "monthly" to "Monthly",
            "quarterly" to "Quarterly",
        )) {
            button(
                classes = if (tab == active) UI.TAB_ACTIVE else UI.TAB_INACTIVE,
            ) {
                attributes["hx-get"] = "/reports/$tab"
                attributes["hx-target"] = "#reports-content"
                attributes["hx-swap"] = "innerHTML"
                +label
            }
        }
    }
}

private fun FlowContent.weeklyTable(reports: List<WeeklyReport>) {
    table(classes = "w-full") {
        thead(classes = "bg-slate-50") {
            tr {
                th(classes = UI.TABLE_HEADER) { +"Date" }
                th(classes = UI.TABLE_HEADER) { +"Title" }
                th(classes = UI.TABLE_HEADER) { +"Status" }
                th(classes = UI.TABLE_HEADER) { +"" }
            }
        }
        tbody {
            for (report in reports) {
                tr(classes = "border-b border-slate-100") {
                    id = "report-${report.id}"
                    td(classes = UI.TABLE_CELL) { +report.date }
                    td(classes = UI.TABLE_CELL + " font-medium") {
                        +report.title
                    }
                    td(classes = UI.TABLE_CELL) {
                        span(classes = badgeClass(report.status)) {
                            +report.status
                        }
                    }
                    td(classes = UI.TABLE_CELL) {
                        button(classes = UI.BTN_SECONDARY + " text-xs") {
                            attributes["hx-delete"] =
                                "/api/reports/${report.id}"
                            attributes["hx-target"] =
                                "#report-${report.id}"
                            attributes["hx-swap"] = "outerHTML"
                            +"Delete"
                        }
                    }
                }
            }
        }
    }
}

private fun badgeClass(status: String): String =
    when (status) {
        "Complete" -> UI.BADGE_GREEN
        "In Review" -> UI.BADGE_YELLOW
        "Draft" -> UI.BADGE_RED
        else -> UI.BADGE_GREEN
    }

private fun FlowContent.monthlyPanel(query: String) {
    h2(classes = "text-lg font-semibold mb-3") { +"Monthly Reports" }
    input {
        attributes["type"] = "search"
        attributes["name"] = "q"
        attributes["placeholder"] = "Search reports..."
        attributes["value"] = query
        attributes["class"] = UI.INPUT + " w-full mb-4"
        attributes["hx-get"] = "/api/reports/monthly/search"
        attributes["hx-trigger"] = "keyup changed delay:300ms"
        attributes["hx-target"] = "#monthly-results"
        attributes["hx-swap"] = "innerHTML"
    }
    val filtered = MONTHLY_ITEMS
    div {
        id = "monthly-results"
        monthlyResultList(filtered)
    }
}

private fun FlowContent.monthlyResultList(items: List<String>) {
    if (items.isEmpty()) {
        p(classes = "text-slate-400 italic") { +"No matching reports." }
    } else {
        for (item in items) {
            div(classes = "p-3 border-b border-slate-100 text-sm") {
                +item
            }
        }
    }
}

private fun FlowContent.quarterlyForm() {
    h2(classes = "text-lg font-semibold mb-3") { +"Generate Report" }
    form {
        attributes["hx-post"] = "/api/reports/generate"
        attributes["hx-target"] = "#form-result"
        attributes["hx-swap"] = "innerHTML"
        method = FormMethod.post
        div(classes = "space-y-4") {
            div {
                label(classes = "block text-sm font-medium text-slate-700 mb-1") {
                    +"Quarter"
                }
                select(classes = UI.INPUT + " w-full") {
                    attributes["name"] = "quarter"
                    for (q in listOf("Q1", "Q2", "Q3", "Q4")) {
                        option { +q }
                    }
                }
            }
            div {
                label(classes = "block text-sm font-medium text-slate-700 mb-1") {
                    +"Report Name"
                }
                input(classes = UI.INPUT + " w-full") {
                    attributes["type"] = "text"
                    attributes["name"] = "name"
                    attributes["placeholder"] = "e.g. Q1 Financial Summary"
                    attributes["required"] = "true"
                }
            }
            button(classes = UI.BTN_PRIMARY) { +"Generate" }
        }
    }
    div { id = "form-result" }
}

// --- Smart Response ---

suspend fun ApplicationCall.respondSmart(
    title: String,
    block: FlowContent.() -> Unit,
) {
    if (request.headers.contains("HX-Request")) {
        respondHtml { body { block() } }
    } else {
        respondHtml { corporateShell(title, block) }
    }
}
