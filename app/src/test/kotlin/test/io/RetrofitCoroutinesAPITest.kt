package test.io

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.Arguments.arguments
import org.junit.jupiter.params.provider.MethodSource
import org.junit.jupiter.params.provider.ValueSource
import test.io.client.*
import test.io.client.retrofit.Dispatcher
import test.io.client.retrofit.RetrofitApi
import test.io.client.retrofit.client
import test.io.runner.Reports
import test.io.runner.createReport
import test.io.server.withTestServer
import java.nio.file.Path
import java.util.concurrent.Executors

@ExtendWith(ReportDirectoryExtension::class)
class RetrofitCoroutinesAPITest {

    companion object {
        @JvmStatic
        fun argumentsFactory(): List<Arguments> = listOf(
            arguments(1, 5),
            arguments(5, 5),
            arguments(10, 5),
            arguments(10, 10),
            arguments(10, 20),
            arguments(20, 5),
        )
    }

    @ReportDirExtension
    lateinit var reportDir: Path

    @ParameterizedTest(name = "Test{index} => callerThreads: {0}, maxThreadsPerHost: {1}")
    @MethodSource("argumentsFactory")
    fun `Retrofit coroutines API`(callerThreads: Int, maxThreadsPerHost: Int) = runBlocking {
        withTestServer { testEnv ->
            val testParams = TestParams.Default
            val okHttpParams = OkHttpParams.Default

            val logger = ClientLogger(isLoggingEnabled = false)
            val clientExecutor = Executors.newCachedThreadPool()
            val callerDispatcher = Dispatchers.IO.limitedParallelism(callerThreads)

            val api = RetrofitApi.create(testEnv) {
                client {
                    dispatcher(
                        Dispatcher(
                            executor = clientExecutor,
                            params = okHttpParams.copy(maxRequestsPerHost = maxThreadsPerHost)
                        )
                    )
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

            println("✓ Test passed for $callerThreads caller threads: ")
            println(createReport(result))
            val reportPath = reportDir.resolve("report-tr$callerThreads-mtph$maxThreadsPerHost.csv")
            Reports.exportCallsData(reportPath.toFile(), result.callsStats)
        }
    }
}
