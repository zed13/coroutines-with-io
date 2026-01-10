@file:Suppress("FunctionName", "UnusedReceiverParameter")

package org.example.ktor

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asCoroutineDispatcher
import okhttp3.Dispatcher
import org.example.LoadTest
import org.example.Logger
import org.example.TestEnv
import org.example.TestParams
import org.example.newJsonConfiguration
import org.example.TestableCaller
import java.util.concurrent.Executors


object Ktor {
    object Cio

    object OkHttp
}


fun Ktor.Cio.singleThread(
    testEnv: TestEnv,
    testParams: TestParams,
): LoadTest {
    val logger = Logger(testEnv.logging)

    val dispatcher = Dispatchers.IO.limitedParallelism(1)

    val httpClient = HttpClient(CIO) {
        engine { this.dispatcher = dispatcher }
        install(ContentNegotiation) {
            json(newJsonConfiguration())
        }
    }
    val api = KtorApi(
        apiAddress = testEnv.endpointUrl,
        httpClient = httpClient,
        logger = logger,
    )
    val apiCaller = TestableCaller<Int, Unit> { delay ->
        api.getDatetime(delay)
    }
    return LoadTest(
        "Ktor.Cio.singleThread",
        callerFactory = { apiCaller },
        dispatcherFactory = { dispatcher },
        logger = logger,
        callDelay = testParams.callDelaySec,
        callsCount = testParams.callsCount,
    )
}

fun Ktor.Cio.dedicatedThreads(
    testEnv: TestEnv,
    testParams: TestParams,
    clientThreadCount: Int,
    callsThreadCount: Int,
): LoadTest {
    val logger = Logger(testEnv.logging)
    val clientDispatcher = Dispatchers.IO.limitedParallelism(clientThreadCount)
    val callDispatcher = Dispatchers.IO.limitedParallelism(callsThreadCount)
    val httpClient = HttpClient(CIO) {
        engine { this.dispatcher = clientDispatcher }
        install(ContentNegotiation) {
            json(newJsonConfiguration())
        }
    }
    val api = KtorApi(
        apiAddress = testEnv.endpointUrl,
        httpClient = httpClient,
        logger = logger,
    )
    val apiCaller = TestableCaller<Int, Unit> { delay ->
        api.getDatetime(delay)
    }
    return LoadTest(
        "Ktor.Cio.dedicatedThread[client: $clientThreadCount, calls: $callsThreadCount]",
        callerFactory = { apiCaller },
        dispatcherFactory = { callDispatcher },
        logger = logger,
        callDelay = testParams.callDelaySec,
        callsCount = testParams.callsCount,
    )
}

fun Ktor.OkHttp.singleThread(
    testEnv: TestEnv,
    testParams: TestParams,
): LoadTest {
    val logger = Logger(testEnv.logging)

    val executor = Executors.newSingleThreadExecutor()
    val dispatcher = executor.asCoroutineDispatcher()

    val httpClient = HttpClient(OkHttp) {
        engine {
            config {
                dispatcher(Dispatcher(executor))
            }
        }
        install(ContentNegotiation) {
            json(newJsonConfiguration())
        }
    }
    val api = KtorApi(
        apiAddress = testEnv.endpointUrl,
        httpClient = httpClient,
        logger = logger,
    )
    val apiCaller = TestableCaller<Int, Unit> { delay ->
        api.getDatetime(delay)
    }
    return LoadTest(
        "Ktor.OkHttp.singleThread",
        callerFactory = { apiCaller },
        dispatcherFactory = { dispatcher },
        logger = logger,
        callDelay = testParams.callDelaySec,
        callsCount = testParams.callsCount,
        onDisposed = executor::shutdown
    )
}

fun Ktor.OkHttp.dedicatedThreads(
    testEnv: TestEnv,
    testParams: TestParams,
    clientThreads: Int,
    callerThreads: Int,
): LoadTest {
    val logger = Logger(testEnv.logging)

    val clientExecutor = Executors.newFixedThreadPool(clientThreads)
    val callerDispatcher = Dispatchers.IO.limitedParallelism(callerThreads)

    val httpClient = HttpClient(OkHttp) {
        engine {
            config {
                dispatcher(Dispatcher(clientExecutor))
            }
        }
        install(ContentNegotiation) {
            json(newJsonConfiguration())
        }
    }
    val api = KtorApi(
        apiAddress = testEnv.endpointUrl,
        httpClient = httpClient,
        logger = logger,
    )
    val apiCaller = TestableCaller<Int, Unit> { delay ->
        api.getDatetime(delay)
    }
    return LoadTest(
        "Ktor.OkHttp.dedicated[client: $clientThreads, caller: $callerDispatcher]",
        callerFactory = { apiCaller },
        dispatcherFactory = { callerDispatcher },
        logger = logger,
        callDelay = testParams.callDelaySec,
        callsCount = testParams.callsCount,
        onDisposed = clientExecutor::shutdown
    )
}
