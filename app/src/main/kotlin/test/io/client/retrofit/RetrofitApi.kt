package test.io.client.retrofit

import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import test.io.client.DateTime
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query
import java.util.UUID

interface RetrofitApi {

    @GET("/datetime")
    suspend fun getDatetime(
        @Query("delay") delaySeconds: Int,
        @Header("X-RequestId") requestId: String = UUID.randomUUID().toString(),
    ): DateTime?

    companion object {
        fun create(
            baseUrl: String,
            config: RetrofitConfig,
        ): RetrofitApi {
            return Retrofit.Builder()
                .baseUrl(baseUrl)
                .addConverterFactory(Json.asConverterFactory("application/json".toMediaType()))
                .apply { config() }
                .build()
                .create(RetrofitApi::class.java)
        }
    }
}