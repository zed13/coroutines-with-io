@file:JvmName("Client")

package org.example

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import okhttp3.Dispatcher
import org.example.ktor.ParallelRequestTest
import org.example.retrofit.RetrofitApiTest
import java.util.concurrent.Executors

fun newOkHttpClient(): HttpClient {
    return HttpClient(OkHttp) {
        engine {
            config {
//                connectionPool(ConnectionPool(
//                    maxIdleConnections = 100,
//                    keepAliveDuration = 5,
//                    timeUnit = TimeUnit.MINUTES,
//                ))
                dispatcher(Dispatcher(Executors.newCachedThreadPool()).apply {
                    maxRequests = 100
                    maxRequestsPerHost = 100
                })
            }
        }
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

fun main(args: Array<String>) {
    runBlocking {
        println("Start CIO client test\n")
        val cioTestResult = ParallelRequestTest(
            clientFactory = ::newCioClient,
            dispatcherFactory = { Dispatchers.IO },
        )

        println("\n\n\nStart OkHttp client test\n")
        val okHttpTestResult = ParallelRequestTest(
            clientFactory = ::newOkHttpClient,
            dispatcherFactory = { Dispatchers.IO.limitedParallelism(2) },
        )

        println("\n\n\nStart Retrofit test\n")
        val retrofitTestResult = RetrofitApiTest(
            dispatcherFactory = { Dispatchers.IO.limitedParallelism(1) }
        )

        println("\n=================================")
        println(cioTestResult.format("CIO"))
        println(okHttpTestResult.format("OkHttp"))
        println(retrofitTestResult.format("Retrofit"))
    }
}

