package test.io.server

import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.install
import io.ktor.server.engine.EmbeddedServer
import io.ktor.server.engine.EngineConnectorBuilder
import io.ktor.server.engine.embeddedServer
import io.ktor.server.jetty.jakarta.Jetty
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.request.header
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.RoutingRoot
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.ktor.util.reflect.TypeInfo
import kotlinx.coroutines.delay
import kotlinx.serialization.json.Json
import kotlin.reflect.typeOf
import kotlin.time.Duration.Companion.seconds

fun newServer(): EmbeddedServer<*, *> {
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
                    println("Got request with id: $reqId and delay: $delay")
                    delay((delay ?: 5).seconds)
                    call.respond(HttpStatusCode.OK, DateTime(), TypeInfo(DateTime::class, typeOf<DateTime>()))
                    println("Request with id: $reqId finished; took: ${System.currentTimeMillis() - startAt}")
                }
            }
        }
    )
}