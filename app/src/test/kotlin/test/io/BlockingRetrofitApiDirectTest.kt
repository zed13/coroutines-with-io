package test.io

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.jupiter.api.Timeout
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.Arguments.arguments
import org.junit.jupiter.params.provider.MethodSource
import test.io.client.*
import test.io.client.retrofit.BlockingRetrofitApi
import test.io.client.retrofit.Dispatcher
import test.io.client.retrofit.client
import test.io.runner.Reports
import test.io.runner.createReport
import test.io.server.withTestServer
import java.nio.file.Path
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.time.Duration.Companion.seconds

@ExtendWith(ReportDirectoryExtension::class)
class BlockingRetrofitApiDirectTest {

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

    @ParameterizedTest(name = "Test#{index} => callerThreads: {0}, maxThreadsPerHost: {1}")
    @MethodSource("argumentsFactory")
//    @Timeout(30, unit = TimeUnit.SECONDS)
    fun `Blocking Retrofit API Direct`(callerThreads: Int, maxThreadsPerHost: Int) = runBlocking {
        withTestServer { testEnv ->
            val testParams = TestParams.Default.copy(callsCount = 20)
            val okHttpParams = OkHttpParams.Default

            val logger = ClientLogger(isLoggingEnabled = false)
            val clientExecutor = Executors.newCachedThreadPool()
            val callerDispatcher = Dispatchers.IO.limitedParallelism(callerThreads)

            val api = BlockingRetrofitApi.create(testEnv) {
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
                api.getDatetime(delay).execute()
            }

            val loadTest = LoadTest(
                testName = "BlockingRetrofit.Direct(caller: $callerThreads)",
                callerFactory = { apiCaller },
                dispatcherFactory = { callerDispatcher },
                logger = logger,
                callDelay = testParams.callDelaySec,
                callsCount = testParams.callsCount,
                onDisposed = clientExecutor::shutdown,
            )

            val result = loadTest.launch()
            loadTest.dispose()

            println("✓ Test passed for $callerThreads caller threads (direct): ")
            println(createReport(result))
            val reportPath = reportDir.resolve("direct-report-tr$callerThreads-mtph$maxThreadsPerHost.csv")
            Reports.exportCallsData(reportPath.toFile(), result.callsStats)
        }
    }
}
