package org.example

data class TestStats(
    val testName: String,
    val callsCount: Int,
    val responseDelay: Int,
    val testDuration: Long,
    val minCallTime: Long,
    val maxCallTime: Long,
    val avgCallTime: Long,
    val callsStats: List<CallData>
)

data class CallData(
    val callId: String,
    val enqueuedAt: Long,
    val startedAt: Long,
    val completedAt: Long,
    val waitingTimeMillis: Long,
    val executionTimeMillis: Long,
    val totalTimeMillis: Long,
) {

    constructor(
        callId: String,
        enqueuedAt: Long,
        startedAt: Long,
        completedAt: Long,
    ) : this(
        callId = callId,
        enqueuedAt = enqueuedAt,
        startedAt = startedAt,
        completedAt = completedAt,
        waitingTimeMillis = enqueuedAt - startedAt,
        executionTimeMillis = completedAt - startedAt,
        totalTimeMillis = completedAt - enqueuedAt,
    )
}