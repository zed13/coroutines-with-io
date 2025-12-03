package org.example.client

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.example.DateTime
import java.time.LocalTime
import java.util.UUID

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
        val response = httpClient.get("$apiAddress/datetime") {
            if (delaySeconds != null) {
                parameter("delay", delaySeconds)
            }
        }
        logd("Request(id: $reqId) finished; took ${System.currentTimeMillis() - startedAt}")
        return when (response.status) {
            HttpStatusCode.OK -> response.body()
            else -> null
        }
    }
}

fun main() = runBlocking {
    val api = Api(
        apiAddress = "http://localhost:8080/",
        httpClient = newCioClient(),
    )

    val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    repeat(100) {
        scope.launch(Dispatchers.IO) {
            api.getDatetime(delaySeconds = 5)
        }
    }
    scope.coroutineContext[Job]?.join()
}
