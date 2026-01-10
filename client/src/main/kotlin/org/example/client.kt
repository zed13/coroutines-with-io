@file:JvmName("Client")

package org.example

import kotlinx.coroutines.runBlocking
import org.example.ktor.Ktor
import org.example.ktor.dedicatedThreads
import org.example.ktor.singleThread
import java.io.File

val reportsDir = File("reports")

fun main(args: Array<String>) = runBlocking {
    val testEnv = TestEnv.Default
    val testParams = TestParams.Default

    when {
        reportsDir.exists() -> {
            reportsDir.deleteRecursively()
            reportsDir.mkdirs()
        }

        else -> reportsDir.mkdirs()
    }

    val tests = listOf(
        // -- Ktor cio tests
        Ktor.Cio.singleThread(testEnv, testParams),
        Ktor.Cio.dedicatedThreads(testEnv, testParams, clientThreadCount = 1, callsThreadCount = 1),
        Ktor.Cio.dedicatedThreads(testEnv, testParams, clientThreadCount = 10, callsThreadCount = 1),
        Ktor.Cio.dedicatedThreads(testEnv, testParams, clientThreadCount = 1, callsThreadCount = 10),
        Ktor.Cio.dedicatedThreads(testEnv, testParams, clientThreadCount = 5, callsThreadCount = 5),
        // -- Ktor OkHttpTests
        Ktor.OkHttp.singleThread(testEnv, testParams.copy(callsCount = 20)), // too long execution for 100 calls
        Ktor.OkHttp.dedicatedThreads(testEnv, testParams.copy(callsCount = 20), clientThreads = 1, callerThreads = 1),
        Ktor.OkHttp.dedicatedThreads(testEnv, testParams.copy(callsCount = 20), clientThreads = 10, callerThreads = 1),
        Ktor.OkHttp.dedicatedThreads(testEnv, testParams.copy(callsCount = 20), clientThreads = 1, callerThreads = 10),
        Ktor.OkHttp.dedicatedThreads(testEnv, testParams.copy(callsCount = 20), clientThreads = 5, callerThreads = 5),
    )

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

private fun TestStats.createReport(): String = buildString {
    appendLine("${testName}(callsCount: ${callsCount}, responseDelay: ${responseDelay})")
    appendLine("Overall duration: $testDuration")
    appendLine("Call execution time [min,max,avg]: $minCallTime, $maxCallTime, $avgCallTime")

    val minWaitTime = callsStats.minOfOrNull { it.waitingTimeMillis }
    val maxWaitingTime = callsStats.maxOfOrNull { it.waitingTimeMillis }
    val avgWaitingTime = callsStats.sumOf { it.waitingTimeMillis } / callsStats.size
    appendLine("Waiting time [min,max,avg]: $minWaitTime, $maxWaitingTime, $avgWaitingTime")

    val minExecutionTime = callsStats.minOfOrNull { it.executionTimeMillis }
    val maxExecutionTime = callsStats.maxOfOrNull { it.executionTimeMillis }
    val avgExecutionTime = callsStats.sumOf { it.executionTimeMillis } / callsStats.size
    appendLine("Execution time [min,max,avg]: $minExecutionTime, $maxExecutionTime, $avgExecutionTime")

    val minTotalTime = callsStats.minOfOrNull { it.totalTimeMillis }
    val maxTotalTime = callsStats.maxOfOrNull { it.totalTimeMillis }
    val avgTotalTime = callsStats.sumOf { it.totalTimeMillis } / callsStats.size
    appendLine("Total time [min,max,avg]: $minTotalTime, $maxTotalTime, $avgTotalTime")
}


