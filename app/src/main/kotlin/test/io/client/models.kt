package test.io.client

import kotlinx.serialization.Serializable
import java.time.LocalDate
import java.time.LocalTime

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

data class TestEnv(
    val endpointUrl: String,
    val port: Int,
    val logging: Boolean, // TODO: Move to another entity
) {
    companion object {
        val Default = TestEnv(
            endpointUrl = BASE_URL,
            port = 8080,
            logging = true,
        )
    }
}

data class TestParams(
    val callDelaySec: Int,
    val callsCount: Int,
) {
    companion object {
        val Default = TestParams(
            callDelaySec = 1,
            callsCount = 100,
        )
    }
}

data class OkHttpParams(
    val maxRequests: Int,
    val maxRequestsPerHost: Int,
) {
    companion object {
        val Default = OkHttpParams(
            maxRequests = 64,
            maxRequestsPerHost = 5,
        )
    }
}

@Serializable
class DateTime(
    val date: String = LocalDate.now().toString(),
    val time: String = LocalTime.now().toString(),
)