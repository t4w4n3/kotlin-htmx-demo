package com.enterprise.app

import io.github.oshai.kotlinlogging.KotlinLogging
import io.kotest.core.listeners.AfterSpecListener
import io.kotest.core.listeners.BeforeSpecListener
import io.kotest.core.spec.Spec
import io.ktor.server.engine.EmbeddedServer
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.netty.NettyApplicationEngine
import java.net.HttpURLConnection
import java.net.URI

private val logger = KotlinLogging.logger {}

private const val READINESS_POLL_MS = 50L
private const val READINESS_TIMEOUT_MS = 10_000L

class ServerExtension :
    BeforeSpecListener,
    AfterSpecListener {
    private lateinit var server: EmbeddedServer<NettyApplicationEngine, NettyApplicationEngine.Configuration>

    val baseUrl: String = "http://localhost:8080"

    override suspend fun beforeSpec(spec: Spec) {
        logger.info { "Starting Ktor server on $baseUrl" }
        server = embeddedServer(Netty, port = 8080) { module() }
        server.start(wait = false)
        waitUntilReady()
        logger.info { "Ktor server started" }
    }

    private fun waitUntilReady() {
        val deadline = System.currentTimeMillis() + READINESS_TIMEOUT_MS
        while (System.currentTimeMillis() < deadline) {
            try {
                val conn = URI("$baseUrl/dashboard").toURL().openConnection() as HttpURLConnection
                conn.connectTimeout = READINESS_POLL_MS.toInt()
                conn.readTimeout = READINESS_POLL_MS.toInt()
                conn.requestMethod = "GET"
                if (conn.responseCode == 200) return
            } catch (_: Exception) {
                // Server not ready yet
            }
            Thread.sleep(READINESS_POLL_MS)
        }
        error("Server did not become ready within ${READINESS_TIMEOUT_MS}ms")
    }

    override suspend fun afterSpec(spec: Spec) {
        logger.info { "Stopping Ktor server" }
        server.stop(gracePeriodMillis = 1000, timeoutMillis = 5000)
        logger.info { "Ktor server stopped" }
    }
}
