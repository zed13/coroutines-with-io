package test.io.client

import kotlinx.serialization.json.Json

fun newJsonConfiguration(): Json {
    return Json {
        prettyPrint = true
        isLenient = true
        ignoreUnknownKeys = true
        encodeDefaults = true
        explicitNulls = false
    }
}