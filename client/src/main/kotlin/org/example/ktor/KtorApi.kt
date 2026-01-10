package org.example.ktor

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.client.request.parameter
import io.ktor.http.HttpStatusCode
import org.example.DateTime
import org.example.Logger
import org.example.log
import java.util.UUID

class KtorApi(
    private val apiAddress: String,
    private val httpClient: HttpClient,
    private val logger: Logger,
) {
    suspend fun getDatetime(delaySeconds: Int? = null): DateTime? {
        val reqId = UUID.randomUUID()
        val startedAt = System.currentTimeMillis()
        logger("Request(id: $reqId) started; delaySeconds: $delaySeconds")
        val endpoint = "$apiAddress/datetime"
        logger("Request(id: $reqId) endpoint = $endpoint")
        val response = httpClient.get(endpoint) {
            if (delaySeconds != null) {
                parameter("delay", delaySeconds)
            }
            headers { append("X-RequestId", reqId.toString()) }
        }
        logger("Request(id: $reqId) finished; took ${System.currentTimeMillis() - startedAt}; status: ${response.status}")
        return when (response.status) {
            HttpStatusCode.OK -> response.body()
            else -> null
        }
    }
}