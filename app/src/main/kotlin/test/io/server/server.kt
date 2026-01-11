@file:Suppress("FunctionName")

package test.io.server

import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.jetty.jakarta.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.util.reflect.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.reflect.typeOf
import kotlin.time.Duration.Companion.seconds

fun newServer(logger: ServerLogger): EmbeddedServer<*, *> {
    return embeddedServer(
        factory = Jetty,
        configure = {
            connectors.add(EngineConnectorBuilder().apply {
                host = "127.0.0.1"
                port = 8080
            })
            connectionGroupSize = 2
            workerGroupSize = 5
            callGroupSize = 10
            shutdownGracePeriod = 2000
            shutdownTimeout = 3000
        },
        module = {
            install(RoutingRoot.Plugin)
            install(ContentNegotiation) {
                json(Json {
                    prettyPrint = true
                    isLenient = true
                    ignoreUnknownKeys = true
                    encodeDefaults = true
                    explicitNulls = false
                })
            }
            routing {
                get("/") {
                    call.respondText("Hello, world!")
                }
                get("/datetime") {
                    val startAt = System.currentTimeMillis()
                    val delay = call.queryParameters["delay"]?.toIntOrNull()
                    val reqId = call.request.header("X-RequestId")
                    logger("Got request with id: $reqId and delay: $delay")
                    delay((delay ?: 5).seconds)
                    call.respond(HttpStatusCode.OK, DateTime(), TypeInfo(DateTime::class, typeOf<DateTime>()))
                    logger("Request with id: $reqId completed; took: ${System.currentTimeMillis() - startAt}")
                }
            }
        }
    )
}

fun serverLog(message: String) {
    println("[Server] $message")
}

typealias ServerLogger = (String) -> Unit

fun ServerLogger(logging: Boolean = false): ServerLogger {
    return { if (logging) serverLog(it) }
}

private val ServerPort = AtomicInteger(8080)

fun withTestServer(logging: Boolean = false, block: suspend () -> Unit) = runBlocking {
    val logger = ServerLogger(logging)
    val server = newServer(logger)
    try {
        server.start(wait = false)
        delay(1000) // Wait for server to start and bind to port
        serverLog("Server started!")
    } catch (e: Exception) {
        serverLog("Failed to start server: ${e.message}")
        throw e
    }
    try {
        block()
    } finally {
        server.stop(100, 5000, TimeUnit.MILLISECONDS)
        serverLog("Server stopped!")
    }
}