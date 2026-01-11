package test.io

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import test.io.client.*
import test.io.client.retrofit.Dispatcher
import test.io.client.retrofit.RetrofitApi
import test.io.client.retrofit.client
import test.io.server.withTestServer
import java.util.concurrent.Executors

class CallerOkHttpTest {

    @ParameterizedTest
    @ValueSource(ints = [1, 5, 10, 20])
    fun `test custom caller threads with different thread counts`(callerThreads: Int) = runBlocking {
        withTestServer { testEnv ->
            val testParams = TestParams.Default
            val okHttpParams = OkHttpParams.Default

            val logger = ClientLogger(isLoggingEnabled = false)
            val clientExecutor = Executors.newCachedThreadPool()
            val callerDispatcher = Dispatchers.IO.limitedParallelism(callerThreads)

            val api = RetrofitApi.create(
                baseUrl = testEnv.endpointUrl,
                port = testEnv.port
            ) {
                client {
                    dispatcher(Dispatcher(clientExecutor, okHttpParams))
                }
            }

            val apiCaller = TestableCaller<Int, Unit> { delay ->
                api.getDatetime(delay)
            }

            val loadTest = LoadTest(
                testName = "Retrofit.customCallerThreads(caller: $callerThreads)",
                callerFactory = { apiCaller },
                dispatcherFactory = { callerDispatcher },
                logger = logger,
                callDelay = testParams.callDelaySec,
                callsCount = testParams.callsCount,
                onDisposed = clientExecutor::shutdown,
            )

            val result = loadTest.launch()
            loadTest.dispose()

            println(
                "✓ Test passed for $callerThreads caller threads: " +
                        "avg=${result.avgCallTime}ms, min=${result.minCallTime}ms, max=${result.maxCallTime}ms"
            )
        }
    }
}
