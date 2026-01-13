package test.io.client.retrofit

import kotlinx.serialization.json.Json
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query
import test.io.client.DateTime
import test.io.client.TestEnv
import java.util.UUID

interface BlockingRetrofitApi {

    @GET("/datetime")
    fun getDatetime(
        @Query("delay") delaySeconds: Int,
        @Header("X-RequestId") requestId: String = UUID.randomUUID().toString(),
    ): Call<DateTime>

    companion object {

        fun create(testEnv: TestEnv, config: RetrofitConfig): BlockingRetrofitApi {
            return create(testEnv.endpointUrl, testEnv.port, config)
        }

        fun create(
            baseUrl: String,
            port: Int,
            config: RetrofitConfig,
        ): BlockingRetrofitApi {
            return Retrofit.Builder()
                .baseUrl("$baseUrl:$port".toHttpUrl())
                .addConverterFactory(Json.asConverterFactory("application/json".toMediaType()))
                .apply { config() }
                .build()
                .create(BlockingRetrofitApi::class.java)
        }
    }
}
