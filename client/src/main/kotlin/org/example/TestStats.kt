package org.example

data class TestStats(
    val testDuration: Long,
    val minTime: Long,
    val maxTime: Long,
    val averageTime: Long,
) {
    fun format(name: String): String {
        return "Test#$name duration: $testDuration; min: $minTime; max: $maxTime; avg: $averageTime;"
    }
}