package test.io.runner

import test.io.client.TestStats

fun TestStats.createReport(): String = buildString {
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