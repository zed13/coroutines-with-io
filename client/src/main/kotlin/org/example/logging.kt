package org.example

import java.time.LocalTime

fun log(message: String) {
    println("${Thread.currentThread().name}@${LocalTime.now()} --> $message")
}