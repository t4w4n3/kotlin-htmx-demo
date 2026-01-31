package com.enterprise.ui

import kotlinx.html.FlowContent
import kotlinx.html.HTML
import kotlinx.html.a
import kotlinx.html.aside
import kotlinx.html.body
import kotlinx.html.div
import kotlinx.html.head
import kotlinx.html.id
import kotlinx.html.link
import kotlinx.html.main
import kotlinx.html.nav
import kotlinx.html.script
import kotlinx.html.title

object UI {
    const val BTN_PRIMARY = "bg-enterprise-blue text-white px-6 py-2 rounded-lg hover:brightness-110 transition-all active:scale-95"
    const val BTN_SECONDARY = "bg-slate-200 text-slate-700 px-4 py-2 rounded-lg hover:bg-slate-300 transition-all"
    const val NAV_LINK = "block p-3 rounded hover:bg-slate-800 text-slate-300 hover:text-white transition"
    const val TAB_ACTIVE = "px-4 py-2 font-semibold border-b-2 border-enterprise-blue text-enterprise-blue"
    const val TAB_INACTIVE = "px-4 py-2 text-slate-500 hover:text-slate-700 border-b-2 border-transparent transition"
    const val TABLE_HEADER = "px-4 py-3 text-left text-xs font-medium text-slate-500 uppercase tracking-wider"
    const val TABLE_CELL = "px-4 py-3 text-sm text-slate-700"
    const val BADGE_GREEN = "inline-block px-2 py-1 text-xs font-semibold rounded-full bg-green-100 text-green-800"
    const val BADGE_YELLOW = "inline-block px-2 py-1 text-xs font-semibold rounded-full bg-yellow-100 text-yellow-800"
    const val BADGE_RED = "inline-block px-2 py-1 text-xs font-semibold rounded-full bg-red-100 text-red-800"
    const val INPUT =
        "border border-slate-300 rounded-lg px-3 py-2 text-sm " +
            "focus:outline-none focus:ring-2 focus:ring-enterprise-blue focus:border-transparent"
}

fun HTML.corporateShell(
    title: String,
    content: FlowContent.() -> Unit,
) {
    head {
        title(title)
        link(rel = "stylesheet", href = "/static/style.css")
        script { src = "/static/htmx.min.js" }
    }
    body(classes = "flex h-screen bg-slate-50") {
        // Persistent Sidebar
        aside(classes = "w-64 bg-slate-900 flex-shrink-0 flex flex-col") {
            div(classes = "p-6 text-xl font-bold text-white") { +"Enterprise OS" }
            nav(classes = "flex-1 px-4") {
                attributes["hx-boost"] = "true"
                attributes["hx-target"] = "#main-area"
                a(href = "/dashboard", classes = UI.NAV_LINK) { +"Dashboard" }
                a(href = "/reports", classes = UI.NAV_LINK) { +"Reports" }
            }
        }
        // Dynamic Content Area
        main(classes = "flex-1 overflow-y-auto p-12") {
            id = "main-area"
            content()
        }
    }
}
