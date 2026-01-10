package org.example

import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import java.util.concurrent.atomic.AtomicLong

class LoadTest(
    val testName: String,
    val callerFactory: () -> TestableCaller<Int, Unit>,
    val dispatcherFactory: CoroutineDispatchersFactory,
    val logger: Logger,
    val callDelay: Int,
    val callsCount: Int,
    val onDisposed: (suspend () -> Unit)? = null,
) {

    suspend fun launch(): TestStats {
        val caller = callerFactory()
        val dispatcher = dispatcherFactory()

        val minTime = AtomicLong(Long.MAX_VALUE)
        val totalTime = AtomicLong()
        val maxTime = AtomicLong()
        val startedAt = System.currentTimeMillis()

        return coroutineScope {
            val callsStats = arrayListOf<Deferred<CallData>>()
            repeat(callsCount) { num ->
                val enqueuedAt = System.currentTimeMillis()
                val callId = "call#$num"
                val stats = async(dispatcher) {
                    val startedAt = startedAt
                    logger("$callId started; took: ${System.currentTimeMillis() - enqueuedAt}")
                    caller.makeCall(callDelay)
                    val taskDuration = System.currentTimeMillis() - enqueuedAt
                    logger("$callId finished; took: $taskDuration")
                    totalTime.addAndGet(taskDuration)
                    minTime.updateAndGet { if (it > taskDuration) taskDuration else it }
                    maxTime.updateAndGet { if (it < taskDuration) taskDuration else it }
                    CallData(
                        callId = callId,
                        enqueuedAt = enqueuedAt,
                        startedAt = startedAt,
                        completedAt = System.currentTimeMillis(),
                    )
                }
                logger("$callId enqueued")
                callsStats.add(stats)
            }

            val callDataStats = callsStats.awaitAll()
            val testDuration = System.currentTimeMillis() - startedAt
            logger("Client test took: $testDuration; min: ${minTime.get()}; max: ${maxTime.get()}; avg: ${totalTime.get() / callsCount}")
            return@coroutineScope TestStats(
                testName = testName,
                testDuration = testDuration,
                minCallTime = minTime.get(),
                maxCallTime = maxTime.get(),
                avgCallTime = totalTime.get() / callsCount,
                callsCount = callsCount,
                callsStats = callDataStats,
                responseDelay = callDelay,
            )
        }
    }

    suspend fun dispose() {
        onDisposed?.invoke()
    }
}