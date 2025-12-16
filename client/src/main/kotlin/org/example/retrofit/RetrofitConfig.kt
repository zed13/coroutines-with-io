package org.example.retrofit

import okhttp3.Dispatcher
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import java.util.concurrent.Executors

typealias RetrofitConfig = Retrofit.Builder.() -> Retrofit.Builder

operator fun RetrofitConfig.plus(other: RetrofitConfig): RetrofitConfig {
    val config = this
    return {
        config()
        other()
    }
}

object RetrofitConfigs {
    fun default(): RetrofitConfig {
        return { this }
    }

    fun singleThreadedClient(
        maxRequests: Int = 100,
        maxRequestsPerHost: Int = 100
    ): RetrofitConfig {
        return {
            client(
                OkHttpClient.Builder()
                    .dispatcher(
                        Dispatcher(Executors.newSingleThreadExecutor())
                            .also { dispatcher ->
                                dispatcher.maxRequests = maxRequests
                                dispatcher.maxRequestsPerHost = maxRequestsPerHost
                            }
                    )
                    .build()
            )
        }
    }

    fun multiThreadedClient(
        threads: Int = 100,
        maxRequests: Int = 100,
        maxRequestsPerHost: Int = 100,
    ): RetrofitConfig {
        return {
            client(
                OkHttpClient.Builder()
                    .dispatcher(
                        Dispatcher(Executors.newFixedThreadPool(threads))
                            .also { dispatcher ->
                                dispatcher.maxRequests = maxRequests
                                dispatcher.maxRequestsPerHost = maxRequestsPerHost
                            }
                    )
                    .build()
            )
        }
    }

    fun multiThreadedRetrofit(retrofitThreads: Int = 5): RetrofitConfig {
        return {
            callbackExecutor(Executors.newFixedThreadPool(retrofitThreads))
        }
    }
}