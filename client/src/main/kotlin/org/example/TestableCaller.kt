package org.example

fun interface TestableCaller<Request, Response> {
    suspend fun makeCall(request: Request): Response

    fun interface Factory<Request, Response> {
        fun create(): TestableCaller<Request, Response>
    }
}