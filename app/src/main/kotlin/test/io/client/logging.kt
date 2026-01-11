@file:Suppress("FunctionName")

package test.io.client

import java.time.LocalTime

typealias ClientLogger = (String) -> Unit

fun ClientLogger(isLoggingEnabled: Boolean = true): ClientLogger {
    return { if (isLoggingEnabled) log(it) }
}

fun log(message: String) {
    println("[Client] ${Thread.currentThread().name}@${LocalTime.now()} --> $message")
}