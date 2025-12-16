package org.example.ktor

import io.ktor.client.HttpClient
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import org.example.TestStats
import org.example.log
import java.util.concurrent.atomic.AtomicLong

suspend fun ParallelRequestTest(
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
        val api = KtorApi(
            apiAddress = "http://localhost:8080",
            httpClient = clientFactory(),
        )
        repeat(taskCount) { num ->
            val startedAt = System.currentTimeMillis()
            launch(dispatcher) {
                log("task#$num started; took: ${System.currentTimeMillis() - startedAt}")
                api.getDatetime(delaySeconds = endpointDelaySecs)
                val taskDuration = System.currentTimeMillis() - startedAt
                log("task#$num finished; took: $taskDuration")
                totalTime.addAndGet(taskDuration)
                minTime.updateAndGet { if (it > taskDuration) taskDuration else it }
                maxTime.updateAndGet { if (it < taskDuration) taskDuration else it }
            }
            log("task#$num enqueued")
        }
    }
    val testDuration = System.currentTimeMillis() - startedAt
    log("Client test took: $testDuration; min: ${minTime.get()}; max: ${maxTime.get()}; avg: ${totalTime.get() / taskCount}")
    return TestStats(
        testDuration = testDuration,
        minTime = minTime.get(),
        maxTime = maxTime.get(),
        averageTime = totalTime.get() / taskCount
    )
}