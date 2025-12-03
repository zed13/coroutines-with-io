@file:JvmName("Client")

package org.example

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.*
import kotlinx.serialization.json.Json

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

fun main(args: Array<String>) {
    runBlocking {
        println("Start CIO client test\n")
        val cioTestResult = ParallelRequestTest(
            clientFactory = ::newCioClient,
            dispatcherFactory = { Dispatchers.Default.limitedParallelism(1) },
        )

        println("\n\n\nStart OkHttp client test\n")
        val okHttpTestResult = ParallelRequestTest(
            clientFactory = ::newOkHttpClient,
            dispatcherFactory = { Dispatchers.IO },
        )

        println("\n=================================")
        println(cioTestResult.format("CIO"))
        println(okHttpTestResult.format("OkHttp"))
    }
}

