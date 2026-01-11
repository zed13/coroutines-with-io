package test.io.client

fun interface TestableCaller<Request, Response> {
    suspend fun makeCall(request: Request): Response

}