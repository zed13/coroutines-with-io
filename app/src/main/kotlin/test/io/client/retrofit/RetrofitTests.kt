@file:Suppress("UnusedReceiverParameter")

package test.io.client.retrofit

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asCoroutineDispatcher
import okhttp3.Dispatcher
import okhttp3.OkHttpClient
import test.io.client.LoadTest
import test.io.client.ClientLogger
import test.io.client.OkHttpParams
import test.io.client.TestEnv
import test.io.client.TestParams
import test.io.client.TestableCaller
import retrofit2.Retrofit
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

fun Retrofit.Builder.client(
    client: OkHttpClient = OkHttpClient(),
    block: OkHttpClient.Builder.() -> Unit,
): Retrofit.Builder {
    val builder = client.newBuilder()
    builder.block()
    client(builder.build())
    return this
}

fun Dispatcher(executor: ExecutorService, params: OkHttpParams?): Dispatcher {
    return Dispatcher(executor)
        .also {
            if (params != null) {
                it.maxRequests = params.maxRequests
                it.maxRequestsPerHost = params.maxRequestsPerHost
            }
        }
}

object RetrofitTests

fun RetrofitTests.singleThread(
    testEnv: TestEnv,
    testParams: TestParams,
    okHttpParams: OkHttpParams? = null,
): LoadTest {
    val logger = ClientLogger(testEnv.logging)

    val executor = Executors.newSingleThreadExecutor()
    val callerDispatcher = executor.asCoroutineDispatcher()

    val api = RetrofitApi.create(testEnv.endpointUrl) {
        client {
            dispatcher(Dispatcher(executor, okHttpParams))
        }
        callbackExecutor(executor)
    }

    val apiCaller = TestableCaller<Int, Unit> { delay ->
        api.getDatetime(delay)
    }

    return LoadTest(
        testName = "Retrofit.singleThread",
        callerFactory = { apiCaller },
        dispatcherFactory = { callerDispatcher },
        logger = logger,
        callDelay = testParams.callDelaySec,
        callsCount = testParams.callsCount,
        onDisposed = executor::shutdown
    )
}

fun RetrofitTests.dedicatedThreads(
    testEnv: TestEnv,
    testParams: TestParams,
    clientThreads: Int,
    callerThreads: Int,
    retrofitThreads: Int = 1,
    okHttpParams: OkHttpParams? = null,

    ): LoadTest {
    val logger = ClientLogger(testEnv.logging)

    val clientExecutor = Executors.newFixedThreadPool(clientThreads)
    val retrofitExecutor = Executors.newFixedThreadPool(retrofitThreads)
    val callerDispatcher = Dispatchers.IO.limitedParallelism(callerThreads)

    val api = RetrofitApi.create(testEnv.endpointUrl) {
        client {
            dispatcher(Dispatcher(clientExecutor, okHttpParams))
        }
        callbackExecutor(retrofitExecutor)
    }

    val apiCaller = TestableCaller<Int, Unit> { delay ->
        api.getDatetime(delay)
    }

    return LoadTest(
        testName = "Retrofit.dedicatedThreads(okhttp: $clientThreads, retrofit: $retrofitThreads, caller: $callerThreads)",
        callerFactory = { apiCaller },
        dispatcherFactory = { callerDispatcher },
        logger = logger,
        callDelay = testParams.callDelaySec,
        callsCount = testParams.callsCount,
        onDisposed = {
            clientExecutor.shutdown()
            retrofitExecutor.shutdown()
        },
    )
}