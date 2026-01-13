package test.io.runner

import test.io.client.TestStats

fun createReport(stats: TestStats): String = buildString {
    appendLine("${stats.testName}(callsCount: ${stats.callsCount}, responseDelay: ${stats.responseDelay})")
    appendLine("Overall duration: ${stats.testDuration}")
    appendLine("Call execution time [min,max,avg]: ${stats.minCallTime}, ${stats.maxCallTime}, ${stats.avgCallTime}")

    val minWaitTime = stats.callsStats.minOfOrNull { it.waitingTimeMillis }
    val maxWaitingTime = stats.callsStats.maxOfOrNull { it.waitingTimeMillis }
    val avgWaitingTime = stats.callsStats.sumOf { it.waitingTimeMillis } / stats.callsStats.size
    appendLine("Waiting time [min,max,avg]: $minWaitTime, $maxWaitingTime, $avgWaitingTime")

    val minExecutionTime = stats.callsStats.minOfOrNull { it.executionTimeMillis }
    val maxExecutionTime = stats.callsStats.maxOfOrNull { it.executionTimeMillis }
    val avgExecutionTime = stats.callsStats.sumOf { it.executionTimeMillis } / stats.callsStats.size
    appendLine("Execution time [min,max,avg]: $minExecutionTime, $maxExecutionTime, $avgExecutionTime")

    val minTotalTime = stats.callsStats.minOfOrNull { it.totalTimeMillis }
    val maxTotalTime = stats.callsStats.maxOfOrNull { it.totalTimeMillis }
    val avgTotalTime = stats.callsStats.sumOf { it.totalTimeMillis } / stats.callsStats.size
    appendLine("Total time [min,max,avg]: $minTotalTime, $maxTotalTime, $avgTotalTime")
}