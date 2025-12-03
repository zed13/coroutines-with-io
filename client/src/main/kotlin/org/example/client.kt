@file:JvmName("Client")

package org.example

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import java.time.LocalTime
import java.util.*

fun newOkHttpClient(): HttpClient {
    return HttpClient(OkHttp) {
        install(ContentNegotiation) {
            json(newJsonConfiguration())
        }
    }
}

fun newCioClient(): HttpClient {
    return HttpClient(CIO) {
        install(ContentNegotiation) {
            json(newJsonConfiguration())
        }
    }
}

private fun newJsonConfiguration(): Json {
    return Json {
        prettyPrint = true
        isLenient = true
        ignoreUnknownKeys = true
        encodeDefaults = true
        explicitNulls = false
    }
}

fun logd(message: String) {
    println("${Thread.currentThread().name}@${LocalTime.now()} --> $message")
}

class Api(
    private val apiAddress: String,
    val httpClient: HttpClient,
) {
    suspend fun getDatetime(delaySeconds: Int? = null): DateTime? {
        val reqId = UUID.randomUUID()
        val startedAt = System.currentTimeMillis()
        logd("Request(id: $reqId) started; delaySeconds: $delaySeconds")
        val endpoint = "$apiAddress/datetime"
        logd("Request(id: $reqId) endpoint = $endpoint")
        val response = httpClient.get(endpoint) {
            if (delaySeconds != null) {
                parameter("delay", delaySeconds)
            }
            headers { append("X-RequestId", reqId.toString()) }
        }
        logd("Request(id: $reqId) finished; took ${System.currentTimeMillis() - startedAt}; status: ${response.status}")
        return when (response.status) {
            HttpStatusCode.OK -> response.body()
            else -> null
        }
    }
}

fun main(args: Array<String>) {
    runBlocking {
        val startedAt = System.currentTimeMillis()
        clientTest()
        logd("client test took ${System.currentTimeMillis() - startedAt}")
    }
}

private suspend fun clientTest() {
    coroutineScope {
        val dispatcher = Dispatchers.IO.limitedParallelism(1)
        val api = Api(
            apiAddress = "http://localhost:8080",
            httpClient = newOkHttpClient(),
        )

        repeat(100) {
            launch(dispatcher) {
                api.getDatetime(delaySeconds = 5)
            }
        }

    }
}
