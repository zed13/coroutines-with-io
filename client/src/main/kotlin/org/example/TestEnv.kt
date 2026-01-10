package org.example

data class TestEnv(
    val endpointUrl: String,
    val logging: Boolean,
) {
    companion object {
        val Default = TestEnv(
            endpointUrl = BASE_URL,
            logging = true,
        )
    }
}

data class TestParams(
    val callDelaySec: Int,
    val callsCount: Int,
) {
    companion object {
        val Default = TestParams(
            callDelaySec = 1,
            callsCount = 100,
        )
    }
}

data class OkHttpParams(
    val maxRequests: Int,
    val maxRequestsPerHost: Int,
) {
    companion object {
        val Default = OkHttpParams(
            maxRequests = 5,
            maxRequestsPerHost = 5,
        )
    }
}