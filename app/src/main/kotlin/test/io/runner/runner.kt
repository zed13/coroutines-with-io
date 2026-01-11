@file:JvmName("Client")

package test.io.runner

import test.io.client.LoadTest
import test.io.client.OkHttpParams
import test.io.client.TestEnv
import test.io.client.TestParams
import test.io.client.ktor.Ktor
import test.io.client.ktor.dedicatedThreads
import test.io.client.ktor.singleThread
import test.io.client.retrofit.RetrofitTests
import test.io.client.retrofit.dedicatedThreads
import test.io.client.retrofit.singleThread
import test.io.server.withTestServer
import java.io.File

val reportsDir = File("reports")

fun main(args: Array<String>) = withTestServer(logging = true) {
    val testEnv = TestEnv.Default
    val testParams = TestParams.Default
    val okHttpTestParams = TestParams.Default.copy(callsCount = 20)

    when {
        reportsDir.exists() -> {
            reportsDir.deleteRecursively()
            reportsDir.mkdirs()
        }

        else -> reportsDir.mkdirs()
    }

    listOf(
        Ktor.Cio.singleThread(testEnv, testParams),
        Ktor.Cio.dedicatedThreads(testEnv, testParams, clientThreadCount = 1, callsThreadCount = 1),
        Ktor.Cio.dedicatedThreads(testEnv, testParams, clientThreadCount = 10, callsThreadCount = 1),
        Ktor.Cio.dedicatedThreads(testEnv, testParams, clientThreadCount = 1, callsThreadCount = 10),
        Ktor.Cio.dedicatedThreads(testEnv, testParams, clientThreadCount = 5, callsThreadCount = 5),
    )

    listOf(
        Ktor.OkHttp.singleThread(testEnv, okHttpTestParams),
        Ktor.OkHttp.dedicatedThreads(testEnv, okHttpTestParams, clientThreads = 1, callerThreads = 1),
        Ktor.OkHttp.dedicatedThreads(testEnv, okHttpTestParams, clientThreads = 10, callerThreads = 1),
        Ktor.OkHttp.dedicatedThreads(testEnv, okHttpTestParams, clientThreads = 1, callerThreads = 10),
        Ktor.OkHttp.dedicatedThreads(testEnv, okHttpTestParams, clientThreads = 5, callerThreads = 5),
    )

    val okHttpParams = OkHttpParams(maxRequests = 10, maxRequestsPerHost = 10)
    val retrofitTests = listOf<LoadTest>(
        RetrofitTests.singleThread(testEnv, okHttpTestParams),
        RetrofitTests.singleThread(testEnv, okHttpTestParams, OkHttpParams(1, 1,)),
        RetrofitTests.singleThread(testEnv, okHttpTestParams, OkHttpParams(10, 10,)),
        RetrofitTests.dedicatedThreads(testEnv, okHttpTestParams, clientThreads = 1, callerThreads = 1),
        RetrofitTests.dedicatedThreads(testEnv, okHttpTestParams, clientThreads = 10, callerThreads = 1),
        RetrofitTests.dedicatedThreads(testEnv, okHttpTestParams, clientThreads = 10, callerThreads = 1, okHttpParams = okHttpParams),
        RetrofitTests.dedicatedThreads(testEnv, okHttpTestParams, clientThreads = 1, callerThreads = 10),
        RetrofitTests.dedicatedThreads(testEnv, okHttpTestParams, clientThreads = 1, callerThreads = 10, okHttpParams = okHttpParams),
        RetrofitTests.dedicatedThreads(testEnv, okHttpTestParams, clientThreads = 5, callerThreads = 5),
        RetrofitTests.dedicatedThreads(testEnv, okHttpTestParams, clientThreads = 5, callerThreads = 5, retrofitThreads = 5),
    )

    val tests = buildList {
        addAll(retrofitTests)
//        addAll(ktorOkHttpTests)
//        addAll(ktorCioTests)
    }

    val testResults = tests.map { test ->
        val result = test.launch()
        test.dispose()
        result
    }

    println("-".repeat(80))
    println("-- REPORT " + "-".repeat(70))
    println("-".repeat(80))

    testResults.forEachIndexed { index, resultStats ->
        println()
        println("-".repeat(80))
        val report = resultStats.createReport()
        print(report)
        File(reportsDir, "report#$index.txt").writeText(resultStats.createReport())
        CsvExporter.exportCallsData(File(reportsDir, "report#$index.csv"), resultStats.callsStats)
    }
}


