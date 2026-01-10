package org.example

import java.time.LocalTime

typealias Logger = (String) -> Unit

fun Logger(isLoggingEnabled: Boolean = true): Logger {
    return { message ->
        if (isLoggingEnabled) {
            log(message)
        }
    }
}

fun log(message: String) {
    println("${Thread.currentThread().name}@${LocalTime.now()} --> $message")
}