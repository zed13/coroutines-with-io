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
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import java.time.LocalTime
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicLong

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
        val executor = Executors.newSingleThreadExecutor()
        println("Start CIO client test\n")
        val cioTestResult = clientTest(
            clientFactory = ::newCioClient,
            dispatcherFactory = { executor.asCoroutineDispatcher() },
        )

        println("\n\n\nStart OkHttp client test\n")
        val okHttpTestResult = clientTest(
            clientFactory = ::newOkHttpClient,
            dispatcherFactory = { executor.asCoroutineDispatcher() },
        )

        println("=================================")
        println(cioTestResult.format("CIO"))
        println(okHttpTestResult.format("OkHttp"))
        executor.shutdownNow()
    }
}

private suspend fun clientTest(
    clientFactory: () -> HttpClient,
    dispatcherFactory: () -> CoroutineDispatcher,
    taskCount: Int = 100,
    endpointDelaySecs: Int = 1,
): TestStats {
    val minTime = AtomicLong(Long.MAX_VALUE)
    val totalTime = AtomicLong()
    val maxTime = AtomicLong()
    val startedAt = System.currentTimeMillis()

    coroutineScope {
        val dispatcher = dispatcherFactory()
        val api = Api(
            apiAddress = "http://localhost:8080",
            httpClient = clientFactory(),
        )
        repeat(taskCount) { num ->
            val startedAt = System.currentTimeMillis()
            launch(dispatcher) {
                logd("task#$num started; took: ${System.currentTimeMillis() - startedAt}")
                api.getDatetime(delaySeconds = endpointDelaySecs)
                val taskDuration = System.currentTimeMillis() - startedAt
                logd("task#$num finished; took: $taskDuration")
                totalTime.addAndGet(taskDuration)
                minTime.updateAndGet { if (it > taskDuration) taskDuration else it }
                maxTime.updateAndGet { if (it < taskDuration) taskDuration else it }
            }
            logd("task#$num enqueued")
        }
    }
    val testDuration = System.currentTimeMillis() - startedAt
    logd("Client test took: $testDuration; min: ${minTime.get()}; max: ${maxTime.get()}; avg: ${totalTime.get() / taskCount}")
    return TestStats(
        testDuration = testDuration,
        minTime = minTime.get(),
        maxTime = maxTime.get(),
        averageTime = totalTime.get() / taskCount
    )
}

data class TestStats(
    val testDuration: Long,
    val minTime: Long,
    val maxTime: Long,
    val averageTime: Long,
) {
    fun format(name: String): String {
        return "Test#$name duration: $testDuration; min: $minTime; max: $maxTime; avg: $averageTime;"
    }
}