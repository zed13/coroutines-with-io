package test.io.client.retrofit

import kotlinx.serialization.json.Json
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import test.io.client.DateTime
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query
import test.io.client.TestEnv
import java.util.UUID

interface RetrofitApi {

    @GET("/datetime")
    suspend fun getDatetime(
        @Query("delay") delaySeconds: Int,
        @Header("X-RequestId") requestId: String = UUID.randomUUID().toString(),
    ): DateTime?

    companion object {

        fun create(testEnv: TestEnv, config: RetrofitConfig): RetrofitApi {
            return create(testEnv.endpointUrl, testEnv.port, config)
        }
        fun create(
            baseUrl: String,
            port: Int,
            config: RetrofitConfig,
        ): RetrofitApi {
            return Retrofit.Builder()
                .baseUrl("$baseUrl:$port".toHttpUrl())
                .addConverterFactory(Json.asConverterFactory("application/json".toMediaType()))
                .apply { config() }
                .build()
                .create(RetrofitApi::class.java)
        }
    }
}