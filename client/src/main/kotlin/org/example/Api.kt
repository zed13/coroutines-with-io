package org.example

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.client.request.parameter
import io.ktor.http.HttpStatusCode
import java.util.UUID

class Api(
    private val apiAddress: String,
    val httpClient: HttpClient,
) {
    suspend fun getDatetime(delaySeconds: Int? = null): DateTime? {
        val reqId = UUID.randomUUID()
        val startedAt = System.currentTimeMillis()
        log("Request(id: $reqId) started; delaySeconds: $delaySeconds")
        val endpoint = "$apiAddress/datetime"
        log("Request(id: $reqId) endpoint = $endpoint")
        val response = httpClient.get(endpoint) {
            if (delaySeconds != null) {
                parameter("delay", delaySeconds)
            }
            headers { append("X-RequestId", reqId.toString()) }
        }
        log("Request(id: $reqId) finished; took ${System.currentTimeMillis() - startedAt}; status: ${response.status}")
        return when (response.status) {
            HttpStatusCode.OK -> response.body()
            else -> null
        }
    }
}