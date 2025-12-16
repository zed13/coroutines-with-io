package org.example.retrofit

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import org.example.BASE_URL
import org.example.TestStats
import org.example.log
import java.util.concurrent.atomic.AtomicLong

suspend fun RetrofitApiTest(
    dispatcherFactory: () -> CoroutineDispatcher,
    baseUrl: String = "http://localhost:8080",
    taskCount: Int = 100,
    responseDelay: Int = 1,
): TestStats {
    val api =
        RetrofitApi.create(baseUrl, RetrofitConfigs.multiThreadedRetrofit() + RetrofitConfigs.multiThreadedClient())
    val dispatcher = dispatcherFactory()

    val minTime = AtomicLong(Long.MAX_VALUE)
    val totalTime = AtomicLong()
    val maxTime = AtomicLong()
    val startedAt = System.currentTimeMillis()

    coroutineScope {
        repeat(taskCount) { num ->
            val startedAt = System.currentTimeMillis()
            launch(dispatcher) {
                log("task#$num started; took: ${System.currentTimeMillis() - startedAt}")
                api.getDatetime(delaySeconds = responseDelay)
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

suspend fun RetrofitSingleThreaded(
    baseUrl: String = BASE_URL,
    logging: Boolean = false,
) {
    val api = RetrofitApi.create(baseUrl, RetrofitConfigs.singleThreadedClient())
    CallTest(
        callerFactory = { { api.getDatetime(it) } },
        dispatcherFactory = { Dispatchers.IO.limitedParallelism(1) },
        testsCount = 100,
        responseDelay = 1,
        logger = { if (logging) log(it) }
    )
}

suspend fun CallTest(
    callerFactory: () -> suspend (Int) -> Unit,
    dispatcherFactory: () -> CoroutineDispatcher,
    testsCount: Int,
    responseDelay: Int,
    logger: (String) -> Unit,
): TestStats {
    val caller = callerFactory()
    val dispatcher = dispatcherFactory()

    val minTime = AtomicLong(Long.MAX_VALUE)
    val totalTime = AtomicLong()
    val maxTime = AtomicLong()
    val startedAt = System.currentTimeMillis()

    coroutineScope {
        repeat(testsCount) { num ->
            val startedAt = System.currentTimeMillis()
            launch(dispatcher) {
                logger("task#$num started; took: ${System.currentTimeMillis() - startedAt}")
                caller(responseDelay)
                val taskDuration = System.currentTimeMillis() - startedAt
                logger("task#$num finished; took: $taskDuration")
                totalTime.addAndGet(taskDuration)
                minTime.updateAndGet { if (it > taskDuration) taskDuration else it }
                maxTime.updateAndGet { if (it < taskDuration) taskDuration else it }
            }
            logger("task#$num enqueued")
        }
    }

    val testDuration = System.currentTimeMillis() - startedAt
    logger("Client test took: $testDuration; min: ${minTime.get()}; max: ${maxTime.get()}; avg: ${totalTime.get() / testsCount}")
    return TestStats(
        testDuration = testDuration,
        minTime = minTime.get(),
        maxTime = maxTime.get(),
        averageTime = totalTime.get() / testsCount
    )
}